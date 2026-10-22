// Complete Two-Phase Kociemba Solver in Node.js
const {
    URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB,
    UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR,
    C_NK, FACT, CubieCube, basicMoves, moveCube, moveNames
} = require('./test_kociemba_core.js');

// 1. Move Tables
console.time('Init Move Tables');
const twistMove = Array.from({ length: 2187 }, () => new Int16Array(18));
const flipMove = Array.from({ length: 2048 }, () => new Int16Array(18));
const sliceMove = Array.from({ length: 495 }, () => new Int16Array(18));

const a = new CubieCube();
for (let u = 0; u < 2187; u++) {
    a.setTwist(u);
    for (let m = 0; m < 18; m++) twistMove[u][m] = a.multiply(moveCube[m]).getTwist();
}
for (let u = 0; u < 2048; u++) {
    a.setFlip(u);
    for (let m = 0; m < 18; m++) flipMove[u][m] = a.multiply(moveCube[m]).getFlip();
}
for (let u = 0; u < 495; u++) {
    a.setSlice(u);
    for (let m = 0; m < 18; m++) sliceMove[u][m] = a.multiply(moveCube[m]).getSlice();
}

// Phase 2 moves: U1, U2, U3, D1, D2, D3, R2, L2, F2, B2
const P2_MOVE_INDICES = [0, 1, 2, 9, 10, 11, 4, 13, 7, 16];
const P2_INV_MAP = new Int8Array(18).fill(-1);
P2_MOVE_INDICES.forEach((m, idx) => P2_INV_MAP[m] = idx);

// Phase 2 move tables:
// cornerPermMove: 40320 x 10
// edgePermMove (UD edges): 40320 x 10
// slicePermMove: 24 x 10
const cornerPermMove = Array.from({ length: 40320 }, () => new Int32Array(10));
const edgePermMove = Array.from({ length: 40320 }, () => new Int32Array(10));
const slicePermMove = Array.from({ length: 24 }, () => new Int8Array(10));

const cCube = new CubieCube();
for (let u = 0; u < 40320; u++) {
    cCube.setCornerPerm(u);
    for (let m = 0; m < 10; m++) {
        cornerPermMove[u][m] = cCube.multiply(moveCube[P2_MOVE_INDICES[m]]).getCornerPerm();
    }
}

// Slice edges permutation (FR=8, FL=9, BL=10, BR=11)
function setSlicePerm(c, idx) {
    const perm = [0, 1, 2, 3];
    let a = idx;
    const p = [];
    for (let j = 1; j <= 4; j++) {
        p[j - 1] = a % j;
        a = Math.floor(a / j);
    }
    const avail = [FR, FL, BL, BR];
    for (let j = 3; j >= 0; j--) {
        c.ep[8 + j] = avail.splice(avail.length - 1 - p[j], 1)[0];
    }
}

for (let u = 0; u < 24; u++) {
    const c = new CubieCube();
    setSlicePerm(c, u);
    for (let m = 0; m < 10; m++) {
        slicePermMove[u][m] = c.multiply(moveCube[P2_MOVE_INDICES[m]]).getSlicePerm();
    }
}

// EdgePerm for UD edges (UR=0..DB=7)
function setUDEdgePerm(c, idx) {
    let a = idx;
    const p = [];
    for (let j = 1; j <= 8; j++) {
        p[j - 1] = a % j;
        a = Math.floor(a / j);
    }
    const avail = [UR, UF, UL, UB, DR, DF, DL, DB];
    for (let j = 7; j >= 0; j--) {
        c.ep[j] = avail.splice(avail.length - 1 - p[j], 1)[0];
    }
}

for (let u = 0; u < 40320; u++) {
    const c = new CubieCube();
    setUDEdgePerm(c, u);
    for (let m = 0; m < 10; m++) {
        edgePermMove[u][m] = c.multiply(moveCube[P2_MOVE_INDICES[m]]).getEdgePerm();
    }
}
console.timeEnd('Init Move Tables');

