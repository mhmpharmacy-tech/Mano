import * as THREE from './three.module.min.js';
import { OrbitControls } from './OrbitControls.js';
import { RoomEnvironment } from './RoomEnvironment.js';

const canvas = document.getElementById('mano-stage');
const fallbackEl = document.getElementById('webgl-fallback');

let renderer;
try {
  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, powerPreference: 'default' });
} catch (err) {
  if (fallbackEl) fallbackEl.style.display = 'flex';
  throw err;
}

// Calibrated Frosted-Glass Render Tuning (Low AO + Restrained Bloom + Soft Reflections + Frosted-Turquoise Mix)
const MANO_RENDER_TUNING = {
  ambientOcclusion: {
    materialAoIntensity: 0.12,
    contactShadowOpacityLight: 0.28,
    contactShadowOpacityDark: 0.34,
  },
  reflection: {
    pmremBlur: 0.115,
    sceneEnvIntensityLight: 0.56,
    sceneEnvIntensityDark: 0.46,
    manoPorcelainEnv: 0.32,
    manoTrimEnv: 0.34,
    manoAccentEnv: 0.36,
    manoVisorEnv: 0.36,
    manoMetalEnv: 0.30,
    manoSoftEnv: 0.28,
    plinthEnv: 0.38,
    squadEnv: 0.32,
  },
  frostedMix: {
    turquoiseColorLight: 0x76dcd2,
    turquoiseColorDark: 0x8bece2,
    mistColorLight: 0xf1fbfa,
    mistColorDark: 0xbdebe7,
    glowStrengthLight: 0.15,
    glowStrengthDark: 0.23,
    transmissionScale: 1.0,
  },
  eyeTracking: {
    maxHeadYaw: 0.27,
    maxHeadPitch: 0.16,
    headRollFactor: 0.24,
    headDamping: 2.6,
    eyeDamping: 6.2,
    maxEyeShiftX: 0.038,
    maxEyeShiftY: 0.022,
    maxSmileShiftX: 0.008,
    pointerHoldMs: 2800,
    tiltHoldMs: 4000,
    idleDriftAmpX: 0.09,
    idleDriftAmpY: 0.05,
  },
  bloom: {
    exposureLight: 0.98,
    exposureDark: 1.03,
    faceGlowLight: 0.56,
    faceGlowDark: 0.68,
    cheekBlushIntensity: 0.32,
    cheekBlushOpacity: 0.58,
    emblemGlowLight: 0.16,
    emblemGlowDark: 0.24,
    trimGlowLight: 0.10,
    trimGlowDark: 0.16,
    squadRimIdle: 0.14,
    squadRimSelected: 0.38,
    warmAuraIntensity: 0.62,
    bokehOpacityLight: 0.32,
    bokehOpacityDark: 0.42,
    moteGlowLight: 0.08,
    moteGlowDark: 0.18,
    moteOpacity: 0.52,
    bubbleGlowLight: 0.05,
    bubbleGlowDark: 0.12,
    bubbleOpacityLight: 0.24,
    bubbleOpacityDark: 0.20,
  },
};

let currentIsDarkTheme = false;
let aoIntensityScale = 1.0;
let bloomIntensityScale = 1.0;
let envReflectionScale = 1.0;
let frostedMixScale = 1.0;

renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.75));
renderer.setSize(window.innerWidth, window.innerHeight, false);
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = MANO_RENDER_TUNING.bloom.exposureLight;
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;

const scene = new THREE.Scene();
scene.background = new THREE.Color(0xf4faff);
scene.fog = new THREE.FogExp2(0xf4faff, 0.024);

const pmrem = new THREE.PMREMGenerator(renderer);
const roomEnv = new RoomEnvironment();
scene.environment = pmrem.fromScene(roomEnv, MANO_RENDER_TUNING.reflection.pmremBlur).texture;
scene.environmentIntensity = MANO_RENDER_TUNING.reflection.sceneEnvIntensityLight;
roomEnv.dispose();
pmrem.dispose();

// Procedural ceramic micro-grain roughness map for tactile PBR highlights
function createMicroNoiseTexture() {
  const c = document.createElement('canvas');
  c.width = 128;
  c.height = 128;
  const ctx = c.getContext('2d');
  const img = ctx.createImageData(128, 128);
  for (let i = 0; i < img.data.length; i += 4) {
    // Deterministic soft ceramic variance centered around ~232..255
    const n = 232 + Math.floor((((i * 19 + 37) % 97) / 97) * 23);
    img.data[i] = n;
    img.data[i + 1] = n;
    img.data[i + 2] = n;
    img.data[i + 3] = 255;
  }
  ctx.putImageData(img, 0, 0);
  const tex = new THREE.CanvasTexture(c);
  tex.wrapS = THREE.RepeatWrapping;
  tex.wrapT = THREE.RepeatWrapping;
  tex.repeat.set(3, 3);
  return tex;
}
const microGrainTex = createMicroNoiseTexture();

const camera = new THREE.PerspectiveCamera(42, window.innerWidth / window.innerHeight, 0.1, 80);
camera.position.set(0.3, 2.35, 5.2);

const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.dampingFactor = 0.075;
controls.maxPolarAngle = Math.PI * 0.475;
controls.minDistance = 2.0;
controls.maxDistance = 9.5;
controls.target.set(0, 0.72, 0);
controls.autoRotate = false;
controls.autoRotateSpeed = 0.0;

// Soft High-Key Scandinavian Studio Lighting Rig (Diffused Key + Airy Fill + Rim + Low-AO Ambient Lift)
const keyLight = new THREE.DirectionalLight(0xfffaf4, 1.38);
keyLight.position.set(4.6, 8.2, 5.0);
keyLight.castShadow = true;
keyLight.shadow.mapSize.set(1024, 1024);
keyLight.shadow.radius = 8.5;
keyLight.shadow.blurSamples = 16;
keyLight.shadow.camera.left = -4.8;
keyLight.shadow.camera.right = 4.8;
keyLight.shadow.camera.top = 4.8;
keyLight.shadow.camera.bottom = -4.8;
keyLight.shadow.camera.near = 0.5;
keyLight.shadow.camera.far = 20;
keyLight.shadow.bias = -0.00025;
keyLight.shadow.normalBias = 0.036;
scene.add(keyLight);

const fillLight = new THREE.DirectionalLight(0xe4f7fa, 1.08);
fillLight.position.set(-4.8, 4.2, 4.0);
scene.add(fillLight);

const rimLight = new THREE.DirectionalLight(0xf0fdff, 0.94);
rimLight.position.set(-1.6, 4.6, -5.2);
scene.add(rimLight);

const hemiLight = new THREE.HemisphereLight(0xf4fcff, 0xe2f2f4, 1.12);
scene.add(hemiLight);

const ambientLiftLight = new THREE.AmbientLight(0xf6fcfd, 0.34);
scene.add(ambientLiftLight);

// Ground clinic studio stage & whisper-soft low-AO radial contact shadow generator
function makeRadialShadowTexture() {
  const c = document.createElement('canvas');
  c.width = 128;
  c.height = 128;
  const ctx = c.getContext('2d');
  const g = ctx.createRadialGradient(64, 64, 4, 64, 64, 62);
  g.addColorStop(0.0, 'rgba(38, 82, 92, 0.10)');
  g.addColorStop(0.42, 'rgba(38, 82, 92, 0.045)');
  g.addColorStop(0.76, 'rgba(38, 82, 92, 0.012)');
  g.addColorStop(1.0, 'rgba(38, 82, 92, 0.0)');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, 128, 128);
  const tex = new THREE.CanvasTexture(c);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}
const contactShadowTex = makeRadialShadowTexture();
const contactShadowMats = [];

function createContactShadow(rx, rz) {
  const mat = new THREE.MeshBasicMaterial({
    map: contactShadowTex,
    transparent: true,
    opacity: MANO_RENDER_TUNING.ambientOcclusion.contactShadowOpacityLight,
    depthWrite: false,
  });
  contactShadowMats.push(mat);
  const mesh = new THREE.Mesh(new THREE.PlaneGeometry(rx, rz), mat);
  mesh.rotation.x = -Math.PI / 2;
  mesh.position.y = 0.004;
  return mesh;
}

const floorMesh = new THREE.Mesh(
  new THREE.CircleGeometry(18, 64),
  new THREE.MeshStandardMaterial({
    color: 0xeaf5fc,
    roughness: 0.94,
    metalness: 0.0,
    aoMapIntensity: MANO_RENDER_TUNING.ambientOcclusion.materialAoIntensity,
  })
);
floorMesh.rotation.x = -Math.PI / 2;
floorMesh.receiveShadow = true;
scene.add(floorMesh);

// Ground-flush frosted milk-glass studio plinth + calm pastel seafoam perimeter trim
const plinthMat = new THREE.MeshPhysicalMaterial({
  color: 0xfdfefe,
  roughness: 0.48,
  roughnessMap: microGrainTex,
  metalness: 0.0,
  clearcoat: 0.10,
  clearcoatRoughness: 0.54,
  reflectivity: 0.18,
  envMapIntensity: MANO_RENDER_TUNING.reflection.plinthEnv,
  aoMapIntensity: MANO_RENDER_TUNING.ambientOcclusion.materialAoIntensity,
});
const plinth = new THREE.Mesh(
  new THREE.CylinderGeometry(2.8, 2.96, 0.12, 64),
  plinthMat
);
plinth.position.y = 0.06;
plinth.receiveShadow = true;
scene.add(plinth);

const plinthTrimMat = new THREE.MeshPhysicalMaterial({
  color: 0x76cec5,
  emissive: 0x52b0a7,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.trimGlowLight,
  roughness: 0.54,
  metalness: 0.08,
  clearcoat: 0.08,
  clearcoatRoughness: 0.56,
  reflectivity: 0.18,
  envMapIntensity: MANO_RENDER_TUNING.reflection.manoTrimEnv,
  aoMapIntensity: MANO_RENDER_TUNING.ambientOcclusion.materialAoIntensity,
});
const plinthTrimRing = new THREE.Mesh(
  new THREE.TorusGeometry(2.79, 0.022, 12, 72),
  plinthTrimMat
);
plinthTrimRing.rotation.x = Math.PI / 2;
plinthTrimRing.position.y = 0.121;
scene.add(plinthTrimRing);

// Procedural Textures
function createSyringeScaleTexture() {
  const c = document.createElement('canvas');
  c.width = 256;
  c.height = 512;
  const ctx = c.getContext('2d');
  ctx.clearRect(0, 0, 256, 512);
  ctx.strokeStyle = '#0f6a73';
  ctx.fillStyle = '#0f6a73';
  ctx.lineWidth = 4;
  ctx.font = 'bold 21px sans-serif';
  for (let i = 0; i <= 20; i++) {
    const y = 466 - i * 20;
    const isMajor = i % 2 === 0;
    ctx.beginPath();
    ctx.moveTo(96, y);
    ctx.lineTo(isMajor ? 156 : 130, y);
    ctx.stroke();
    if (i > 0 && isMajor) {
      ctx.fillText(String(i), 164, y + 7);
    }
  }
  ctx.font = 'bold 22px sans-serif';
  ctx.fillText('mL', 158, 492);
  const tex = new THREE.CanvasTexture(c);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

function createBottleLabelTexture() {
  const c = document.createElement('canvas');
  c.width = 512;
  c.height = 256;
  const ctx = c.getContext('2d');
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, 512, 256);
  ctx.fillStyle = '#13828c';
  ctx.fillRect(0, 0, 512, 54);
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 28px sans-serif';
  ctx.fillText('MANO PEDIATRIC', 32, 37);
  ctx.fillStyle = '#163038';
  ctx.font = 'bold 30px sans-serif';
  ctx.fillText('Oral Suspension 120mg / 5mL', 32, 118);
  ctx.fillStyle = '#536f77';
  ctx.font = '22px sans-serif';
  ctx.fillText('Measure only with graduated mL syringe', 32, 168);
  ctx.fillStyle = '#e76f51';
  ctx.fillRect(32, 200, 170, 28);
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 18px sans-serif';
  ctx.fillText('SHAKE WELL', 56, 220);
  const tex = new THREE.CanvasTexture(c);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

const thermoCanvas = document.createElement('canvas');
thermoCanvas.width = 256;
thermoCanvas.height = 128;
const thermoCtx = thermoCanvas.getContext('2d');
const thermoTex = new THREE.CanvasTexture(thermoCanvas);
thermoTex.colorSpace = THREE.SRGBColorSpace;

let currentThermoTempC = null;
function drawThermoScreen(isFever, tempValue) {
  if (typeof tempValue === 'number' && Number.isFinite(tempValue)) {
    currentThermoTempC = tempValue;
  }
  const shownTemp =
    currentThermoTempC !== null
      ? `${currentThermoTempC.toFixed(1)}°C`
      : (isFever ? '38.5°C' : '37.2°C');
  thermoCtx.fillStyle = isFever ? '#fff0ee' : '#e6fbf4';
  thermoCtx.fillRect(0, 0, 256, 128);
  thermoCtx.strokeStyle = isFever ? '#d64545' : '#0f7a68';
  thermoCtx.lineWidth = 6;
  thermoCtx.strokeRect(4, 4, 248, 120);
  thermoCtx.fillStyle = isFever ? '#b82525' : '#0b6656';
  thermoCtx.font = 'bold 54px sans-serif';
  thermoCtx.fillText(shownTemp, 24, 76);
  thermoCtx.font = 'bold 22px sans-serif';
  thermoCtx.fillText(isFever ? 'FEVER ALERT' : 'NORMAL', 26, 108);
  thermoTex.needsUpdate = true;
}
drawThermoScreen(false, null);

// =========================================================================
// SCENE 1: PEDIATRIC CARE WORKBENCH (Oral Syringe + Medicine Bottle + Thermometer)
// =========================================================================
const workbenchGroup = new THREE.Group();
workbenchGroup.position.y = 0.12; // sits flush atop plinth
scene.add(workbenchGroup);

const clickableProps = [];
function tagInteractive(obj, partId) {
  obj.userData.partId = partId;
  obj.traverse((child) => {
    if (child.isMesh) child.userData.partId = partId;
  });
  clickableProps.push(obj);
}

// --- 1A. Oral Syringe Assembly (7 subparts) ---
const syringeGroup = new THREE.Group();
syringeGroup.position.set(0, 0, 0.32);
workbenchGroup.add(syringeGroup);
syringeGroup.add(createContactShadow(1.25, 1.25));

const barrelMat = new THREE.MeshPhysicalMaterial({
  color: 0xf4fbfd,
  roughness: 0.16,
  metalness: 0.0,
  transmission: 0.42,
  thickness: 0.25,
  transparent: true,
  opacity: 0.88,
});
const barrel = new THREE.Mesh(new THREE.CylinderGeometry(0.23, 0.23, 1.85, 36, 1, true), barrelMat);
barrel.position.y = 1.15;
barrel.castShadow = true;
syringeGroup.add(barrel);

// Printed mL scale sleeve slightly outside barrel
const scaleSleeve = new THREE.Mesh(
  new THREE.CylinderGeometry(0.233, 0.233, 1.75, 36, 1, true),
  new THREE.MeshBasicMaterial({ map: createSyringeScaleTexture(), transparent: true, depthWrite: false, side: THREE.DoubleSide })
);
scaleSleeve.position.y = 1.15;
scaleSleeve.rotation.y = -Math.PI * 0.35;
syringeGroup.add(scaleSleeve);

// Tapered safety dispensing nozzle via LatheGeometry
const nozzlePoints = [
  new THREE.Vector2(0.04, 0.0),
  new THREE.Vector2(0.055, 0.24),
  new THREE.Vector2(0.18, 0.26),
  new THREE.Vector2(0.23, 0.29),
];
const nozzle = new THREE.Mesh(
  new THREE.LatheGeometry(nozzlePoints, 32),
  new THREE.MeshStandardMaterial({ color: 0xe6f3f5, roughness: 0.24, metalness: 0.05 })
);
nozzle.position.y = 0.01;
nozzle.castShadow = true;
syringeGroup.add(nozzle);

// Rounded silicone base cap / rest pad (touches y=0 cleanly)
const tipStand = new THREE.Mesh(
  new THREE.CylinderGeometry(0.32, 0.36, 0.06, 32),
  new THREE.MeshStandardMaterial({ color: 0x13828c, roughness: 0.38, metalness: 0.08 })
);
tipStand.position.y = 0.03;
tipStand.castShadow = true;
tipStand.receiveShadow = true;
syringeGroup.add(tipStand);

// Barrel finger flange collar
const collar = new THREE.Mesh(
  new THREE.CylinderGeometry(0.36, 0.36, 0.07, 32),
  new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.28 })
);
collar.position.y = 2.07;
collar.castShadow = true;
syringeGroup.add(collar);

