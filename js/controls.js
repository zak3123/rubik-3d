/**
 * Controls System
 * Supports:
 * 1. Raycast direct face dragging (gesture to rotate layer)
 * 2. Orbit camera dragging on background
 * 3. Mouse wheel & pinch zoom
 * 4. Keyboard shortcuts (Rubik notation, Undo, Scramble)
 */

class CubeControls {
  constructor(cubeView, rotationManager, audio) {
    this.cubeView = cubeView;
    this.rotMgr = rotationManager;
    this.audio = audio;

    this.domElement = cubeView.renderer.domElement;
    this.camera = cubeView.camera;

    // Raycaster & interaction state
    this.raycaster = new THREE.Raycaster();
    this.mouse = new THREE.Vector2();

    this.isPointerDown = false;
    this.mode = 'idle'; // 'orbit' | 'layerDrag'
    this.startPointer = { x: 0, y: 0 };
    this.currentPointer = { x: 0, y: 0 };
    this.selectedSticker = null;

    // Camera spherical coords
    this.spherical = {
      radius: 8.5,
      theta: 0.65, // horizontal angle
      phi: 1.05    // vertical angle
    };
    this.updateCameraPosition();

    // Pinch zoom tracking
    this.touchDistance = null;

    this.bindEvents();
  }

  updateCameraPosition() {
    this.spherical.phi = Math.max(0.15, Math.min(Math.PI - 0.15, this.spherical.phi));
    this.spherical.radius = Math.max(4.5, Math.min(15.0, this.spherical.radius));

    const x = this.spherical.radius * Math.sin(this.spherical.phi) * Math.sin(this.spherical.theta);
    const y = this.spherical.radius * Math.cos(this.spherical.phi);
    const z = this.spherical.radius * Math.sin(this.spherical.phi) * Math.cos(this.spherical.theta);

    this.camera.position.set(x, y, z);
    this.camera.lookAt(0, 0, 0);
  }

  bindEvents() {
    const el = this.domElement;

    el.addEventListener('pointerdown', (e) => this.onPointerDown(e));
    window.addEventListener('pointermove', (e) => this.onPointerMove(e));
    window.addEventListener('pointerup', (e) => this.onPointerUp(e));
    window.addEventListener('pointercancel', (e) => this.onPointerUp(e));

    el.addEventListener('wheel', (e) => this.onWheel(e), { passive: false });
    el.addEventListener('touchstart', (e) => this.onTouchStart(e), { passive: false });
    el.addEventListener('touchmove', (e) => this.onTouchMove(e), { passive: false });
    el.addEventListener('touchend', () => { this.touchDistance = null; });

    window.addEventListener('keydown', (e) => this.onKeyDown(e));
  }

  getPointerPos(e) {
    const rect = this.domElement.getBoundingClientRect();
    return {
      x: ((e.clientX - rect.left) / rect.width) * 2 - 1,
      y: -((e.clientY - rect.top) / rect.height) * 2 + 1,
      clientX: e.clientX,
      clientY: e.clientY
    };
  }

  onPointerDown(e) {
    if (this.audio) this.audio.ensureActive();

    const pos = this.getPointerPos(e);
    this.isPointerDown = true;
    this.startPointer = { x: pos.clientX, y: pos.clientY };
    this.currentPointer = { ...this.startPointer };

    this.mouse.x = pos.x;
    this.mouse.y = pos.y;
    this.raycaster.setFromCamera(this.mouse, this.camera);

    const intersects = this.raycaster.intersectObjects(this.cubeView.raycastTargets, false);

    if (intersects.length > 0) {
      this.mode = 'layerDrag';
      this.selectedSticker = intersects[0].object.userData;
    } else {
      this.mode = 'orbit';
      this.selectedSticker = null;
    }
  }