// 2. Pruning Tables
console.time('Init Prun Tables');
// Phase 1 Pruning: Slice (495) x Flip (2048) -> 1,013,760 entries
const sliceFlipPrun = new Int8Array(495 * 2048).fill(-1);
sliceFlipPrun[0] = 0;
let filled = 1;
for (let d = 0; d < 10 && filled < 495 * 2048; d++) {
    for (let i = 0; i < 495 * 2048; i++) {
        if (sliceFlipPrun[i] === d) {
            const s = Math.floor(i / 2048);
            const f = i % 2048;
            for (let m = 0; m < 18; m++) {
                const ns = sliceMove[s][m];
                const nf = flipMove[f][m];
                const ni = ns * 2048 + nf;
                if (sliceFlipPrun[ni] === -1) {
                    sliceFlipPrun[ni] = d + 1;
                    filled++;
                }
            }
        }
    }
}

// Phase 1 Pruning: Slice (495) x Twist (2187) -> 1,082,565 entries
const sliceTwistPrun = new Int8Array(495 * 2187).fill(-1);
sliceTwistPrun[0] = 0;
filled = 1;
for (let d = 0; d < 10 && filled < 495 * 2187; d++) {
    for (let i = 0; i < 495 * 2187; i++) {
        if (sliceTwistPrun[i] === d) {
            const s = Math.floor(i / 2187);
            const t = i % 2187;
            for (let m = 0; m < 18; m++) {
                const ns = sliceMove[s][m];
                const nt = twistMove[t][m];
                const ni = ns * 2187 + nt;
                if (sliceTwistPrun[ni] === -1) {
                    sliceTwistPrun[ni] = d + 1;
                    filled++;
                }
            }
        }
    }
}

// Phase 2 Pruning: SlicePerm (24) x CornerPerm (40320) -> 967,680 entries
const sliceCornerPrun = new Int8Array(24 * 40320).fill(-1);
sliceCornerPrun[0] = 0;
filled = 1;
for (let d = 0; d < 14 && filled < 24 * 40320; d++) {
    for (let i = 0; i < 24 * 40320; i++) {
        if (sliceCornerPrun[i] === d) {
            const sp = Math.floor(i / 40320);
            const cp = i % 40320;
            for (let m = 0; m < 10; m++) {
                const nsp = slicePermMove[sp][m];
                const ncp = cornerPermMove[cp][m];
                const ni = nsp * 40320 + ncp;
                if (sliceCornerPrun[ni] === -1) {
                    sliceCornerPrun[ni] = d + 1;
                    filled++;
                }
            }
        }
    }
}

// Phase 2 Pruning: SlicePerm (24) x EdgePerm (40320) -> 967,680 entries
const sliceEdgePrun = new Int8Array(24 * 40320).fill(-1);
sliceEdgePrun[0] = 0;
filled = 1;
for (let d = 0; d < 14 && filled < 24 * 40320; d++) {
    for (let i = 0; i < 24 * 40320; i++) {
        if (sliceEdgePrun[i] === d) {
            const sp = Math.floor(i / 40320);
            const ep = i % 40320;
            for (let m = 0; m < 10; m++) {
                const nsp = slicePermMove[sp][m];
                const nep = edgePermMove[ep][m];
                const ni = nsp * 40320 + nep;
                if (sliceEdgePrun[ni] === -1) {
                    sliceEdgePrun[ni] = d + 1;
                    filled++;
                }
            }
        }
    }
}
console.timeEnd('Init Prun Tables');