// Dynamic colored liquid column inside barrel
const liquidGeo = new THREE.CylinderGeometry(0.195, 0.195, 1.0, 28);
liquidGeo.translate(0, 0.5, 0); // bottom-anchored for clean scaling from base
const liquidMesh = new THREE.Mesh(
  liquidGeo,
  new THREE.MeshStandardMaterial({ color: 0xff8a65, roughness: 0.18, metalness: 0.02, transparent: true, opacity: 0.9 })
);
liquidMesh.position.y = 0.27;
syringeGroup.add(liquidMesh);

// Plunger sub-group (gasket + shaft + thumb disk) moves cleanly with dose mL
const plungerGroup = new THREE.Group();
syringeGroup.add(plungerGroup);

const gasket = new THREE.Mesh(
  new THREE.CylinderGeometry(0.202, 0.202, 0.12, 28),
  new THREE.MeshStandardMaterial({ color: 0x1b434b, roughness: 0.68 })
);
plungerGroup.add(gasket);

const shaft = new THREE.Mesh(
  new THREE.CylinderGeometry(0.065, 0.065, 1.45, 20),
  new THREE.MeshStandardMaterial({ color: 0x1aa0aa, roughness: 0.34, metalness: 0.05 })
);
shaft.position.y = 0.78;
shaft.castShadow = true;
plungerGroup.add(shaft);

const thumbDisk = new THREE.Mesh(
  new THREE.CylinderGeometry(0.26, 0.24, 0.07, 32),
  new THREE.MeshStandardMaterial({ color: 0x13828c, roughness: 0.32 })
);
thumbDisk.position.y = 1.52;
thumbDisk.castShadow = true;
plungerGroup.add(thumbDisk);

// Glowing target meniscus ring for child weight-synced dose alignment
const targetRingMat = new THREE.MeshStandardMaterial({
  color: 0xf59e0b,
  emissive: 0xd97706,
  emissiveIntensity: 0.72,
  roughness: 0.25,
});
const targetRing = new THREE.Mesh(new THREE.TorusGeometry(0.258, 0.02, 12, 48), targetRingMat);
targetRing.rotation.x = Math.PI / 2;
targetRing.position.y = 0.27 + (0.14 + (5.0 / 20.0) * 1.48);
targetRing.visible = false;
syringeGroup.add(targetRing);

tagInteractive(syringeGroup, 'syringe');

// --- 1B. Amber Medicine Bottle Assembly (5 parts) ---
const bottleGroup = new THREE.Group();
bottleGroup.position.set(-1.35, 0, -0.25);
workbenchGroup.add(bottleGroup);
bottleGroup.add(createContactShadow(1.35, 1.35));

const bottleProfile = [
  new THREE.Vector2(0.0, 0.0),
  new THREE.Vector2(0.42, 0.0),
  new THREE.Vector2(0.45, 0.06),
  new THREE.Vector2(0.45, 1.02),
  new THREE.Vector2(0.38, 1.18),
  new THREE.Vector2(0.22, 1.30),
  new THREE.Vector2(0.22, 1.45),
];
const amberGlass = new THREE.Mesh(
  new THREE.LatheGeometry(bottleProfile, 40),
  new THREE.MeshPhysicalMaterial({
    color: 0x9c4a12,
    roughness: 0.15,
    metalness: 0.04,
    clearcoat: 0.7,
    clearcoatRoughness: 0.12,
  })
);
amberGlass.castShadow = true;
amberGlass.receiveShadow = true;
bottleGroup.add(amberGlass);

const bottleLabel = new THREE.Mesh(
  new THREE.CylinderGeometry(0.454, 0.454, 0.72, 40, 1, true),
  new THREE.MeshStandardMaterial({ map: createBottleLabelTexture(), roughness: 0.55 })
);
bottleLabel.position.y = 0.56;
bottleLabel.rotation.y = Math.PI * 0.28;
bottleGroup.add(bottleLabel);

const safetyCap = new THREE.Mesh(
  new THREE.CylinderGeometry(0.28, 0.28, 0.28, 36),
  new THREE.MeshStandardMaterial({ color: 0xfbfdfd, roughness: 0.32 })
);
safetyCap.position.y = 1.56;
safetyCap.castShadow = true;
bottleGroup.add(safetyCap);

const capRing = new THREE.Mesh(
  new THREE.TorusGeometry(0.26, 0.028, 12, 32),
  new THREE.MeshStandardMaterial({ color: 0x13828c, roughness: 0.36 })
);
capRing.rotation.x = Math.PI / 2;
capRing.position.y = 1.42;
bottleGroup.add(capRing);

tagInteractive(bottleGroup, 'bottle');

// --- 1C. Digital Pediatric Thermometer Assembly (5 parts) ---
const thermoGroup = new THREE.Group();
thermoGroup.position.set(1.38, 0, -0.18);
workbenchGroup.add(thermoGroup);
thermoGroup.add(createContactShadow(1.2, 1.2));

const dockBase = new THREE.Mesh(
  new THREE.CylinderGeometry(0.42, 0.46, 0.12, 32),
  new THREE.MeshStandardMaterial({ color: 0xdff2f4, roughness: 0.4 })
);
dockBase.position.y = 0.06;
dockBase.castShadow = true;
dockBase.receiveShadow = true;
thermoGroup.add(dockBase);

const thermoBody = new THREE.Mesh(
  new THREE.CapsuleGeometry(0.16, 1.02, 12, 24),
  new THREE.MeshPhysicalMaterial({
    color: 0xffffff,
    roughness: 0.18,
    metalness: 0.03,
    clearcoat: 0.85,
    clearcoatRoughness: 0.12,
  })
);
thermoBody.position.y = 0.85;
thermoBody.rotation.z = -0.12;
thermoBody.castShadow = true;
thermoGroup.add(thermoBody);

const probeTip = new THREE.Mesh(
  new THREE.ConeGeometry(0.065, 0.34, 24),
  new THREE.MeshStandardMaterial({ color: 0xd4dde0, roughness: 0.16, metalness: 0.92 })
);
probeTip.position.set(-0.08, 0.22, 0);
probeTip.rotation.z = Math.PI - 0.12;
thermoGroup.add(probeTip);

const lcdScreen = new THREE.Mesh(
  new THREE.PlaneGeometry(0.25, 0.46),
  new THREE.MeshBasicMaterial({ map: thermoTex })
);
lcdScreen.position.set(0.02, 0.94, 0.158);
lcdScreen.rotation.z = -Math.PI / 2 - 0.12;
thermoGroup.add(lcdScreen);

const feverRingMat = new THREE.MeshStandardMaterial({
  color: 0x1aa0aa,
  emissive: 0x0f7a68,
  emissiveIntensity: 0.25,
  roughness: 0.3,
});
const feverRing = new THREE.Mesh(new THREE.TorusGeometry(0.22, 0.022, 12, 32), feverRingMat);
feverRing.rotation.x = Math.PI / 2;
feverRing.position.y = 0.13;
thermoGroup.add(feverRing);

tagInteractive(thermoGroup, 'thermometer');

// =========================================================================
// SCENE 2: 3D PBR MANO HERO + 6 COLOR-CODED MANO SQUAD CHARACTERS
// =========================================================================
const manoRoot = new THREE.Group();
manoRoot.position.y = 0.12;
manoRoot.visible = false;
scene.add(manoRoot);
manoRoot.add(createContactShadow(2.38, 2.38));

const manoChar = new THREE.Group();
manoRoot.add(manoChar);

// Soft Sky-Mint Fill Aura lifting under-chin & joint shadows without specular bloom flare
const manoWarmLight = new THREE.PointLight(0xe2f8f5, MANO_RENDER_TUNING.bloom.warmAuraIntensity, 6.2, 1.4);
manoWarmLight.position.set(0.95, 2.15, 2.05);
manoRoot.add(manoWarmLight);

// Soft Frosted-Translucency + Pastel Turquoise Mix PBR Materials (Seamless Banner Harmony)
const manoAoMaterials = [];
const manoReflectionEntries = [];
const manoFrostedMixEntries = [];
function registerManoAoMaterial(mat, baseEnvIntensity) {
  mat.aoMapIntensity = MANO_RENDER_TUNING.ambientOcclusion.materialAoIntensity;
  manoAoMaterials.push(mat);
  if (typeof baseEnvIntensity === 'number') {
    mat.envMapIntensity = baseEnvIntensity;
    manoReflectionEntries.push({ mat, baseEnv: baseEnvIntensity });
  }
  return mat;
}
function registerManoEnvMaterial(mat, baseEnvIntensity) {
  mat.envMapIntensity = baseEnvIntensity;
  manoReflectionEntries.push({ mat, baseEnv: baseEnvIntensity });
  return mat;
}

// Hybrid Mix Material: blends a frosted translucent glass layer (transmission + micro-grain + sheen)
// with a subtle Fresnel pastel-turquoise glow layer so MANO harmonizes seamlessly with frosted-glass banners.
function createManoFrostedMixMaterial(params, baseEnvIntensity, mixOpts = {}) {
  const frostedCfg = MANO_RENDER_TUNING.frostedMix;
  const baseTransmission = typeof params.transmission === 'number' ? params.transmission : 0.34;
  const baseMixWeight = typeof mixOpts.mixWeight === 'number' ? mixOpts.mixWeight : 0.34;
  const glowMultiplier = typeof mixOpts.glowMultiplier === 'number' ? mixOpts.glowMultiplier : 1.0;

  const uniforms = {
    uFrostedTurquoiseColor: { value: new THREE.Color(mixOpts.turquoiseHex || frostedCfg.turquoiseColorLight) },
    uFrostedMistColor: { value: new THREE.Color(mixOpts.mistHex || frostedCfg.mistColorLight) },
    uFrostedMixWeight: { value: baseMixWeight },
    uFrostedGlowStrength: { value: frostedCfg.glowStrengthLight * glowMultiplier },
  };

  const mat = new THREE.MeshPhysicalMaterial(params);
  mat.customProgramCacheKey = () => 'mano-frosted-turquoise-mix-v1';
  mat.onBeforeCompile = (shader) => {
    shader.uniforms.uFrostedTurquoiseColor = uniforms.uFrostedTurquoiseColor;
    shader.uniforms.uFrostedMistColor = uniforms.uFrostedMistColor;
    shader.uniforms.uFrostedMixWeight = uniforms.uFrostedMixWeight;
    shader.uniforms.uFrostedGlowStrength = uniforms.uFrostedGlowStrength;

    shader.fragmentShader = shader.fragmentShader
      .replace(
        '#include <common>',
        `#include <common>
uniform vec3 uFrostedTurquoiseColor;
uniform vec3 uFrostedMistColor;
uniform float uFrostedMixWeight;
uniform float uFrostedGlowStrength;`
      )
      .replace(
        '#include <emissivemap_fragment>',
        `#include <emissivemap_fragment>
{
  vec3 frostedViewDir = normalize( vViewPosition );
  float frostedFresnel = pow( clamp( 1.0 - abs( dot( normalize( normal ), frostedViewDir ) ), 0.0, 1.0 ), 2.35 );
  float frostedMix = clamp( uFrostedMixWeight * ( 0.34 + 0.66 * frostedFresnel ), 0.0, 0.72 );
  vec3 frostedTint = mix( uFrostedMistColor, uFrostedTurquoiseColor, 0.46 );
  diffuseColor.rgb = mix( diffuseColor.rgb, frostedTint, frostedMix * 0.42 );
  totalEmissiveRadiance += uFrostedTurquoiseColor * ( frostedFresnel * uFrostedGlowStrength );
}`
      );
  };

  registerManoAoMaterial(mat, baseEnvIntensity);
  manoFrostedMixEntries.push({
    mat,
    uniforms,
    baseTransmission,
    baseMixWeight,
    glowMultiplier,
    customTurquoiseHex: mixOpts.turquoiseHex || null,
  });
  return mat;
}

registerManoEnvMaterial(plinthMat, MANO_RENDER_TUNING.reflection.plinthEnv);
registerManoEnvMaterial(plinthTrimMat, MANO_RENDER_TUNING.reflection.manoTrimEnv);

// Main MANO Ceramic-Frosted Shell (solid clinical pearl-white with subtle frosted-turquoise edge mist)
const manoPorcelainMat = createManoFrostedMixMaterial({
  color: 0xfbfdfc,
  emissive: 0xe6f7f5,
  emissiveIntensity: 0.038,
  roughness: 0.56,
  roughnessMap: microGrainTex,
  metalness: 0.0,
  transmission: 0.14,
  thickness: 0.42,
  attenuationColor: new THREE.Color(0xc4f2ec),
  attenuationDistance: 1.65,
  clearcoat: 0.08,
  clearcoatRoughness: 0.58,
  sheen: 0.86,
  sheenColor: new THREE.Color(0xeefbf9),
  sheenRoughness: 0.64,
  ior: 1.38,
  reflectivity: 0.15,
  transparent: true,
  opacity: 0.96,
  depthWrite: true,
}, MANO_RENDER_TUNING.reflection.manoPorcelainEnv, {
  mixWeight: 0.22,
  glowMultiplier: 0.58,
});

// Primary Frosted-Turquoise Hybrid Trim (replaces metallic collars, bezel, cuffs & ear rings)
const manoGoldMat = createManoFrostedMixMaterial({
  color: 0xaee6df,
  emissive: 0x5cc5bb,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.trimGlowLight,
  roughness: 0.50,
  roughnessMap: microGrainTex,
  metalness: 0.02,
  transmission: 0.34,
  thickness: 0.52,
  attenuationColor: new THREE.Color(0x7fdcd3),
  attenuationDistance: 1.32,
  clearcoat: 0.10,
  clearcoatRoughness: 0.54,
  sheen: 0.78,
  sheenColor: new THREE.Color(0xdcf8f4),
  sheenRoughness: 0.60,
  ior: 1.40,
  reflectivity: 0.18,
  transparent: true,
  opacity: 0.92,
  depthWrite: true,
}, MANO_RENDER_TUNING.reflection.manoTrimEnv, {
  mixWeight: 0.38,
  glowMultiplier: 1.0,
});

// Secondary Frosted Mist-Pearl & Turquoise Accent (replaces warm metallic rims & stage halo)
const manoAmberAccentMat = createManoFrostedMixMaterial({
  color: 0xdfeff0,
  emissive: 0x77cfc9,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.trimGlowLight,
  roughness: 0.50,
  roughnessMap: microGrainTex,
  metalness: 0.02,
  transmission: 0.36,
  thickness: 0.50,
  attenuationColor: new THREE.Color(0x9ce7e0),
  attenuationDistance: 1.40,
  clearcoat: 0.10,
  clearcoatRoughness: 0.54,
  sheen: 0.70,
  sheenColor: new THREE.Color(0xf0faf9),
  sheenRoughness: 0.62,
  ior: 1.39,
  reflectivity: 0.18,
  transparent: true,
  opacity: 0.91,
  depthWrite: true,
}, MANO_RENDER_TUNING.reflection.manoAccentEnv, {
  mixWeight: 0.34,
  glowMultiplier: 0.92,
});

