import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';

const MODEL_URL = '/dolphin.glb';
const CAMERA = { fov: 40, distance: 11, near: 4, far: 30 } as const;
const ANIM_FPS = 30;

// Model is ~6 units long; layout numbers below are world units at load-time zoom.
const HALF_LENGTH = 3;
const EDGE_MARGIN = 0.3;
const BOB = 0.06;
const MAX_WIDTH = 0.8;

// Grab hit pad only — no hover play; interactive solely while dragging
const HIT_PAD = 0.35;

// Fluid spring toward scroll lane / grab (v2)
const SPRING_K = 18;
const SPRING_D = 4.2;
const DRAG_SPRING_K = 52; // snappy follow while held
const DRAG_SPRING_D = 2.4;

// 3D aim: nose follows direction, world-up kept (no free belly roll)
const STEER = 0.45;
const ORIENT_RATE = 6.0;
const BLEND_RATE = 12.0;
const AIM_BLEND_RATE = 5.0;

// Real 3D turns: nose aims into the dive, body swims along that heading through XYZ
const TURN_INTO = 1.35;
const SWIM_PUSH = 0.55; // blend velocity onto the nose while turning
const HOME_Z_K = 5.5; // soft pull back to the lane plane after the dive
const HOME_Z_D = 3.2;
const Z_LIMIT = 6.0;
const ARC_TIME = 0.85;
const FLIP_STICKY = 0.9;
const FLIP_COOLDOWN = 1.1;

// Biomechanics
const STROUAL = 0.22;
const BANK_MAX = 0.5; // bank into 3D turns; not a flat spin
const TURN_K = 0.045;
const ESCAPE_SPEED = 7.5;
const ESCAPE_TIME = 0.75;
const SCROLL_TAIL = 0.7; // was *4 — scroll must not thrash the tailbeat
const DRAG_TAIL_CAP = 0.9; // drag spring is fast — never let that thrash the mixer

// Light intensities were tuned before three switched to physically based units (r155).
const LIGHT_SCALE = Number(THREE.REVISION) >= 155 ? Math.PI : 1;

const vertexShader = `
  varying vec2 vUv;
  void main() {
    vUv = uv;
    gl_Position = vec4(position.xy, 0.0, 1.0);
  }
`;

const fragmentShader = `
  uniform sampler2D tex;
  uniform sampler2D depthTex;
  uniform vec2 texel;
  uniform float uNear;
  uniform float uFar;
  varying vec2 vUv;

  float linearDepth(float d) {
    float z = d * 2.0 - 1.0;
    return 2.0 * uNear * uFar / (uFar + uNear - z * (uFar - uNear));
  }

  float neighbourAlpha(vec2 offset) {
    return texture2D(tex, vUv + offset * texel).a;
  }

  // 1e3 for empty neighbours so they never count as "in front"
  float neighbourDepth(vec2 offset) {
    vec2 uv = vUv + offset * texel;
    if (texture2D(tex, uv).a < 0.5) return 1e3;
    return linearDepth(texture2D(depthTex, uv).x);
  }

  void main() {
    vec4 c = texture2D(tex, vUv);
    if (c.a < 0.5) discard;

    float empty =
      step(neighbourAlpha(vec2(0.0, -1.0)), 0.5) +
      step(neighbourAlpha(vec2(0.0, 1.0)), 0.5) +
      step(neighbourAlpha(vec2(-1.0, 0.0)), 0.5) +
      step(neighbourAlpha(vec2(1.0, 0.0)), 0.5);
    float silhouette = step(1.0, empty);

    float depth = linearDepth(texture2D(depthTex, vUv).x);
    float gap = 0.7;
    float votes =
      step(gap, depth - neighbourDepth(vec2(0.0, -1.0))) +
      step(gap, depth - neighbourDepth(vec2(0.0, 1.0))) +
      step(gap, depth - neighbourDepth(vec2(-1.0, 0.0))) +
      step(gap, depth - neighbourDepth(vec2(1.0, 0.0))) +
      step(gap, depth - neighbourDepth(vec2(-1.0, -1.0))) +
      step(gap, depth - neighbourDepth(vec2(1.0, -1.0))) +
      step(gap, depth - neighbourDepth(vec2(-1.0, 1.0))) +
      step(gap, depth - neighbourDepth(vec2(1.0, 1.0)));
    float crease = step(4.0, votes);
    float outline = max(silhouette, crease);

    vec3 dark = vec3(0.0, 0.4, 0.4);
    vec3 mid = vec3(0.0, 0.7, 0.7);
    vec3 light = vec3(0.0, 1.0, 1.0);
    vec3 base = c.g < 0.55 ? dark : c.g < 0.85 ? mid : light;
    gl_FragColor = vec4(outline > 0.5 ? dark : base, 1.0);
  }
`;

