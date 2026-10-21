/**
 * Rubik 3D - Main Application Bootstrap
 * Connects 3D Scene, Audio, Math State, Controls, Timer, and UI.
 */

class RubikApp {
  constructor() {
    this.container = document.getElementById('canvas-container');

    // 1. Core Systems
    this.audio = new MechanicalAudio();
    this.cubeState = new CubeState();
    this.cubeView = new CubeView(this.container, this.cubeState);
    this.rotMgr = new RotationManager(this.cubeView, this.cubeState, this.audio);
    this.controls = new CubeControls(this.cubeView, this.rotMgr, this.audio);
    this.scrambleGen = new ScrambleGenerator(this.rotMgr);

    // 2. Timer & Counter
    this.timer = new GameTimer((formattedTime) => {
      if (this.ui) this.ui.updateTimer(formattedTime);
    });

    // 3. UI Controller
    this.ui = new UIController(this);

    // 4. Hook Rotation Callbacks
    this.rotMgr.onMoveStart = () => {
      const moves = this.timer.incrementMove();
      this.ui.updateMoves(moves);
    };

    this.rotMgr.onSolved = () => {
      const finalMs = this.timer.stop();
      this.ui.showVictory(this.timer.format(finalMs), this.timer.moveCount);
    };

    // Global hook for controls (e.g. Space key)
    window.cubeScramble = () => this.scramble();

    // 5. Start Render Loop
    this.render = this.render.bind(this);
    requestAnimationFrame(this.render);
  }

  executeMove(notation) {
    if (this.audio) this.audio.ensureActive();
    this.rotMgr.executeNotation(notation);
  }

  scramble() {
    if (this.audio) this.audio.ensureActive();
    this.timer.reset();
    this.ui.updateMoves(0);
    this.ui.updateTimer('00:00.00');
    this.rotMgr.clearHistory();
    this.scrambleGen.scramble(22);
  }

  reset() {
    if (this.audio) this.audio.ensureActive();
    this.timer.reset();
    this.ui.updateMoves(0);
    this.ui.updateTimer('00:00.00');
    this.rotMgr.clearHistory();
    this.cubeState.reset();
    this.cubeView.syncWithState();
  }

  undo() {
    if (this.audio) this.audio.ensureActive();
    this.rotMgr.undo();
  }

  render() {
    this.cubeView.render();
    requestAnimationFrame(this.render);
  }
}

// Bootstrap on DOM ready
window.addEventListener('DOMContentLoaded', () => {
  window.app = new RubikApp();
});