const manoVisorMat = registerManoAoMaterial(new THREE.MeshPhysicalMaterial({
  color: 0x2d4c59,
  roughness: 0.36,
  metalness: 0.02,
  clearcoat: 0.16,
  clearcoatRoughness: 0.44,
  ior: 1.38,
  reflectivity: 0.20,
}), MANO_RENDER_TUNING.reflection.manoVisorEnv);

const manoFaceGlowMat = registerManoEnvMaterial(new THREE.MeshPhysicalMaterial({
  color: 0xb6f6ec,
  emissive: 0x80e4d6,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.faceGlowLight,
  roughness: 0.42,
  clearcoat: 0.06,
  clearcoatRoughness: 0.50,
}), MANO_RENDER_TUNING.reflection.manoSoftEnv);

const manoCheekBlushMat = registerManoEnvMaterial(new THREE.MeshPhysicalMaterial({
  color: 0xffbfd0,
  emissive: 0xf4a2b6,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.cheekBlushIntensity,
  roughness: 0.58,
  transparent: true,
  opacity: MANO_RENDER_TUNING.bloom.cheekBlushOpacity,
}), MANO_RENDER_TUNING.reflection.manoSoftEnv);

const manoCatchlightMat = new THREE.MeshBasicMaterial({
  color: 0xf7fcfc,
});

// Core Chest Emblem & Ear Cap Frosted-Glass Mix (luminous turquoise-frosted glass core)
const manoCoreEmblemMat = createManoFrostedMixMaterial({
  color: 0xcff2ee,
  emissive: 0x67cfc6,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.emblemGlowLight,
  roughness: 0.46,
  roughnessMap: microGrainTex,
  metalness: 0.02,
  transmission: 0.36,
  thickness: 0.48,
  attenuationColor: new THREE.Color(0x7adbd1),
  attenuationDistance: 1.25,
  clearcoat: 0.12,
  clearcoatRoughness: 0.50,
  sheen: 0.80,
  sheenColor: new THREE.Color(0xe6faf7),
  sheenRoughness: 0.58,
  ior: 1.40,
  reflectivity: 0.18,
  transparent: true,
  opacity: 0.92,
  depthWrite: true,
}, MANO_RENDER_TUNING.reflection.manoAccentEnv, {
  mixWeight: 0.36,
  glowMultiplier: 1.12,
});

const manoSproutMat = registerManoAoMaterial(new THREE.MeshPhysicalMaterial({
  color: 0x90d384,
  emissive: 0x5ca878,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.trimGlowLight,
  roughness: 0.54,
  metalness: 0.0,
  clearcoat: 0.08,
  clearcoatRoughness: 0.54,
  sheen: 0.78,
  sheenColor: new THREE.Color(0xeaf9cb),
  sheenRoughness: 0.62,
  reflectivity: 0.14,
}), MANO_RENDER_TUNING.reflection.manoSoftEnv);

const manoScarfMat = createManoFrostedMixMaterial({
  color: 0x8adbd1,
  emissive: 0x52bdb2,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.trimGlowLight,
  roughness: 0.54,
  roughnessMap: microGrainTex,
  metalness: 0.0,
  transmission: 0.28,
  thickness: 0.44,
  attenuationColor: new THREE.Color(0x8be4db),
  attenuationDistance: 1.38,
  clearcoat: 0.08,
  clearcoatRoughness: 0.56,
  sheen: 0.86,
  sheenColor: new THREE.Color(0xdaf9f4),
  sheenRoughness: 0.64,
  ior: 1.39,
  reflectivity: 0.15,
  transparent: true,
  opacity: 0.93,
  depthWrite: true,
}, MANO_RENDER_TUNING.reflection.manoSoftEnv, {
  mixWeight: 0.32,
  glowMultiplier: 0.88,
});

// Replaced Metallic Hardware Material -> Frosted-Glass + Soft Turquoise Glow Mix Material
const manoFrostedMixMat = createManoFrostedMixMaterial({
  color: 0xdff4f3,
  emissive: 0x6cd3c9,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.trimGlowLight,
  roughness: 0.48,
  roughnessMap: microGrainTex,
  metalness: 0.02,
  transmission: 0.38,
  thickness: 0.56,
  attenuationColor: new THREE.Color(0x88e2d9),
  attenuationDistance: 1.30,
  clearcoat: 0.12,
  clearcoatRoughness: 0.52,
  sheen: 0.82,
  sheenColor: new THREE.Color(0xe5faf8),
  sheenRoughness: 0.58,
  ior: 1.40,
  reflectivity: 0.18,
  transparent: true,
  opacity: 0.90,
  depthWrite: true,
}, MANO_RENDER_TUNING.reflection.manoMetalEnv, {
  mixWeight: 0.40,
  glowMultiplier: 1.05,
});
const metalMat = manoFrostedMixMat;

// Calm Seafoam Stage Halo Ring under MANO on the Pearl Plinth
const manoStageHalo = new THREE.Mesh(
  new THREE.TorusGeometry(0.68, 0.024, 14, 56),
  manoAmberAccentMat
);
manoStageHalo.rotation.x = Math.PI / 2;
manoStageHalo.position.y = 0.012;
manoRoot.add(manoStageHalo);

// Grounded warm ivory feet with dual-tone Energetic Orange + Emerald ankle collars
[-0.25, 0.25].forEach((x) => {
  const legColumn = new THREE.Mesh(new THREE.CapsuleGeometry(0.115, 0.18, 12, 18), manoPorcelainMat);
  legColumn.position.set(x, 0.32, 0.02);
  legColumn.castShadow = true;
  manoChar.add(legColumn);

  const foot = new THREE.Mesh(new THREE.SphereGeometry(0.19, 26, 20), manoPorcelainMat);
  foot.scale.set(1.02, 0.68, 1.34);
  foot.position.set(x, 0.125, 0.06);
  foot.castShadow = true;
  foot.receiveShadow = false;
  manoChar.add(foot);

  const footSole = new THREE.Mesh(new THREE.CylinderGeometry(0.17, 0.18, 0.03, 24), manoGoldMat);
  footSole.scale.set(1.0, 1.0, 1.25);
  footSole.position.set(x, 0.02, 0.06);
  manoChar.add(footSole);

  const ankleEmeraldRing = new THREE.Mesh(new THREE.TorusGeometry(0.138, 0.022, 10, 24), manoScarfMat);
  ankleEmeraldRing.rotation.x = Math.PI / 2;
  ankleEmeraldRing.position.set(x, 0.225, 0.04);
  manoChar.add(ankleEmeraldRing);

  const ankleOrangeRing = new THREE.Mesh(new THREE.TorusGeometry(0.142, 0.020, 10, 24), manoGoldMat);
  ankleOrangeRing.rotation.x = Math.PI / 2;
  ankleOrangeRing.position.set(x, 0.255, 0.04);
  manoChar.add(ankleOrangeRing);
});

// Warm porcelain torso + Sculpted 3D Orange-Gold "M+" Chest Emblem (MANO V1.0 Signature)
const torso = new THREE.Mesh(new THREE.SphereGeometry(0.56, 36, 28), manoPorcelainMat);
torso.scale.set(1.0, 1.08, 0.90);
torso.position.y = 0.78;
torso.castShadow = true;
torso.receiveShadow = false;
manoChar.add(torso);

// Warm orange-amber belt/waist accent line for extra visual pop
const waistTrim = new THREE.Mesh(new THREE.TorusGeometry(0.49, 0.016, 10, 40), manoAmberAccentMat);
waistTrim.rotation.x = Math.PI / 2;
waistTrim.scale.set(1.0, 0.89, 1.0);
waistTrim.position.set(0, 0.52, 0.0);
manoChar.add(waistTrim);

// Sculpted 3D "M+" Chest Logo Group
const chestLogoGroup = new THREE.Group();
chestLogoGroup.position.set(0, 0.83, 0.495);
chestLogoGroup.rotation.x = -0.08;
manoChar.add(chestLogoGroup);

// Subtle warm glowing badge plate behind the M+ emblem
const chestAuraRing = new THREE.Mesh(new THREE.TorusGeometry(0.21, 0.014, 12, 36), manoCoreEmblemMat);
chestAuraRing.position.set(0, 0.0, -0.015);
chestLogoGroup.add(chestAuraRing);

// Left & Right vertical pillars of "M"
[-0.105, 0.105].forEach((mx) => {
  const mPillar = new THREE.Mesh(new THREE.CapsuleGeometry(0.030, 0.15, 10, 16), manoGoldMat);
  mPillar.position.set(mx, -0.02, 0.01);
  mPillar.castShadow = false;
  chestLogoGroup.add(mPillar);
});

// Left & Right diagonal inner strokes of "M"
[-1, 1].forEach((dir) => {
  const mDiag = new THREE.Mesh(new THREE.CapsuleGeometry(0.026, 0.115, 10, 16), manoAmberAccentMat);
  mDiag.position.set(dir * 0.052, -0.005, 0.016);
  mDiag.rotation.z = dir * 0.56;
  mDiag.castShadow = false;
  chestLogoGroup.add(mDiag);
});

// Floating Orange-Gold Diamond Droplet above center of "M"
const mDiamond = new THREE.Mesh(new THREE.OctahedronGeometry(0.052, 0), manoCoreEmblemMat);
mDiamond.scale.set(0.95, 1.25, 0.55);
mDiamond.position.set(0, 0.115, 0.02);
chestLogoGroup.add(mDiamond);

// Emerald "+" Cross at top-right of "M"
const plusVert = new THREE.Mesh(new THREE.BoxGeometry(0.024, 0.076, 0.024), manoScarfMat);
plusVert.position.set(0.165, 0.105, 0.015);
const plusHoriz = new THREE.Mesh(new THREE.BoxGeometry(0.076, 0.024, 0.024), manoScarfMat);
plusHoriz.position.set(0.165, 0.105, 0.015);
chestLogoGroup.add(plusVert, plusHoriz);

// Dual-Tone Neck Collar Ring (Thick Vibrant Orange-Gold Upper + Emerald Lower Trim)
const stethCollar = new THREE.Mesh(new THREE.TorusGeometry(0.35, 0.042, 16, 40), manoGoldMat);
stethCollar.rotation.x = Math.PI * 0.5;
stethCollar.position.set(0, 1.31, 0.02);
stethCollar.castShadow = false;
manoChar.add(stethCollar);

const neckEmeraldCollar = new THREE.Mesh(new THREE.TorusGeometry(0.365, 0.024, 14, 36), manoScarfMat);
neckEmeraldCollar.rotation.x = Math.PI * 0.5;
neckEmeraldCollar.position.set(0, 1.265, 0.02);
manoChar.add(neckEmeraldCollar);

// Articulated Head Pivot Group (pivots softly at the neck collar for Soft Eye Tracking)
const MANO_HEAD_PIVOT_Y = 1.32;
const MANO_HEAD_PIVOT_Z = 0.02;
const manoHeadGroup = new THREE.Group();
manoHeadGroup.position.set(0, MANO_HEAD_PIVOT_Y, MANO_HEAD_PIVOT_Z);
manoChar.add(manoHeadGroup);

// Head chassis + Side Ear Pods + Visor Bezel + Articulated Glowing Ring-Eyes
const head = new THREE.Mesh(new THREE.SphereGeometry(0.50, 36, 28), manoPorcelainMat);
head.scale.set(1.12, 0.96, 0.95);
head.position.set(0, 1.72 - MANO_HEAD_PIVOT_Y, -MANO_HEAD_PIVOT_Z);
head.castShadow = true;
head.receiveShadow = false;
manoHeadGroup.add(head);

[-0.54, 0.54].forEach((ex) => {
  const sign = ex > 0 ? 1 : -1;
  const earPod = new THREE.Mesh(new THREE.CylinderGeometry(0.145, 0.145, 0.09, 28), manoPorcelainMat);
  earPod.rotation.z = Math.PI / 2;
  earPod.position.set(ex, 1.72 - MANO_HEAD_PIVOT_Y, 0.0);
  earPod.castShadow = false;
  manoHeadGroup.add(earPod);

  const earOrangeCap = new THREE.Mesh(new THREE.CylinderGeometry(0.105, 0.105, 0.025, 24), manoCoreEmblemMat);
  earOrangeCap.rotation.z = Math.PI / 2;
  earOrangeCap.position.set(ex + sign * 0.042, 1.72 - MANO_HEAD_PIVOT_Y, 0.0);
  manoHeadGroup.add(earOrangeCap);

  const earRing = new THREE.Mesh(new THREE.TorusGeometry(0.125, 0.022, 12, 28), manoGoldMat);
  earRing.rotation.y = Math.PI / 2;
  earRing.position.set(ex + sign * 0.036, 1.72 - MANO_HEAD_PIVOT_Y, 0.0);
  manoHeadGroup.add(earRing);
});

const visorBezel = new THREE.Mesh(new THREE.TorusGeometry(0.362, 0.052, 18, 48), manoGoldMat);
visorBezel.scale.set(1.18, 0.86, 1.05);
visorBezel.position.set(0, 1.72 - MANO_HEAD_PIVOT_Y, 0.365 - MANO_HEAD_PIVOT_Z);
visorBezel.castShadow = false;
manoHeadGroup.add(visorBezel);

const visorInnerAmberRim = new THREE.Mesh(new THREE.TorusGeometry(0.335, 0.018, 14, 44), manoAmberAccentMat);
visorInnerAmberRim.scale.set(1.18, 0.86, 1.0);
visorInnerAmberRim.position.set(0, 1.72 - MANO_HEAD_PIVOT_Y, 0.392 - MANO_HEAD_PIVOT_Z);
manoHeadGroup.add(visorInnerAmberRim);

const visorScreen = new THREE.Mesh(new THREE.SphereGeometry(0.415, 32, 24), manoVisorMat);
visorScreen.scale.set(0.96, 0.73, 0.56);
visorScreen.position.set(0, 1.72 - MANO_HEAD_PIVOT_Y, 0.255 - MANO_HEAD_PIVOT_Z);
manoHeadGroup.add(visorScreen);

// Articulated Eye Tracking Group (eyes glide smoothly across the curved visor ahead of the head)
const manoEyeGroup = new THREE.Group();
manoHeadGroup.add(manoEyeGroup);
const manoEyeNodes = [];

[-0.145, 0.145].forEach((x) => {
  const eyeAnchor = new THREE.Group();
  eyeAnchor.position.set(x, 1.755 - MANO_HEAD_PIVOT_Y, 0.476 - MANO_HEAD_PIVOT_Z);
  manoEyeGroup.add(eyeAnchor);
  manoEyeNodes.push(eyeAnchor);

  const eyeRing = new THREE.Mesh(new THREE.TorusGeometry(0.060, 0.016, 14, 32), manoFaceGlowMat);
  eyeAnchor.add(eyeRing);

  const catchlight = new THREE.Mesh(new THREE.SphereGeometry(0.019, 12, 12), manoCatchlightMat);
  catchlight.position.set(-0.024, 0.023, 0.012);
  eyeAnchor.add(catchlight);

  const secondaryDot = new THREE.Mesh(new THREE.SphereGeometry(0.009, 10, 10), manoCatchlightMat);
  secondaryDot.position.set(0.022, -0.023, 0.010);
  eyeAnchor.add(secondaryDot);
});

[-0.225, 0.225].forEach((cx) => {
  const cheekGlow = new THREE.Mesh(new THREE.SphereGeometry(0.032, 14, 12), manoCheekBlushMat);
  cheekGlow.scale.set(1.35, 0.72, 0.35);
  cheekGlow.position.set(cx, 1.668 - MANO_HEAD_PIVOT_Y, 0.458 - MANO_HEAD_PIVOT_Z);
  manoHeadGroup.add(cheekGlow);
});

