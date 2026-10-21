/**
 * Rubik 3D View & Three.js Scene Setup
 * Realistic Speedcube aesthetic with studio lighting, rounded sticker tiles, and soft shadows.
 */

class CubeView {
  constructor(container, cubeState) {
    this.container = container;
    this.cubeState = cubeState;

    this.scene = null;
    this.camera = null;
    this.renderer = null;
    this.pivotGroup = null; // Group for rotating active layers
    this.cubeGroup = null;  // Root group for all 26 cubies
    this.cubieMeshes = [];  // Array of { mesh, state: CubieState, stickers: {} }
    this.raycastTargets = []; // Sticker meshes for interaction raycasting

    this.initScene();
    this.buildCube();
  }

  initScene() {
    this.scene = new THREE.Scene();

    // Perspective Camera
    const aspect = this.container.clientWidth / this.container.clientHeight;
    this.camera = new THREE.PerspectiveCamera(38, aspect, 0.1, 100);
    this.camera.position.set(4.8, 3.8, 6.2);
    this.camera.lookAt(0, 0, 0);

    // WebGL Renderer
    this.renderer = new THREE.WebGLRenderer({
      antialias: true,
      alpha: true,
      powerPreference: 'high-performance'
    });
    this.renderer.setSize(this.container.clientWidth, this.container.clientHeight);
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    this.renderer.shadowMap.enabled = true;
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    if (THREE.sRGBEncoding) this.renderer.outputEncoding = THREE.sRGBEncoding;

    this.container.appendChild(this.renderer.domElement);

    // Studio Lighting
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.65);
    this.scene.add(ambientLight);

    // Key Light
    const keyLight = new THREE.DirectionalLight(0xfff6ea, 1.25);
    keyLight.position.set(6, 10, 8);
    keyLight.castShadow = true;
    keyLight.shadow.mapSize.width = 1024;
    keyLight.shadow.mapSize.height = 1024;
    keyLight.shadow.camera.near = 1;
    keyLight.shadow.camera.far = 25;
    keyLight.shadow.camera.left = -4;
    keyLight.shadow.camera.right = 4;
    keyLight.shadow.camera.top = 4;
    keyLight.shadow.camera.bottom = -4;
    keyLight.shadow.bias = -0.001;
    this.scene.add(keyLight);

    // Rim / Back Light
    const rimLight = new THREE.DirectionalLight(0xa5c9ff, 0.8);
    rimLight.position.set(-8, 6, -8);
    this.scene.add(rimLight);

    // Bottom soft bounce
    const bounceLight = new THREE.DirectionalLight(0x404859, 0.4);
    bounceLight.position.set(0, -6, 2);
    this.scene.add(bounceLight);

    // Floor Shadow plane
    const shadowGeo = new THREE.PlaneGeometry(10, 10);
    const shadowMat = new THREE.ShadowMaterial({ opacity: 0.38 });
    const floor = new THREE.Mesh(shadowGeo, shadowMat);
    floor.rotation.x = -Math.PI / 2;
    floor.position.y = -2.3;
    floor.receiveShadow = true;
    this.scene.add(floor);

    // Groups
    this.cubeGroup = new THREE.Group();
    this.pivotGroup = new THREE.Group();
    this.scene.add(this.cubeGroup);
    this.scene.add(this.pivotGroup);

