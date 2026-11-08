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

// Pointer interaction (v2)
const INFLUENCE = 2.5;
const PULL = 0.35;
const PLAY_OFFSET = INFLUENCE * PULL;

// Fluid spring toward scroll lane / grab (v2)
const SPRING_K = 16;
const SPRING_D = 4.5;

// Wide upright turns — slow yaw flips, gentle tip (late v2)
const MAX_TURN = 0.7; // rad/s yaw cap — keeps flips from whipping
const MAX_TIP = 1.4;
const STEER = 0.45;
const ORIENT_RATE = 5.0;
const BLEND_RATE = 4.0;
const FLIP_X = 0.45; // |dx| needed before committing a left/right flip

// v3 biomechanics seasoning
const STROUAL = 0.25;
const BANK_MAX = 0.45;
const TURN_K = 0.03;
const ESCAPE_SPEED = 10;
const ESCAPE_TIME = 0.9;

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
  const pointer = new THREE.Vector3();
  let pointerFresh = false;
  const dragPos = new THREE.Vector3();
  const dragOffset = new THREE.Vector3();
  let dragId: number | null = null;
  let dragBlend = 0;
  let modelReady = false;

  // ---- fluid spring + upright aim ----
  const vel = new THREE.Vector3();
  let excitement = 0;
  let greetCooldown = 0;
  let escapeTimer = 0;
  let flick = 0; // 1 → 0: idle barrel-roll on the main (nose) axis
  let idleTimer = 5; // seconds until the next idle flourish
  let fClip = 1 / 1.2;
  let yaw = -0.35;
  let tip = 0;
  let bank = 0;
  let prevYaw = yaw;

  const forward = new THREE.Vector3();
  const aimQuat = new THREE.Quaternion();
  const aimEuler = new THREE.Euler();
  const pathEuler = new THREE.Euler();
  const pathQuat = new THREE.Quaternion();
  const desiredQuat = new THREE.Quaternion();
  const qRoll = new THREE.Quaternion();
  const AXIS_X = new THREE.Vector3(1, 0, 0);

  const ease = (cur: number, target: number, rate: number, dtAim: number) =>
    cur + (target - cur) * (1 - Math.exp(-rate * dtAim));

  const deltaAngle = (from: number, to: number) => {
    let d = to - from;
    while (d > Math.PI) d -= Math.PI * 2;
    while (d < -Math.PI) d += Math.PI * 2;
    return d;
  };

  // Rate-limited upright aim: yaw on Y, tip on Z — belly stays down, turns take space
  const setAimUpright = (dx: number, dy: number, dtAim: number) => {
    // sticky facing: only flip when the aim clearly crosses sides
    const desiredYaw = dx < -FLIP_X ? Math.PI - 0.35 : dx > FLIP_X ? -0.35 : yaw;
    yaw += THREE.MathUtils.clamp(deltaAngle(yaw, desiredYaw), -MAX_TURN * dtAim, MAX_TURN * dtAim);
    const tipTarget = THREE.MathUtils.clamp(
      Math.atan2(dy, Math.max(0.35, Math.abs(dx))),
      -0.55,
      0.55,
    );
    tip += THREE.MathUtils.clamp(tipTarget - tip, -MAX_TIP * dtAim, MAX_TIP * dtAim);
    const side = Math.cos(yaw) >= 0 ? 1 : -1;
    aimEuler.set(0, yaw, tip * side);
    aimQuat.setFromEuler(aimEuler);
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
    const hx = HALF_LENGTH * scale * 1.05 + PLAY_OFFSET;
    const hy = 1.0 * scale + BOB * zoom + PLAY_OFFSET;
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

  addEventListener('pointermove', (e) => {
    toWorld(e.clientX, e.clientY, pointer);
    pointerFresh = true;
  }, { passive: true, signal });
  addEventListener('pointerout', (e) => {
    if (!e.relatedTarget) pointerFresh = false;
  }, { signal });

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
    const scrollVel = dt > 0 ? Math.abs(smooth - prevSmooth) / dt : 0;
    scrollSpeed += (Math.min(1, scrollVel * 1.33) - scrollSpeed) * (1 - Math.exp(-6 * dt));

    const p = smooth;
    const range = Math.max(0, halfWidth - HALF_LENGTH * scale - EDGE_MARGIN * zoom);
    const pathX = (p * 2 - 1) * range;
    const pathY = 1.5 - p * 3 + Math.sin(steppedTime * 1.1) * BOB * zoom;

    // cursor play: drift the home target toward the pointer
    let targetX = pathX;
    let targetY = pathY;
    let nearPointer = 0;
    if (pointerFresh && dragBlend < 0.5 && escapeTimer <= 0) {
      const dx = pointer.x - pathX;
      const dy = pointer.y - pathY;
      const t = THREE.MathUtils.clamp(1 - Math.hypot(dx, dy) / INFLUENCE, 0, 1);
      nearPointer = t * t;
      targetX += dx * nearPointer * PULL;
      targetY += dy * nearPointer * PULL;
    }

    dragBlend = ease(dragBlend, dragId !== null ? 1 : 0, BLEND_RATE, dt);
    const tx = THREE.MathUtils.lerp(targetX, dragPos.x - dragOffset.x, dragBlend);
    const ty = THREE.MathUtils.lerp(targetY, dragPos.y - dragOffset.y, dragBlend);

    // ---- fluid spring (mass + water) ----
    // during escape, spring is weaker so the startle impulse can carry
    const springScale = escapeTimer > 0 ? 0.35 : 1;
    vel.x += (tx - dolphin.position.x) * SPRING_K * springScale * dt;
    vel.y += (ty - dolphin.position.y) * SPRING_K * springScale * dt;
    vel.multiplyScalar(Math.exp(-SPRING_D * dt));

    // ---- upright orientation: path pose by default; pointer/drag aims the nose ----
    prevYaw = yaw;
    pathEuler.set(0, -0.4 + 0.3 * Math.sin(p * Math.PI * 2), -0.08 - 0.15 * p);
    pathQuat.setFromEuler(pathEuler);

    const lookBlend = Math.max(dragBlend, nearPointer);
    let aimBlendTarget = 0;
    if (lookBlend > 0.05) {
      const lookX = (dragId !== null ? dragPos.x : pointer.x) - dolphin.position.x;
      const lookY = (dragId !== null ? dragPos.y : pointer.y) - dolphin.position.y;
      if (lookX * lookX + lookY * lookY > 0.0025) {
        setAimUpright(lookX, lookY, dt);
        aimBlendTarget = lookBlend;
      }
    } else {
      // stay on the scroll pose — tip a little with vertical motion, never flip for swim vel
      yaw += THREE.MathUtils.clamp(deltaAngle(yaw, pathEuler.y), -MAX_TURN * 0.45 * dt, MAX_TURN * 0.45 * dt);
      const tipTarget = pathEuler.z + THREE.MathUtils.clamp(vel.y * 0.1, -0.35, 0.35);
      tip += THREE.MathUtils.clamp(tipTarget - tip, -MAX_TIP * dt, MAX_TIP * dt);
      const side = Math.cos(yaw) >= 0 ? 1 : -1;
      aimEuler.set(0, yaw, tip * side);
      aimQuat.setFromEuler(aimEuler);
      aimBlendTarget = 0.25;
    }

    const turnRate = deltaAngle(yaw, prevYaw) / Math.max(dt, 1e-4);
    const swim = Math.hypot(vel.x, vel.y);
    const bankCap = dragBlend > 0.4 ? BANK_MAX : BANK_MAX * 0.5;
    const bankTarget = THREE.MathUtils.clamp(turnRate * Math.max(swim, 1) * TURN_K, -bankCap, bankCap);
    bank = ease(bank, bankTarget, 3.5, dt);

    const side = Math.cos(yaw) >= 0 ? 1 : -1;
    aimEuler.set(0, yaw, tip * side);
    aimQuat.setFromEuler(aimEuler);
    desiredQuat.copy(pathQuat).slerp(aimQuat, Math.max(aimBlendTarget, lookBlend));
    // bank + idle barrel-roll around the main (nose) axis
    const roll = bank + (flick > 0 ? (1 - flick) * Math.PI * 2 : 0);
    qRoll.setFromAxisAngle(AXIS_X, roll);
    desiredQuat.multiply(qRoll);
    dolphin.quaternion.slerp(desiredQuat, 1 - Math.exp(-ORIENT_RATE * dt));

    // Nose-steer only while dragged/escaping — on the scroll lane it was
    // swimming down the tipped path heading and fighting the spring.
    if ((dragBlend > 0.2 || escapeTimer > 0) && swim > 0.05) {
      forward.set(1, 0, 0).applyQuaternion(dolphin.quaternion);
      vel.x += (forward.x * swim - vel.x) * STEER;
      vel.y += (forward.y * swim - vel.y) * STEER;
    }
    dolphin.position.x += vel.x * dt;
    dolphin.position.y += vel.y * dt;
    dolphin.position.z = 0;
    dolphin.scale.setScalar(scale);

    // ---- aliveness ----
    escapeTimer = Math.max(0, escapeTimer - dt);
    excitement = Math.max(
      excitement * (1 - dt * 1.2),
      Math.min(1, swim * 0.35),
      nearPointer,
      escapeTimer > 0 ? 1 : 0,
    );
    greetCooldown -= dt;
    idleTimer -= dt;
    if (dragId === null && lookBlend < 0.15 && idleTimer <= 0 && swim < 0.55 && escapeTimer <= 0) {
      idleTimer = 5 + Math.random() * 6;
      flick = 1;
    }
    flick = Math.max(0, flick - dt * 1.4);
    if (nearPointer > 0.5 && greetCooldown <= 0 && dragId === null) {
      greetCooldown = 12;
      excitement = 1;
      chirp(1);
    }

    // Strouhal tailbeat from swim speed, with a little scroll/excitement boost
    const A = 0.6 * scale;
    const swimSpeed = Math.max(swim, scrollSpeed * 4);
    const fBase = (STROUAL * Math.max(2, swimSpeed)) / (2 * Math.max(0.1, A));
    const f = fBase * (1 + excitement * 0.5) * (dragId !== null ? 1.35 : 1);
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
