/**
 * CubeState: Mathematical representation of Rubik 3x3x3
 * Manages 26 cubies, their positions (-1, 0, 1) and face sticker orientations.
 * Uses exact right-hand coordinate transformations consistent with Three.js.
 */

const CUBE_COLORS = {
  W: 0xffffff, // Up: White
  Y: 0xffd500, // Down: Yellow
  R: 0xc41e3a, // Right: Red
  O: 0xff5800, // Left: Orange
  G: 0x009e60, // Front: Green
  B: 0x0051ba, // Back: Blue
  K: 0x18191c  // Inner: Black plastic
};

class CubieState {
  constructor(x, y, z) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.faces = {
      px: x === 1 ? 'R' : 'K',
      nx: x === -1 ? 'O' : 'K',
      py: y === 1 ? 'W' : 'K',
      ny: y === -1 ? 'Y' : 'K',
      pz: z === 1 ? 'G' : 'K',
      nz: z === -1 ? 'B' : 'K'
    };
  }

  clone() {
    const c = new CubieState(this.x, this.y, this.z);
    c.faces = { ...this.faces };
    return c;
  }
}

class CubeState {
  constructor() {
    this.cubies = [];
    this.init();
  }

  init() {
    this.cubies = [];
    for (let x = -1; x <= 1; x++) {
      for (let y = -1; y <= 1; y++) {
        for (let z = -1; z <= 1; z++) {
          if (x === 0 && y === 0 && z === 0) continue;
          this.cubies.push(new CubieState(x, y, z));
        }
      }
    }
  }

  reset() {
    this.init();
  }

  /**
   * Apply mathematical right-hand rotation
   * @param {'X'|'Y'|'Z'} axis
   * @param {number} layer (-1, 0, 1)
   * @param {number} quarterTurns (+1 for +90°, -1 for -90°, +2 or -2 for 180°)
   */
  rotateLayer(axis, layer, quarterTurns) {
    const turns = ((quarterTurns % 4) + 4) % 4; // normalize to 0, 1, 2, 3
    if (turns === 0) return;

    for (let t = 0; t < turns; t++) {
      this.step90(axis, layer);
    }
  }

  step90(axis, layer) {
    const axisKey = axis.toLowerCase();
    for (const c of this.cubies) {
      if (c[axisKey] !== layer) continue;

      const { x, y, z } = c;
      const f = { ...c.faces };

      if (axis === 'X') {
        // +90° around +X: y' = -z, z' = y
        c.y = -z;
        c.z = y;
        c.faces.pz = f.py;
        c.faces.ny = f.pz;
        c.faces.nz = f.ny;
        c.faces.py = f.nz;
      } else if (axis === 'Y') {
        // +90° around +Y: z' = -x, x' = z
        c.x = z;
        c.z = -x;
        c.faces.px = f.pz;
        c.faces.nz = f.px;
        c.faces.nx = f.nz;
        c.faces.pz = f.nx;
      } else if (axis === 'Z') {
        // +90° around +Z: x' = -y, y' = x
        c.x = -y;
        c.y = x;
        c.faces.py = f.px;
        c.faces.nx = f.py;
        c.faces.ny = f.nx;
        c.faces.px = f.ny;
      }
    }
  }

  /**
   * Verify if all 6 exterior faces have uniform colors
   */
  isSolved() {
    const faces = [
      { coord: 'x', val: 1, prop: 'px' },
      { coord: 'x', val: -1, prop: 'nx' },
      { coord: 'y', val: 1, prop: 'py' },
      { coord: 'y', val: -1, prop: 'ny' },
      { coord: 'z', val: 1, prop: 'pz' },
      { coord: 'z', val: -1, prop: 'nz' }
    ];

    for (const f of faces) {
      const stickers = this.cubies
        .filter(c => c[f.coord] === f.val)
        .map(c => c.faces[f.prop]);

      if (stickers.length !== 9) return false;
      const first = stickers[0];
      if (first === 'K') return false;
      if (!stickers.every(color => color === first)) {
        return false;
      }
    }
    return true;
  }
}

window.CUBE_COLORS = CUBE_COLORS;
window.CubeState = CubeState;
