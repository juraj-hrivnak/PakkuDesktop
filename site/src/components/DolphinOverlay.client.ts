import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';

const MODEL_URL = '/dolphin.glb';
const CAMERA = { fov: 40, distance: 11, near: 4, far: 30 } as const;
const ANIM_FPS = 30;

// Model is ~6 units long. Layout numbers below are world units at load-time zoom.
const HALF_LENGTH = 3;
const EDGE_MARGIN = 0.3;
const BOB = 0.06;
const MAX_WIDTH = 0.8;

// Extra padding on the invisible drag hit box. Interaction is drag-only.
const HIT_PAD = 0.35;

// Spring toward the scroll lane / grab target.
const SPRING_K = 18;
const SPRING_D = 6.5; // damping ratio ~0.77; slight overshoot, no looping bounce

// Drag chase: forward swim only — limited cruise, turn rate, and accel.
const DRAG_CRUISE = 5.0; // max chase speed (world units/s)
const DRAG_ARRIVE = 2.2; // speed ≈ dist * this, capped by DRAG_CRUISE
const DRAG_TURN_RATE = 3.0; // rad/s cap on swim heading changes
const DRAG_SPEED_RATE = 2.5; // how fast chase speed eases in/out
const DRAG_FLIP_DIST = 1.2; // how far behind the cursor must be to flip facing
const DRAG_PITCH_GAIN = 0.45; // pitch scale while chasing
const DRAG_PITCH_RATE = 3.5; // slower pitch while chasing
const MAX_SPEED = 16;
const BLEND_RATE = 12;

// Orientation: yaw (through −Z on reverse), pitch, bank.
const TURN_TIME = 1.15; // full facing reverse duration (quintic ease)
const PITCH_RATE = 7;
const PITCH_MAX = 0.95;
const REST_TILT = 0.35; // idle nose angled a bit toward the camera
const BANK_MAX = 0.95; // peak bank into a turn

// Facing reverse needs clear intent + cooldown so it doesn't chatter.
const FLIP_THRESHOLD = 0.5; // |unit dir x| before we consider flipping
const FLIP_HOLD = 0.2; // how long that intent must hold
const FLIP_COOLDOWN = 0.6;

// Scroll-driven facing: down/top → right, up/bottom → left.
const EDGE_P = 0.04; // near 0 or 1 scroll progress
const SCROLL_FACE_V = 0.04; // min |scroll speed| to count as a direction

// U-turn arc in depth (nose into −Z). Shallow so it doesn't read as zoom.
const TURN_RADIUS_K = 0.18; // radius vs half body length
const TURN_DIP = 0.18; // downward sink mid-turn
const TURN_PITCH = 0.45; // nose down then up through the turn
const SETTLE_TIME = 0.9; // glide back to the lane plane

const ESCAPE_SPEED = 3.5; // release shove
const ESCAPE_TIME = 0.75;

// Pre-r155 lights were authored in non-PBR units.
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

const { clamp, lerp, smoothstep } = THREE.MathUtils;

// Frame-rate independent exponential ease
const ease = (cur: number, target: number, rate: number, dt: number) =>
    cur + (target - cur) * (1 - Math.exp(-rate * dt));

const wrapPi = (a: number) => {
  while (a > Math.PI) a -= Math.PI * 2;
  while (a < -Math.PI) a += Math.PI * 2;
  return a;
};

// Scratch for setUnit (avoids per-frame allocation)
const dirTmp = { x: 1, y: 0 };
const setUnit = (x: number, y: number, fx: number, fy: number) => {
  const l = Math.hypot(x, y);
  if (l > 1e-4) {
    dirTmp.x = x / l;
    dirTmp.y = y / l;
  } else {
    dirTmp.x = fx;
    dirTmp.y = fy;
  }
};

