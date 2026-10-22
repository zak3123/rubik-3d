// Test 54-facelet string extraction from 3D Cube
const fs = require('fs');
const { NodeCube } = (() => {
    const code = fs.readFileSync('test_solver_proto.js', 'utf8');
    return eval(`(() => { ${code}; return { NodeCube }; })()`);
})();

function getFaceletString(cube) {
    // Facelet layout:
    // U: y=1, z from -1 to 1 (row: z=-1, z=0, z=1; col: x=-1, x=0, x=1)
    // R: x=1, y from 1 to -1 (row: y=1, y=0, y=-1; col: z=1, z=0, z=-1)
    // F: z=1, y from 1 to -1 (row: y=1, y=0, y=-1; col: x=-1, x=0, x=1)
    // D: y=-1, z from 1 to -1 (row: z=1, z=0, z=-1; col: x=-1, x=0, x=1)
    // L: x=-1, y from 1 to -1 (row: y=1, y=0, y=-1; col: z=-1, z=0, z=1)
    // B: z=-1, y from 1 to -1 (row: y=1, y=0, y=-1; col: x=1, x=0, x=-1)

    function getStickerAt(x, y, z, worldFace) {
        if (x === 0 && y === 0 && z === 0) return worldFace;
        // Centers: if x,y,z has only 1 non-zero, it's center
        const nonZero = (x !== 0 ? 1 : 0) + (y !== 0 ? 1 : 0) + (z !== 0 ? 1 : 0);
        if (nonZero === 1) return worldFace; // centers are fixed

        const cubie = cube.cubies.find(c => c.cx === x && c.cy === y && c.cz === z);
        if (!cubie) return '?';

        // Check which local sticker of cubie points along worldFace
        for (const localFace of ['U', 'D', 'F', 'B', 'L', 'R']) {
            if (cube.getStickerDirection(cubie, localFace) === worldFace) {
                // Initial cubie face for localFace:
                // If cubie initial coords had this sticker, what was its color?
                if (localFace === 'U' && cubie.iy === 1) return 'U';
                if (localFace === 'D' && cubie.iy === -1) return 'D';
                if (localFace === 'F' && cubie.iz === 1) return 'F';
                if (localFace === 'B' && cubie.iz === -1) return 'B';
                if (localFace === 'L' && cubie.ix === -1) return 'L';
                if (localFace === 'R' && cubie.ix === 1) return 'R';
            }
        }
        return '?';
    }

    let s = "";
    // U face
    for (let z = -1; z <= 1; z++) {
        for (let x = -1; x <= 1; x++) {
            s += getStickerAt(x, 1, z, 'U');
        }
    }
    // R face
    for (let y = 1; y >= -1; y--) {
        for (let z = 1; z >= -1; z--) {
            s += getStickerAt(1, y, z, 'R');
        }
    }
    // F face
    for (let y = 1; y >= -1; y--) {
        for (let x = -1; x <= 1; x++) {
            s += getStickerAt(x, y, 1, 'F');
        }
    }
    // D face
    for (let z = 1; z >= -1; z--) {
        for (let x = -1; x <= 1; x++) {
            s += getStickerAt(x, -1, z, 'D');
        }
    }
    // L face
    for (let y = 1; y >= -1; y--) {
        for (let z = -1; z <= 1; z++) {
            s += getStickerAt(-1, y, z, 'L');
        }
    }
    // B face
    for (let y = 1; y >= -1; y--) {
        for (let x = 1; x >= -1; x--) {
            s += getStickerAt(x, y, -1, 'B');
        }
    }
    return s;
}

const c = new NodeCube();
const solvedStr = getFaceletString(c);
console.log("Solved facelet string:");
console.log(solvedStr);
console.log("Expected:");
console.log("UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB");
console.log("Matches:", solvedStr === "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB");