const start = (canvas: HTMLCanvasElement, hint: HTMLElement | null) => {
  const abort = new AbortController();
  const { signal } = abort;

  const renderer = new THREE.WebGLRenderer({ canvas, antialias: false, alpha: true });
  renderer.setClearColor(0x000000, 0);

  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(CAMERA.fov, 1, CAMERA.near, CAMERA.far);
  camera.position.z = CAMERA.distance;

  const sun = new THREE.DirectionalLight(0xffffff, 0.8 * LIGHT_SCALE);
  sun.position.set(-3, 5, 6);
  scene.add(new THREE.AmbientLight(0xffffff, 0.35 * LIGHT_SCALE), sun);

  const material = new THREE.MeshLambertMaterial({ color: 0x00ffff });
  // pre-r150 only
  if ('skinning' in material) (material as { skinning?: boolean }).skinning = true;

  const depthTexture = new THREE.DepthTexture(2, 2, THREE.UnsignedIntType);
  const target = new THREE.WebGLRenderTarget(2, 2, {
    minFilter: THREE.NearestFilter,
    magFilter: THREE.NearestFilter,
    format: THREE.RGBAFormat,
    depthTexture,
  });

  const texel = new THREE.Vector2();
  const post = new THREE.ShaderMaterial({
    uniforms: {
      tex: { value: target.texture },
      depthTex: { value: depthTexture },
      texel: { value: texel },
      uNear: { value: CAMERA.near },
      uFar: { value: CAMERA.far },
    },
    vertexShader,
    fragmentShader,
    depthTest: false,
    depthWrite: false,
  });
  const quad = new THREE.Mesh(new THREE.PlaneGeometry(2, 2), post);
  quad.frustumCulled = false;
  const postScene = new THREE.Scene().add(quad);
  const postCamera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1);

  const dolphin = new THREE.Group();
  scene.add(dolphin);
  let mixer: THREE.AnimationMixer | undefined;

  // ---- pointer interaction ----
  const hitEl = document.getElementById('dolphin-hit');
  const raycaster = new THREE.Raycaster();
  const zPlane = new THREE.Plane(new THREE.Vector3(0, 0, 1), 0);
  const ndc = new THREE.Vector2();
  const dragPos = new THREE.Vector3();
  const dragOffset = new THREE.Vector3();
  let dragId: number | null = null;
  let dragBlend = 0;
  let modelReady = false;

  // ---- fluid spring + 3D aim (nose along dir, belly via world-up) ----
  const vel = new THREE.Vector3();
  let excitement = 0;
  let escapeTimer = 0;
  let flick = 0; // 1 → 0: idle barrel-roll on the main (nose) axis
  let idleTimer = 5; // seconds until the next idle flourish
  let fClip = 1 / 1.2;
  let bank = 0;
  let prevAng = -0.35;
  let faceSign = 1; // sticky left/right facing — no jitter flips
  let flipCool = 0;
  let aimBlend = 0.25;
  let turnArc = 0; // 1 → 0: dive into screen while reversing facing

  const forward = new THREE.Vector3();
  const aimDir = new THREE.Vector3(1, 0, 0);
  const right = new THREE.Vector3();
  const up = new THREE.Vector3();
  const worldUp = new THREE.Vector3(0, 1, 0);
  const aimQuat = new THREE.Quaternion();
  const pathEuler = new THREE.Euler();
  const pathQuat = new THREE.Quaternion();
  const desiredQuat = new THREE.Quaternion();
  const qRoll = new THREE.Quaternion();
  const qBank = new THREE.Quaternion();
  const basis = new THREE.Matrix4();
  const AXIS_X = new THREE.Vector3(1, 0, 0);

  const ease = (cur: number, target: number, rate: number, dtAim: number) =>
    cur + (target - cur) * (1 - Math.exp(-rate * dtAim));

  const deltaAngle = (from: number, to: number) => {
    let d = to - from;
    while (d > Math.PI) d -= Math.PI * 2;
    while (d < -Math.PI) d += Math.PI * 2;
    return d;
  };

  // Nose along full 3D dir; up from world-up + bank — turns can point into the screen
  const setAimFromDir = (dx: number, dy: number, dz: number, bankRad: number) => {
    const len = Math.hypot(dx, dy, dz);
    if (len < 1e-4) return;
    forward.set(dx / len, dy / len, dz / len);
    right.crossVectors(forward, worldUp);
    if (right.lengthSq() < 1e-8) {
      // nearly vertical — pick a stable side axis
      right.set(forward.y > 0 ? 1 : -1, 0, 0);
      right.crossVectors(forward, right);
    }
    right.normalize();
    up.crossVectors(right, forward).normalize();
    right.crossVectors(forward, up).normalize();
    qBank.setFromAxisAngle(forward, bankRad);
    up.applyQuaternion(qBank);
    right.applyQuaternion(qBank);
    basis.makeBasis(forward, up, right);
    aimQuat.setFromRotationMatrix(basis);
  };

  // ---- chirp: synthesized, no audio files ----
  let audio: AudioContext | null = null;
  let audioUnlocked = false;
  const chirp = (n = 1) => {
    if (!audioUnlocked || audio === null) return;
    try {
      if (audio.state === 'suspended') void audio.resume();
      const t0 = audio.currentTime;
      for (let i = 0; i < n; i++) {
        const t = t0 + i * 0.15;
        const osc = audio.createOscillator();
        const gain = audio.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(900, t);
        osc.frequency.exponentialRampToValueAtTime(2600, t + 0.09);
        gain.gain.setValueAtTime(0.0001, t);
        gain.gain.linearRampToValueAtTime(0.12, t + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.12);
        osc.connect(gain).connect(audio.destination);
        osc.start(t);
        osc.stop(t + 0.14);
      }
    } catch {
      /* audio is a bonus; never fatal */
    }
  };

  const toWorld = (x: number, y: number, out: THREE.Vector3) => {
    ndc.set(
      (x / Math.max(1, innerWidth)) * 2 - 1,
      -((y / Math.max(1, innerHeight)) * 2 - 1),
    );
    raycaster.setFromCamera(ndc, camera);
    raycaster.ray.intersectPlane(zPlane, out);
    return out;
  };

  const placeHit = () => {
    if (!(hitEl instanceof HTMLElement) || !modelReady) return;
    const w = Math.max(1, innerWidth);
    const h = Math.max(1, innerHeight);
    const halfH = halfWidth / camera.aspect;
    const hx = HALF_LENGTH * scale * 1.05 + HIT_PAD;
    const hy = 1.0 * scale + BOB * zoom + HIT_PAD;
    const pxX = w / (2 * halfWidth);
    const pxY = h / (2 * halfH);
    hitEl.style.transform = `translate(${w / 2 + (dolphin.position.x - hx) * pxX}px, ${h / 2 - (dolphin.position.y + hy) * pxY}px)`;
    hitEl.style.width = `${2 * hx * pxX}px`;
    hitEl.style.height = `${2 * hy * pxY}px`;
  };

  new GLTFLoader()
    .loadAsync(MODEL_URL)
    .then(({ scene: model, animations }) => {
      if (signal.aborted) return;
      let hasColors = false;
      model.traverse((obj) => {
        if (!(obj instanceof THREE.Mesh)) return;
        obj.material = material;
        obj.frustumCulled = false;
        hasColors ||= obj.geometry.hasAttribute('color');
      });
      material.vertexColors = hasColors;
      material.needsUpdate = true;

      dolphin.add(model);
      modelReady = true;
      if (hitEl instanceof HTMLElement) hitEl.style.visibility = 'visible';
      if (animations[0]) {
        mixer = new THREE.AnimationMixer(model);
        mixer.clipAction(animations[0]).play();
        if (animations[0].duration > 0) fClip = 1 / animations[0].duration;
      }
    })
    .catch((err) => console.error('GLB load failed', err));

  if (hitEl instanceof HTMLElement) {
    const unlockAudio = () => {
      if (audioUnlocked) return;
      audioUnlocked = true;
      try {
        audio ??= new AudioContext();
      } catch {
        audio = null;
      }
    };
    const endDrag = (e: PointerEvent) => {
      if (e.pointerId !== dragId) return;
      dragId = null;
      hitEl.classList.remove('dragging');
      // startle escape: shove away from the grab point, then spring home
      const awayX = dolphin.position.x - dragPos.x;
      const awayY = dolphin.position.y - dragPos.y;
      const len = Math.hypot(awayX, awayY) || 1;
      vel.x += (awayX / len) * ESCAPE_SPEED;
      vel.y += (awayY / len) * ESCAPE_SPEED;
      escapeTimer = ESCAPE_TIME;
      excitement = 1;
      chirp(2);
    };
    hitEl.addEventListener('pointerdown', (e) => {
      if (dragId !== null || !modelReady) return;
      if (e.pointerType === 'mouse' && e.button !== 0) return;
      unlockAudio();
      toWorld(e.clientX, e.clientY, dragPos);
      dragOffset.set(dragPos.x - dolphin.position.x, dragPos.y - dolphin.position.y, 0);
      dragId = e.pointerId;
      hitEl.setPointerCapture(e.pointerId);
      hitEl.classList.add('dragging');
      escapeTimer = 0;
      excitement = 1;
      chirp(1);
      e.preventDefault();
    }, { signal });
    hitEl.addEventListener('pointermove', (e) => {
      if (e.pointerId !== dragId) return;
      toWorld(e.clientX, e.clientY, dragPos);
      const halfH = halfWidth / camera.aspect;
      dragPos.x = THREE.MathUtils.clamp(dragPos.x, -(halfWidth - 0.5), halfWidth - 0.5);
      dragPos.y = THREE.MathUtils.clamp(dragPos.y, -(halfH - 0.5), halfH - 0.5);
    }, { passive: true, signal });
    hitEl.addEventListener('pointerup', endDrag, { signal });
    hitEl.addEventListener('pointercancel', endDrag, { signal });
    hitEl.addEventListener('lostpointercapture', endDrag, { signal });
  }

  const baseDpr = devicePixelRatio || 1;
  let zoom = 1;
  let scale = 0.8;
  let halfWidth = 7;
  let scroll = 0;

  const readScroll = () => {
    const max = document.documentElement.scrollHeight - innerHeight;
    scroll = max > 0 ? THREE.MathUtils.clamp(scrollY / max, 0, 1) : 0;
    if (hint) hint.style.opacity = scrollY > 40 ? '0' : '0.6';
  };

  const resize = () => {
    const w = Math.max(1, innerWidth);
    const h = Math.max(1, innerHeight);
    const aspect = w / h;
    zoom = (devicePixelRatio || 1) / baseDpr;

    renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
    renderer.setSize(w, h, false);

    const texelPx = w * zoom < 700 ? 3 : 4;
    const bufferW = Math.max(2, Math.round(w / texelPx));
    const bufferH = Math.max(2, Math.round(h / texelPx));
    target.setSize(bufferW, bufferH);
    depthTexture.image.width = bufferW;
    depthTexture.image.height = bufferH;
    texel.set(1 / bufferW, 1 / bufferH);

    camera.aspect = aspect;
    camera.updateProjectionMatrix();

    halfWidth = Math.tan(THREE.MathUtils.degToRad(CAMERA.fov / 2)) * CAMERA.distance * aspect;
    const base = THREE.MathUtils.clamp(aspect * 0.55, 0.35, 0.85);
    scale = Math.min(base * zoom, (halfWidth * MAX_WIDTH) / HALF_LENGTH);

    readScroll();
  };

  const watchDpr = () => {
    matchMedia(`(resolution: ${devicePixelRatio}dppx)`).addEventListener(
      'change',
      () => {
        resize();
        watchDpr();
      },
      { once: true, signal },
    );
  };

  addEventListener('resize', resize, { signal });
  addEventListener('scroll', readScroll, { passive: true, signal });
  resize();
  watchDpr();

  // Snap onto the scroll-lane start pose (p=0 → left, y=1.5) — never rise from (0,0,0)
  {
    const range0 = Math.max(0, halfWidth - HALF_LENGTH * scale - EDGE_MARGIN * zoom);
    dolphin.position.set(-range0, 1.5, 0);
    dolphin.scale.setScalar(scale);
  }

  const clock = new THREE.Clock();
  let smooth = scroll;
  let scrollSpeed = 0;
  let animTime = 0;
  let steppedTime = 0;
  let raf = 0;

  const frame = () => {
    raf = requestAnimationFrame(frame);
    const dt = Math.min(clock.getDelta(), 0.05);

    animTime += dt;
    const prevStep = steppedTime;
    steppedTime = Math.floor(animTime * ANIM_FPS) / ANIM_FPS;

    // ---- scroll lane (v1/v2 choreography) ----
    const prevSmooth = smooth;
    smooth += (scroll - smooth) * Math.min(1, dt * 5);
    // signed: <0 = scroll up (lane rises), >0 = scroll down — drives nose tip
    const signedScrollVel = dt > 0 ? (smooth - prevSmooth) / dt : 0;
    scrollSpeed += (Math.min(1, Math.abs(signedScrollVel) * 1.33) - scrollSpeed) * (1 - Math.exp(-6 * dt));

    const p = smooth;
    const range = Math.max(0, halfWidth - HALF_LENGTH * scale - EDGE_MARGIN * zoom);
    const pathX = (p * 2 - 1) * range;
    const pathY = 1.5 - p * 3 + Math.sin(steppedTime * 1.1) * BOB * zoom;

    // Home is the scroll lane only — no hover pull / glance / greet
    dragBlend = ease(dragBlend, dragId !== null ? 1 : 0, BLEND_RATE, dt);
    const tx = THREE.MathUtils.lerp(pathX, dragPos.x - dragOffset.x, dragBlend);
    const ty = THREE.MathUtils.lerp(pathY, dragPos.y - dragOffset.y, dragBlend);

    // ---- fluid spring (mass + water); stiffer while dragged ----
    const springScale = escapeTimer > 0 ? 0.35 : 1;
    const k = THREE.MathUtils.lerp(SPRING_K, DRAG_SPRING_K, dragBlend) * springScale;
    const damp = THREE.MathUtils.lerp(SPRING_D, DRAG_SPRING_D, dragBlend);
    vel.x += (tx - dolphin.position.x) * k * dt;
    vel.y += (ty - dolphin.position.y) * k * dt;
    // XY damp only — Z comes from swimming along the nose
    const vzKeep = vel.z;
    vel.z = 0;
    vel.multiplyScalar(Math.exp(-damp * dt));
    vel.z = vzKeep;

    // ---- 3D orientation: nose along swim / look dir, turns dive into −Z ----
    pathEuler.set(0, -0.35, -0.06 - 0.1 * p);
    pathQuat.setFromEuler(pathEuler);

    const swim = Math.hypot(vel.x, vel.y);
    let rawX = aimDir.x;
    let rawY = aimDir.y;
    let wantAim = 0.2;

    if (dragBlend > 0.25) {
      // Drag only: face travel (not cursor−body — that overshoots and flip-flops)
      if (swim > 0.18) {
        rawX = vel.x;
        rawY = vel.y;
      } else {
        rawX = tx - dolphin.position.x;
        rawY = ty - dolphin.position.y;
      }
      wantAim = Math.max(0.7, dragBlend);
    } else if (swim > 0.12) {
      rawX = vel.x;
      rawY = vel.y;
      if (signedScrollVel < -0.12) rawY += 1.1 * Math.abs(signedScrollVel);
      wantAim = THREE.MathUtils.clamp(0.35 + swim * 0.35, 0.35, 0.95);
    } else {
      rawX = Math.max(0.4, range * 0.15);
      rawY = signedScrollVel < -0.08 ? 0.45 : -0.15 - 0.1 * p;
      wantAim = 0.3;
    }

    // Sticky facing: one committed reverse + into-screen arc, then cooldown
    flipCool = Math.max(0, flipCool - dt);
    turnArc = Math.max(0, turnArc - dt / ARC_TIME);
    if (
      Math.abs(rawX) > FLIP_STICKY &&
      Math.sign(rawX) !== faceSign &&
      turnArc <= 0.02 &&
      flipCool <= 0
    ) {
      faceSign = Math.sign(rawX) || faceSign;
      turnArc = 1;
      flipCool = FLIP_COOLDOWN;
    }

    // Keep horizontal facing stable; tip from rawY; into −Z only during a committed arc
    const dirX = faceSign * Math.max(0.35, Math.abs(rawX));
    const dirY = rawY;
    const ang = Math.atan2(dirY, dirX);
    const turnRate = THREE.MathUtils.clamp(
      deltaAngle(prevAng, ang) / Math.max(dt, 1e-4),
      -2.5,
      2.5,
    );
    prevAng = ang;

    // Peak into-screen at mid-arc so the turn reads as going away, then coming back
    const arcWave = turnArc > 0 ? Math.sin(turnArc * Math.PI) : 0;
    const turnAmount = THREE.MathUtils.clamp(
      Math.max(arcWave, Math.abs(turnRate) / 3.5 * (1 - dragBlend * 0.7)),
      0,
      1,
    );

    const into = turnAmount * TURN_INTO;
    aimDir.set(dirX, dirY, -into);
    if (aimDir.lengthSq() > 1e-6) aimDir.normalize();
    else aimDir.set(faceSign, 0, 0);

    const bankTarget = THREE.MathUtils.clamp(
      turnRate * Math.max(swim, 0.6) * TURN_K,
      -BANK_MAX,
      BANK_MAX,
    );
    bank = ease(bank, bankTarget, 5, dt);

    setAimFromDir(aimDir.x, aimDir.y, aimDir.z, bank);
    aimBlend = ease(aimBlend, wantAim, AIM_BLEND_RATE, dt);
    desiredQuat.copy(pathQuat).slerp(aimQuat, aimBlend);
    if (flick > 0) {
      qRoll.setFromAxisAngle(AXIS_X, (1 - flick) * Math.PI * 2);
      desiredQuat.multiply(qRoll);
    }
    const orientRate = dragBlend > 0.3 ? ORIENT_RATE * 1.35 : ORIENT_RATE;
    dolphin.quaternion.slerp(desiredQuat, 1 - Math.exp(-orientRate * dt));

    // Swim in 3D along the nose — turns that point into −Z actually carry the body there
    forward.set(1, 0, 0).applyQuaternion(dolphin.quaternion);
    const push = Math.max(swim, turnAmount * 7);
    if (turnAmount > 0.04 || escapeTimer > 0) {
      const blend = escapeTimer > 0 ? STEER : SWIM_PUSH * Math.max(turnAmount, 0.35);
      vel.x += (forward.x * push - vel.x) * blend;
      vel.y += (forward.y * push - vel.y) * blend;
      vel.z += (forward.z * push - vel.z) * blend;
    }
    // Soft home to the lane plane so a dive doesn't leave him stuck in the distance
    vel.z += (0 - dolphin.position.z) * HOME_Z_K * (1 - turnAmount * 0.85) * dt;
    vel.z *= Math.exp(-HOME_Z_D * dt);

    dolphin.position.x += vel.x * dt;
    dolphin.position.y += vel.y * dt;
    dolphin.position.z = THREE.MathUtils.clamp(dolphin.position.z + vel.z * dt, -Z_LIMIT, 0.5);
    dolphin.scale.setScalar(scale);

    // ---- aliveness ----
    escapeTimer = Math.max(0, escapeTimer - dt);
    excitement = Math.max(
      excitement * (1 - dt * 1.2),
      Math.min(1, swim * 0.35),
      escapeTimer > 0 ? 1 : 0,
    );
    idleTimer -= dt;
    if (dragId === null && dragBlend < 0.15 && idleTimer <= 0 && swim < 0.55 && escapeTimer <= 0) {
      idleTimer = 5 + Math.random() * 6;
      flick = 1;
    }
    flick = Math.max(0, flick - dt * 1.4);

    // Strouhal tailbeat — calm while dragged (spring speed must not drive the mixer)
    const A = 0.6 * scale;
    let swimSpeed = Math.max(swim, scrollSpeed * SCROLL_TAIL);
    if (dragBlend > 0.15 || dragId !== null) {
      swimSpeed = Math.min(swimSpeed, DRAG_TAIL_CAP);
    }
    const fBase = (STROUAL * Math.max(0.75, swimSpeed)) / (2 * Math.max(0.1, A));
    const fExcite = dragBlend > 0.15 ? 0 : excitement * 0.2;
    const f = Math.min(fBase * (1 + fExcite), dragBlend > 0.15 ? fClip * 1.05 : fClip * 2.2);
    mixer?.update((steppedTime - prevStep) * (f / fClip));

    placeHit();

    renderer.setRenderTarget(target);
    renderer.render(scene, camera);
    renderer.setRenderTarget(null);
    renderer.render(postScene, postCamera);
  };
  frame();

  return () => {
    abort.abort();
    cancelAnimationFrame(raf);
    mixer?.stopAllAction();
    quad.geometry.dispose();
    post.dispose();
    material.dispose();
    depthTexture.dispose();
    target.dispose();
    renderer.dispose();
  };
};

const canvas = document.getElementById('gl');
const hint = document.getElementById('hint');
const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)');

if (canvas instanceof HTMLCanvasElement) {
  const remove = () => {
    canvas.remove();
    hint?.remove();
    document.getElementById('dolphin-hit')?.remove();
  };

  if (reduceMotion.matches) {
    remove();
  } else {
    let stop: (() => void) | undefined;
    const boot = () => {
      if (reduceMotion.matches) return;
      stop = start(canvas, hint);
    };
    if ('requestIdleCallback' in window) requestIdleCallback(boot, { timeout: 1500 });
    else setTimeout(boot, 1);

    reduceMotion.addEventListener('change', (e) => {
      if (!e.matches) return;
      stop?.();
      remove();
    });
  }
}
