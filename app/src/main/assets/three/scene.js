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

renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.75));
renderer.setSize(window.innerWidth, window.innerHeight, false);
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.05;
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;

const scene = new THREE.Scene();
scene.background = new THREE.Color(0xf4faff);
scene.fog = new THREE.FogExp2(0xf4faff, 0.024);

const pmrem = new THREE.PMREMGenerator(renderer);
const roomEnv = new RoomEnvironment();
scene.environment = pmrem.fromScene(roomEnv, 0.045).texture;
scene.environmentIntensity = 0.78;
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

// 3-Point Soft Studio Lighting Rig (Key + Aqua Fill + Luminous Back Rim + Hemisphere)
const keyLight = new THREE.DirectionalLight(0xfff8ef, 2.05);
keyLight.position.set(4.2, 7.2, 4.6);
keyLight.castShadow = true;
keyLight.shadow.mapSize.set(1536, 1536);
keyLight.shadow.radius = 4.5;
keyLight.shadow.camera.left = -4.8;
keyLight.shadow.camera.right = 4.8;
keyLight.shadow.camera.top = 4.8;
keyLight.shadow.camera.bottom = -4.8;
keyLight.shadow.camera.near = 0.5;
keyLight.shadow.camera.far = 19;
keyLight.shadow.bias = -0.0004;
keyLight.shadow.normalBias = 0.022;
scene.add(keyLight);

const fillLight = new THREE.DirectionalLight(0xd9f4f8, 0.78);
fillLight.position.set(-4.6, 3.8, 3.6);
scene.add(fillLight);

const rimLight = new THREE.DirectionalLight(0xe9fcff, 0.76);
rimLight.position.set(-1.6, 4.4, -5.2);
scene.add(rimLight);

const hemiLight = new THREE.HemisphereLight(0xe8f9fc, 0xc5dbd9, 0.74);
scene.add(hemiLight);

// Ground clinic studio stage & radial contact shadow generator
function makeRadialShadowTexture() {
  const c = document.createElement('canvas');
  c.width = 128;
  c.height = 128;
  const ctx = c.getContext('2d');
  const g = ctx.createRadialGradient(64, 64, 6, 64, 64, 62);
  g.addColorStop(0, 'rgba(10, 32, 38, 0.34)');
  g.addColorStop(0.50, 'rgba(10, 32, 38, 0.13)');
  g.addColorStop(1, 'rgba(10, 32, 38, 0.0)');
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
    opacity: 0.78,
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
  new THREE.MeshStandardMaterial({ color: 0xe6f3fe, roughness: 0.90, metalness: 0.0 })
);
floorMesh.rotation.x = -Math.PI / 2;
floorMesh.receiveShadow = true;
scene.add(floorMesh);

// Ground-flush clearcoated ceramic studio plinth + vibrant orange-amber perimeter trim
const plinthMat = new THREE.MeshPhysicalMaterial({
  color: 0xffffff,
  roughness: 0.20,
  roughnessMap: microGrainTex,
  metalness: 0.03,
  clearcoat: 0.65,
  clearcoatRoughness: 0.14,
  envMapIntensity: 1.0,
});
const plinth = new THREE.Mesh(
  new THREE.CylinderGeometry(2.8, 2.96, 0.12, 64),
  plinthMat
);
plinth.position.y = 0.06;
plinth.receiveShadow = true;
scene.add(plinth);