  onPointerMove(e) {
    if (!this.isPointerDown) return;

    const dx = e.clientX - this.startPointer.x;
    const dy = e.clientY - this.startPointer.y;

    if (this.mode === 'orbit') {
      this.spherical.theta -= dx * 0.007;
      this.spherical.phi -= dy * 0.007;
      this.updateCameraPosition();
      this.startPointer = { x: e.clientX, y: e.clientY };
    } else if (this.mode === 'layerDrag' && this.selectedSticker) {
      const dist = Math.hypot(dx, dy);
      // Require threshold drag distance before triggering rotation
      if (dist > 18 && !this.rotMgr.isAnimating) {
        this.processLayerDrag(dx, dy);
        this.mode = 'idle';
        this.selectedSticker = null;
      }
    }
  }

  processLayerDrag(screenDx, screenDy) {
    const { cubieState, normal } = this.selectedSticker;

    // Approximate camera basis in world coordinates
    const camRight = new THREE.Vector3(1, 0, 0).applyQuaternion(this.camera.quaternion);
    const camUp = new THREE.Vector3(0, 1, 0).applyQuaternion(this.camera.quaternion);

    // World-space drag vector
    const dragWorld = new THREE.Vector3()
      .addScaledVector(camRight, screenDx)
      .addScaledVector(camUp, -screenDy);

    // Project drag vector onto the sticker face plane
    const projDrag = dragWorld.clone().sub(normal.clone().multiplyScalar(dragWorld.dot(normal)));
    if (projDrag.lengthSq() < 0.001) return;
    projDrag.normalize();

    // Cross product: Axis_dir = Normal × Drag
    const rotVector = new THREE.Vector3().crossVectors(normal, projDrag);

    // Identify dominant axis (X, Y, or Z)
    const absX = Math.abs(rotVector.x);
    const absY = Math.abs(rotVector.y);
    const absZ = Math.abs(rotVector.z);

    let chosenAxis = 'X';
    let dirSign = Math.sign(rotVector.x);

    if (absY > absX && absY > absZ) {
      chosenAxis = 'Y';
      dirSign = Math.sign(rotVector.y);
    } else if (absZ > absX && absZ > absY) {
      chosenAxis = 'Z';
      dirSign = Math.sign(rotVector.z);
    }

    if (dirSign === 0) return;

    const layerCoord = cubieState[chosenAxis.toLowerCase()];
    this.rotMgr.executeRaw(chosenAxis, layerCoord, dirSign);
  }

  onPointerUp() {
    this.isPointerDown = false;
    this.mode = 'idle';
    this.selectedSticker = null;
  }

  onWheel(e) {
    e.preventDefault();
    this.spherical.radius += e.deltaY * 0.006;
    this.updateCameraPosition();
  }

  onTouchStart(e) {
    if (e.touches.length === 2) {
      this.mode = 'pinch';
      const dx = e.touches[0].clientX - e.touches[1].clientX;
      const dy = e.touches[0].clientY - e.touches[1].clientY;
      this.touchDistance = Math.hypot(dx, dy);
    }
  }

  onTouchMove(e) {
    if (e.touches.length === 2 && this.touchDistance) {
      e.preventDefault();
      const dx = e.touches[0].clientX - e.touches[1].clientX;
      const dy = e.touches[0].clientY - e.touches[1].clientY;
      const newDist = Math.hypot(dx, dy);
      const factor = (this.touchDistance - newDist) * 0.02;
      this.spherical.radius += factor;
      this.updateCameraPosition();
      this.touchDistance = newDist;
    }
  }

  onKeyDown(e) {
    // Ignore input if typing in an input field
    if (['INPUT', 'SELECT', 'TEXTAREA'].includes(e.target.tagName)) return;

    if (this.audio) this.audio.ensureActive();

    // Ctrl+Z for undo
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
      e.preventDefault();
      this.rotMgr.undo();
      return;
    }

    const key = e.key.toUpperCase();

    if (key === ' ') {
      e.preventDefault();
      if (window.cubeScramble) window.cubeScramble();
      return;
    }

    if (['U', 'D', 'L', 'R', 'F', 'B'].includes(key)) {
      e.preventDefault();
      const notation = e.shiftKey ? `${key}'` : key;
      this.rotMgr.executeNotation(notation);
    }
  }
}

window.CubeControls = CubeControls;
