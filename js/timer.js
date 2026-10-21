/**
 * Game Timer & Move Counter
 * Tracks exact elapsed time with millisecond precision (00:00.00)
 * Uses performance.now() to prevent drift.
 */

class GameTimer {
  constructor(onTick = null) {
    this.onTick = onTick;

    this.startTime = 0;
    this.elapsedBeforePause = 0;
    this.isRunning = false;
    this.timerId = null;

    this.moveCount = 0;
    this.hasStartedPlaying = false; // Starts on first manual turn after scramble/reset
  }

  start() {
    if (this.isRunning) return;
    this.isRunning = true;
    this.startTime = performance.now() - this.elapsedBeforePause;

    this.timerId = requestAnimationFrame(() => this.updateLoop());
  }

  updateLoop() {
    if (!this.isRunning) return;
    const currentElapsed = performance.now() - this.startTime;
    if (this.onTick) {
      this.onTick(this.format(currentElapsed), currentElapsed);
    }
    this.timerId = requestAnimationFrame(() => this.updateLoop());
  }

  stop() {
    if (!this.isRunning) return this.elapsedBeforePause;
    this.isRunning = false;
    if (this.timerId) cancelAnimationFrame(this.timerId);
    this.elapsedBeforePause = performance.now() - this.startTime;
    return this.elapsedBeforePause;
  }

  reset() {
    this.stop();
    this.elapsedBeforePause = 0;
    this.moveCount = 0;
    this.hasStartedPlaying = false;
    if (this.onTick) {
      this.onTick('00:00.00', 0);
    }
  }

  incrementMove() {
    this.moveCount++;
    if (!this.isRunning && !this.hasStartedPlaying) {
      this.hasStartedPlaying = true;
      this.start();
    }
    return this.moveCount;
  }

  format(ms) {
    const totalCentis = Math.floor(ms / 10);
    const centis = totalCentis % 100;
    const totalSecs = Math.floor(totalCentis / 100);
    const secs = totalSecs % 60;
    const mins = Math.floor(totalSecs / 60);

    const mm = String(mins).padStart(2, '0');
    const ss = String(secs).padStart(2, '0');
    const cc = String(centis).padStart(2, '0');
    return `${mm}:${ss}.${cc}`;
  }
}

window.GameTimer = GameTimer;