const plinthTrimMat = new THREE.MeshPhysicalMaterial({
  color: 0xff7a18,
  emissive: 0xff5500,
  emissiveIntensity: 0.28,
  roughness: 0.20,
  metalness: 0.82,
  clearcoat: 0.55,
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
manoRoot.add(createContactShadow(2.15, 2.15));

const manoChar = new THREE.Group();
manoRoot.add(manoChar);

// Warm Orange-Amber Spotlight Aura to give MANO vibrant contrast against the white/sky-blue stage
const manoWarmLight = new THREE.PointLight(0xff8c2b, 1.35, 5.5, 1.6);
manoWarmLight.position.set(0.85, 2.15, 1.85);
manoRoot.add(manoWarmLight);

// Signature PBR Materials for Hero MANO (Warm Ivory Shell + Energetic Orange-Amber & Mango-Gold Palette)
const manoPorcelainMat = new THREE.MeshPhysicalMaterial({
  color: 0xfffaf2,
  emissive: 0xff9e44,
  emissiveIntensity: 0.04,
  roughness: 0.14,
  roughnessMap: microGrainTex,
  metalness: 0.03,
  clearcoat: 1.0,
  clearcoatRoughness: 0.05,
  sheen: 0.38,
  sheenColor: new THREE.Color(0xffe0c2),
  sheenRoughness: 0.35,
  ior: 1.52,
  reflectivity: 0.92,
  envMapIntensity: 1.2,
});

const manoGoldMat = new THREE.MeshPhysicalMaterial({
  color: 0xff6d00,
  emissive: 0xff4800,
  emissiveIntensity: 0.38,
  roughness: 0.16,
  metalness: 0.82,
  clearcoat: 0.78,
  clearcoatRoughness: 0.08,
  envMapIntensity: 1.55,
});

const manoAmberAccentMat = new THREE.MeshPhysicalMaterial({
  color: 0xffab00,
  emissive: 0xff7700,
  emissiveIntensity: 0.46,
  roughness: 0.15,
  metalness: 0.76,
  clearcoat: 0.85,
  clearcoatRoughness: 0.06,
  envMapIntensity: 1.5,
});

const manoVisorMat = new THREE.MeshPhysicalMaterial({
  color: 0x071210,
  roughness: 0.03,
  metalness: 0.12,
  clearcoat: 1.0,
  clearcoatRoughness: 0.02,
  ior: 1.58,
  reflectivity: 1.0,
  envMapIntensity: 1.45,
});

const manoFaceGlowMat = new THREE.MeshPhysicalMaterial({
  color: 0x45f79f,
  emissive: 0x26e885,
  emissiveIntensity: 1.55,
  roughness: 0.14,
  clearcoat: 0.45,
});

const manoCheekBlushMat = new THREE.MeshPhysicalMaterial({
  color: 0xff7043,
  emissive: 0xff5722,
  emissiveIntensity: 0.95,
  roughness: 0.25,
  transparent: true,
  opacity: 0.82,
});

const manoCatchlightMat = new THREE.MeshBasicMaterial({
  color: 0xffffff,
});

const manoCoreEmblemMat = new THREE.MeshPhysicalMaterial({
  color: 0xff7a18,
  emissive: 0xff5500,
  emissiveIntensity: 0.82,
  roughness: 0.16,
  metalness: 0.48,
  clearcoat: 0.90,
});

const manoSproutMat = new THREE.MeshPhysicalMaterial({
  color: 0x68b818,
  emissive: 0x2f7a08,
  emissiveIntensity: 0.20,
  roughness: 0.24,
  metalness: 0.02,
  clearcoat: 0.62,
  clearcoatRoughness: 0.16,
  sheen: 0.60,
  sheenColor: new THREE.Color(0xd9f99d),
  sheenRoughness: 0.38,
});

const manoScarfMat = new THREE.MeshPhysicalMaterial({
  color: 0x16a34a,
  emissive: 0x0d7a35,
  emissiveIntensity: 0.18,
  roughness: 0.32,
  metalness: 0.18,
  clearcoat: 0.45,
  sheen: 0.75,
  sheenColor: new THREE.Color(0x88f7be),
  sheenRoughness: 0.42,
});

const metalMat = new THREE.MeshPhysicalMaterial({
  color: 0xe3ecee,
  roughness: 0.12,
  metalness: 0.95,
  clearcoat: 0.38,
  envMapIntensity: 1.35,
});

// Energetic Orange-Amber Stage Halo Ring under MANO on the White Plinth
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
  foot.receiveShadow = true;
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
torso.receiveShadow = true;
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
  mPillar.castShadow = true;
  chestLogoGroup.add(mPillar);
});