const smileBaseY = 1.678 - MANO_HEAD_PIVOT_Y;
const smileBaseZ = 0.482 - MANO_HEAD_PIVOT_Z;
const smileCurve = new THREE.Mesh(
  new THREE.TorusGeometry(0.088, 0.017, 12, 28, Math.PI * 0.76),
  manoFaceGlowMat
);
smileCurve.rotation.z = Math.PI * 1.12;
smileCurve.position.set(0, smileBaseY, smileBaseZ);
manoHeadGroup.add(smileCurve);

// Signature MANO V1.0 Single Lush Curved Leaf Sprout + Base Mount attached to Head
const sproutCollar = new THREE.Mesh(new THREE.CylinderGeometry(0.076, 0.095, 0.062, 24), manoGoldMat);
sproutCollar.position.set(0, 2.20 - MANO_HEAD_PIVOT_Y, -MANO_HEAD_PIVOT_Z);
manoHeadGroup.add(sproutCollar);

const sproutInnerRing = new THREE.Mesh(new THREE.TorusGeometry(0.068, 0.016, 10, 24), manoAmberAccentMat);
sproutInnerRing.rotation.x = Math.PI / 2;
sproutInnerRing.position.set(0, 2.232 - MANO_HEAD_PIVOT_Y, -MANO_HEAD_PIVOT_Z);
manoHeadGroup.add(sproutInnerRing);

const sproutStem = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.028, 0.20, 14), manoSproutMat);
sproutStem.position.set(0.015, 2.30 - MANO_HEAD_PIVOT_Y, -MANO_HEAD_PIVOT_Z);
sproutStem.rotation.z = -0.18;
sproutStem.castShadow = false;
manoHeadGroup.add(sproutStem);

// Lush main sculpted leaf tilting gracefully to the right (matching MANO V1.0 reference sheet)
const mainLeaf = new THREE.Mesh(new THREE.SphereGeometry(0.135, 20, 16), manoSproutMat);
mainLeaf.scale.set(1.48, 0.38, 0.82);
mainLeaf.position.set(0.135, 2.41 - MANO_HEAD_PIVOT_Y, 0.01 - MANO_HEAD_PIVOT_Z);
mainLeaf.rotation.z = 0.38;
mainLeaf.rotation.y = -0.12;
mainLeaf.castShadow = false;
manoHeadGroup.add(mainLeaf);

const leafVein = new THREE.Mesh(new THREE.CapsuleGeometry(0.012, 0.19, 8, 12), manoAmberAccentMat);
leafVein.position.set(0.125, 2.415 - MANO_HEAD_PIVOT_Y, 0.032 - MANO_HEAD_PIVOT_Z);
leafVein.rotation.z = -1.18;
manoHeadGroup.add(leafVein);

// Helper to build articulated MANO V1.0 arm with shoulder rings, elbow band, dual wrist cuff, and 3D hand + fingers
function populateManoArm(armGroup, isRightArm) {
  const shoulderEmerald = new THREE.Mesh(new THREE.TorusGeometry(0.115, 0.022, 12, 24), manoScarfMat);
  shoulderEmerald.rotation.x = Math.PI / 2;
  armGroup.add(shoulderEmerald);

  const shoulderOrange = new THREE.Mesh(new THREE.SphereGeometry(0.112, 18, 14), manoGoldMat);
  armGroup.add(shoulderOrange);

  const armMesh = new THREE.Mesh(new THREE.CapsuleGeometry(0.106, 0.32, 14, 22), manoPorcelainMat);
  armMesh.position.y = -0.19;
  armMesh.castShadow = true;
  armGroup.add(armMesh);

  const elbowEmeraldRing = new THREE.Mesh(new THREE.TorusGeometry(0.106, 0.018, 10, 24), manoScarfMat);
  elbowEmeraldRing.rotation.x = Math.PI / 2;
  elbowEmeraldRing.position.y = -0.17;
  armGroup.add(elbowEmeraldRing);

  const wristOrangeCuff = new THREE.Mesh(new THREE.TorusGeometry(0.102, 0.022, 12, 24), manoGoldMat);
  wristOrangeCuff.rotation.x = Math.PI / 2;
  wristOrangeCuff.position.y = -0.34;
  armGroup.add(wristOrangeCuff);

  const wristEmeraldCuff = new THREE.Mesh(new THREE.TorusGeometry(0.098, 0.015, 10, 24), manoScarfMat);
  wristEmeraldCuff.rotation.x = Math.PI / 2;
  wristEmeraldCuff.position.y = -0.368;
  armGroup.add(wristEmeraldCuff);

  // Rounded 3D Hand Palm + Thumb + 3 Cute Finger Capsules
  const palm = new THREE.Mesh(new THREE.SphereGeometry(0.096, 18, 14), manoPorcelainMat);
  palm.scale.set(1.05, 0.92, 0.78);
  palm.position.y = -0.435;
  palm.castShadow = false;
  armGroup.add(palm);

  const thumbDir = isRightArm ? -1 : 1;
  const thumb = new THREE.Mesh(new THREE.CapsuleGeometry(0.026, 0.055, 8, 12), manoPorcelainMat);
  thumb.position.set(thumbDir * 0.088, -0.425, 0.025);
  thumb.rotation.z = thumbDir * 0.65;
  armGroup.add(thumb);

  [-0.045, 0.0, 0.045].forEach((fx) => {
    const finger = new THREE.Mesh(new THREE.CapsuleGeometry(0.024, 0.065, 8, 12), manoPorcelainMat);
    finger.position.set(fx, -0.515, 0.01);
    armGroup.add(finger);
  });
}

const leftArmGroup = new THREE.Group();
leftArmGroup.position.set(-0.57, 0.98, 0.02);
leftArmGroup.rotation.z = 0.44;
manoChar.add(leftArmGroup);
populateManoArm(leftArmGroup, false);

const rightArmGroup = new THREE.Group();
rightArmGroup.position.set(0.57, 1.05, 0.05);
rightArmGroup.rotation.z = -2.28;
manoChar.add(rightArmGroup);
populateManoArm(rightArmGroup, true);

tagInteractive(manoChar, 'mano');

// 6 Color-Coded Orbiting PBR Mini-MANO Squad Characters
const stationsRing = new THREE.Group();
stationsRing.position.y = 0.72;
manoRoot.add(stationsRing);

const squadConfigs = [
  { id: 'mano_drug', color: 0x5ab8ab, emissive: 0x6ecbc0, kind: 'drug', angle: 0 },
  { id: 'mano_growth', color: 0x6aa8d8, emissive: 0x84bce6, kind: 'growth', angle: Math.PI / 3 },
  { id: 'mano_skin', color: 0xe3948a, emissive: 0xefa9a0, kind: 'skin', angle: (2 * Math.PI) / 3 },
  { id: 'mano_nutrition', color: 0x8db874, emissive: 0xa2cb8b, kind: 'nutrition', angle: Math.PI },
  { id: 'mano_care', color: 0x62bcc7, emissive: 0x7fd0da, kind: 'care', angle: (4 * Math.PI) / 3 },
  { id: 'mano_connect', color: 0x9c88cf, emissive: 0xb19ee0, kind: 'connect', angle: (5 * Math.PI) / 3 },
];

const squadMeshesById = new Map();
squadConfigs.forEach((cfg, idx) => {
  const stGroup = new THREE.Group();
  const radius = 1.78;
  stGroup.position.set(Math.cos(cfg.angle) * radius, 0, Math.sin(cfg.angle) * radius);
  stGroup.userData.baseAngle = cfg.angle;
  stGroup.userData.accentColor = cfg.color;
  stGroup.userData.emissiveColor = cfg.emissive;

  const badgeBase = new THREE.Mesh(
    new THREE.CylinderGeometry(0.28, 0.30, 0.06, 28),
    plinthMat
  );
  badgeBase.castShadow = false;
  badgeBase.receiveShadow = false;
  stGroup.add(badgeBase);

  const suitMat = registerManoAoMaterial(new THREE.MeshPhysicalMaterial({
    color: cfg.color,
    emissive: cfg.emissive,
    emissiveIntensity: 0.06,
    roughness: 0.54,
    roughnessMap: microGrainTex,
    metalness: 0.02,
    clearcoat: 0.10,
    clearcoatRoughness: 0.56,
    sheen: 0.55,
    sheenRoughness: 0.64,
    ior: 1.36,
    reflectivity: 0.16,
  }), MANO_RENDER_TUNING.reflection.squadEnv);

  const rimMat = registerManoAoMaterial(new THREE.MeshPhysicalMaterial({
    color: cfg.emissive,
    emissive: cfg.emissive,
    emissiveIntensity: MANO_RENDER_TUNING.bloom.squadRimIdle,
    roughness: 0.50,
    metalness: 0.14,
    clearcoat: 0.08,
    clearcoatRoughness: 0.54,
  }), MANO_RENDER_TUNING.reflection.manoTrimEnv);
  const baseRing = new THREE.Mesh(new THREE.TorusGeometry(0.28, 0.016, 10, 28), rimMat);
  baseRing.rotation.x = Math.PI / 2;
  baseRing.position.y = 0.032;
  stGroup.add(baseRing);

  // Mini-MANO porcelain torso + color-coded dome helmet
  const miniBody = new THREE.Mesh(new THREE.SphereGeometry(0.155, 22, 18), manoPorcelainMat);
  miniBody.scale.set(1.0, 1.05, 0.92);
  miniBody.position.y = 0.21;
  miniBody.castShadow = true;
  stGroup.add(miniBody);

  const miniChestBand = new THREE.Mesh(new THREE.TorusGeometry(0.135, 0.022, 10, 24), suitMat);
  miniChestBand.rotation.x = Math.PI / 2;
  miniChestBand.position.y = 0.22;
  stGroup.add(miniChestBand);

  const miniHead = new THREE.Mesh(new THREE.SphereGeometry(0.15, 24, 18), suitMat);
  miniHead.scale.set(1.08, 0.96, 0.95);
  miniHead.position.y = 0.45;
  miniHead.castShadow = true;
  stGroup.add(miniHead);

  const miniVisor = new THREE.Mesh(new THREE.SphereGeometry(0.122, 20, 16), manoVisorMat);
  miniVisor.scale.set(0.98, 0.72, 0.56);
  miniVisor.position.set(0, 0.45, 0.088);
  stGroup.add(miniVisor);

  [-0.042, 0.042].forEach((ex) => {
    const miniEye = new THREE.Mesh(new THREE.SphereGeometry(0.016, 10, 8), manoFaceGlowMat);
    miniEye.position.set(ex, 0.46, 0.152);
    stGroup.add(miniEye);
  });

  // Role-distinct head topper emblem in brushed gold / glow
  if (cfg.kind === 'drug') {
    const plusV = new THREE.Mesh(new THREE.BoxGeometry(0.026, 0.085, 0.026), manoGoldMat);
    plusV.position.y = 0.64;
    const plusH = new THREE.Mesh(new THREE.BoxGeometry(0.085, 0.026, 0.026), manoGoldMat);
    plusH.position.y = 0.64;
    stGroup.add(plusV, plusH);
  } else if (cfg.kind === 'growth' || cfg.kind === 'nutrition') {
    const miniLeaf = new THREE.Mesh(new THREE.SphereGeometry(0.048, 14, 10), suitMat);
    miniLeaf.scale.set(1.3, 0.42, 0.72);
    miniLeaf.position.set(0.03, 0.63, 0);
    miniLeaf.rotation.z = 0.4;
    stGroup.add(miniLeaf);
  } else {
    const miniHalo = new THREE.Mesh(new THREE.TorusGeometry(0.078, 0.014, 10, 22), manoGoldMat);
    miniHalo.rotation.x = Math.PI / 2;
    miniHalo.position.y = 0.63;
    stGroup.add(miniHalo);
  }

  stGroup.userData.suitMat = suitMat;
  stGroup.userData.rimMat = rimMat;
  stGroup.userData.phase = idx * 1.047;
  tagInteractive(stGroup, cfg.id);
  stationsRing.add(stGroup);
  squadMeshesById.set(cfg.id, stGroup);
});

// =========================================================================
// SCENE 3: PEDIATRIC WEIGHING GUIDE (Digital Scale + 3D Child Mannequin)
// =========================================================================
const weighingRoot = new THREE.Group();
weighingRoot.position.y = 0.12;
weighingRoot.visible = false;
scene.add(weighingRoot);
weighingRoot.add(createContactShadow(3.0, 2.5));

// Dynamic Scale LCD CanvasTexture
const scaleLcdCanvas = document.createElement('canvas');
scaleLcdCanvas.width = 320;
scaleLcdCanvas.height = 140;
const scaleLcdCtx = scaleLcdCanvas.getContext('2d');
const scaleLcdTex = new THREE.CanvasTexture(scaleLcdCanvas);
scaleLcdTex.colorSpace = THREE.SRGBColorSpace;

let currentChildWeightKg = 11.5;
let weighingStep = 1;
let weighingPosture = 'toddler'; // 'infant' | 'toddler'

function drawScaleScreen(step, weightKg) {
  const isTare = step === 1;
  const isPrep = step === 2;
  const isCentering = step === 3;
  const isLocked = step === 4;

  scaleLcdCtx.fillStyle = isLocked ? '#e7fced' : (isTare ? '#e6fbf4' : '#fff8e8');
  scaleLcdCtx.fillRect(0, 0, 320, 140);
  scaleLcdCtx.strokeStyle = isLocked ? '#16a34a' : (isTare ? '#0f7a68' : '#d97706');
  scaleLcdCtx.lineWidth = 7;
  scaleLcdCtx.strokeRect(4, 4, 312, 132);

  let mainText = '0.00 kg';
  let subText = 'TARE READY';
  if (isPrep) {
    mainText = '0.00 kg';
    subText = 'LIGHT CLOTHES';
  } else if (isCentering) {
    mainText = `${(weightKg * 0.96).toFixed(1)} kg`;
    subText = 'CENTERING...';
  } else if (isLocked) {
    mainText = `${weightKg.toFixed(1)} kg`;
    subText = 'LOCKED ✓';
  }

  scaleLcdCtx.fillStyle = isLocked ? '#15803d' : (isTare ? '#0b6656' : '#9a5808');
  scaleLcdCtx.font = 'bold 54px sans-serif';
  scaleLcdCtx.fillText(mainText, 24, 78);
  scaleLcdCtx.font = 'bold 22px sans-serif';
  scaleLcdCtx.fillText(subText, 26, 116);
  scaleLcdTex.needsUpdate = true;
}
drawScaleScreen(1, currentChildWeightKg);

// Helper Side Mat & Clothes Basket (at x = -1.65)
const sideMatGroup = new THREE.Group();
sideMatGroup.position.set(-1.65, 0, 0.05);
weighingRoot.add(sideMatGroup);
sideMatGroup.add(createContactShadow(1.3, 1.3));

const helperMat = new THREE.Mesh(
  new THREE.CylinderGeometry(0.56, 0.6, 0.03, 36),
  new THREE.MeshStandardMaterial({ color: 0xdbeff2, roughness: 0.72 })
);
helperMat.position.y = 0.015;
helperMat.receiveShadow = true;
sideMatGroup.add(helperMat);

const clothesBasket = new THREE.Mesh(
  new THREE.BoxGeometry(0.58, 0.08, 0.42),
  new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.35 })
);
clothesBasket.position.set(0, 0.04, 0.62);
clothesBasket.castShadow = true;
clothesBasket.receiveShadow = true;
sideMatGroup.add(clothesBasket);

const foldedBasketJacket = new THREE.Mesh(
  new THREE.BoxGeometry(0.42, 0.06, 0.28),
  new THREE.MeshStandardMaterial({ color: 0xf28463, roughness: 0.52 })
);
foldedBasketJacket.position.set(0, 0.1, 0.62);
foldedBasketJacket.castShadow = true;
foldedBasketJacket.scale.setScalar(0.001);
foldedBasketJacket.visible = false;
sideMatGroup.add(foldedBasketJacket);