// 3. Search Engine (IDA*)
function solveKociemba(cube) {
    const twist0 = cube.getTwist();
    const flip0 = cube.getFlip();
    const slice0 = cube.getSlice();

    let p1Moves = [];
    let bestSolution = null;

    // Search Phase 1
    function search1(twist, flip, slice, maxDepth, lastFace) {
        if (maxDepth === 0) {
            if (twist === 0 && flip === 0 && slice === 0) {
                // Reached G1! Search Phase 2
                const p2 = searchPhase2(p1Moves);
                if (p2) {
                    bestSolution = [...p1Moves, ...p2];
                    return true;
                }
            }
            return false;
        }

        const h1 = sliceFlipPrun[slice * 2048 + flip];
        const h2 = sliceTwistPrun[slice * 2187 + twist];
        const h = Math.max(h1, h2);
        if (h > maxDepth) return false;

        for (let m = 0; m < 18; m++) {
            const face = Math.floor(m / 3);
            if (face === lastFace) continue;
            // Commutative faces: e.g. U then D is ok, but avoid D then U
            if (face === 3 && lastFace === 0) continue; // D after U
            if (face === 4 && lastFace === 1) continue; // L after R
            if (face === 5 && lastFace === 2) continue; // B after F

            const nt = twistMove[twist][m];
            const nf = flipMove[flip][m];
            const ns = sliceMove[slice][m];

            p1Moves.push(m);
            if (search1(nt, nf, ns, maxDepth - 1, face)) return true;
            p1Moves.pop();
        }
        return false;
    }

    // Helper to evaluate cube state after Phase 1 and run Phase 2
    function searchPhase2(phase1Moves) {
        let cur = cube.copy();
        for (const m of phase1Moves) cur = cur.multiply(moveCube[m]);

        const cp0 = cur.getCornerPerm();
        const ep0 = cur.getEdgePerm();
        const sp0 = cur.getSlicePerm();

        const p2Moves = [];

        function search2(cp, ep, sp, maxDepth, lastFace) {
            if (maxDepth === 0) {
                return (cp === 0 && ep === 0 && sp === 0);
            }

            const h1 = sliceCornerPrun[sp * 40320 + cp];
            const h2 = sliceEdgePrun[sp * 40320 + ep];
            const h = Math.max(h1, h2);
            if (h > maxDepth) return false;

            for (let m = 0; m < 10; m++) {
                const moveIdx18 = P2_MOVE_INDICES[m];
                const face = Math.floor(moveIdx18 / 3);
                if (face === lastFace) continue;
                if (face === 3 && lastFace === 0) continue;
                if (face === 4 && lastFace === 1) continue;
                if (face === 5 && lastFace === 2) continue;

                const ncp = cornerPermMove[cp][m];
                const nep = edgePermMove[ep][m];
                const nsp = slicePermMove[sp][m];

                p2Moves.push(moveIdx18);
                if (search2(ncp, nep, nsp, maxDepth - 1, face)) return true;
                p2Moves.pop();
            }
            return false;
        }

        for (let d2 = 0; d2 <= 14; d2++) {
            if (search2(cp0, ep0, sp0, d2, -1)) {
                return p2Moves;
            }
        }
        return null;
    }

    for (let depth1 = 0; depth1 <= 12; depth1++) {
        if (search1(twist0, flip0, slice0, depth1, -1)) {
            break;
        }
    }

    if (bestSolution) {
        return bestSolution.map(m => moveNames[m]);
    }
    return null;
}

// TEST SOLVER
console.log("Testing Kociemba Two-Phase on 10 random scrambles...");
const testMoves = ["R", "U", "F", "L", "D", "B", "R'", "U'", "F'", "L'", "D'", "B'", "R2", "U2", "F2", "L2", "D2", "B2"];

for (let s = 1; s <= 10; s++) {
    let cube = new CubieCube();
    const scramble = [];
    for (let i = 0; i < 25; i++) {
        const m = Math.floor(Math.random() * 18);
        cube = cube.multiply(moveCube[m]);
        scramble.push(moveNames[m]);
    }

    console.time('solve ' + s);
    const solution = solveKociemba(cube);
    console.timeEnd('solve ' + s);

    if (solution) {
        // Verify solution
        let ver = cube.copy();
        for (const m of solution) {
            const idx = moveNames.indexOf(m);
            ver = ver.multiply(moveCube[idx]);
        }
        const isSolved = (ver.getTwist() === 0 && ver.getFlip() === 0 && ver.getSlice() === 0 && ver.getCornerPerm() === 0 && ver.getEdgePerm() === 0 && ver.getSlicePerm() === 0);
        console.log(`[#${s}] Solved in ${solution.length} moves! Verified: ${isSolved}`);
        console.log(`    Moves: ${solution.join(' ')}`);
        if (!isSolved) {
            console.error("Verification failed!");
            break;
        }
    } else {
        console.error("No solution found for scramble!");
        break;
    }
}
