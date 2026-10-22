// Kociemba Two-Phase Solver Validator in Node.js

// 8 Corners
const URF = 0, UFL = 1, ULB = 2, UBR = 3, DFR = 4, DLF = 5, DBL = 6, DRB = 7;
// 12 Edges
const UR = 0, UF = 1, UL = 2, UB = 3, DR = 4, DF = 5, DL = 6, DB = 7, FR = 8, FL = 9, BL = 10, BR = 11;

// Binomial coefficient table C(n, k)
const C_NK = Array.from({ length: 13 }, (_, n) =>
    Array.from({ length: 13 }, (_, k) => {
        if (k > n || k < 0) return 0;
        if (k === 0 || k === n) return 1;
        let c = 1;
        for (let i = 1; i <= k; i++) {
            c = (c * (n - i + 1)) / i;
        }
        return Math.round(c);
    })
);

// Factorials
const FACT = [1, 1, 2, 6, 24, 120, 720, 5040, 40320];

class CubieCube {
    constructor() {
        this.cp = [URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB];
        this.co = [0, 0, 0, 0, 0, 0, 0, 0];
        this.ep = [UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR];
        this.eo = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
    }

    copy() {
        const c = new CubieCube();
        c.cp = [...this.cp];
        c.co = [...this.co];
        c.ep = [...this.ep];
        c.eo = [...this.eo];
        return c;
    }

    multiply(b) {
        const res = new CubieCube();
        // Corners
        for (let c = 0; c < 8; c++) {
            res.cp[c] = this.cp[b.cp[c]];
            res.co[c] = (this.co[b.cp[c]] + b.co[c]) % 3;
        }
        // Edges
        for (let e = 0; e < 12; e++) {
            res.ep[e] = this.ep[b.ep[e]];
            res.eo[e] = (this.eo[b.ep[e]] + b.eo[e]) % 2;
        }
        return res;
    }

    getTwist() {
        let ret = 0;
        for (let i = 0; i < 7; i++) {
            ret = 3 * ret + this.co[i];
        }
        return ret;
    }

    setTwist(twist) {
        let twistSum = 0;
        for (let i = 6; i >= 0; i--) {
            this.co[i] = twist % 3;
            twistSum += this.co[i];
            twist = Math.floor(twist / 3);
        }
        this.co[7] = (3 - (twistSum % 3)) % 3;
    }

    getFlip() {
        let ret = 0;
        for (let i = 0; i < 11; i++) {
            ret = 2 * ret + this.eo[i];
        }
        return ret;
    }

    setFlip(flip) {
        let flipSum = 0;
        for (let i = 10; i >= 0; i--) {
            this.eo[i] = flip % 2;
            flipSum += this.eo[i];
            flip = Math.floor(flip / 2);
        }
        this.eo[11] = (2 - (flipSum % 2)) % 2;
    }

    // Slice coordinate: C(12, 4) = 495
    getSlice() {
        let a = 0;
        let x = 0;
        for (let j = 11; j >= 0; j--) {
            if (this.ep[j] >= FR && this.ep[j] <= BR) {
                a += C_NK[11 - j][x + 1];
                x++;
            }
        }
        return a;
    }

    setSlice(slice) {
        const sliceEdge = [FR, FL, BL, BR];
        const otherEdge = [UR, UF, UL, UB, DR, DF, DL, DB];
        let a = slice;
        let x = 4;
        for (let j = 0; j < 12; j++) this.ep[j] = -1;
        for (let j = 0; j < 12; j++) {
            if (a >= C_NK[11 - j][x]) {
                this.ep[j] = sliceEdge[4 - x];
                a -= C_NK[11 - j][x];
                x--;
            }
        }
        let y = 0;
        for (let j = 0; j < 12; j++) {
            if (this.ep[j] === -1) {
                this.ep[j] = otherEdge[y++];
            }
        }
    }

    getCornerPerm() {
        let a = 0;
        for (let j = 7; j > 0; j--) {
            let s = 0;
            for (let k = j - 1; k >= 0; k--) {
                if (this.cp[k] > this.cp[j]) s++;
            }
            a = (a + s) * j;
        }
        return a;
    }

