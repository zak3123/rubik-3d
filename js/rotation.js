/**
 * Rotation Manager
 * Handles animated 3D layer turns with easing, input queuing, sound triggers, and undo history.
 */

// Move mapping to (axis, layer, turns)
// Follows right-hand coordinate convention:
// R: +X, layer 1, turns 1 | L: +X, layer -1, turns -1
// U: +Y, layer 1, turns -1 | D: +Y, layer -1, turns 1
// F: +Z, layer 1, turns -1 | B: +Z, layer -1, turns 1
const NOTATION_MAP = {
  R:  { axis: 'X', layer:  1, turns:  1 },
  "R'": { axis: 'X', layer:  1, turns: -1 },
  R2: { axis: 'X', layer:  1, turns:  2 },

  L:  { axis: 'X', layer: -1, turns: -1 },
  "L'": { axis: 'X', layer: -1, turns:  1 },
  L2: { axis: 'X', layer: -1, turns:  2 },

  U:  { axis: 'Y', layer:  1, turns: -1 },
  "U'": { axis: 'Y', layer:  1, turns:  1 },
  U2: { axis: 'Y', layer:  1, turns:  2 },

  D:  { axis: 'Y', layer: -1, turns:  1 },
  "D'": { axis: 'Y', layer: -1, turns: -1 },
  D2: { axis: 'Y', layer: -1, turns:  2 },

  F:  { axis: 'Z', layer:  1, turns: -1 },
  "F'": { axis: 'Z', layer:  1, turns:  1 },
  F2: { axis: 'Z', layer:  1, turns:  2 },

  B:  { axis: 'Z', layer: -1, turns:  1 },
  "B'": { axis: 'Z', layer: -1, turns: -1 },
  B2: { axis: 'Z', layer: -1, turns:  2 }
};

class RotationManager {
  constructor(cubeView, cubeState, audio) {
    this.cubeView = cubeView;
    this.cubeState = cubeState;
    this.audio = audio;

    this.isAnimating = false;
    this.queue = [];
    this.history = []; // stack of { axis, layer, turns, notation, isScramble }
    this.moveDuration = 180; // ms (PRD standard 150-220ms)
    this.scrambleDuration = 55; // ms for quick scramble

    this.onMoveStart = null;
    this.onMoveEnd = null;
    this.onSolved = null;
  }

  setAnimationSpeed(durationMs) {
    this.moveDuration = Math.max(80, Math.min(400, durationMs));
  }

  /**
   * Execute or queue a move by notation ('U', "U'", 'R2', etc.)
   */
  executeNotation(notation, isScramble = false) {
    const moveDef = NOTATION_MAP[notation];
    if (!moveDef) return;
    this.queueMove({
      axis: moveDef.axis,
      layer: moveDef.layer,
      turns: moveDef.turns,
      notation,
      isScramble
    });
  }

  /**
   * Execute or queue a raw layer rotation (e.g. from mouse drag)
   */
  executeRaw(axis, layer, turns, isScramble = false) {
    this.queueMove({
      axis,
      layer,
      turns,
      notation: null,
      isScramble
    });
  }

  queueMove(move) {
    this.queue.push(move);
    if (!this.isAnimating) {
      this.processNext();
    }
  }

  /**
   * Undo last user move (scrambles are ignored)
   */
  undo() {
    if (this.isAnimating || this.queue.length > 0) return false;
    if (this.history.length === 0) return false;

    const last = this.history.pop();
    // Invert the turn
    this.queueMove({
      axis: last.axis,
      layer: last.layer,
      turns: -last.turns,
      notation: 'UNDO',
      isScramble: true, // Don't count undo itself as a new penalty or push to history
      isUndoAction: true
    });
    return true;
  }

  clearHistory() {
    this.history = [];
    this.queue = [];
  }

  processNext() {
    if (this.queue.length === 0) {
      this.isAnimating = false;
      return;
    }

    this.isAnimating = true;
    const move = this.queue.shift();
    const duration = move.isScramble ? this.scrambleDuration : this.moveDuration;

    if (!move.isScramble && !move.isUndoAction && this.onMoveStart) {
      this.onMoveStart(move);
    }

    // Play friction audio
    if (this.audio) {
      this.audio.playFriction(duration);
    }

    const { axis, layer, turns } = move;
    const axisLower = axis.toLowerCase();
    const targetAngle = turns * (Math.PI / 2);

    // 1. Collect affected meshes from cubeGroup and attach to pivotGroup
    const affected = [];
    for (const item of this.cubeView.cubieMeshes) {
      if (item.state[axisLower] === layer) {
        affected.push(item);
      }
    }

    const pivot = this.cubeView.pivotGroup;
    pivot.rotation.set(0, 0, 0);
    pivot.position.set(0, 0, 0);

    affected.forEach(item => {
      // Reparent to pivot while keeping world transforms
      this.cubeView.cubeGroup.remove(item.group);
      pivot.add(item.group);
    });

    // 2. Animate rotation of pivotGroup using smooth easing
    const startTime = performance.now();

    const animateStep = (now) => {
      const elapsed = now - startTime;
      const progress = Math.min(1, elapsed / duration);
      // Smooth sinusoidal cubic ease-in-out
      const ease = 0.5 - 0.5 * Math.cos(progress * Math.PI);

      pivot.rotation[axisLower] = targetAngle * ease;

      if (progress < 1) {
        requestAnimationFrame(animateStep);
      } else {
        // Complete animation
        pivot.rotation[axisLower] = targetAngle;

        // Mathematical state update
        this.cubeState.rotateLayer(axis, layer, turns);

        // Reparent all cubies back to cubeGroup
        affected.forEach(item => {
          pivot.remove(item.group);
          this.cubeView.cubeGroup.add(item.group);
        });
        pivot.rotation.set(0, 0, 0);

        // Re-sync positions and stickers exactly to avoid float drift
        this.cubeView.syncWithState();

        // Mechanical click sound
        if (this.audio) {
          if (Math.abs(turns) === 2) {
            this.audio.playDoubleRotationClick();
          } else {
            this.audio.playClick();
          }
        }

        // Record history
        if (!move.isScramble && !move.isUndoAction) {
          this.history.push(move);
        }

        if (this.onMoveEnd) {
          this.onMoveEnd(move);
        }

        // Check if solved (only for player moves)
        if (!move.isScramble && this.cubeState.isSolved()) {
          if (this.audio) this.audio.playVictory();
          if (this.onSolved) this.onSolved();
        }

        // Run next in queue
        this.processNext();
      }
    };

    requestAnimationFrame(animateStep);
  }
}

window.NOTATION_MAP = NOTATION_MAP;
window.RotationManager = RotationManager;
