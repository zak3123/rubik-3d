/**
 * Mechanical Audio System for Rubik 3D
 * Synthesizes realistic mechanical clicks, friction sliding, and victory fanfare
 * using the Web Audio API without any external audio files.
 */
class MechanicalAudio {
  constructor() {
    this.ctx = null;
    this.masterGain = null;
    this.soundEnabled = true;
    this.volume = 0.6; // default 60%
    this.activeFrictionNodes = new Set();
  }

  init() {
    if (this.ctx) return;
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return;

    this.ctx = new AudioCtx();
    this.masterGain = this.ctx.createGain();
    this.masterGain.gain.setValueAtTime(this.soundEnabled ? this.volume : 0, this.ctx.currentTime);
    this.masterGain.connect(this.ctx.destination);
  }

  ensureActive() {
    this.init();
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  setVolume(val) {
    this.volume = Math.max(0, Math.min(1, val));
    if (this.masterGain && this.ctx) {
      this.masterGain.gain.setValueAtTime(this.soundEnabled ? this.volume : 0, this.ctx.currentTime);
    }
  }

  setSoundEnabled(enabled) {
    this.soundEnabled = !!enabled;
    if (this.masterGain && this.ctx) {
      this.masterGain.gain.setValueAtTime(this.soundEnabled ? this.volume : 0, this.ctx.currentTime);
    }
  }

  /**
   * Sharp mechanical lock click (90° snap)
   * Pitch varies slightly for natural acoustic variation.
   */
  playClick(pitchMod = 1.0) {
    if (!this.soundEnabled) return;
    this.ensureActive();
    if (!this.ctx) return;

    const t = this.ctx.currentTime;

    // 1. High transient impact (plastic edge snap)
    const bufLen = Math.floor(this.ctx.sampleRate * 0.025);
    const noiseBuffer = this.ctx.createBuffer(1, bufLen, this.ctx.sampleRate);
    const data = noiseBuffer.getChannelData(0);
    for (let i = 0; i < bufLen; i++) {
      data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (this.ctx.sampleRate * 0.004));
    }
    const noise = this.ctx.createBufferSource();
    noise.buffer = noiseBuffer;

    const bandpass = this.ctx.createBiquadFilter();
    bandpass.type = 'bandpass';
    bandpass.frequency.setValueAtTime((1800 + Math.random() * 300) * pitchMod, t);
    bandpass.Q.setValueAtTime(4.0, t);

    const noiseGain = this.ctx.createGain();
    noiseGain.gain.setValueAtTime(0.75, t);
    noiseGain.gain.exponentialRampToValueAtTime(0.001, t + 0.025);

    noise.connect(bandpass);
    bandpass.connect(noiseGain);
    noiseGain.connect(this.masterGain);
    noise.start(t);

    // 2. Hollow plastic body resonance ("clack")
    const osc = this.ctx.createOscillator();
    const oscGain = this.ctx.createGain();
    const baseFreq = (240 + Math.random() * 40) * pitchMod;
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(baseFreq, t);
    osc.frequency.exponentialRampToValueAtTime(baseFreq * 0.35, t + 0.04);

    oscGain.gain.setValueAtTime(0.85, t);
    oscGain.gain.exponentialRampToValueAtTime(0.001, t + 0.045);

    osc.connect(oscGain);
    oscGain.connect(this.masterGain);
    osc.start(t);
    osc.stop(t + 0.048);

    // 3. Micro spring latch release
    const springOsc = this.ctx.createOscillator();
    const springGain = this.ctx.createGain();
    springOsc.type = 'sine';
    springOsc.frequency.setValueAtTime((900 + Math.random() * 150) * pitchMod, t + 0.008);
    springOsc.frequency.exponentialRampToValueAtTime(140, t + 0.025);

    springGain.gain.setValueAtTime(0.001, t);
    springGain.gain.setValueAtTime(0.2, t + 0.008);
    springGain.gain.exponentialRampToValueAtTime(0.001, t + 0.025);

    springOsc.connect(springGain);
    springGain.connect(this.masterGain);
    springOsc.start(t + 0.008);
    springOsc.stop(t + 0.028);
  }

  /**
   * Double rotation sound ("Klik-klak!")
   */
  playDoubleRotationClick() {
    this.playClick(0.95);
    setTimeout(() => {
      this.playClick(1.08);
    }, 70);
  }

  /**
   * Plastic sliding friction sound during rotation animation ("Krrk...")
   * duration in ms
   */
  playFriction(durationMs = 180) {
    if (!this.soundEnabled) return;
    this.ensureActive();
    if (!this.ctx) return;

    const t = this.ctx.currentTime;
    const durSec = Math.max(0.05, durationMs / 1000);

    const bufLen = Math.floor(this.ctx.sampleRate * durSec);
    const buf = this.ctx.createBuffer(1, bufLen, this.ctx.sampleRate);
    const channel = buf.getChannelData(0);
    for (let i = 0; i < bufLen; i++) {
      // Pinkish noise with subtle modulation
      const env = Math.sin((i / bufLen) * Math.PI);
      channel[i] = (Math.random() * 2 - 1) * env * 0.35;
    }

    const source = this.ctx.createBufferSource();
    source.buffer = buf;

    const lowpass = this.ctx.createBiquadFilter();
    lowpass.type = 'lowpass';
    lowpass.frequency.setValueAtTime(750, t);
    lowpass.frequency.linearRampToValueAtTime(600, t + durSec);

    const gain = this.ctx.createGain();
    gain.gain.setValueAtTime(0.18, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + durSec);

    source.connect(lowpass);
    lowpass.connect(gain);
    gain.connect(this.masterGain);

    source.start(t);
  }

  /**
   * Victory sound fanfare when solved
   */
  playVictory() {
    if (!this.soundEnabled) return;
    this.ensureActive();
    if (!this.ctx) return;

    const notes = [261.63, 329.63, 392.00, 523.25, 659.25, 783.99]; // C major chord arpeggio
    notes.forEach((freq, idx) => {
      const t = this.ctx.currentTime + idx * 0.08;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, t);

      gain.gain.setValueAtTime(0.001, t);
      gain.gain.linearRampToValueAtTime(0.35, t + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, t + 0.35);

      osc.connect(gain);
      gain.connect(this.masterGain);

      osc.start(t);
      osc.stop(t + 0.4);
    });
  }
}

window.MechanicalAudio = MechanicalAudio;
window.cubeAudio = new MechanicalAudio();