// Left & Right diagonal inner strokes of "M"
[-1, 1].forEach((dir) => {
  const mDiag = new THREE.Mesh(new THREE.CapsuleGeometry(0.026, 0.115, 10, 16), manoAmberAccentMat);
  mDiag.position.set(dir * 0.052, -0.005, 0.016);
  mDiag.rotation.z = dir * 0.56;
  mDiag.castShadow = true;
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
stethCollar.castShadow = true;
manoChar.add(stethCollar);

const neckEmeraldCollar = new THREE.Mesh(new THREE.TorusGeometry(0.365, 0.024, 14, 36), manoScarfMat);
neckEmeraldCollar.rotation.x = Math.PI * 0.5;
neckEmeraldCollar.position.set(0, 1.265, 0.02);
manoChar.add(neckEmeraldCollar);

// Head chassis + Energetic Orange Side Ear Pods + Thick Orange-Amber Visor Bezel + Glowing Ring-Eyes
const head = new THREE.Mesh(new THREE.SphereGeometry(0.50, 36, 28), manoPorcelainMat);
head.scale.set(1.12, 0.96, 0.95);
head.position.y = 1.72;
head.castShadow = true;
head.receiveShadow = true;
manoChar.add(head);

[-0.54, 0.54].forEach((ex) => {
  const sign = ex > 0 ? 1 : -1;
  const earPod = new THREE.Mesh(new THREE.CylinderGeometry(0.145, 0.145, 0.09, 28), manoPorcelainMat);
  earPod.rotation.z = Math.PI / 2;
  earPod.position.set(ex, 1.72, 0.02);
  earPod.castShadow = true;
  manoChar.add(earPod);

  const earOrangeCap = new THREE.Mesh(new THREE.CylinderGeometry(0.105, 0.105, 0.025, 24), manoCoreEmblemMat);
  earOrangeCap.rotation.z = Math.PI / 2;
  earOrangeCap.position.set(ex + sign * 0.042, 1.72, 0.02);
  manoChar.add(earOrangeCap);

  const earRing = new THREE.Mesh(new THREE.TorusGeometry(0.125, 0.022, 12, 28), manoGoldMat);
  earRing.rotation.y = Math.PI / 2;
  earRing.position.set(ex + sign * 0.036, 1.72, 0.02);
  manoChar.add(earRing);
});

const visorBezel = new THREE.Mesh(new THREE.TorusGeometry(0.362, 0.052, 18, 48), manoGoldMat);
visorBezel.scale.set(1.18, 0.86, 1.05);
visorBezel.position.set(0, 1.72, 0.365);
visorBezel.castShadow = true;
manoChar.add(visorBezel);

const visorInnerAmberRim = new THREE.Mesh(new THREE.TorusGeometry(0.335, 0.018, 14, 44), manoAmberAccentMat);
visorInnerAmberRim.scale.set(1.18, 0.86, 1.0);
visorInnerAmberRim.position.set(0, 1.72, 0.392);
manoChar.add(visorInnerAmberRim);

const visorScreen = new THREE.Mesh(new THREE.SphereGeometry(0.415, 32, 24), manoVisorMat);
visorScreen.scale.set(0.96, 0.73, 0.56);
visorScreen.position.set(0, 1.72, 0.255);
manoChar.add(visorScreen);

// Glowing Ring-Eyes with Crisp White Specular Catchlights + Warm Coral-Orange Cheek Blushes
[-0.145, 0.145].forEach((x) => {
  const eyeRing = new THREE.Mesh(new THREE.TorusGeometry(0.060, 0.016, 14, 32), manoFaceGlowMat);
  eyeRing.position.set(x, 1.755, 0.476);
  manoChar.add(eyeRing);

  const catchlight = new THREE.Mesh(new THREE.SphereGeometry(0.019, 12, 12), manoCatchlightMat);
  catchlight.position.set(x - 0.024, 1.778, 0.488);
  manoChar.add(catchlight);

  const secondaryDot = new THREE.Mesh(new THREE.SphereGeometry(0.009, 10, 10), manoCatchlightMat);
  secondaryDot.position.set(x + 0.022, 1.732, 0.486);
  manoChar.add(secondaryDot);
});

[-0.225, 0.225].forEach((cx) => {
  const cheekGlow = new THREE.Mesh(new THREE.SphereGeometry(0.032, 14, 12), manoCheekBlushMat);
  cheekGlow.scale.set(1.35, 0.72, 0.35);
  cheekGlow.position.set(cx, 1.668, 0.458);
  manoChar.add(cheekGlow);
});

const smileCurve = new THREE.Mesh(
  new THREE.TorusGeometry(0.088, 0.017, 12, 28, Math.PI * 0.76),
  manoFaceGlowMat
);
smileCurve.rotation.z = Math.PI * 1.12;
smileCurve.position.set(0, 1.678, 0.482);
manoChar.add(smileCurve);

// Signature MANO V1.0 Single Lush Curved Leaf Sprout + Dual Orange-Gold Base Mount on Head
const sproutCollar = new THREE.Mesh(new THREE.CylinderGeometry(0.076, 0.095, 0.062, 24), manoGoldMat);
sproutCollar.position.set(0, 2.20, 0);
manoChar.add(sproutCollar);

const sproutInnerRing = new THREE.Mesh(new THREE.TorusGeometry(0.068, 0.016, 10, 24), manoAmberAccentMat);
sproutInnerRing.rotation.x = Math.PI / 2;
sproutInnerRing.position.set(0, 2.232, 0);
manoChar.add(sproutInnerRing);

const sproutStem = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.028, 0.20, 14), manoSproutMat);
sproutStem.position.set(0.015, 2.30, 0);
sproutStem.rotation.z = -0.18;
sproutStem.castShadow = true;
manoChar.add(sproutStem);