// Pediatric Digital Scale Assembly (at x = 0.28, z = 0.05)
const scaleGroup = new THREE.Group();
scaleGroup.position.set(0.28, 0, 0.05);
weighingRoot.add(scaleGroup);
scaleGroup.add(createContactShadow(2.05, 1.55));

[
  [-0.68, -0.44],
  [0.68, -0.44],
  [-0.68, 0.44],
  [0.68, 0.44],
].forEach(([fx, fz]) => {
  const foot = new THREE.Mesh(
    new THREE.CylinderGeometry(0.075, 0.085, 0.04, 20),
    new THREE.MeshStandardMaterial({ color: 0x264148, roughness: 0.75 })
  );
  foot.position.set(fx, 0.02, fz);
  foot.castShadow = true;
  scaleGroup.add(foot);
});

const scaleBase = new THREE.Mesh(
  new THREE.BoxGeometry(1.62, 0.11, 1.12),
  new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.28, metalness: 0.04 })
);
scaleBase.position.y = 0.095;
scaleBase.castShadow = true;
scaleBase.receiveShadow = true;
scaleGroup.add(scaleBase);

const loadCellNeck = new THREE.Mesh(
  new THREE.CylinderGeometry(0.32, 0.36, 0.05, 28),
  new THREE.MeshStandardMaterial({ color: 0xd3e8eb, roughness: 0.35 })
);
loadCellNeck.position.y = 0.175;
scaleGroup.add(loadCellNeck);

const scaleTray = new THREE.Mesh(
  new THREE.BoxGeometry(1.54, 0.055, 0.96),
  new THREE.MeshStandardMaterial({ color: 0xf7fcfd, roughness: 0.3 })
);
scaleTray.position.y = 0.22;
scaleTray.castShadow = true;
scaleTray.receiveShadow = true;
scaleGroup.add(scaleTray);

[-0.74, 0.74].forEach((rx) => {
  const rim = new THREE.Mesh(
    new THREE.CapsuleGeometry(0.042, 0.84, 8, 16),
    new THREE.MeshStandardMaterial({ color: 0x1aa0aa, roughness: 0.34 })
  );
  rim.rotation.x = Math.PI / 2;
  rim.position.set(rx, 0.255, 0);
  rim.castShadow = true;
  scaleGroup.add(rim);
});

const scaleCenterRingMat = new THREE.MeshStandardMaterial({
  color: 0x13828c,
  emissive: 0x0f7a68,
  emissiveIntensity: 0.35,
  roughness: 0.3,
});
const scaleCenterRing = new THREE.Mesh(
  new THREE.TorusGeometry(0.34, 0.018, 12, 48),
  scaleCenterRingMat
);
scaleCenterRing.rotation.x = Math.PI / 2;
scaleCenterRing.position.y = 0.252;
scaleGroup.add(scaleCenterRing);

const stabilityHaloMat = new THREE.MeshStandardMaterial({
  color: 0x16a34a,
  emissive: 0x15803d,
  emissiveIntensity: 0.65,
  roughness: 0.24,
});
const stabilityHalo = new THREE.Mesh(
  new THREE.TorusGeometry(0.52, 0.022, 12, 48),
  stabilityHaloMat
);
stabilityHalo.rotation.x = Math.PI / 2;
stabilityHalo.position.y = 1.62;
stabilityHalo.visible = false;
scaleGroup.add(stabilityHalo);

tagInteractive(scaleGroup, 'scale');

// Angled Front LCD Console
const lcdConsole = new THREE.Group();
lcdConsole.position.set(0, 0.145, 0.62);
lcdConsole.rotation.x = -0.52;
scaleGroup.add(lcdConsole);

const lcdHousing = new THREE.Mesh(
  new THREE.BoxGeometry(0.68, 0.30, 0.07),
  new THREE.MeshStandardMaterial({ color: 0x163038, roughness: 0.32 })
);
lcdHousing.castShadow = true;
lcdConsole.add(lcdHousing);

const scaleLcdMesh = new THREE.Mesh(
  new THREE.PlaneGeometry(0.60, 0.24),
  new THREE.MeshBasicMaterial({ map: scaleLcdTex })
);
scaleLcdMesh.position.z = 0.037;
lcdConsole.add(scaleLcdMesh);

tagInteractive(lcdConsole, 'scale_lcd');

// Soft Tare Blanket / Pad
const blanketGroup = new THREE.Group();
blanketGroup.position.set(1.55, 0.025, 0.22);
weighingRoot.add(blanketGroup);

const blanketPad = new THREE.Mesh(
  new THREE.BoxGeometry(0.92, 0.038, 0.64),
  new THREE.MeshPhysicalMaterial({
    color: 0xfbf3df,
    roughness: 0.72,
    sheen: 0.82,
    sheenColor: new THREE.Color(0xfffaf0),
    sheenRoughness: 0.65,
  })
);
blanketPad.castShadow = true;
blanketPad.receiveShadow = true;
blanketGroup.add(blanketPad);

const blanketTrim = new THREE.Mesh(
  new THREE.BoxGeometry(0.95, 0.02, 0.67),
  new THREE.MeshPhysicalMaterial({ color: 0xe6d4b3, roughness: 0.75, sheen: 0.65 })
);
blanketTrim.position.y = -0.008;
blanketGroup.add(blanketTrim);

tagInteractive(blanketGroup, 'blanket');

// Articulated 3D Child Mannequin (مانکن سه‌بعدی کودک) with soft PBR skin & textile sheen
const mannequinGroup = new THREE.Group();
mannequinGroup.position.set(-1.65, 0.03, 0.05);
weighingRoot.add(mannequinGroup);

const mannequinPosePivot = new THREE.Group();
mannequinGroup.add(mannequinPosePivot);

const mqSkinMat = new THREE.MeshPhysicalMaterial({
  color: 0xf6d8c7,
  roughness: 0.42,
  metalness: 0.01,
  clearcoat: 0.08,
  clearcoatRoughness: 0.45,
  sheen: 0.34,
  sheenColor: new THREE.Color(0xffe6d9),
  sheenRoughness: 0.55,
});
const mqJointMat = new THREE.MeshPhysicalMaterial({
  color: 0xe3baa4,
  roughness: 0.38,
  metalness: 0.02,
  sheen: 0.28,
  sheenColor: new THREE.Color(0xffdfd0),
});
const mqDiaperMat = new THREE.MeshPhysicalMaterial({
  color: 0x1aa0aa,
  roughness: 0.55,
  sheen: 0.68,
  sheenColor: new THREE.Color(0x9be6ec),
});
const mqHairMat = new THREE.MeshPhysicalMaterial({
  color: 0x7a5c4d,
  roughness: 0.52,
  sheen: 0.45,
  sheenColor: new THREE.Color(0xb89685),
});
const mqJacketMat = new THREE.MeshPhysicalMaterial({
  color: 0xf28463,
  roughness: 0.64,
  sheen: 0.85,
  sheenColor: new THREE.Color(0xffc3b2),
  sheenRoughness: 0.58,
  transparent: true,
  opacity: 1.0,
});
const eyeMat = new THREE.MeshPhysicalMaterial({ color: 0x10272d, roughness: 0.08, metalness: 0.1, clearcoat: 0.9 });
const cheekMat = new THREE.MeshPhysicalMaterial({ color: 0xff9f8e, roughness: 0.42, sheen: 0.35 });

// Articulated Left & Right Legs (feet bottom sits at local y = 0)
const mqLegs = [];
[-0.105, 0.105].forEach((lx) => {
  const legPivot = new THREE.Group();
  legPivot.position.set(lx, 0.44, 0);
  mannequinPosePivot.add(legPivot);

  const hipBall = new THREE.Mesh(new THREE.SphereGeometry(0.066, 16, 14), mqJointMat);
  legPivot.add(hipBall);

  const legCyl = new THREE.Mesh(new THREE.CapsuleGeometry(0.066, 0.24, 10, 16), mqSkinMat);
  legCyl.position.y = -0.21;
  legCyl.castShadow = true;
  legPivot.add(legCyl);

  const footMesh = new THREE.Mesh(new THREE.SphereGeometry(0.062, 18, 14), mqSkinMat);
  footMesh.scale.set(0.92, 0.56, 1.35);
  footMesh.position.set(0, -0.405, 0.035);
  footMesh.castShadow = true;
  legPivot.add(footMesh);

  mqLegs.push(legPivot);
});

// Pelvis / Light Dry Diaper Shorts
const mqPelvis = new THREE.Mesh(new THREE.SphereGeometry(0.185, 24, 20), mqDiaperMat);
mqPelvis.scale.set(1.06, 0.80, 0.88);
mqPelvis.position.y = 0.48;
mqPelvis.castShadow = true;
mannequinPosePivot.add(mqPelvis);

const mqWaistBand = new THREE.Mesh(
  new THREE.TorusGeometry(0.175, 0.02, 10, 28),
  new THREE.MeshStandardMaterial({ color: 0xffffff, roughness: 0.35 })
);
mqWaistBand.rotation.x = Math.PI / 2;
mqWaistBand.position.y = 0.58;
mannequinPosePivot.add(mqWaistBand);

// Upper Body + Head + Arms
const mqUpperBody = new THREE.Group();
mqUpperBody.position.y = 0.56;
mannequinPosePivot.add(mqUpperBody);

const mqTorso = new THREE.Mesh(new THREE.CapsuleGeometry(0.162, 0.24, 12, 24), mqSkinMat);
mqTorso.position.y = 0.21;
mqTorso.castShadow = true;
mqUpperBody.add(mqTorso);

const mqNeck = new THREE.Mesh(new THREE.CylinderGeometry(0.056, 0.064, 0.09, 16), mqJointMat);
mqNeck.position.y = 0.43;
mqUpperBody.add(mqNeck);

const mqHead = new THREE.Mesh(new THREE.SphereGeometry(0.20, 28, 22), mqSkinMat);
mqHead.position.y = 0.61;
mqHead.castShadow = true;
mqUpperBody.add(mqHead);

const mqHair = new THREE.Mesh(
  new THREE.SphereGeometry(0.206, 24, 16, 0, Math.PI * 2, 0, Math.PI * 0.54),
  mqHairMat
);
mqHair.position.set(0, 0.625, -0.012);
mqHair.rotation.x = -0.18;
mqUpperBody.add(mqHair);

[-0.072, 0.072].forEach((ex) => {
  const mEye = new THREE.Mesh(new THREE.SphereGeometry(0.023, 12, 10), eyeMat);
  mEye.position.set(ex, 0.62, 0.182);
  mqUpperBody.add(mEye);
});

[-0.118, 0.118].forEach((cx) => {
  const mCheek = new THREE.Mesh(new THREE.SphereGeometry(0.034, 12, 10), cheekMat);
  mCheek.scale.set(1.2, 0.7, 0.35);
  mCheek.position.set(cx, 0.56, 0.16);
  mqUpperBody.add(mCheek);
});

const mqArms = [];
[-0.205, 0.205].forEach((ax, idx) => {
  const shoulder = new THREE.Mesh(new THREE.SphereGeometry(0.056, 14, 12), mqJointMat);
  shoulder.position.set(ax, 0.35, 0);
  mqUpperBody.add(shoulder);

  const armPivot = new THREE.Group();
  armPivot.position.set(ax, 0.35, 0);
  armPivot.rotation.z = idx === 0 ? -0.18 : 0.18;
  mqUpperBody.add(armPivot);

  const armCyl = new THREE.Mesh(new THREE.CapsuleGeometry(0.05, 0.21, 10, 16), mqSkinMat);
  armCyl.position.y = -0.15;
  armCyl.castShadow = true;
  armPivot.add(armCyl);

  const handBall = new THREE.Mesh(new THREE.SphereGeometry(0.052, 14, 12), mqSkinMat);
  handBall.position.y = -0.30;
  armPivot.add(handBall);

  mqArms.push(armPivot);
});

// Removable Coral Sweater/Jacket Shell on Mannequin (lifts away in Step 2)
const wornJacketGroup = new THREE.Group();
mqUpperBody.add(wornJacketGroup);

const jacketVest = new THREE.Mesh(new THREE.CylinderGeometry(0.182, 0.194, 0.32, 24), mqJacketMat);
jacketVest.position.y = 0.20;
jacketVest.castShadow = true;
wornJacketGroup.add(jacketVest);

[-0.205, 0.205].forEach((sx, idx) => {
  const sleeve = new THREE.Mesh(new THREE.CylinderGeometry(0.068, 0.060, 0.20, 16), mqJacketMat);
  sleeve.position.set(sx + (idx === 0 ? -0.02 : 0.02), 0.25, 0);
  sleeve.rotation.z = idx === 0 ? -0.18 : 0.18;
  wornJacketGroup.add(sleeve);
});

tagInteractive(mannequinGroup, 'mannequin');

// Instanced Ambient Floating Pastel Motes (>24 particles using single InstancedMesh)
const MOTE_COUNT = 32;
const moteMat = registerManoEnvMaterial(new THREE.MeshPhysicalMaterial({
  color: 0xaee6ea,
  emissive: 0x78d6cf,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.moteGlowLight,
  roughness: 0.48,
  clearcoat: 0.12,
  clearcoatRoughness: 0.52,
  transparent: true,
  opacity: MANO_RENDER_TUNING.bloom.moteOpacity,
}), MANO_RENDER_TUNING.reflection.manoTrimEnv);
const moteMesh = new THREE.InstancedMesh(
  new THREE.IcosahedronGeometry(0.042, 0),
  moteMat,
  MOTE_COUNT
);
scene.add(moteMesh);
const moteSeeds = Array.from({ length: MOTE_COUNT }, (_, i) => ({
  r: 2.0 + (i % 5) * 0.45,
  y: 0.45 + ((i * 0.31) % 2.4),
  phase: i * 0.61,
  speed: 0.22 + (i % 4) * 0.08,
}));

// Serene Background Particle System: Layer 1 (Soft Floating Luminous Light Orbs — restrained bloom)
function createBokehGlowTexture() {
  const c = document.createElement('canvas');
  c.width = 64;
  c.height = 64;
  const ctx = c.getContext('2d');
  const g = ctx.createRadialGradient(32, 32, 2, 32, 32, 30);
  g.addColorStop(0.0, 'rgba(255, 255, 255, 0.82)');
  g.addColorStop(0.32, 'rgba(232, 250, 252, 0.56)');
  g.addColorStop(0.68, 'rgba(154, 226, 226, 0.16)');
  g.addColorStop(1.0, 'rgba(154, 226, 226, 0.0)');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, 64, 64);
  const tex = new THREE.CanvasTexture(c);
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

const LIGHT_PARTICLE_COUNT = 96;
const LIGHT_Y_MIN = 0.22;
const LIGHT_Y_SPAN = 5.1;
const lightPositions = new Float32Array(LIGHT_PARTICLE_COUNT * 3);
const lightColors = new Float32Array(LIGHT_PARTICLE_COUNT * 3);
const lightPalette = [
  new THREE.Color(0x6bd6ce), // Soft Pastel Teal
  new THREE.Color(0xa6ede8), // Mint Whisper
  new THREE.Color(0xc8ecff), // Soft Sky Mist
  new THREE.Color(0xfff1d6), // Warm Pearl
];
const lightSeeds = Array.from({ length: LIGHT_PARTICLE_COUNT }, (_, i) => {
  const angle = ((i * 2.399963) % (Math.PI * 2)); // golden-angle distribution
  const radius = 2.65 + ((i * 0.73) % 6.6);
  const yOffset = ((i * 0.41) % 1.0) * LIGHT_Y_SPAN;
  const paletteColor = lightPalette[i % lightPalette.length];
  lightColors[i * 3] = paletteColor.r;
  lightColors[i * 3 + 1] = paletteColor.g;
  lightColors[i * 3 + 2] = paletteColor.b;
  lightPositions[i * 3] = Math.cos(angle) * radius;
  lightPositions[i * 3 + 1] = LIGHT_Y_MIN + yOffset;
  lightPositions[i * 3 + 2] = Math.sin(angle) * radius;
  return {
    angle,
    radius,
    yOffset,
    riseSpeed: 0.075 + (i % 6) * 0.024,
    swayAmp: 0.14 + (i % 4) * 0.06,
    swayFreq: 0.35 + (i % 5) * 0.11,
    driftSpeed: (i % 2 === 0 ? 1 : -1) * (0.025 + (i % 4) * 0.012),
    phase: i * 0.87,
  };
});

