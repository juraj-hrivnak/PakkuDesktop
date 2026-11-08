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
const INFLUENCE = 2.5; // cursor proximity that pulls the dolphin
const PULL = 0.35;
const PLAY_OFFSET = INFLUENCE * PULL; // pads the grab rect for cursor play

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

  // Invisible grab surface that tracks the dolphin (canvas stays pointer-events: none).
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
  let face = 0;
  let faceTo = 0;
  let pitch = 0;
  let pitchTo = 0;
  let prevDragX = 0;
  let prevDragY = 0;
  let modelReady = false;

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
      // vertexColors with no color attribute renders black
      material.vertexColors = hasColors;
      material.needsUpdate = true;

      dolphin.add(model);
      modelReady = true;
      if (hitEl instanceof HTMLElement) hitEl.style.visibility = 'visible';
      if (animations[0]) {
        mixer = new THREE.AnimationMixer(model);
        mixer.clipAction(animations[0]).play();
      }
    })
    .catch((err) => console.error('GLB load failed', err));

  if (hitEl instanceof HTMLElement) {
    const endDrag = (e: PointerEvent) => {
      if (e.pointerId !== dragId) return;
      dragId = null;
      hitEl.classList.remove('dragging');
    };
    hitEl.addEventListener('pointerdown', (e) => {
      if (dragId !== null || !modelReady) return;
      if (e.pointerType === 'mouse' && e.button !== 0) return;
      toWorld(e.clientX, e.clientY, dragPos);
      dragOffset.set(dragPos.x - dolphin.position.x, dragPos.y - dolphin.position.y, 0);
      prevDragX = dragPos.x;
      prevDragY = dragPos.y;
      dragId = e.pointerId;
      hitEl.setPointerCapture(e.pointerId);
      hitEl.classList.add('dragging');
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

  // The layout is the original one at load-time zoom. Browser zoom is detected
  // through devicePixelRatio and applied on top, so the dolphin, its margin and
  // its texels keep their CSS size like any other page content.
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
    // Not canvas.clientWidth: setSize(..., false) leaves CSS sizing to the
    // stylesheet, and an unstyled canvas would report its own buffer size.
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
    // older three versions leave the depth texture at its old size
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

  // zoom changes the ratio without always firing `resize`; the query has to be
  // re-armed since it embeds the current ratio
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

  const clock = new THREE.Clock();
  let smooth = 0;
  let speed = 0;
  let animTime = 0;
  let steppedTime = 0;
  let raf = 0;

  const frame = () => {
    raf = requestAnimationFrame(frame);
    const dt = Math.min(clock.getDelta(), 0.05);

    // the swim cycle and bobbing run at a fixed low fps
    animTime += dt;
    const prevStep = steppedTime;
    steppedTime = Math.floor(animTime * ANIM_FPS) / ANIM_FPS;

    const prev = smooth;
    smooth += (scroll - smooth) * Math.min(1, dt * 5);
    const velocity = dt > 0 ? Math.abs(smooth - prev) / dt : 0;
    speed += (Math.min(1, velocity * 1.33) - speed) * (1 - Math.exp(-6 * dt));

    const p = smooth;
    const range = Math.max(0, halfWidth - HALF_LENGTH * scale - EDGE_MARGIN * zoom);
    const pathX = (p * 2 - 1) * range;
    const pathY = 1.5 - p * 3 + Math.sin(steppedTime * 1.1) * BOB * zoom;

    let targetX = pathX;
    let targetY = pathY;
    let excitement = 0;
    if (pointerFresh && dragBlend < 0.5) {
      const dx = pointer.x - pathX;
      const dy = pointer.y - pathY;
      const t = THREE.MathUtils.clamp(1 - Math.hypot(dx, dy) / INFLUENCE, 0, 1);
      excitement = t * t;
      targetX += dx * excitement * PULL;
      targetY += dy * excitement * PULL;
    }

    mixer?.update((steppedTime - prevStep) * (1 + speed * 1.6 + excitement * 0.8));

    const grabbing = dragId !== null;
    // grab snaps in; release eases back onto the scroll path
    dragBlend += ((grabbing ? 1 : 0) - dragBlend) * (1 - Math.exp(-(grabbing ? 12 : 3.2) * dt));
    dolphin.scale.setScalar(scale);
    dolphin.position.set(
      THREE.MathUtils.lerp(targetX, dragPos.x - dragOffset.x, dragBlend),
      THREE.MathUtils.lerp(targetY, dragPos.y - dragOffset.y, dragBlend),
      0,
    );

    // face/pitch follow scroll or drag intent (not bob/cursor jitter)
    const scrollVel = dt > 0 ? (smooth - prev) / dt : 0;
    if (grabbing) {
      const dvx = dt > 0 ? (dragPos.x - prevDragX) / dt : 0;
      const dvy = dt > 0 ? (dragPos.y - prevDragY) / dt : 0;
      if (dvx < -0.2) faceTo = 1;
      else if (dvx > 0.2) faceTo = 0;
      if (dvy > 0.2) pitchTo = 1;
      else if (dvy < -0.2) pitchTo = 0;
    } else if (dragBlend < 0.05) {
      if (p < 0.08) faceTo = 0;
      else if (p > 0.92) faceTo = 1;
      else if (scrollVel < -0.2) faceTo = 1;
      else if (scrollVel > 0.2) faceTo = 0;

      if (p < 0.08) pitchTo = 0;
      else if (p > 0.92 && scrollVel > -0.04) pitchTo = 0.35;
      else if (scrollVel < -0.15) pitchTo = 1;
      else if (scrollVel > 0.2) pitchTo = 0;
      // else keep pitchTo — nose-up holds after a climb until top or scroll-down
    }
    prevDragX = dragPos.x;
    prevDragY = dragPos.y;

    face += (faceTo - face) * (1 - Math.exp(-3.5 * dt));
    pitch += (pitchTo - pitch) * (1 - Math.exp(-4 * dt));
    dolphin.rotation.y = -0.4 + 0.3 * Math.sin(p * Math.PI * 2) * (1 - face) + face * Math.PI;
    dolphin.rotation.z = THREE.MathUtils.lerp(-0.08 - 0.3 * p, 0.55, pitch) * (1 - dragBlend);

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
