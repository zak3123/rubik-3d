/**
 * Scramble Generator
 * Generates 20-25 standard Rubik's Cube scramble moves (WCA style).
 * Ensures no consecutive redundant or cancelling moves on the same face.
 */

class ScrambleGenerator {
  constructor(rotationManager) {
    this.rotMgr = rotationManager;
    this.faces = ['U', 'D', 'L', 'R', 'F', 'B'];
    this.modifiers = ['', "'", '2'];
  }

  /**
   * Generates a valid sequence of moves
   * @param {number} length (default 22)
   * @returns {string[]} array of move notations e.g. ["R", "U'", "F2", ...]
   */
  generateSequence(length = 22) {
    const sequence = [];
    let lastFace = '';
    let secondLastFace = '';

    // Opposite faces that shouldn't alternate endlessly (e.g. U D U)
    const opposites = {
      U: 'D', D: 'U',
      L: 'R', R: 'L',
      F: 'B', B: 'F'
    };

    while (sequence.length < length) {
      const face = this.faces[Math.floor(Math.random() * this.faces.length)];

      // Don't repeat the same face consecutively
      if (face === lastFace) continue;

      // Don't do opposite moves sandwich (e.g. R L R)
      if (face === secondLastFace && opposites[face] === lastFace) continue;

      const mod = this.modifiers[Math.floor(Math.random() * this.modifiers.length)];
      sequence.push(face + mod);

      secondLastFace = lastFace;
      lastFace = face;
    }

    return sequence;
  }

  /**
   * Run scramble animation on the cube
   */
  scramble(length = 22, onComplete = null) {
    const sequence = this.generateSequence(length);

    sequence.forEach((notation, idx) => {
      this.rotMgr.executeNotation(notation, true);
    });

    if (onComplete) {
      const checkInterval = setInterval(() => {
        if (!this.rotMgr.isAnimating && this.rotMgr.queue.length === 0) {
          clearInterval(checkInterval);
          onComplete(sequence);
        }
      }, 50);
    }

    return sequence;
  }
}

window.ScrambleGenerator = ScrambleGenerator;