    // Resize handler
    window.addEventListener('resize', () => this.onResize());
  }

  onResize() {
    if (!this.container || !this.renderer || !this.camera) return;
    const w = this.container.clientWidth;
    const h = this.container.clientHeight;
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(w, h);
  }

  buildCube() {
    // Shared black plastic base material
    const blackPlasticMat = new THREE.MeshStandardMaterial({
      color: 0x141518,
      roughness: 0.45,
      metalness: 0.12,
      polygonOffset: true,
      polygonOffsetFactor: 1,
      polygonOffsetUnits: 1
    });

    // Base cubie geometry
    const CUBIE_SIZE = 0.94;
    const cubieGeo = new THREE.BoxGeometry(CUBIE_SIZE, CUBIE_SIZE, CUBIE_SIZE);

    // Sticker tile geometry (slightly raised thin plate)
    const TILE_SIZE = 0.83;
    const TILE_THICKNESS = 0.02;
    const tileGeo = new THREE.BoxGeometry(TILE_SIZE, TILE_SIZE, TILE_THICKNESS);

    // Color materials cache
    const stickerMats = {};
    for (const [code, hex] of Object.entries(window.CUBE_COLORS)) {
      stickerMats[code] = new THREE.MeshStandardMaterial({
        color: hex,
        roughness: 0.28,
        metalness: 0.05
      });
    }

    this.cubieMeshes = [];
    this.raycastTargets = [];

    // Outer face configurations
    const faceConfig = [
      { name: 'px', normal: new THREE.Vector3(1, 0, 0),  rot: [0, Math.PI / 2, 0], pos: [CUBIE_SIZE / 2, 0, 0] },
      { name: 'nx', normal: new THREE.Vector3(-1, 0, 0), rot: [0, -Math.PI / 2, 0], pos: [-CUBIE_SIZE / 2, 0, 0] },
      { name: 'py', normal: new THREE.Vector3(0, 1, 0),  rot: [-Math.PI / 2, 0, 0], pos: [0, CUBIE_SIZE / 2, 0] },
      { name: 'ny', normal: new THREE.Vector3(0, -1, 0), rot: [Math.PI / 2, 0, 0], pos: [0, -CUBIE_SIZE / 2, 0] },
      { name: 'pz', normal: new THREE.Vector3(0, 0, 1),  rot: [0, 0, 0], pos: [0, 0, CUBIE_SIZE / 2] },
      { name: 'nz', normal: new THREE.Vector3(0, 0, -1), rot: [0, Math.PI, 0], pos: [0, 0, -CUBIE_SIZE / 2] }
    ];

    const SPACING = 1.0;

    for (const state of this.cubeState.cubies) {
      const cubieGroup = new THREE.Group();
      cubieGroup.position.set(state.x * SPACING, state.y * SPACING, state.z * SPACING);

      // Core black cubie
      const coreMesh = new THREE.Mesh(cubieGeo, blackPlasticMat);
      coreMesh.castShadow = true;
      coreMesh.receiveShadow = true;
      cubieGroup.add(coreMesh);

      const stickerMap = {};

      // Outer sticker tiles
      for (const fc of faceConfig) {
        const colorKey = state.faces[fc.name];
        if (colorKey !== 'K') {
          const tileMesh = new THREE.Mesh(tileGeo, stickerMats[colorKey]);
          tileMesh.position.set(...fc.pos);
          tileMesh.rotation.set(...fc.rot);
          tileMesh.castShadow = true;

          // Attach metadata for raycasting & rotation
          tileMesh.userData = {
            cubieState: state,
            faceName: fc.name,
            normal: fc.normal.clone(),
            cubieGroup
          };

          cubieGroup.add(tileMesh);
          stickerMap[fc.name] = tileMesh;
          this.raycastTargets.push(tileMesh);
        }
      }

      this.cubeGroup.add(cubieGroup);
      this.cubieMeshes.push({
        group: cubieGroup,
        state,
        stickers: stickerMap
      });
    }
  }

  /**
   * Sync visual position and sticker materials from cubeState
   */
  syncWithState() {
    const SPACING = 1.0;
    const stickerMats = {};
    for (const [code, hex] of Object.entries(window.CUBE_COLORS)) {
      stickerMats[code] = new THREE.MeshStandardMaterial({
        color: hex,
        roughness: 0.28,
        metalness: 0.05
      });
    }

    for (const item of this.cubieMeshes) {
      item.group.position.set(item.state.x * SPACING, item.state.y * SPACING, item.state.z * SPACING);
      item.group.rotation.set(0, 0, 0);

      // Update sticker colors
      for (const [faceName, tileMesh] of Object.entries(item.stickers)) {
        const colorKey = item.state.faces[faceName];
        if (colorKey && stickerMats[colorKey]) {
          tileMesh.material = stickerMats[colorKey];
        }
      }
    }
  }

  render() {
    if (this.renderer && this.scene && this.camera) {
      this.renderer.render(this.scene, this.camera);
    }
  }
}

window.CubeView = CubeView;
