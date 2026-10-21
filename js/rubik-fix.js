/*
 * Rubik 3D interaction + sticker fix
 * Drop-in patch for https://github.com/zak3123/rubik-3d
 * Load AFTER js/main.js, BEFORE DOMContentLoaded.
 * No external dependencies beyond the existing THREE.js project.
 */
(function () {
  'use strict';

  if (typeof CubeView !== 'function' || typeof CubeControls !== 'function' || typeof MechanicalAudio !== 'function') {
    console.error('[rubik-fix] Load this script after js/main.js and its dependencies.');
    return;
  }

  const FACE_DATA = [
    ['px', [1, 0, 0], [0, Math.PI / 2, 0], [0.47, 0, 0]],
    ['nx', [-1, 0, 0], [0, -Math.PI / 2, 0], [-0.47, 0, 0]],
    ['py', [0, 1, 0], [-Math.PI / 2, 0, 0], [0, 0.47, 0]],
    ['ny', [0, -1, 0], [Math.PI / 2, 0, 0], [0, -0.47, 0]],
    ['pz', [0, 0, 1], [0, 0, 0], [0, 0, 0.47]],
    ['nz', [0, 0, -1], [0, Math.PI, 0], [0, 0, -0.47]]
  ];

  // ISSUE 1: The original build creates stickers ONLY for each piece's STARTING
  // visible faces. After turning, a cubie can require a different visible face,
  // producing the black/blank tiles seen in the screenshot.
  // Fix: every piece has six sticker slots. Show the 1-3 currently outward slots.
  const originalBuild = CubeView.prototype.buildCube;
  CubeView.prototype.buildCube = function () {
    originalBuild.call(this);
    this._fixTileGeometry = new THREE.BoxGeometry(0.83, 0.83, 0.02);
    this._fixStickerMaterials = {};
    for (const [code, color] of Object.entries(window.CUBE_COLORS)) {
      this._fixStickerMaterials[code] = new THREE.MeshStandardMaterial({
        color,
        roughness: 0.32,
        metalness: 0.025
      });
    }

    for (const item of this.cubieMeshes) {
      for (const [face, normal, rotation, position] of FACE_DATA) {
        if (item.stickers[face]) continue;
        const tile = new THREE.Mesh(this._fixTileGeometry, this._fixStickerMaterials.K);
        tile.position.set(...position);
        tile.rotation.set(...rotation);
        tile.castShadow = true;
        tile.visible = false;
        tile.userData = {
          cubieState: item.state,
          faceName: face,
          normal: new THREE.Vector3(...normal),
          cubieGroup: item.group
        };
        item.group.add(tile);
        item.stickers[face] = tile;
      }
    }
    this.syncWithState();
  };

  // ISSUE 2: The original sync only changes sticker color, even if a sticker
  // has become an INNER face. It also allocates 7 new materials EVERY move.
  // ISSUE 3: CubeState.reset() creates new state objects but visual cubies still
  // point to the discarded ones. Rebind them when state objects have changed.
  CubeView.prototype.syncWithState = function () {
    if (!this._fixStickerMaterials) return;
    const currentStates = this.cubeState.cubies;
    const didReset = this.cubieMeshes.some((item, i) => item.state !== currentStates[i] && !currentStates.includes(item.state));

    if (didReset) {
      for (let i = 0; i < this.cubieMeshes.length; i++) {
        const item = this.cubieMeshes[i];
        item.state = currentStates[i];
        for (const tile of Object.values(item.stickers)) {
          tile.userData.cubieState = item.state;
        }
      }
    }

    this.raycastTargets = [];
    for (const item of this.cubieMeshes) {
      item.group.position.set(item.state.x, item.state.y, item.state.z);
      item.group.rotation.set(0, 0, 0);
      for (const [face, tile] of Object.entries(item.stickers)) {
        const colorCode = item.state.faces[face];
        tile.visible = Boolean(colorCode && colorCode !== 'K');
        tile.material = this._fixStickerMaterials[colorCode] || this._fixStickerMaterials.K;
        if (tile.visible) this.raycastTargets.push(tile);
      }
    }
  };

  // ISSUE 4: Capturing only one "drag of a face" can be confused with a camera
  // drag, and the original touch + pointer listeners fight on some devices.
  // Use ONLY Pointer Events (also works for pen/touch), with pinch zoom.
  CubeControls.prototype.bindEvents = function () {
    const el = this.domElement;
    el.style.touchAction = 'none';
    el.style.cursor = 'grab';
    this.pointers = new Map();
    this.activePointerId = null;
    this.touchDistance = null;

    el.addEventListener('pointerdown', e => this.onPointerDown(e));
    el.addEventListener('pointermove', e => this.onPointerMove(e));
    el.addEventListener('pointerup', e => this.onPointerUp(e));
    el.addEventListener('pointercancel', e => this.onPointerUp(e));
    el.addEventListener('lostpointercapture', e => this.onPointerUp(e));
    el.addEventListener('contextmenu', e => e.preventDefault());
    el.addEventListener('wheel', e => this.onWheel(e), { passive: false });
    window.addEventListener('keydown', e => this.onKeyDown(e));
  };

  CubeControls.prototype.onPointerDown = function (e) {
    if (e.pointerType === 'mouse' && e.button !== 0 && e.button !== 2) return;
    e.preventDefault();
    if (this.audio) this.audio.ensureActive();
    try { this.domElement.setPointerCapture(e.pointerId); } catch (_) { /* optional */ }
    this.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });

    if (this.pointers.size >= 2) {
      const [a, b] = [...this.pointers.values()];
      this.touchDistance = Math.hypot(a.x - b.x, a.y - b.y);
      this.mode = 'pinch';
      this.selectedSticker = null;
      this.activePointerId = null;
      return;
    }

    this.activePointerId = e.pointerId;
    this.isPointerDown = true;
    this.startPointer = { x: e.clientX, y: e.clientY };
    this.currentPointer = { ...this.startPointer };
    this.selectedSticker = null;

    // Drag empty space or right-click / Shift+drag / Alt+drag on cube to orbit.
    if (e.button === 2 || e.shiftKey || e.altKey || this.rotMgr.isAnimating) {
      this.mode = 'orbit';
      return;
    }

    const pos = this.getPointerPos(e);
    this.mouse.set(pos.x, pos.y);
    this.raycaster.setFromCamera(this.mouse, this.camera);
    const hits = this.raycaster.intersectObjects(this.cubeView.raycastTargets, false);
    const hit = hits.find(result => result.object.visible);
    if (hit) {
      this.mode = 'layerDrag';
      this.selectedSticker = hit.object.userData;
    } else {
      this.mode = 'orbit';
    }
  };

  CubeControls.prototype.onPointerMove = function (e) {
    if (!this.pointers.has(e.pointerId)) return;
    e.preventDefault();
    this.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });

    if (this.pointers.size >= 2) {
      const [a, b] = [...this.pointers.values()];
      const nextDistance = Math.hypot(a.x - b.x, a.y - b.y);
      if (this.touchDistance) {
        this.spherical.radius += (this.touchDistance - nextDistance) * 0.018;
        this.updateCameraPosition();
      }
      this.touchDistance = nextDistance;
      this.mode = 'pinch';
      return;
    }
    if (this.activePointerId !== e.pointerId) return;

    const dx = e.clientX - this.startPointer.x;
    const dy = e.clientY - this.startPointer.y;
    if (this.mode === 'orbit') {
      this.spherical.theta -= dx * 0.007;
      this.spherical.phi -= dy * 0.007;
      this.updateCameraPosition();
      this.startPointer = { x: e.clientX, y: e.clientY };
    } else if (this.mode === 'layerDrag' && this.selectedSticker) {
      if (Math.hypot(dx, dy) >= 14 && !this.rotMgr.isAnimating) {
        this.processLayerDrag(dx, dy);
        this.mode = 'used'; // exactly one turn per gesture
        this.selectedSticker = null;
      }
    }
  };

  CubeControls.prototype.onPointerUp = function (e) {
    if (e && e.pointerId !== undefined) this.pointers.delete(e.pointerId);
    else this.pointers.clear();
    this.mode = 'idle';
    this.selectedSticker = null;
    this.isPointerDown = false;
    this.activePointerId = null;
    this.touchDistance = null;
  };

  // Remove the toy-like resonant oscillator and use short filtered mechanical
  // transients. All sounds are synthesized and work without external WAVs.
  MechanicalAudio.prototype.playClick = function (pitch = 1) {
    if (!this.soundEnabled) return;
    this.ensureActive();
    const ctx = this.ctx;
    if (!ctx || !this.masterGain) return;
    const now = ctx.currentTime;
    const click = (delay, len, center, amount) => {
      const frames = Math.max(8, Math.ceil(ctx.sampleRate * len));
      const buffer = ctx.createBuffer(1, frames, ctx.sampleRate);
      const data = buffer.getChannelData(0);
      let previous = 0;
      for (let i = 0; i < frames; i++) {
        const input = Math.random() * 2 - 1;
        previous = previous * 0.25 + input * 0.75;
        data[i] = previous * Math.exp(-7 * i / frames);
      }
      const source = ctx.createBufferSource();
      source.buffer = buffer;
      const filter = ctx.createBiquadFilter();
      filter.type = 'bandpass';
      filter.frequency.setValueAtTime(center * pitch, now + delay);
      filter.Q.value = 0.8;
      const gain = ctx.createGain();
      gain.gain.setValueAtTime(amount, now + delay);
      gain.gain.exponentialRampToValueAtTime(0.001, now + delay + len);
      source.connect(filter).connect(gain).connect(this.masterGain);
      source.start(now + delay);
      source.stop(now + delay + len);
    };
    // Plastic teeth engagement, then a smaller final contact click.
    click(0, 0.020, 1700 + Math.random() * 380, 0.38);
    click(0.011, 0.028, 570 + Math.random() * 130, 0.20);
  };

  MechanicalAudio.prototype.playFriction = function (durationMs = 180) {
    if (!this.soundEnabled) return;
    this.ensureActive();
    const ctx = this.ctx;
    if (!ctx || !this.masterGain) return;
    const duration = Math.max(0.04, Math.min(0.6, durationMs / 1000));
    const frames = Math.ceil(ctx.sampleRate * duration);
    const buffer = ctx.createBuffer(1, frames, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < frames; i++) {
      const shape = Math.sin(Math.PI * i / frames);
      data[i] = (Math.random() * 2 - 1) * shape * 0.22;
    }
    const source = ctx.createBufferSource();
    source.buffer = buffer;
    const filter = ctx.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.value = 1250;
    const gain = ctx.createGain();
    gain.gain.value = 0.12;
    source.connect(filter).connect(gain).connect(this.masterGain);
    source.start();
  };

  // Correct last-step sound timing to keep a compact two-click snap.
  MechanicalAudio.prototype.playDoubleRotationClick = function () {
    this.playClick(0.94);
    setTimeout(() => this.playClick(1.06), 42);
  };

  const hint = document.querySelector('.hint-bar');
  if (hint) {
    hint.textContent = 'Geser stiker: putar lapisan · Geser area kosong / Shift + geser: putar kamera · Scroll / cubit: zoom · Tombol U D L R F B';
  }
})();