const start = (canvas: HTMLCanvasElement, hint: HTMLElement | null) => {
  const abort = new AbortController();
  const { signal } = abort;

  const renderer = new THREE.WebGLRenderer({ canvas, antialias: false, alpha: true });
  renderer.setClearColor(0x000000, 0);

  let contextLost = false;
  canvas.addEventListener('webglcontextlost', (e) => {
    e.preventDefault();
    contextLost = true;
  }, { signal });
  canvas.addEventListener('webglcontextrestored', () => {
    contextLost = false;
  }, { signal });

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
  let model: THREE.Object3D | undefined;

  // --- LAYOUT ---
  const baseDpr = devicePixelRatio || 1;
  let zoom = 1;
  let scale = 0.8;
  let halfWidth = 7;
  let scroll = 0;

  // --- POINTER ---
  const hitEl = document.getElementById('dolphin-hit');
  const hit = hitEl instanceof HTMLElement ? hitEl : null;
  if (hit) {
    // touch-drag must not become a scroll gesture
    Object.assign(hit.style, {
      position: 'fixed',
      left: '0',
      top: '0',
      right: 'auto',
      bottom: 'auto',
      touchAction: 'none',
    });
  }
  const raycaster = new THREE.Raycaster();
  const zPlane = new THREE.Plane(new THREE.Vector3(0, 0, 1), 0);
  const ndc = new THREE.Vector2();
  const dragPos = new THREE.Vector3();
  const dragOffset = new THREE.Vector2();
  let dragId: number | null = null;
  let dragBlend = 0;
  let modelReady = false;

  // --- MOTION STATE ---
  // pos/vel live on the home plane (on-screen). Depth is applied later in applyPose
  // so the hit box and drag mapping stay put while the body arcs.
  const pos = new THREE.Vector2();
  const vel = new THREE.Vector2();
  let depthK = 1; // always 1; hit sizing still divides by it
  let swimS = 0; // smoothed swim speed (spring speed is too noisy to steer by)
  let scrollUp = 0; // tips nose up
  let escapeEase = 0;
  let escapeTimer = 0;
  let flick = 0; // 1 → 0: idle barrel-roll on nose axis
  let idleTimer = 5;

  // orientation
  let faceSign = 1; // +1 right, −1 left
  let flipTimer = 0;
  let turnFrom = 0; // 0 = right, 1 = left
  let turnTo = 0;
  let turnT = 1; // 0 → 1 over TURN_TIME; 1 = idle
  let turnP = 0;
  let tiltS = REST_TILT;
  let settleT = 1; // 0 at turn start, 1 after glide back
  let flipCool = 0;
  let yaw = -REST_TILT; // 0 = right, π = left; in-between = into screen
  let yawVel = 0;
  let pitch = -0.15;
  let bank = 0;

  const forward = new THREE.Vector3();
  const right = new THREE.Vector3();
  const up = new THREE.Vector3();
  const worldUp = new THREE.Vector3(0, 1, 0);
  const aimQuat = new THREE.Quaternion();
  const qRoll = new THREE.Quaternion();
  const qBank = new THREE.Quaternion();
  const basis = new THREE.Matrix4();
  const AXIS_X = new THREE.Vector3(1, 0, 0);

  // Yaw + pitch, belly toward world-down, bank around the nose.
  // Pitch stays off ±90° so forward never aligns with world-up (basis stays stable).
  const setAim = (yawA: number, pitchA: number, bankA: number) => {
    const cp = Math.cos(pitchA);
    forward.set(Math.cos(yawA) * cp, Math.sin(pitchA), -Math.sin(yawA) * cp);
    right.crossVectors(forward, worldUp).normalize();
    up.crossVectors(right, forward).normalize();
    right.crossVectors(forward, up).normalize();
    qBank.setFromAxisAngle(forward, bankA);
    up.applyQuaternion(qBank);
    right.applyQuaternion(qBank);
    basis.makeBasis(forward, up, right);
    aimQuat.setFromRotationMatrix(basis);
  };

  // Visual-only turn arc / dip offsets (physics stays on `pos`).
  let visXOff = 0;
  let visYOff = 0;
  const applyPose = (z: number, xOff: number, yOff: number) => {
    depthK = 1;
    visXOff = xOff;
    visYOff = yOff;
    dolphin.position.set(pos.x + xOff, pos.y + yOff, z);
  };

  const clampSpeed = () => {
    const sp = vel.length();
    if (sp > MAX_SPEED) vel.multiplyScalar(MAX_SPEED / sp);
  };

  // --- CHIRP ---
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
        osc.onended = () => {
          osc.disconnect();
          gain.disconnect();
        };
        osc.start(t);
        osc.stop(t + 0.14);
      }
    } catch {
      /* non-fatal */
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

  let lastTransform = '';
  let lastSize = '';
  const placeHit = () => {
    if (!hit || !modelReady) return;
    const w = Math.max(1, innerWidth);
    const h = Math.max(1, innerHeight);
    const halfH = halfWidth / Math.max(1e-4, camera.aspect);
    const hx = (HALF_LENGTH * scale * 1.05) / depthK + HIT_PAD;
    const hy = (1.0 * scale) / depthK + BOB * zoom + HIT_PAD;
    const pxX = w / (2 * halfWidth);
    const pxY = h / (2 * halfH);
    const x = w / 2 + (pos.x + visXOff - hx) * pxX;
    const y = h / 2 - (pos.y + visYOff + hy) * pxY;
    const transform = `translate(${x.toFixed(1)}px, ${y.toFixed(1)}px)`;
    if (transform !== lastTransform) {
      hit.style.transform = transform;
      lastTransform = transform;
    }
    const size = `${(2 * hx * pxX).toFixed(1)}|${(2 * hy * pxY).toFixed(1)}`;
    if (size !== lastSize) {
      hit.style.width = `${2 * hx * pxX}px`;
      hit.style.height = `${2 * hy * pxY}px`;
      lastSize = size;
    }
  };

  new GLTFLoader()
      .loadAsync(MODEL_URL)
      .then(({ scene: loaded, animations }) => {
        if (signal.aborted) return;
        model = loaded;
        let hasColors = false;
        loaded.traverse((obj) => {
          if (!(obj instanceof THREE.Mesh)) return;
          obj.material = material;
          obj.frustumCulled = false;
          hasColors ||= obj.geometry.hasAttribute('color');
        });
        material.vertexColors = hasColors;
        material.needsUpdate = true;

        dolphin.add(loaded);
        modelReady = true;
        if (hit) hit.style.visibility = 'visible';
        if (animations[0]) {
          mixer = new THREE.AnimationMixer(loaded);
          mixer.clipAction(animations[0]).play();
        }
      })
      .catch((err) => console.error('GLB load failed', err));

  if (hit) {
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
      hit.classList.remove('dragging');
      // shove away from the grab point, then spring home
      let awayX = pos.x - dragPos.x;
      let awayY = pos.y - dragPos.y;
      let len = Math.hypot(awayX, awayY);
      if (len < 1e-3) {
        // released dead-centre: pick a stable direction
        awayX = faceSign;
        awayY = 0.3;
        len = Math.hypot(awayX, awayY);
      }
      vel.x += (awayX / len) * ESCAPE_SPEED;
      vel.y += (awayY / len) * ESCAPE_SPEED;
      clampSpeed();
      escapeTimer = ESCAPE_TIME;
      chirp(2);
    };
    hit.addEventListener('pointerdown', (e) => {
      if (dragId !== null || !modelReady) return;
      if (e.pointerType === 'mouse' && e.button !== 0) return;
      unlockAudio();
      toWorld(e.clientX, e.clientY, dragPos);
      dragOffset.set(dragPos.x - pos.x, dragPos.y - pos.y);
      dragId = e.pointerId;
      hit.setPointerCapture(e.pointerId);
      hit.classList.add('dragging');
      escapeTimer = 0;
      chirp(1);
      e.preventDefault();
    }, { signal });
    hit.addEventListener('pointermove', (e) => {
      if (e.pointerId !== dragId) return;
      toWorld(e.clientX, e.clientY, dragPos);
      const halfH = halfWidth / Math.max(1e-4, camera.aspect);
      const bx = Math.max(0, halfWidth - 0.5);
      const by = Math.max(0, halfH - 0.5);
      dragPos.x = clamp(dragPos.x, -bx, bx);
      dragPos.y = clamp(dragPos.y, -by, by);
    }, { passive: true, signal });
    hit.addEventListener('pointerup', endDrag, { signal });
    hit.addEventListener('pointercancel', endDrag, { signal });
    hit.addEventListener('lostpointercapture', endDrag, { signal });
  }

  const readScroll = () => {
    const max = document.documentElement.scrollHeight - innerHeight;
    scroll = max > 0 ? clamp(scrollY / max, 0, 1) : 0;
    if (hint) hint.style.opacity = scrollY > 40 ? '0' : '0.6';
  };

  const laneRange = () => Math.max(0, halfWidth - HALF_LENGTH * scale - EDGE_MARGIN * zoom);

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
    const base = clamp(aspect * 0.55, 0.35, 0.85);
    scale = Math.min(base * zoom, (halfWidth * MAX_WIDTH) / HALF_LENGTH);
    dolphin.scale.setScalar(scale);

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

  let smooth = 0;
  {
    // Boot at p=0 (left, y=1.5) — avoid rising from the origin
    pos.set(-laneRange(), 1.5);
    applyPose(0, 0, 0);
    smooth = scroll; // catch up via spring if loaded mid-scroll
  }

  const clock = new THREE.Clock();
  let scrollSpeed = 0;
  let animTime = 0;
  let steppedTime = 0;
  let raf = 0;

  const frame = () => {
    raf = requestAnimationFrame(frame);
    const dt = Math.min(clock.getDelta(), 0.05);
    if (contextLost || dt <= 0) return;

    animTime += dt;
    const prevStep = steppedTime;
    steppedTime = Math.floor(animTime * ANIM_FPS) / ANIM_FPS;

    // --- SCROLL LANE ---
    const prevSmooth = smooth;
    smooth = ease(smooth, scroll, 5, dt);
    // signed: <0 = scroll up, >0 = scroll down
    const signedScrollVel = (smooth - prevSmooth) / dt;
    scrollSpeed = ease(scrollSpeed, Math.min(1, Math.abs(signedScrollVel) * 1.33), 6, dt);
    scrollUp = ease(scrollUp, Math.max(0, -signedScrollVel), 8, dt);

    const p = smooth;
    const range = laneRange();
    const pathX = (p * 2 - 1) * range;
    // p=0 → y=1.5, p=1 → y=-1.5 (+ bob)
    const pathY = 1.5 - p * 3 + Math.sin(animTime * 1.1) * BOB * zoom;

    dragBlend = ease(dragBlend, dragId !== null ? 1 : 0, BLEND_RATE, dt);

    // --- MOTION ---
    escapeEase = ease(escapeEase, escapeTimer > 0 ? 1 : 0, 8, dt);
    const springScale = lerp(1, 0.35, escapeEase);
    const dampL = Math.exp(-SPRING_D * dt);
    const vlx = (vel.x + (pathX - pos.x) * SPRING_K * springScale * dt) * dampL;
    const vly = (vel.y + (pathY - pos.y) * SPRING_K * springScale * dt) * dampL;
    if (dragBlend > 0.001) {
      // Forward-only chase toward the cursor (capped turn rate + speed)
      const toX = dragPos.x - dragOffset.x - pos.x;
      const toY = dragPos.y - dragOffset.y - pos.y;
      const dist = Math.hypot(toX, toY);
      const sp0 = vel.length();
      const curAng = sp0 > 0.15 ? Math.atan2(vel.y, vel.x) : faceSign > 0 ? 0 : Math.PI;
      const err = wrapPi(Math.atan2(toY, toX) - curAng);
      const ang = dist > 0.05 ? curAng + clamp(err, -DRAG_TURN_RATE * dt, DRAG_TURN_RATE * dt) : curAng;
      const align = Math.max(0, Math.cos(err));
      const spTarget = Math.min(DRAG_CRUISE, dist * DRAG_ARRIVE) * (0.3 + 0.7 * align);
      const sp = ease(Math.max(sp0, 0.2), spTarget, DRAG_SPEED_RATE, dt);
      vel.set(
          lerp(vlx, Math.cos(ang) * sp, dragBlend),
          lerp(vly, Math.sin(ang) * sp, dragBlend),
      );
    } else {
      vel.set(vlx, vly);
    }
    clampSpeed();

    swimS = ease(swimS, vel.length(), 10, dt);

    // --- HEADING ---
    // Idle: tip from lane position / scroll-up
    const idleTipY = lerp(-0.15 - 0.1 * p, 0.45, smoothstep(scrollUp, 0.04, 0.15));
    setUnit(faceSign, idleTipY, faceSign, 0);
    const idleX = dirTmp.x;
    const idleY = dirTmp.y;
    let dx = idleX;
    let dy = idleY;

    // Swim: face travel direction
    const motionW = smoothstep(swimS, 0.1, 0.5);
    setUnit(vel.x, vel.y + 1.1 * scrollUp, idleX, idleY);
    dx = lerp(dx, dirTmp.x, motionW);
    dy = lerp(dy, dirTmp.y, motionW);

    setUnit(dx, dy, idleX, idleY);
    dx = dirTmp.x;
    dy = dirTmp.y;

    // --- FACING ---
    flipCool = Math.max(0, flipCool - dt);
    let wantSign = faceSign;
    if (dragBlend > 0.5) {
      const cx = dragPos.x - dragOffset.x - pos.x;
      const cy = dragPos.y - dragOffset.y - pos.y;
      const cd = Math.hypot(cx, cy);
      if (cd > DRAG_FLIP_DIST && Math.abs(cx) / cd > FLIP_THRESHOLD) wantSign = Math.sign(cx);
    } else if (p < EDGE_P) {
      wantSign = 1; // top: face right
    } else if (p > 1 - EDGE_P) {
      wantSign = -1; // bottom: face left
    } else if (signedScrollVel > SCROLL_FACE_V) {
      wantSign = 1; // scrolling down: face right
    } else if (signedScrollVel < -SCROLL_FACE_V) {
      wantSign = -1; // scrolling up: face left
    }
    // no mid-turn reversals
    const turnDone = turnT >= 1;
    if (wantSign !== faceSign) flipTimer += dt;
    else flipTimer = Math.max(0, flipTimer - dt * 2);
    if (flipTimer >= FLIP_HOLD && flipCool <= 0 && turnDone) {
      faceSign = wantSign;
      turnFrom = turnP;
      turnTo = faceSign < 0 ? 1 : 0;
      turnT = 0;
      settleT = 0;
      flipTimer = 0;
      flipCool = FLIP_COOLDOWN;
    }

    // --- ORIENTATION ---
    pitch = ease(
        pitch,
        clamp(Math.atan2(dy, Math.max(0.35, Math.abs(dx))), -PITCH_MAX, PITCH_MAX) *
        lerp(1, DRAG_PITCH_GAIN, dragBlend),
        lerp(PITCH_RATE, DRAG_PITCH_RATE, dragBlend),
        dt,
    );

    const aimStrength = clamp(Math.max(motionW, dragBlend), 0, 1);
    const tilt = REST_TILT * (1 - 0.7 * aimStrength);
    // Quintic ease over TURN_TIME; arc / bank / dip derive from this
    turnT = Math.min(1, turnT + dt / TURN_TIME);
    const e = turnT * turnT * turnT * (turnT * (turnT * 6 - 15) + 10);
    turnP = lerp(turnFrom, turnTo, e);
    tiltS = ease(tiltS, tilt, 4, dt);
    const prevYaw = yaw;
    yaw = -tiltS + turnP * (Math.PI + 2 * tiltS);
    yawVel = ease(yawVel, (yaw - prevYaw) / dt, 25, dt);

    // 0 sideways, 1 into screen
    const turn = Math.sin(clamp(yaw, 0, Math.PI));

    // Bank peaks mid-turn (bell curve)
    const bell = 16 * turnT * turnT * (1 - turnT) * (1 - turnT);
    bank = ease(bank, faceSign * BANK_MAX * bell, 10, dt);

    const w = e; // progress along the turn (1 at rest)
    const turnFade = 1 - 0.25 * dragBlend;
    // Nose down entering, up leaving
    const loopPitch = -TURN_PITCH * Math.sin(w * Math.PI * 2) * turnFade;

    setAim(yaw, clamp(pitch + loopPitch, -1.25, 1.25), bank);
    if (flick > 0) {
      const a = smoothstep(1 - flick, 0, 1) * Math.PI * 2;
      qRoll.setFromAxisAngle(AXIS_X, a);
      aimQuat.multiply(qRoll);
    }
    dolphin.quaternion.copy(aimQuat);

    // --- INTEGRATE ---
    pos.x += vel.x * dt;
    pos.y += vel.y * dt;
    {
      const halfH = halfWidth / Math.max(1e-4, camera.aspect);
      const bx = Math.max(0, halfWidth - 0.5);
      const by = Math.max(0, halfH - 0.5);
      if (Math.abs(pos.x) > bx) vel.x += (Math.sign(pos.x) * bx - pos.x) * 18 * dt;
      if (Math.abs(pos.y) > by) vel.y += (Math.sign(pos.y) * by - pos.y) * 18 * dt;
      pos.x = clamp(pos.x, -bx - 0.5, bx + 0.5);
      pos.y = clamp(pos.y, -by - 0.5, by + 0.5);
    }

    // --- U-TURN PATH ---
    if (turnT > 0.8) settleT = Math.min(1, settleT + dt / SETTLE_TIME);
    const settle = smoothstep(settleT, 0, 1);
    const R = TURN_RADIUS_K * HALF_LENGTH * scale;
    const arc = Math.sin(w * Math.PI);
    const zArc = -R * (1 - Math.cos(w * Math.PI)) * (1 - settle) * turnFade;
    const reach = (HALF_LENGTH * scale * Math.abs(Math.cos(yaw))) / depthK + 0.3;
    const allowed = Math.max(0, halfWidth - reach);
    const xRaw = -faceSign * R * arc * turnFade;
    // U-turn x overshoot, clamped on-screen
    const xOff = clamp(xRaw, Math.min(0, -allowed - pos.x), Math.max(0, allowed - pos.x));
    applyPose(zArc, xOff, -TURN_DIP * arc * turnFade);

    // --- ALIVENESS ---
    escapeTimer = Math.max(0, escapeTimer - dt);
    idleTimer -= dt;
    if (
        dragId === null &&
        dragBlend < 0.15 &&
        idleTimer <= 0 &&
        swimS < 0.55 &&
        escapeTimer <= 0 &&
        turn < 0.05 &&
        Math.abs(yawVel) < 0.3
    ) {
      idleTimer = 5 + Math.random() * 6;
      flick = 1;
    }
    flick = Math.max(0, flick - dt * 1.4);

    // Tailbeat: scroll-linked, plus turn and drag/swim boosts
    mixer?.update((steppedTime - prevStep) * (1 + scrollSpeed * 1.6 + turn * 1.0 + dragBlend * Math.min(1.2, swimS * 0.3)));

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
    if (model) {
      mixer?.uncacheRoot(model);
      model.traverse((obj) => {
        if (obj instanceof THREE.Mesh) obj.geometry.dispose();
      });
    }
    quad.geometry.dispose();
    post.dispose();
    material.dispose();
    depthTexture.dispose();
    target.dispose();
    renderer.dispose();
    void audio?.close().catch(() => {});
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
      if (reduceMotion.matches || stop) return;
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