// Lush main sculpted leaf tilting gracefully to the right (matching MANO V1.0 reference sheet)
const mainLeaf = new THREE.Mesh(new THREE.SphereGeometry(0.135, 20, 16), manoSproutMat);
mainLeaf.scale.set(1.48, 0.38, 0.82);
mainLeaf.position.set(0.135, 2.41, 0.01);
mainLeaf.rotation.z = 0.38;
mainLeaf.rotation.y = -0.12;
mainLeaf.castShadow = true;
manoChar.add(mainLeaf);

const leafVein = new THREE.Mesh(new THREE.CapsuleGeometry(0.012, 0.19, 8, 12), manoAmberAccentMat);
leafVein.position.set(0.125, 2.415, 0.032);
leafVein.rotation.z = -1.18;
manoChar.add(leafVein);

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
  palm.castShadow = true;
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
  { id: 'mano_drug', color: 0x0f8c79, emissive: 0x16b89f, kind: 'drug', angle: 0 },
  { id: 'mano_growth', color: 0x1d78c1, emissive: 0x3898e8, kind: 'growth', angle: Math.PI / 3 },
  { id: 'mano_skin', color: 0xe06356, emissive: 0xf48477, kind: 'skin', angle: (2 * Math.PI) / 3 },
  { id: 'mano_nutrition', color: 0x6b8e23, emissive: 0x8fb834, kind: 'nutrition', angle: Math.PI },
  { id: 'mano_care', color: 0xe67e22, emissive: 0xf59d47, kind: 'care', angle: (4 * Math.PI) / 3 },
  { id: 'mano_connect', color: 0x7c5cbf, emissive: 0x9b7be0, kind: 'connect', angle: (5 * Math.PI) / 3 },
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
  badgeBase.castShadow = true;
  badgeBase.receiveShadow = true;
  stGroup.add(badgeBase);

  const suitMat = new THREE.MeshPhysicalMaterial({
    color: cfg.color,
    emissive: cfg.emissive,
    emissiveIntensity: 0.14,
    roughness: 0.18,
    roughnessMap: microGrainTex,
    metalness: 0.08,
    clearcoat: 0.95,
    clearcoatRoughness: 0.08,
    ior: 1.5,
  });

  const rimMat = new THREE.MeshPhysicalMaterial({
    color: cfg.emissive,
    emissive: cfg.emissive,
    emissiveIntensity: 0.32,
    roughness: 0.25,
    metalness: 0.6,
  });
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
const moteMat = new THREE.MeshPhysicalMaterial({
  color: 0x8de0e6,
  emissive: 0x4fd1c5,
  emissiveIntensity: 0.14,
  roughness: 0.22,
  clearcoat: 0.65,
  transparent: true,
  opacity: 0.72,
});
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

function damp(current, target, k, dt) {
  return current + (target - current) * (1 - Math.exp(-k * dt));
}

