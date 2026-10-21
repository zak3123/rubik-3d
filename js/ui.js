/**
 * UI Controller for Rubik 3D
 * Handles HUD, dock controls, settings modal, and victory modal.
 */

class UIController {
  constructor(app) {
    this.app = app;

    // Elements
    this.timerEl = document.getElementById('hud-timer');
    this.movesEl = document.getElementById('hud-moves');

    this.settingsModal = document.getElementById('settings-modal');
    this.winModal = document.getElementById('win-modal');

    this.winTimeEl = document.getElementById('win-time');
    this.winMovesEl = document.getElementById('win-moves');

    this.soundChk = document.getElementById('setting-sound');
    this.volumeRange = document.getElementById('setting-volume');
    this.speedRange = document.getElementById('setting-speed');
    this.volumeValEl = document.getElementById('val-volume');
    this.speedValEl = document.getElementById('val-speed');

    this.initEvents();
  }

  initEvents() {
    // Dock Actions
    const btnScramble = document.getElementById('btn-scramble');
    if (btnScramble) btnScramble.addEventListener('click', () => this.app.scramble());

    const btnReset = document.getElementById('btn-reset');
    if (btnReset) btnReset.addEventListener('click', () => this.app.reset());

    const btnUndo = document.getElementById('btn-undo');
    if (btnUndo) btnUndo.addEventListener('click', () => this.app.undo());

    const btnSettings = document.getElementById('btn-settings');
    if (btnSettings) btnSettings.addEventListener('click', () => this.openModal(this.settingsModal));

    const btnCloseSettings = document.getElementById('btn-close-settings');
    if (btnCloseSettings) btnCloseSettings.addEventListener('click', () => this.closeModal(this.settingsModal));

    const btnCloseWin = document.getElementById('btn-close-win');
    if (btnCloseWin) btnCloseWin.addEventListener('click', () => this.closeModal(this.winModal));

    const btnPlayAgain = document.getElementById('btn-play-again');
    if (btnPlayAgain) {
      btnPlayAgain.addEventListener('click', () => {
        this.closeModal(this.winModal);
        this.app.scramble();
      });
    }

    // Modal background close
    [this.settingsModal, this.winModal].forEach(modal => {
      if (!modal) return;
      modal.addEventListener('click', (e) => {
        if (e.target === modal) this.closeModal(modal);
      });
    });

    // Notation Buttons
    document.querySelectorAll('[data-move]').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const move = e.currentTarget.getAttribute('data-move');
        if (move) this.app.executeMove(move);
      });
    });

    // Settings Controls
    if (this.soundChk) {
      this.soundChk.addEventListener('change', (e) => {
        this.app.audio.setSoundEnabled(e.target.checked);
      });
    }

    if (this.volumeRange) {
      this.volumeRange.addEventListener('input', (e) => {
        const val = parseInt(e.target.value, 10);
        if (this.volumeValEl) this.volumeValEl.textContent = `${val}%`;
        this.app.audio.setVolume(val / 100);
      });
    }

    if (this.speedRange) {
      this.speedRange.addEventListener('input', (e) => {
        const val = parseInt(e.target.value, 10);
        if (this.speedValEl) this.speedValEl.textContent = `${val}ms`;
        this.app.rotMgr.setAnimationSpeed(val);
      });
    }
  }

  updateTimer(timeStr) {
    if (this.timerEl) this.timerEl.textContent = timeStr;
  }

  updateMoves(count) {
    if (this.movesEl) this.movesEl.textContent = count;
  }

  showVictory(timeStr, moveCount) {
    if (this.winTimeEl) this.winTimeEl.textContent = timeStr;
    if (this.winMovesEl) this.winMovesEl.textContent = moveCount;
    this.openModal(this.winModal);
  }

  openModal(modal) {
    if (modal) modal.classList.add('active');
  }

  closeModal(modal) {
    if (modal) modal.classList.remove('active');
  }
}

window.UIController = UIController;
