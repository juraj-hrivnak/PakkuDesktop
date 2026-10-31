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
  uniform vec3 uShadowColor;
  uniform float uShadowStrength;
  varying vec2 vUv;

  float linearDepth(float d) {
    float z = d * 2.0 - 1.0;
    return 2.0 * uNear * uFar / (uFar + uNear - z * (uFar - uNear));
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

    float depth = linearDepth(texture2D(depthTex, vUv).x);
    float nearest = min(
      min(neighbourDepth(vec2(0.0, -1.0)), neighbourDepth(vec2(0.0, 1.0))),
      min(neighbourDepth(vec2(1.0, 0.0)), neighbourDepth(vec2(-1.0, 0.0)))
    );
    float outline = smoothstep(0.25, 0.32, depth - nearest);

    float q = c.g < 0.55 ? 0.4 : c.g < 0.85 ? 0.7 : 1.0;
    vec3 base = vec3(0.0, q, q);
    gl_FragColor = vec4(mix(base, uShadowColor, outline * uShadowStrength), 1.0);
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
      uShadowColor: { value: new THREE.Color(0x000000) },
      uShadowStrength: { value: 0.85 },
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
      if (animations[0]) {
        mixer = new THREE.AnimationMixer(model);
        mixer.clipAction(animations[0]).play();
      }
    })
    .catch((err) => console.error('GLB load failed', err));

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
    mixer?.update((steppedTime - prevStep) * (1 + speed * 1.6));

    const p = smooth;
    const range = Math.max(0, halfWidth - HALF_LENGTH * scale - EDGE_MARGIN * zoom);
    dolphin.scale.setScalar(scale);
    dolphin.position.set(
      (p * 2 - 1) * range,
      1.5 - p * 3 + Math.sin(steppedTime * 1.1) * BOB * zoom,
      0,
    );
    dolphin.rotation.y = -0.4 + 0.3 * Math.sin(p * Math.PI * 2);
    dolphin.rotation.z = -0.08 - 0.3 * p;

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