const lightGeo = new THREE.BufferGeometry();
lightGeo.setAttribute('position', new THREE.BufferAttribute(lightPositions, 3));
lightGeo.setAttribute('color', new THREE.BufferAttribute(lightColors, 3));
const lightGlowTex = createBokehGlowTexture();
const lightParticleMat = new THREE.PointsMaterial({
  size: 0.20,
  map: lightGlowTex,
  vertexColors: true,
  transparent: true,
  opacity: MANO_RENDER_TUNING.bloom.bokehOpacityLight,
  depthWrite: false,
  sizeAttenuation: true,
  blending: THREE.NormalBlending,
});
const backgroundLightParticles = new THREE.Points(lightGeo, lightParticleMat);
backgroundLightParticles.frustumCulled = false;
scene.add(backgroundLightParticles);

// Serene Background Particle System: Layer 2 (Translucent Rising Frosted Micro-Bubbles)
const BUBBLE_COUNT = 44;
const BUBBLE_Y_MIN = 0.18;
const BUBBLE_Y_SPAN = 4.6;
const bubbleMat = registerManoEnvMaterial(new THREE.MeshPhysicalMaterial({
  color: 0xe6f8fa,
  emissive: 0x62cfc7,
  emissiveIntensity: MANO_RENDER_TUNING.bloom.bubbleGlowLight,
  roughness: 0.28,
  metalness: 0.02,
  clearcoat: 0.28,
  clearcoatRoughness: 0.34,
  iridescence: 0.20,
  transparent: true,
  opacity: MANO_RENDER_TUNING.bloom.bubbleOpacityLight,
  depthWrite: false,
}), MANO_RENDER_TUNING.reflection.plinthEnv);
const bubbleMesh = new THREE.InstancedMesh(
  new THREE.SphereGeometry(0.062, 14, 12),
  bubbleMat,
  BUBBLE_COUNT
);
bubbleMesh.frustumCulled = false;
scene.add(bubbleMesh);

const bubbleSeeds = Array.from({ length: BUBBLE_COUNT }, (_, i) => ({
  angle: ((i * 2.399963 + 0.55) % (Math.PI * 2)),
  radius: 2.85 + ((i * 0.91) % 4.9),
  yOffset: ((i * 0.29) % 1.0) * BUBBLE_Y_SPAN,
  riseSpeed: 0.10 + (i % 5) * 0.028,
  wobbleAmp: 0.12 + (i % 4) * 0.045,
  wobbleFreq: 0.62 + (i % 5) * 0.16,
  orbitSpeed: (i % 2 === 0 ? 1 : -1) * (0.03 + (i % 3) * 0.015),
  baseScale: 0.52 + (i % 5) * 0.18,
  phase: i * 1.13,
}));

// Pre-allocated scratch math objects (zero allocations in RAF loop)
const scratchVec = new THREE.Vector3();
const scratchTarget = new THREE.Vector3(0, 0.72, 0);
const desiredCamPos = new THREE.Vector3(0.3, 2.35, 5.2);
const desiredTarget = new THREE.Vector3(0, 0.72, 0);
const scratchMat = new THREE.Matrix4();
const scratchQuat = new THREE.Quaternion();
const scratchScale = new THREE.Vector3(1, 1, 1);

let activeScene = 'workbench';
let selectedSquadId = 'default';
let targetSyringeMl = 5.0;
let currentSyringeMl = 5.0;
let targetZoneMl = 0.0;
let feverMode = false;
let manoBounce = 0.0;
let bottleShakeTimer = 0.0;
let guidedStep = 0;
let cameraLerpActive = false;

// Adaptive WebGL performance profile for mid-range vs flagship Android hardware
let activePerfMode = 'auto';
let activeMoteCount = 24;
let activeLightCount = LIGHT_PARTICLE_COUNT;
let activeBubbleCount = BUBBLE_COUNT;
let activeFrameIntervalMs = 14;
let idleFrameIntervalMs = 33;

function detectMidRangeHardware() {
  const cores = typeof navigator !== 'undefined' && navigator.hardwareConcurrency ? navigator.hardwareConcurrency : 8;
  const memGb = typeof navigator !== 'undefined' && navigator.deviceMemory ? navigator.deviceMemory : 6;
  return cores <= 4 || memGb <= 4;
}

function applyPerformanceProfile(mode) {
  const normalized = mode === 'balanced' ? 'balanced' : (mode === 'high' ? 'high' : 'auto');
  activePerfMode = normalized;
  const effective = normalized === 'auto' ? (detectMidRangeHardware() ? 'balanced' : 'high') : normalized;
  const dpr = window.devicePixelRatio || 1;

  if (effective === 'balanced') {
    renderer.setPixelRatio(Math.min(dpr, 1.15));
    renderer.shadowMap.enabled = false;
    activeMoteCount = Math.min(MOTE_COUNT, 12);
    activeLightCount = Math.min(LIGHT_PARTICLE_COUNT, 42);
    activeBubbleCount = Math.min(BUBBLE_COUNT, 18);
    activeFrameIntervalMs = 22; // smooth ~45fps during touch/auto-rotate on mid-range GPUs
    idleFrameIntervalMs = 45;
  } else {
    renderer.setPixelRatio(Math.min(dpr, 1.75));
    renderer.shadowMap.enabled = true;
    activeMoteCount = MOTE_COUNT;
    activeLightCount = LIGHT_PARTICLE_COUNT;
    activeBubbleCount = BUBBLE_COUNT;
    activeFrameIntervalMs = 14;
    idleFrameIntervalMs = 33;
  }

  moteMesh.count = activeMoteCount;
  bubbleMesh.count = activeBubbleCount;
  lightGeo.setDrawRange(0, activeLightCount);
  renderer.setSize(window.innerWidth, window.innerHeight, false);
  if (typeof markInteractive === 'function') {
    markInteractive(800);
  }
}

function damp(current, target, k, dt) {
  return current + (target - current) * (1 - Math.exp(-k * dt));
}

function setCameraFocus(posArr, targetArr) {
  desiredCamPos.set(posArr[0], posArr[1], posArr[2]);
  desiredTarget.set(targetArr[0], targetArr[1], targetArr[2]);
  cameraLerpActive = true;
  if (typeof noteUserInteraction === 'function') {
    noteUserInteraction(1600);
  }
}

function applyWeighingCameraForStep(step) {
  if (step === 1) {
    setCameraFocus([0.55, 1.55, 3.75], [0.28, 0.35, 0.16]);
  } else if (step === 2) {
    setCameraFocus([-1.18, 1.48, 3.25], [-1.58, 0.55, 0.22]);
  } else if (step === 3) {
    setCameraFocus([0.42, 1.92, 3.55], [0.28, 0.68, 0.08]);
  } else {
    setCameraFocus([0.32, 1.45, 3.15], [0.28, 0.45, 0.30]);
  }
}

function applyManoAtmosphereIntensities(dark) {
  const aoCfg = MANO_RENDER_TUNING.ambientOcclusion;
  const reflCfg = MANO_RENDER_TUNING.reflection;
  const frostedCfg = MANO_RENDER_TUNING.frostedMix;
  const bloomCfg = MANO_RENDER_TUNING.bloom;

  const targetAo = aoCfg.materialAoIntensity * aoIntensityScale;
  floorMesh.material.aoMapIntensity = targetAo;
  plinthMat.aoMapIntensity = targetAo;
  plinthTrimMat.aoMapIntensity = targetAo;
  for (let i = 0; i < manoAoMaterials.length; i++) {
    manoAoMaterials[i].aoMapIntensity = targetAo;
  }

  const baseSceneEnv = dark ? reflCfg.sceneEnvIntensityDark : reflCfg.sceneEnvIntensityLight;
  scene.environmentIntensity = Math.max(0.0, Math.min(1.5, baseSceneEnv * envReflectionScale));
  const darkEnvFactor = dark ? 0.88 : 1.0;
  for (let i = 0; i < manoReflectionEntries.length; i++) {
    const entry = manoReflectionEntries[i];
    entry.mat.envMapIntensity = Math.max(
      0.0,
      Math.min(1.5, entry.baseEnv * darkEnvFactor * envReflectionScale)
    );
  }

  const targetTurquoiseHex = dark ? frostedCfg.turquoiseColorDark : frostedCfg.turquoiseColorLight;
  const targetMistHex = dark ? frostedCfg.mistColorDark : frostedCfg.mistColorLight;
  const baseFrostedGlow = (dark ? frostedCfg.glowStrengthDark : frostedCfg.glowStrengthLight) * bloomIntensityScale;
  for (let i = 0; i < manoFrostedMixEntries.length; i++) {
    const entry = manoFrostedMixEntries[i];
    if (!entry.customTurquoiseHex) {
      entry.uniforms.uFrostedTurquoiseColor.value.setHex(targetTurquoiseHex);
    }
    entry.uniforms.uFrostedMistColor.value.setHex(targetMistHex);
    entry.uniforms.uFrostedMixWeight.value = Math.max(
      0.0,
      Math.min(0.75, entry.baseMixWeight * frostedMixScale)
    );
    entry.uniforms.uFrostedGlowStrength.value = Math.max(
      0.0,
      Math.min(0.55, baseFrostedGlow * entry.glowMultiplier * frostedMixScale)
    );
    entry.mat.transmission = Math.max(
      0.0,
      Math.min(0.65, entry.baseTransmission * frostedCfg.transmissionScale * frostedMixScale)
    );
  }

  const baseContactOpacity = dark ? aoCfg.contactShadowOpacityDark : aoCfg.contactShadowOpacityLight;
  const targetContactOpacity = Math.max(0.0, Math.min(1.0, baseContactOpacity * aoIntensityScale));
  for (let i = 0; i < contactShadowMats.length; i++) {
    contactShadowMats[i].opacity = targetContactOpacity;
  }

  const trimGlow = (dark ? bloomCfg.trimGlowDark : bloomCfg.trimGlowLight) * bloomIntensityScale;
  plinthTrimMat.emissiveIntensity = trimGlow;
  manoGoldMat.emissiveIntensity = trimGlow;
  manoAmberAccentMat.emissiveIntensity = trimGlow;
  manoScarfMat.emissiveIntensity = trimGlow;
  manoSproutMat.emissiveIntensity = trimGlow;
  manoFrostedMixMat.emissiveIntensity = trimGlow;

  manoFaceGlowMat.emissiveIntensity =
    (dark ? bloomCfg.faceGlowDark : bloomCfg.faceGlowLight) * bloomIntensityScale;
  manoCheekBlushMat.emissiveIntensity = bloomCfg.cheekBlushIntensity * bloomIntensityScale;
  manoCheekBlushMat.opacity = Math.max(0.0, Math.min(1.0, bloomCfg.cheekBlushOpacity * bloomIntensityScale));
  manoCoreEmblemMat.emissiveIntensity =
    (dark ? bloomCfg.emblemGlowDark : bloomCfg.emblemGlowLight) * bloomIntensityScale;
  manoWarmLight.intensity = bloomCfg.warmAuraIntensity * (0.65 + 0.35 * bloomIntensityScale);

  moteMat.emissiveIntensity =
    (dark ? bloomCfg.moteGlowDark : bloomCfg.moteGlowLight) * bloomIntensityScale;
  moteMat.opacity = Math.max(0.0, Math.min(1.0, bloomCfg.moteOpacity * (0.65 + 0.35 * bloomIntensityScale)));

  lightParticleMat.blending = THREE.NormalBlending;
  lightParticleMat.opacity = Math.max(
    0.0,
    Math.min(1.0, (dark ? bloomCfg.bokehOpacityDark : bloomCfg.bokehOpacityLight) * bloomIntensityScale)
  );
  lightParticleMat.size = dark ? 0.21 : 0.19;
  lightParticleMat.needsUpdate = true;

  bubbleMat.color.setHex(dark ? 0xaaf0ee : 0xe6f8fa);
  bubbleMat.emissiveIntensity =
    (dark ? bloomCfg.bubbleGlowDark : bloomCfg.bubbleGlowLight) * bloomIntensityScale;
  bubbleMat.opacity = Math.max(
    0.0,
    Math.min(1.0, (dark ? bloomCfg.bubbleOpacityDark : bloomCfg.bubbleOpacityLight) * (0.6 + 0.4 * bloomIntensityScale))
  );
}