    setCornerPerm(cp) {
        const p = [];
        let a = cp;
        for (let j = 1; j <= 8; j++) {
            p[j - 1] = a % j;
            a = Math.floor(a / j);
        }
        const avail = [0, 1, 2, 3, 4, 5, 6, 7];
        for (let j = 7; j >= 0; j--) {
            this.cp[j] = avail.splice(avail.length - 1 - p[j], 1)[0];
        }
    }

    getEdgePerm() {
        let a = 0;
        for (let j = 7; j > 0; j--) {
            let s = 0;
            for (let k = j - 1; k >= 0; k--) {
                if (this.ep[k] > this.ep[j]) s++;
            }
            a = (a + s) * j;
        }
        return a;
    }

    getSlicePerm() {
        const perm = [];
        for (let i = 8; i < 12; i++) perm.push(this.ep[i] - 8);
        let a = 0;
        for (let j = 3; j > 0; j--) {
            let s = 0;
            for (let k = j - 1; k >= 0; k--) {
                if (perm[k] > perm[j]) s++;
            }
            a = (a + s) * j;
        }
        return a;
    }
}

// 6 Basic Move CubieCubes
const basicMoves = Array.from({ length: 6 }, () => new CubieCube());

// Move U
basicMoves[0].cp = [UBR, URF, UFL, ULB, DFR, DLF, DBL, DRB];
basicMoves[0].co = [0, 0, 0, 0, 0, 0, 0, 0];
basicMoves[0].ep = [UB, UR, UF, UL, DR, DF, DL, DB, FR, FL, BL, BR];
basicMoves[0].eo = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

// Move R
basicMoves[1].cp = [DFR, UFL, ULB, URF, DRB, DLF, DBL, UBR];
basicMoves[1].co = [2, 0, 0, 1, 1, 0, 0, 2];
basicMoves[1].ep = [FR, UF, UL, UB, BR, DF, DL, DB, DR, FL, BL, UR];
basicMoves[1].eo = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

// Move F
basicMoves[2].cp = [UFL, DLF, ULB, UBR, URF, DFR, DBL, DRB];
basicMoves[2].co = [1, 2, 0, 0, 2, 1, 0, 0];
basicMoves[2].ep = [UR, FL, UL, UB, DR, FR, DL, DB, UF, DF, BL, BR];
basicMoves[2].eo = [0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0];

// Move D
basicMoves[3].cp = [URF, UFL, ULB, UBR, DLF, DBL, DRB, DFR];
basicMoves[3].co = [0, 0, 0, 0, 0, 0, 0, 0];
basicMoves[3].ep = [UR, UF, UL, UB, DF, DL, DB, DR, FR, FL, BL, BR];
basicMoves[3].eo = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

// Move L
basicMoves[4].cp = [URF, ULB, DBL, UBR, DFR, UFL, DLF, DRB];
basicMoves[4].co = [0, 1, 2, 0, 0, 2, 1, 0];
basicMoves[4].ep = [UR, UF, BL, UB, DR, DF, FL, DB, FR, UL, DL, BR];
basicMoves[4].eo = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

// Move B
basicMoves[5].cp = [URF, UFL, UBR, DRB, DFR, DLF, ULB, DBL];
basicMoves[5].co = [0, 0, 1, 2, 0, 0, 2, 1];
basicMoves[5].ep = [UR, UF, UL, BR, DR, DF, DL, BL, FR, FL, UB, DB];
basicMoves[5].eo = [0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1];

// Generate 18 moves: U, U2, U', R, R2, R', F, F2, F', D, D2, D', L, L2, L', B, B2, B'
const moveCube = [];
for (let f = 0; f < 6; f++) {
    let cur = new CubieCube();
    for (let p = 0; p < 3; p++) {
        cur = cur.multiply(basicMoves[f]);
        moveCube.push(cur);
    }
}

const moveNames = [
    "U", "U2", "U'", "R", "R2", "R'", "F", "F2", "F'",
    "D", "D2", "D'", "L", "L2", "L'", "B", "B2", "B'"
];

module.exports = {
    URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB,
    UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR,
    C_NK, FACT, CubieCube, basicMoves, moveCube, moveNames
};
