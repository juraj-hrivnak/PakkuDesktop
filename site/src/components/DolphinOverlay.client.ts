import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';

const reduce = matchMedia('(prefers-reduced-motion: reduce)');
const canvas = document.getElementById('gl');
const hint = document.getElementById('hint');

const start = (canvas: HTMLCanvasElement) => {
  const CYAN = 0x00ffff;
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: false, alpha: true });
  renderer.setClearColor(0x000000, 0);
  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(40, 1, 4, 30);
  camera.position.set(0, 0, 11);
  scene.add(new THREE.AmbientLight(0xffffff, 0.35));
  const sun = new THREE.DirectionalLight(0xffffff, 0.8);
  sun.position.set(-3, 5, 6);
  scene.add(sun);

  // r128 still needs skinning on MeshLambertMaterial
  const toon = new THREE.MeshLambertMaterial({ color: CYAN, vertexColors: true }) as THREE.MeshLambertMaterial & {
    skinning: boolean;
  };
  toon.skinning = true;

  const rt = new THREE.WebGLRenderTarget(2, 2, {
    minFilter: THREE.NearestFilter,
    magFilter: THREE.NearestFilter,
    format: THREE.RGBAFormat,
  });
  rt.depthTexture = new THREE.DepthTexture(2, 2);
  const post = new THREE.ShaderMaterial({
    uniforms: {
      tex: { value: rt.texture },
      depthTex: { value: rt.depthTexture },
      uNear: { value: 4 },
      uFar: { value: 30 },
      res: { value: new THREE.Vector2(2, 2) },
      uShadowColor: { value: new THREE.Color(0x000000) },
      uShadowStrength: { value: 0.85 },
    },
    vertexShader: 'varying vec2 vUv; void main(){ vUv=uv; gl_Position=vec4(position.xy,0.0,1.0); }',
    fragmentShader: `
      uniform sampler2D tex; uniform sampler2D depthTex;
      uniform vec2 res; uniform float uNear; uniform float uFar;
      uniform vec3 uShadowColor; uniform float uShadowStrength;
      varying vec2 vUv;
      float lin(float d){ float z = d*2.0-1.0; return 2.0*uNear*uFar/(uFar+uNear - z*(uFar-uNear)); }
      void main(){
        vec2 e = 1.0/res;
        vec4 c = texture2D(tex, vUv);
        if (c.a < 0.5) discard;
        float depth = lin(texture2D(depthTex, vUv).x);
        float au = texture2D(tex, vUv + vec2(0.0,-e.y)).a;
        float an = texture2D(tex, vUv + vec2(0.0, e.y)).a;
        float ar = texture2D(tex, vUv + vec2( e.x,0.0)).a;
        float al = texture2D(tex, vUv + vec2(-e.x,0.0)).a;
        vec4 am = step(vec4(0.5), vec4(au, an, ar, al));
        float du = lin(texture2D(depthTex, vUv + vec2(0.0,-e.y)).x);
        float dn = lin(texture2D(depthTex, vUv + vec2(0.0, e.y)).x);
        float dr = lin(texture2D(depthTex, vUv + vec2( e.x,0.0)).x);
        float dl = lin(texture2D(depthTex, vUv + vec2(-e.x,0.0)).x);
        float m = min(min(mix(1e3, du, am.x), mix(1e3, dn, am.y)), min(mix(1e3, dr, am.z), mix(1e3, dl, am.w)));
        float outline = smoothstep(0.25, 0.32, depth - m);
        float l = c.g;
        float q = l < 0.55 ? 0.4 : (l < 0.85 ? 0.7 : 1.0);
        vec3 base = vec3(0.0, q, q);
        vec3 col = mix(base, mix(base, uShadowColor, uShadowStrength), outline);
        gl_FragColor = vec4(col, 1.0);
      }`,
    depthTest: false,
    depthWrite: false,
  });
  const quad = new THREE.Mesh(new THREE.PlaneGeometry(2, 2), post);
  quad.frustumCulled = false;
  const postScene = new THREE.Scene();
  postScene.add(quad);
  const postCam = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1);

  const dolphin = new THREE.Group();
  scene.add(dolphin);
  let mixer: THREE.AnimationMixer | null = null;

  new GLTFLoader().load(
    '/dolphin.glb',
    (gltf) => {
      gltf.scene.traverse((o) => {
        if (!(o as THREE.SkinnedMesh).isSkinnedMesh) return;
        const mesh = o as THREE.SkinnedMesh;
        mesh.material = toon;
        mesh.frustumCulled = false;
      });
      dolphin.add(gltf.scene);
      mixer = new THREE.AnimationMixer(gltf.scene);
      const clip = gltf.animations.at(0);
      if (clip) mixer.clipAction(clip).play();
    },
    undefined,
    (e) => console.error('GLB load failed', e),
  );

  let W = 1;
  let H = 1;
  let sc = 0.8;
  let hw = 7;
  const resize = () => {
    W = innerWidth;
    H = innerHeight;
    renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
    renderer.setSize(W, H, false);
    const px = W < 700 ? 3 : 4;
    const lw = Math.max(2, Math.round(W / px));
    const lh = Math.max(2, Math.round(H / px));
    rt.setSize(lw, lh);
    const res = post.uniforms.res?.value;
    if (res instanceof THREE.Vector2) res.set(lw, lh);
    camera.aspect = W / H;
    camera.updateProjectionMatrix();
    sc = Math.min(0.85, Math.max(0.35, (W / H) * 0.55));
    hw = Math.tan((20 * Math.PI) / 180) * 11 * (W / H);
  };
  addEventListener('resize', resize);
  resize();

  let target = 0;
  let smooth = 0;
  let last = 0;
  let speed = 0;
  let T = 0;
  let animT = 0;
  let stepT = 0;
  const ANIM_FPS = 30;
  const readScroll = () => {
    const max = document.documentElement.scrollHeight - innerHeight;
    target = max > 0 ? Math.min(1, Math.max(0, scrollY / max)) : 0;
    if (hint) hint.style.opacity = scrollY > 40 ? '0' : '0.6';
  };
  addEventListener('scroll', readScroll, { passive: true });
  readScroll();

  const clock = new THREE.Clock();
  let running = true;
  const frame = () => {
    if (!running) return;
    const dt = Math.min(clock.getDelta(), 0.05);
    animT += dt;
    const stepped = Math.floor(animT * ANIM_FPS) / ANIM_FPS;
    const dStep = stepped - stepT;
    stepT = stepped;
    T = stepped;
    smooth += (target - smooth) * Math.min(1, dt * 5);
    speed += (Math.min(1, Math.abs(smooth - last) * 80) - speed) * 0.1;
    last = smooth;
    mixer?.update(dStep * (1 + speed * 1.6));

    const p = smooth;
    const range = hw - 3 * sc - 0.3;
    dolphin.scale.setScalar(sc);
    dolphin.position.set((p * 2 - 1) * range, 1.5 - p * 3.0 + Math.sin(T * 1.1) * 0.06, 0);
    dolphin.rotation.y = -0.4 + 0.3 * Math.sin(p * Math.PI * 2);
    dolphin.rotation.z = -0.08 - 0.3 * p;
    renderer.setRenderTarget(rt);
    renderer.setClearColor(0x000000, 0);
    renderer.clear();
    renderer.render(scene, camera);
    renderer.setRenderTarget(null);
    renderer.clear();
    renderer.render(postScene, postCam);
    requestAnimationFrame(frame);
  };
  frame();

  reduce.addEventListener('change', (e) => {
    if (!e.matches) return;
    running = false;
    renderer.dispose();
    canvas.remove();
    hint?.remove();
  });
};

if (canvas instanceof HTMLCanvasElement) {
  if (reduce.matches) {
    canvas.remove();
    hint?.remove();
  } else {
    const ric = window.requestIdleCallback;
    if (typeof ric === 'function') ric(() => start(canvas), { timeout: 1500 });
    else setTimeout(() => start(canvas), 1);
  }
}