// Public bridge called from Android Kotlin via evaluateJavascript
window.Mano3D = {
  setScene(mode) {
    activeScene = mode === 'mano' ? 'mano' : (mode === 'weighing' ? 'weighing' : 'workbench');
    workbenchGroup.visible = activeScene === 'workbench';
    manoRoot.visible = activeScene === 'mano';
    weighingRoot.visible = activeScene === 'weighing';
    if (activeScene === 'mano') {
      manoBounce = 1.0;
      setCameraFocus([0.0, 2.15, 5.4], [0, 1.08, 0]);
    } else if (activeScene === 'weighing') {
      drawScaleScreen(weighingStep, currentChildWeightKg);
      applyWeighingCameraForStep(weighingStep);
    } else {
      setCameraFocus([0.3, 2.35, 5.2], [0, 0.72, 0]);
    }
  },
  setThemeMode(isDark) {
    const dark = Boolean(isDark);
    currentIsDarkTheme = dark;
    const bgHex = dark ? 0x08191f : 0xf4faff;
    const floorHex = dark ? 0x0d2128 : 0xeaf5fc;
    const plinthHex = dark ? 0x152f39 : 0xfdfefe;
    scene.background.setHex(bgHex);
    scene.fog.color.setHex(bgHex);
    floorMesh.material.color.setHex(floorHex);
    plinthMat.color.setHex(plinthHex);
    plinthMat.roughness = dark ? 0.44 : 0.48;
    plinthMat.clearcoat = dark ? 0.12 : 0.10;

    renderer.toneMappingExposure = dark
      ? MANO_RENDER_TUNING.bloom.exposureDark
      : MANO_RENDER_TUNING.bloom.exposureLight;

    keyLight.color.setHex(dark ? 0xf4fbff : 0xfffaf4);
    keyLight.intensity = dark ? 1.24 : 1.34;
    fillLight.color.setHex(dark ? 0x74c6d4 : 0xe4f7fa);
    fillLight.intensity = dark ? 0.84 : 1.08;
    rimLight.color.setHex(dark ? 0x86e8de : 0xf0fdff);
    rimLight.intensity = dark ? 0.96 : 0.86;

    hemiLight.color.setHex(dark ? 0x5aa3b2 : 0xf4fcff);
    hemiLight.groundColor.setHex(dark ? 0x16313b : 0xe2f2f4);
    hemiLight.intensity = dark ? 0.78 : 1.12;
    ambientLiftLight.color.setHex(dark ? 0x264b56 : 0xf6fcfd);
    ambientLiftLight.intensity = dark ? 0.26 : 0.34;

    applyManoAtmosphereIntensities(dark);

    document.documentElement.classList.toggle('dark-theme', dark);
    document.documentElement.style.background = dark ? '#08191f' : '#f4faff';
    document.body.style.background = dark ? '#08191f' : '#f4faff';
    markInteractive(900);
  },
  setLightingAtmosphere(options = {}) {
    if (typeof options.aoIntensity === 'number' && Number.isFinite(options.aoIntensity)) {
      aoIntensityScale = Math.max(0.0, Math.min(2.0, options.aoIntensity));
    }
    if (typeof options.bloomIntensity === 'number' && Number.isFinite(options.bloomIntensity)) {
      bloomIntensityScale = Math.max(0.0, Math.min(2.0, options.bloomIntensity));
    }
    if (
      typeof options.envReflectionIntensity === 'number' &&
      Number.isFinite(options.envReflectionIntensity)
    ) {
      envReflectionScale = Math.max(0.0, Math.min(2.0, options.envReflectionIntensity));
    }
    if (
      typeof options.frostedMixIntensity === 'number' &&
      Number.isFinite(options.frostedMixIntensity)
    ) {
      frostedMixScale = Math.max(0.0, Math.min(1.6, options.frostedMixIntensity));
    }
    applyManoAtmosphereIntensities(currentIsDarkTheme);
    markInteractive(600);
  },
  selectSquadMember(memberId) {
    selectedSquadId = squadMeshesById.has(memberId) ? memberId : 'default';
    if (activeScene !== 'mano') {
      window.Mano3D.setScene('mano');
    }
    manoBounce = 0.85;
    const picked = squadMeshesById.get(selectedSquadId);
    if (picked) {
      manoCoreEmblemMat.color.setHex(picked.userData.accentColor);
      manoCoreEmblemMat.emissive.setHex(picked.userData.emissiveColor);
      plinthTrimMat.emissive.setHex(picked.userData.emissiveColor);
    } else {
      manoCoreEmblemMat.color.setHex(0xcff2ee);
      manoCoreEmblemMat.emissive.setHex(0x67cfc6);
      plinthTrimMat.emissive.setHex(0x52b0a7);
    }
    noteUserInteraction(1600);
  },
  setSyringeMl(ml) {
    const clamped = Math.max(0.5, Math.min(20.0, Number(ml) || 5.0));
    targetSyringeMl = clamped;
  },
  setTargetMl(ml) {
    const val = Number(ml) || 0;
    if (val > 0) {
      targetZoneMl = Math.max(0.5, Math.min(20.0, val));
      const targetHeight = 0.14 + (targetZoneMl / 20.0) * 1.48;
      targetRing.position.y = 0.27 + targetHeight;
      targetRing.visible = true;
    } else {
      targetZoneMl = 0.0;
      targetRing.visible = false;
    }
  },
  triggerBottleShake() {
    if (activeScene !== 'workbench') {
      window.Mano3D.setScene('workbench');
    }
    bottleShakeTimer = 1.65;
  },
  setGuidedStep(step) {
    guidedStep = Number(step) || 0;
    if (activeScene !== 'workbench') {
      window.Mano3D.setScene('workbench');
    }
    if (guidedStep === 1) {
      setCameraFocus([0.3, 2.35, 5.2], [0, 0.72, 0]);
    } else if (guidedStep === 2) {
      bottleShakeTimer = 1.65;
      setCameraFocus([-1.05, 1.65, 2.75], [-1.35, 0.85, -0.25]);
    } else if (guidedStep === 3) {
      setCameraFocus([0.22, 1.75, 2.7], [0, 1.05, 0.32]);
    } else if (guidedStep === 4) {
      setCameraFocus([1.42, 1.55, 2.65], [1.38, 0.85, -0.18]);
    } else {
      window.Mano3D.resetCamera();
    }
  },
  setWeighingStep(step) {
    weighingStep = Math.max(1, Math.min(4, Number(step) || 1));
    if (activeScene !== 'weighing') {
      window.Mano3D.setScene('weighing');
    } else {
      drawScaleScreen(weighingStep, currentChildWeightKg);
      applyWeighingCameraForStep(weighingStep);
    }
  },
  setWeighingPosture(posture) {
    weighingPosture = posture === 'infant' ? 'infant' : 'toddler';
  },
  setChildWeightKg(kg) {
    const val = Number(kg);
    if (Number.isFinite(val) && val >= 2.0 && val <= 45.0) {
      currentChildWeightKg = val;
      drawScaleScreen(weighingStep, currentChildWeightKg);
    }
  },
  setFeverMode(enabled, tempC) {
    feverMode = Boolean(enabled);
    if (typeof tempC === 'number' && Number.isFinite(tempC)) {
      drawThermoScreen(feverMode, tempC);
    } else {
      currentThermoTempC = null;
      drawThermoScreen(feverMode, null);
    }
    feverRingMat.color.setHex(feverMode ? 0xe74c3c : 0x1aa0aa);
    feverRingMat.emissive.setHex(feverMode ? 0xc0392b : 0x0f7a68);
    feverRingMat.emissiveIntensity = feverMode ? 0.85 : 0.25;
  },
  focusTarget(partId) {
    if (partId === 'syringe') {
      window.Mano3D.setScene('workbench');
      setCameraFocus([0.2, 1.85, 3.0], [0, 1.05, 0.32]);
    } else if (partId === 'bottle') {
      window.Mano3D.setScene('workbench');
      setCameraFocus([-1.15, 1.65, 2.75], [-1.35, 0.85, -0.25]);
    } else if (partId === 'thermometer') {
      window.Mano3D.setScene('workbench');
      setCameraFocus([1.45, 1.55, 2.65], [1.38, 0.85, -0.18]);
    } else if (partId === 'mano') {
      window.Mano3D.setScene('mano');
    } else if (partId === 'weighing') {
      window.Mano3D.setScene('weighing');
    } else {
      window.Mano3D.resetCamera();
    }
  },
  resetCamera() {
    if (activeScene === 'mano') {
      setCameraFocus([0.0, 2.15, 5.4], [0, 1.08, 0]);
    } else if (activeScene === 'weighing') {
      setCameraFocus([0.1, 2.1, 4.9], [-0.25, 0.65, 0.1]);
    } else {
      setCameraFocus([0.3, 2.35, 5.2], [0, 0.72, 0]);
    }
  },
  setPaused(paused) {
    setScenePaused(Boolean(paused));
  },
  setPerformanceProfile(mode) {
    applyPerformanceProfile(mode);
  },
  setGazeTilt(nx, ny) {
    if (!eyeTrackingEnabled) return;
    if (typeof nx === 'number' && Number.isFinite(nx)) {
      tiltGazeX = Math.max(-1.0, Math.min(1.0, nx));
    }
    if (typeof ny === 'number' && Number.isFinite(ny)) {
      tiltGazeY = Math.max(-1.0, Math.min(1.0, ny));
    }
    nativeTiltActive = true;
    lastTiltGazeMs = performance.now();
  },
  setEyeTrackingEnabled(enabled) {
    eyeTrackingEnabled = Boolean(enabled);
    if (!eyeTrackingEnabled) {
      pointerGazeX = 0;
      pointerGazeY = 0;
      tiltGazeX = 0;
      tiltGazeY = 0;
    }
    markInteractive(600);
  },
};

const AUTO_ROTATE_IDLE_MS = 5000;
const AUTO_ROTATE_TARGET_SPEED = 0.85;
let lastUserInteractionMs = performance.now();
let isPointerInteracting = false;

let interactionBoostUntilMs = performance.now() + 1800;
function markInteractive(durationMs = 1500) {
  interactionBoostUntilMs = Math.max(interactionBoostUntilMs, performance.now() + durationMs);
}

function noteUserInteraction(durationMs = 1500) {
  lastUserInteractionMs = performance.now();
  controls.autoRotate = false;
  controls.autoRotateSpeed = 0.0;
  markInteractive(durationMs);
}

controls.addEventListener('start', () => {
  isPointerInteracting = true;
  noteUserInteraction(2500);
});
controls.addEventListener('change', () => {
  if (!controls.autoRotate) {
    markInteractive(1200);
  }
});
controls.addEventListener('end', () => {
  isPointerInteracting = false;
  noteUserInteraction(1400);
});

// Tap raycasting to focus / trigger character haptic feedback / notify Android
const raycaster = new THREE.Raycaster();
const pointerNdc = new THREE.Vector2();
let downX = 0;
let downY = 0;

function isCharacterPart(partId) {
  return partId === 'mano' || squadMeshesById.has(partId) || partId === 'mannequin';
}

function triggerCharacterHaptic(partId) {
  if (!isCharacterPart(partId)) return;
  if (window.AndroidBridge && typeof window.AndroidBridge.onCharacterTapped === 'function') {
    window.AndroidBridge.onCharacterTapped(partId);
    return;
  }
  try {
    if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
      navigator.vibrate(partId === 'mano' ? [18, 28, 22] : 14);
    }
  } catch (_) {
    // Ignore vibration errors in restricted browser contexts
  }
}

// Soft Eye Tracking state (pointer/touch + phone tilt + calm emotional presence & soft blink)
let eyeTrackingEnabled = true;
let pointerGazeX = 0.0;
let pointerGazeY = 0.0;
let lastPointerGazeMs = 0;
let tiltGazeX = 0.0;
let tiltGazeY = 0.0;
let lastTiltGazeMs = 0;
let nativeTiltActive = false;
let webOrientationBaseBeta = null;
let webOrientationBaseGamma = null;
let targetGazeX = 0.0;
let targetGazeY = 0.0;
let smoothEyeGazeX = 0.0;
let smoothEyeGazeY = 0.0;
let smoothHeadYaw = 0.0;
let smoothHeadPitch = 0.0;
let smoothHeadRoll = 0.0;
let blinkTimerSec = 2.6;
let blinkProgress = -1.0;

function updatePointerGazeFromClient(clientX, clientY) {
  if (!eyeTrackingEnabled) return;
  const rect = canvas.getBoundingClientRect();
  if (rect.width <= 0 || rect.height <= 0) return;
  const nx = ((clientX - rect.left) / rect.width) * 2 - 1;
  const ny = -(((clientY - rect.top) / rect.height) * 2 - 1);
  pointerGazeX = Math.max(-1.0, Math.min(1.0, nx));
  pointerGazeY = Math.max(-1.0, Math.min(1.0, ny));
  lastPointerGazeMs = performance.now();
}

// Built-in Web DeviceOrientation fallback when native Android sensor bridge is not driving tilt
if (typeof window !== 'undefined' && 'DeviceOrientationEvent' in window) {
  window.addEventListener(
    'deviceorientation',
    (e) => {
      if (!eyeTrackingEnabled || nativeTiltActive || activeScene !== 'mano') return;
      if (typeof e.gamma !== 'number' || typeof e.beta !== 'number') return;
      if (webOrientationBaseBeta === null || webOrientationBaseGamma === null) {
        webOrientationBaseBeta = e.beta;
        webOrientationBaseGamma = e.gamma;
      } else {
        // Slowly adapt baseline so holding the phone in a new posture stays natural
        webOrientationBaseBeta += (e.beta - webOrientationBaseBeta) * 0.012;
        webOrientationBaseGamma += (e.gamma - webOrientationBaseGamma) * 0.012;
      }
      const dGamma = e.gamma - webOrientationBaseGamma;
      const dBeta = e.beta - webOrientationBaseBeta;
      tiltGazeX = Math.max(-1.0, Math.min(1.0, dGamma / 26.0));
      tiltGazeY = Math.max(-1.0, Math.min(1.0, -dBeta / 24.0));
      lastTiltGazeMs = performance.now();
    },
    { passive: true }
  );
}

canvas.addEventListener('pointerdown', (e) => {
  downX = e.clientX;
  downY = e.clientY;
  cameraLerpActive = false;
  isPointerInteracting = true;
  updatePointerGazeFromClient(e.clientX, e.clientY);
  noteUserInteraction(2000);
});
canvas.addEventListener('pointermove', (e) => {
  if (activeScene === 'mano' && (isPointerInteracting || e.pointerType === 'mouse')) {
    updatePointerGazeFromClient(e.clientX, e.clientY);
  }
}, { passive: true });
canvas.addEventListener('pointercancel', () => {
  isPointerInteracting = false;
  noteUserInteraction(1200);
});
canvas.addEventListener('wheel', () => {
  noteUserInteraction(1500);
}, { passive: true });
canvas.addEventListener('pointerup', (e) => {
  isPointerInteracting = false;
  updatePointerGazeFromClient(e.clientX, e.clientY);
  noteUserInteraction(1400);
  const dx = e.clientX - downX;
  const dy = e.clientY - downY;
  if (dx * dx + dy * dy > 36) return; // ignore drags
  const rect = canvas.getBoundingClientRect();
  pointerNdc.x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
  pointerNdc.y = -((e.clientY - rect.top) / rect.height) * 2 + 1;
  raycaster.setFromCamera(pointerNdc, camera);
  const rootTargets =
    activeScene === 'mano'
      ? [manoRoot]
      : (activeScene === 'weighing' ? [weighingRoot] : [workbenchGroup]);
  const hits = raycaster.intersectObjects(rootTargets, true);
  for (const hit of hits) {
    const partId = hit.object.userData.partId;
    if (partId) {
      if (partId === 'mano') {
        window.Mano3D.selectSquadMember('default');
        manoBounce = 1.0;
        // Center MANO's warm gaze directly on the user when tapped directly
        pointerGazeX = 0.0;
        pointerGazeY = 0.08;
        lastPointerGazeMs = performance.now();
      } else if (squadMeshesById.has(partId)) {
        window.Mano3D.selectSquadMember(partId);
      }
      triggerCharacterHaptic(partId);
      noteUserInteraction(1600);
      if (window.AndroidBridge && typeof window.AndroidBridge.onPartSelected === 'function') {
        window.AndroidBridge.onPartSelected(partId);
      }
      break;
    }
  }
});

window.addEventListener('resize', () => {
  const w = window.innerWidth;
  const h = window.innerHeight;
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
  renderer.setSize(w, h, false);
  markInteractive(1000);
});

// Adaptive power-aware Animation Loop (60fps during interaction/auto-rotate, ~30fps idle, 0fps paused/hidden)
let lastMs = performance.now();
let lastRenderMs = 0;
let rafId = 0;
let scenePaused = false;

function setScenePaused(paused) {
  if (scenePaused === paused) return;
  scenePaused = paused;
  if (scenePaused) {
    if (rafId) {
      cancelAnimationFrame(rafId);
      rafId = 0;
    }
  } else {
    lastMs = performance.now();
    lastRenderMs = 0;
    noteUserInteraction(1200);
    if (!rafId && !document.hidden) {
      rafId = requestAnimationFrame(animate);
    }
  }
}

document.addEventListener('visibilitychange', () => {
  setScenePaused(document.hidden);
});

function isMotionActive(nowMs) {
  const gazeDelta =
    Math.abs(targetGazeX - smoothEyeGazeX) + Math.abs(targetGazeY - smoothEyeGazeY);
  return (
    nowMs < interactionBoostUntilMs ||
    controls.autoRotate ||
    cameraLerpActive ||
    bottleShakeTimer > 0 ||
    manoBounce > 0 ||
    (activeScene === 'mano' && (gazeDelta > 0.025 || blinkProgress >= 0.0)) ||
    Math.abs(currentSyringeMl - targetSyringeMl) > 0.02
  );
}

