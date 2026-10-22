const fs = require('fs');
const { NodeCube } = (() => {
    const code = fs.readFileSync('test_solver_proto.js', 'utf8');
    return eval('(() => { ' + code + '; return { NodeCube }; })()');
})();

const {
    URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB,
    UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR,
    CubieCube, moveCube, moveNames
} = require('./test_kociemba_core.js');

const faceletCode = fs.readFileSync('test_facelet.js', 'utf8');
const getFaceletString = eval('(() => { ' + faceletCode + '; return getFaceletString; })()');

// Phase 1 & 2 move tables & prun tables from test_kociemba_full.js
const {
    twistMove, flipMove, sliceMove,
    cornerPermMove, edgePermMove, slicePermMove,
    sliceFlipPrun, sliceTwistPrun, sliceCornerPrun, sliceEdgePrun,
    solveKociemba, P2_MOVE_INDICES
} = (() => {
    let full = fs.readFileSync('test_kociemba_full.js', 'utf8');
    const cutIdx = full.indexOf('// TEST SOLVER');
    if (cutIdx !== -1) full = full.substring(0, cutIdx);
    full += '\nreturn { twistMove, flipMove, sliceMove, cornerPermMove, edgePermMove, slicePermMove, sliceFlipPrun, sliceTwistPrun, sliceCornerPrun, sliceEdgePrun, solveKociemba, P2_MOVE_INDICES };';
    return eval('(() => {\n' + full + '\n})()');
})();

const cornerFacelet = [
    [8, 9, 20],   // URF: U, R, F
    [6, 18, 38],  // UFL: U, F, L
    [0, 36, 47],  // ULB: U, L, B
    [2, 45, 11],  // UBR: U, B, R
    [29, 26, 15], // DFR: D, F, R
    [27, 44, 24], // DLF: D, L, F
    [33, 53, 42], // DBL: D, B, L
    [35, 17, 51]  // DRB: D, R, B
];

const cornerColor = [
    ['U', 'R', 'F'],
    ['U', 'F', 'L'],
    ['U', 'L', 'B'],
    ['U', 'B', 'R'],
    ['D', 'F', 'R'],
    ['D', 'L', 'F'],
    ['D', 'B', 'L'],
    ['D', 'R', 'B']
];

const edgeFacelet = [
    [5, 10],  // UR
    [7, 19],  // UF
    [3, 37],  // UL
    [1, 46],  // UB
    [32, 16], // DR
    [28, 25], // DF
    [30, 43], // DL
    [34, 52], // DB
    [23, 12], // FR
    [21, 41], // FL
    [50, 39], // BL
    [48, 14]  // BR
];

const edgeColor = [
    ['U', 'R'],
    ['U', 'F'],
    ['U', 'L'],
    ['U', 'B'],
    ['D', 'R'],
    ['D', 'F'],
    ['D', 'L'],
    ['D', 'B'],
    ['F', 'R'],
    ['F', 'L'],
    ['B', 'L'],
    ['B', 'R']
];

function faceletToCubie(fStr) {
    const cc = new CubieCube();
    for (let i = 0; i < 8; i++) {
        const [f0, f1, f2] = cornerFacelet[i];
        const col0 = fStr[f0], col1 = fStr[f1], col2 = fStr[f2];
        let ori = 0;
        if (col0 === 'U' || col0 === 'D') ori = 0;
        else if (col1 === 'U' || col1 === 'D') ori = 1;
        else if (col2 === 'U' || col2 === 'D') ori = 2;
        cc.co[i] = ori;

        const cols = [col0, col1, col2].sort().join('');
        for (let c = 0; c < 8; c++) {
            const expected = [...cornerColor[c]].sort().join('');
            if (cols === expected) {
                cc.cp[i] = c;
                break;
            }
        }
    }

    for (let i = 0; i < 12; i++) {
        const [f0, f1] = edgeFacelet[i];
        const col0 = fStr[f0], col1 = fStr[f1];
        let ori = 0;
        const cols = [col0, col1].sort().join('');
        for (let e = 0; e < 12; e++) {
            const expected = [...edgeColor[e]].sort().join('');
            if (cols === expected) {
                cc.ep[i] = e;
                if (col0 === edgeColor[e][0]) ori = 0;
                else ori = 1;
                break;
            }
        }
        cc.eo[i] = ori;
    }
    return cc;
}

// Test with 5 random scrambles on 3D cube!
console.log("Starting 3D NodeCube End-to-End Solve Test...");
for (let s = 1; s <= 10; s++) {
    const cube = new NodeCube();
    const movePool = ["R", "U", "F", "L", "D", "B", "R'", "U'", "F'", "L'", "D'", "B'", "R2", "U2", "F2", "L2", "D2", "B2"];
    const scrambleMoves = [];
    for (let i = 0; i < 20; i++) {
        const m = movePool[Math.floor(Math.random() * movePool.length)];
        scrambleMoves.push(m);
        cube.move(m);
    }
    console.log(`\n[Trial ${s}] Scramble: ${scrambleMoves.join(' ')}`);
    console.log(`Initial solved: ${cube.isSolved()}`);
    
    const fStr = getFaceletString(cube);
    const cc = faceletToCubie(fStr);
    
    console.time('solve');
    const solution = solveKociemba(cc);
    console.timeEnd('solve');
    
    console.log(`Solution (${solution.length} moves): ${solution.join(' ')}`);
    for (const m of solution) {
        cube.move(m);
    }
    const fStrSolved = getFaceletString(cube);
    const isFaceSolved = (fStrSolved === "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB");
    console.log(`Facelets match solved: ${isFaceSolved}`);
    if (!isFaceSolved) {
        console.error("FAIL: Facelets not solved!");
        process.exit(1);
    }
}
console.log("\nALL 5 END-TO-END TESTS PASSED!");