function setCameraFocus(posArr, targetArr) {
  desiredCamPos.set(posArr[0], posArr[1], posArr[2]);
  desiredTarget.set(targetArr[0], targetArr[1], targetArr[2]);
  cameraLerpActive = true;
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
    const bgHex = dark ? 0x08191f : 0xf4faff;
    const floorHex = dark ? 0x0d2128 : 0xe6f3fe;
    const plinthHex = dark ? 0x142e38 : 0xffffff;
    scene.background.setHex(bgHex);
    scene.fog.color.setHex(bgHex);
    floorMesh.material.color.setHex(floorHex);
    plinthMat.color.setHex(plinthHex);
    plinthMat.roughness = dark ? 0.20 : 0.20;
    plinthMat.clearcoat = dark ? 0.65 : 0.65;
    plinthTrimMat.emissiveIntensity = dark ? 0.45 : 0.28;

    scene.environmentIntensity = dark ? 0.62 : 0.82;
    renderer.toneMappingExposure = dark ? 1.12 : 1.06;

    keyLight.color.setHex(dark ? 0xf4fbff : 0xfff8ef);
    keyLight.intensity = dark ? 1.75 : 2.10;
    fillLight.color.setHex(dark ? 0x56b2c4 : 0xd9f4f8);
    fillLight.intensity = dark ? 0.58 : 0.80;
    rimLight.color.setHex(dark ? 0xffb366 : 0xfff0d9);
    rimLight.intensity = dark ? 1.38 : 0.92;

    hemiLight.color.setHex(dark ? 0x3b8696 : 0xe8f9fc);
    hemiLight.groundColor.setHex(dark ? 0x0b1f26 : 0xd4eaf7);
    hemiLight.intensity = dark ? 0.50 : 0.76;

    manoFaceGlowMat.emissiveIntensity = dark ? 1.60 : 1.45;
    manoCoreEmblemMat.emissiveIntensity = dark ? 1.10 : 0.85;
    manoGoldMat.emissiveIntensity = dark ? 0.48 : 0.38;
    moteMat.emissiveIntensity = dark ? 0.42 : 0.16;
    for (let i = 0; i < contactShadowMats.length; i++) {
      contactShadowMats[i].opacity = dark ? 0.90 : 0.80;
    }

    document.documentElement.style.background = dark ? '#08191f' : '#f4faff';
    document.body.style.background = dark ? '#08191f' : '#f4faff';
    markInteractive(900);
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
      manoCoreEmblemMat.color.setHex(0xff7a18);
      manoCoreEmblemMat.emissive.setHex(0xff5500);
      plinthTrimMat.emissive.setHex(0xff5500);
    }
    markInteractive(1600);
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
};

let interactionBoostUntilMs = performance.now() + 1800;
function markInteractive(durationMs = 1500) {
  interactionBoostUntilMs = Math.max(interactionBoostUntilMs, performance.now() + durationMs);
}

controls.addEventListener('start', () => markInteractive(2500));
controls.addEventListener('change', () => markInteractive(1200));
controls.addEventListener('end', () => markInteractive(1400));

// Tap raycasting to focus / notify Android
const raycaster = new THREE.Raycaster();
const pointerNdc = new THREE.Vector2();
let downX = 0;
let downY = 0;
canvas.addEventListener('pointerdown', (e) => {
  downX = e.clientX;
  downY = e.clientY;
  cameraLerpActive = false;
  markInteractive(2000);
});
canvas.addEventListener('pointerup', (e) => {
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
      } else if (squadMeshesById.has(partId)) {
        window.Mano3D.selectSquadMember(partId);
      }
      markInteractive(1600);
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

// Adaptive power-aware Animation Loop (60fps during interaction, ~30fps idle, 0fps paused/hidden)
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
    markInteractive(1200);
    if (!rafId && !document.hidden) {
      rafId = requestAnimationFrame(animate);
    }
  }
}

document.addEventListener('visibilitychange', () => {
  setScenePaused(document.hidden);
});

function isMotionActive(nowMs) {
  return (
    nowMs < interactionBoostUntilMs ||
    cameraLerpActive ||
    bottleShakeTimer > 0 ||
    manoBounce > 0 ||
    Math.abs(currentSyringeMl - targetSyringeMl) > 0.02
  );
}

function animate(nowMs) {
  if (scenePaused || document.hidden) {
    rafId = 0;
    return;
  }
  rafId = requestAnimationFrame(animate);

  const minIntervalMs = isMotionActive(nowMs) ? 14 : 33;
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

  // 3D MANO gentle breath, bounce & orbiting PBR squad characters
  if (activeScene === 'mano') {
    manoBounce = Math.max(0, manoBounce - dt * 2.2);
    const hop = Math.sin(manoBounce * Math.PI) * 0.26;
    manoChar.position.y = hop;
    const breath = 1.0 + Math.sin(t * 2.4) * 0.016;
    manoChar.scale.set(breath, 2.0 - breath, breath);
    rightArmGroup.rotation.z = -2.28 + Math.sin(t * 4.2) * 0.20;
    leftArmGroup.rotation.z = 0.48 + Math.sin(t * 2.4) * 0.04;
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
        fig.userData.rimMat.emissiveIntensity = isSelected ? 0.85 : 0.28;
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
  for (let i = 0; i < MOTE_COUNT; i++) {
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

  controls.update();
  renderer.render(scene, camera);
}

rafId = requestAnimationFrame(animate);