function animate(nowMs) {
  if (scenePaused || document.hidden) {
    rafId = 0;
    return;
  }
  rafId = requestAnimationFrame(animate);

  const minIntervalMs = isMotionActive(nowMs) ? activeFrameIntervalMs : idleFrameIntervalMs;
  if (lastRenderMs > 0 && nowMs - lastRenderMs < minIntervalMs) {
    return;
  }
  lastRenderMs = nowMs;

  const dt = Math.min(0.05, Math.max(0.001, (nowMs - lastMs) / 1000));
  lastMs = nowMs;
  const t = nowMs * 0.001;

  // Smooth oral syringe volume & plunger travel (0.5 mL .. 20.0 mL)
  currentSyringeMl = damp(currentSyringeMl, targetSyringeMl, 10, dt);
  const ratio = currentSyringeMl / 20.0;
  const colHeight = 0.14 + ratio * 1.48;
  liquidMesh.scale.y = colHeight;
  plungerGroup.position.y = 0.27 + colHeight + 0.06;

  // Target dose meniscus ring color & subtle breath pulse
  if (targetRing.visible) {
    const matched = Math.abs(currentSyringeMl - targetZoneMl) <= 0.25;
    targetRingMat.color.setHex(matched ? 0x16a34a : 0xf59e0b);
    targetRingMat.emissive.setHex(matched ? 0x15803d : 0xd97706);
    targetRing.scale.setScalar(1.0 + Math.sin(t * 4.5) * 0.035);
  }

  // Damped medicine bottle shake oscillation
  if (bottleShakeTimer > 0) {
    bottleShakeTimer = Math.max(0, bottleShakeTimer - dt);
    const decay = bottleShakeTimer / 1.65;
    bottleGroup.rotation.z = Math.sin(t * 28.0) * 0.16 * decay;
    bottleGroup.position.y = Math.abs(Math.sin(t * 14.0)) * 0.06 * decay;
  } else {
    bottleGroup.rotation.z = 0;
    bottleGroup.position.y = 0;
  }

  // Subtle fever pulse
  if (feverMode) {
    feverRing.scale.setScalar(1.0 + Math.sin(t * 5.0) * 0.08);
  } else {
    feverRing.scale.setScalar(1.0);
  }

  // 3D MANO gentle breath, bounce, Soft Eye Tracking (head + eyes + blink) & orbiting PBR squad
  if (activeScene === 'mano') {
    manoBounce = Math.max(0, manoBounce - dt * 2.2);
    const hop = Math.sin(manoBounce * Math.PI) * 0.26;
    manoChar.position.y = hop;
    const breath = 1.0 + Math.sin(t * 2.4) * 0.016;
    manoChar.scale.set(breath, 2.0 - breath, breath);
    rightArmGroup.rotation.z = -2.28 + Math.sin(t * 4.2) * 0.20;
    leftArmGroup.rotation.z = 0.48 + Math.sin(t * 2.4) * 0.04;

    // Soft Eye Tracking: blend user touch/pointer gaze, phone tilt gaze, and calm idle presence
    const eyeCfg = MANO_RENDER_TUNING.eyeTracking;
    if (eyeTrackingEnabled) {
      const pointerAge = nowMs - lastPointerGazeMs;
      const pointerWeight =
        lastPointerGazeMs > 0 && pointerAge < eyeCfg.pointerHoldMs
          ? Math.max(0.0, 1.0 - Math.pow(pointerAge / eyeCfg.pointerHoldMs, 1.8))
          : 0.0;
      const tiltAge = nowMs - lastTiltGazeMs;
      const tiltWeight =
        lastTiltGazeMs > 0 && tiltAge < eyeCfg.tiltHoldMs
          ? Math.max(0.0, 1.0 - Math.pow(tiltAge / eyeCfg.tiltHoldMs, 2.2))
          : 0.0;

      const idleX = Math.sin(t * 0.55) * eyeCfg.idleDriftAmpX + Math.sin(t * 1.13) * 0.03;
      const idleY = Math.cos(t * 0.42) * eyeCfg.idleDriftAmpY;

      const ambientX = idleX * (1.0 - tiltWeight * 0.7) + tiltGazeX * tiltWeight;
      const ambientY = idleY * (1.0 - tiltWeight * 0.7) + tiltGazeY * tiltWeight;

      targetGazeX = Math.max(-1.0, Math.min(1.0, ambientX * (1.0 - pointerWeight) + pointerGazeX * pointerWeight));
      targetGazeY = Math.max(-1.0, Math.min(1.0, ambientY * (1.0 - pointerWeight) + pointerGazeY * pointerWeight));
    } else {
      targetGazeX = 0.0;
      targetGazeY = 0.0;
    }

    // Eyes lead softly ahead of head movement for lifelike emotional attentiveness
    smoothEyeGazeX = damp(smoothEyeGazeX, targetGazeX, eyeCfg.eyeDamping, dt);
    smoothEyeGazeY = damp(smoothEyeGazeY, targetGazeY, eyeCfg.eyeDamping, dt);

    const desiredHeadYaw = targetGazeX * eyeCfg.maxHeadYaw;
    const desiredHeadPitch = -targetGazeY * eyeCfg.maxHeadPitch + Math.sin(manoBounce * Math.PI) * -0.08;
    const desiredHeadRoll = -targetGazeX * eyeCfg.maxHeadYaw * eyeCfg.headRollFactor;

    smoothHeadYaw = damp(smoothHeadYaw, desiredHeadYaw, eyeCfg.headDamping, dt);
    smoothHeadPitch = damp(smoothHeadPitch, desiredHeadPitch, eyeCfg.headDamping, dt);
    smoothHeadRoll = damp(smoothHeadRoll, desiredHeadRoll, eyeCfg.headDamping, dt);

    manoHeadGroup.rotation.set(smoothHeadPitch, smoothHeadYaw, smoothHeadRoll);

    // Eyes glide smoothly along curved visor surface without clipping
    const eyeShiftX = smoothEyeGazeX * eyeCfg.maxEyeShiftX;
    const eyeShiftY = smoothEyeGazeY * eyeCfg.maxEyeShiftY;
    const eyeCurveDepth = -0.007 * (smoothEyeGazeX * smoothEyeGazeX + smoothEyeGazeY * smoothEyeGazeY);
    manoEyeGroup.position.set(eyeShiftX, eyeShiftY, eyeCurveDepth);
    smileCurve.position.set(smoothEyeGazeX * eyeCfg.maxSmileShiftX, smileBaseY + eyeShiftY * 0.22, smileBaseZ);

    // Gentle emotional micro-blink every ~4.6–6.4 seconds
    if (blinkProgress < 0.0) {
      blinkTimerSec -= dt;
      if (blinkTimerSec <= 0.0) {
        blinkProgress = 0.0;
        blinkTimerSec = 4.6 + ((Math.sin(t * 7.3) + 1.0) * 0.9);
      }
    } else {
      blinkProgress += dt * 5.2; // ~0.19s gentle blink cycle
      if (blinkProgress >= 1.0) {
        blinkProgress = -1.0;
      }
    }
    const blinkScaleY =
      blinkProgress >= 0.0 ? 1.0 - Math.sin(blinkProgress * Math.PI) * 0.84 : 1.0;
    for (let e = 0; e < manoEyeNodes.length; e++) {
      manoEyeNodes[e].scale.set(1.0 + (1.0 - blinkScaleY) * 0.08, blinkScaleY, 1.0);
    }

    stationsRing.rotation.y = t * 0.30;
    for (let i = 0; i < stationsRing.children.length; i++) {
      const fig = stationsRing.children[i];
      const isSelected = fig.userData.partId === selectedSquadId;
      const targetScale = isSelected ? 1.24 : 1.0;
      const nextScale = damp(fig.scale.x, targetScale, 8, dt);
      fig.scale.setScalar(nextScale);
      const bobY = (isSelected ? 0.16 : 0.0) + Math.sin(t * 2.1 + fig.userData.phase) * 0.11;
      fig.position.y = damp(fig.position.y, bobY, 7, dt);
      fig.rotation.y = -stationsRing.rotation.y + Math.sin(t * 1.4 + i) * 0.16;
      if (fig.userData.rimMat) {
        const baseRim = isSelected
          ? MANO_RENDER_TUNING.bloom.squadRimSelected
          : MANO_RENDER_TUNING.bloom.squadRimIdle;
        fig.userData.rimMat.emissiveIntensity = baseRim * bloomIntensityScale;
      }
    }
  }

  // 3D Weighing Guide: animate blanket, jacket removal, mannequin posture & lock halo
  if (activeScene === 'weighing') {
    const isInfant = weighingPosture === 'infant';
    const onScale = weighingStep >= 3;
    const jacketRemoved = weighingStep >= 2;

    // Tare blanket smoothly slides onto scale center (y = 0.266)
    const targetBlanketX = 0.28;
    const targetBlanketY = 0.268;
    const targetBlanketZ = 0.05;
    blanketGroup.position.x = damp(blanketGroup.position.x, targetBlanketX, 7, dt);
    blanketGroup.position.y = damp(blanketGroup.position.y, targetBlanketY, 7, dt);
    blanketGroup.position.z = damp(blanketGroup.position.z, targetBlanketZ, 7, dt);

    // Removable coral jacket transitions smoothly to clothes basket in Steps 2-4
    mqJacketMat.opacity = damp(mqJacketMat.opacity, jacketRemoved ? 0.0 : 1.0, 9, dt);
    wornJacketGroup.position.y = damp(wornJacketGroup.position.y, jacketRemoved ? 0.35 : 0.0, 7, dt);
    wornJacketGroup.visible = mqJacketMat.opacity > 0.03;

    foldedBasketJacket.visible = jacketRemoved;
    const targetBasketScale = jacketRemoved ? 1.0 : 0.001;
    const nextBasketScale = damp(foldedBasketJacket.scale.x, targetBasketScale, 9, dt);
    foldedBasketJacket.scale.setScalar(nextBasketScale);

    // Mannequin target position, scale & supine/standing posture
    const mqTargetScale = isInfant ? 0.84 : 1.0;
    const nextMqScale = damp(mannequinGroup.scale.x, mqTargetScale, 8, dt);
    mannequinGroup.scale.setScalar(nextMqScale);

    const mqTargetX = onScale ? (isInfant ? 0.79 : 0.28) : (isInfant ? -1.16 : -1.65);
    const mqTargetY = onScale ? (isInfant ? 0.425 : 0.288) : (isInfant ? 0.165 : 0.03);
    const mqTargetZ = 0.05;
    mannequinGroup.position.x = damp(mannequinGroup.position.x, mqTargetX, 6.5, dt);
    mannequinGroup.position.y = damp(mannequinGroup.position.y, mqTargetY, 6.5, dt);
    mannequinGroup.position.z = damp(mannequinGroup.position.z, mqTargetZ, 6.5, dt);

    const targetGroupRotY = isInfant ? Math.PI / 2 : (onScale ? 0.0 : 0.28);
    const targetPivotRotX = isInfant ? -Math.PI / 2 : 0.0;
    mannequinGroup.rotation.y = damp(mannequinGroup.rotation.y, targetGroupRotY, 7.5, dt);
    mannequinPosePivot.rotation.x = damp(mannequinPosePivot.rotation.x, targetPivotRotX, 7.5, dt);

    // Gentle mannequin breathing & relaxed arms/legs
    const breathArm = Math.sin(t * 2.2) * 0.03;
    mqArms[0].rotation.z = -0.16 + breathArm;
    mqArms[1].rotation.z = 0.16 - breathArm;
    mqLegs[0].rotation.x = isInfant ? 0.08 : 0.0;
    mqLegs[1].rotation.x = isInfant ? 0.08 : 0.0;

    // Scale center alignment ring & Step 4 locked stability halo
    const isCentered = weighingStep >= 3;
    scaleCenterRingMat.color.setHex(isCentered ? 0x16a34a : 0x13828c);
    scaleCenterRingMat.emissive.setHex(isCentered ? 0x15803d : 0x0f7a68);
    scaleCenterRing.scale.setScalar(1.0 + Math.sin(t * 4.0) * 0.035);

    stabilityHalo.visible = weighingStep === 4;
    if (stabilityHalo.visible) {
      stabilityHalo.position.y = (isInfant ? 0.85 : 1.62) + Math.sin(t * 3.0) * 0.04;
      stabilityHalo.scale.setScalar(1.0 + Math.sin(t * 4.2) * 0.04);
    }
  }

  // Update ambient floating motes via InstancedMesh without per-frame allocations
  for (let i = 0; i < activeMoteCount; i++) {
    const s = moteSeeds[i];
    const ang = t * s.speed + s.phase;
    scratchVec.set(
      Math.cos(ang) * s.r,
      s.y + Math.sin(t * 1.4 + s.phase) * 0.18,
      Math.sin(ang) * s.r
    );
    scratchScale.setScalar(0.85 + 0.25 * Math.sin(t * 2.0 + s.phase));
    scratchMat.compose(scratchVec, scratchQuat, scratchScale);
    moteMesh.setMatrixAt(i, scratchMat);
  }
  moteMesh.instanceMatrix.needsUpdate = true;

  // Update serene background luminous light orbs (gentle rise + soft horizontal sway)
  for (let i = 0; i < activeLightCount; i++) {
    const s = lightSeeds[i];
    const ang = s.angle + t * s.driftSpeed;
    const sway = Math.sin(t * s.swayFreq + s.phase) * s.swayAmp;
    const r = s.radius + sway;
    const yProgress = (s.yOffset + t * s.riseSpeed) % LIGHT_Y_SPAN;
    const idx3 = i * 3;
    lightPositions[idx3] = Math.cos(ang) * r;
    lightPositions[idx3 + 1] = LIGHT_Y_MIN + yProgress;
    lightPositions[idx3 + 2] = Math.sin(ang) * r;
  }
  lightGeo.attributes.position.needsUpdate = true;

  // Update translucent rising background micro-bubbles with soft spawn/fade envelope
  for (let i = 0; i < activeBubbleCount; i++) {
    const s = bubbleSeeds[i];
    const ang = s.angle + t * s.orbitSpeed;
    const wobbleX = Math.sin(t * s.wobbleFreq + s.phase) * s.wobbleAmp;
    const wobbleZ = Math.cos(t * s.wobbleFreq * 0.85 + s.phase) * s.wobbleAmp;
    const yProgress = (s.yOffset + t * s.riseSpeed) % BUBBLE_Y_SPAN;
    const normY = yProgress / BUBBLE_Y_SPAN;
    // Smooth fade-in at bottom and graceful fade-out near top
    const lifeEnvelope = Math.sin(normY * Math.PI);
    const breathPulse = 0.90 + 0.12 * Math.sin(t * 1.7 + s.phase);
    const sc = Math.max(0.001, s.baseScale * Math.pow(lifeEnvelope, 0.45) * breathPulse);
    scratchVec.set(
      Math.cos(ang) * s.radius + wobbleX,
      BUBBLE_Y_MIN + yProgress,
      Math.sin(ang) * s.radius + wobbleZ
    );
    scratchScale.setScalar(sc);
    scratchMat.compose(scratchVec, scratchQuat, scratchScale);
    bubbleMesh.setMatrixAt(i, scratchMat);
  }
  bubbleMesh.instanceMatrix.needsUpdate = true;

  if (cameraLerpActive) {
    camera.position.x = damp(camera.position.x, desiredCamPos.x, 7, dt);
    camera.position.y = damp(camera.position.y, desiredCamPos.y, 7, dt);
    camera.position.z = damp(camera.position.z, desiredCamPos.z, 7, dt);
    controls.target.x = damp(controls.target.x, desiredTarget.x, 7, dt);
    controls.target.y = damp(controls.target.y, desiredTarget.y, 7, dt);
    controls.target.z = damp(controls.target.z, desiredTarget.z, 7, dt);
    if (camera.position.distanceTo(desiredCamPos) < 0.025) {
      cameraLerpActive = false;
    }
  }

  const shouldAutoRotate =
    !isPointerInteracting &&
    !cameraLerpActive &&
    nowMs - lastUserInteractionMs >= AUTO_ROTATE_IDLE_MS;

  if (shouldAutoRotate) {
    controls.autoRotate = true;
    controls.autoRotateSpeed = damp(controls.autoRotateSpeed, AUTO_ROTATE_TARGET_SPEED, 2.2, dt);
  } else {
    controls.autoRotate = false;
    controls.autoRotateSpeed = 0.0;
  }

  controls.update(dt);
  renderer.render(scene, camera);
  const loaderEl = document.getElementById('stage-loader');
  if (loaderEl && !loaderEl.classList.contains('hidden')) {
    loaderEl.classList.add('hidden');
  }
}

applyPerformanceProfile('auto');
rafId = requestAnimationFrame(animate);
