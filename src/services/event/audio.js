/**
 * Audio Service for musical note playback
 * Uses iframe to get clean AudioContext (avoiding conflicts with iJewel3d SDK)
 */
import { getDiamondConfig, DIAMOND_CONFIGS } from '../../constants/eventConstants';

// Note frequencies (Hz) - Standard tuning A4 = 440Hz
// 7 notes: Octave 4 only (Đô - Si)
const NOTE_FREQUENCIES = {
  C4: 261.63,   // Đô
  D4: 293.66,   // Rê
  E4: 329.63,   // Mi
  F4: 349.23,   // Pha
  G4: 392.00,   // Son
  A4: 440.00,   // La
  B4: 493.88,   // Si
};

// Map Y position (0-6) on staff to note
// 7 notes (C4-B4) mapped to 7 positions
// Position 0 = bottom (Đô/C4), Position 6 = top (Si/B4)
// Counting from bottom to top (từ dưới lên)
export const POSITION_TO_NOTE = {
  0: 'C4',  // Line 3 (bottom) - Đô (lowest)
  1: 'D4',  // Space 2         - Rê
  2: 'E4',  // Line 2          - Mi
  3: 'F4',  // Space 1         - Pha
  4: 'G4',  // Line 1          - Son
  5: 'A4',  // Space 0         - La
  6: 'B4',  // Line 0 (top)    - Si (highest)
};

// Vietnamese note names for display - 7 notes only
export const NOTE_NAMES_VI = {
  'C4': 'Đô', 'D4': 'Rê', 'E4': 'Mi', 'F4': 'Pha', 'G4': 'Son', 'A4': 'La', 'B4': 'Si',
};

// All 7 notes in order (for scale playback)
export const ALL_NOTES = ['C4', 'D4', 'E4', 'F4', 'G4', 'A4', 'B4'];

// Get Vietnamese name for a position
export const getNoteName = (positionY) => {
  const note = POSITION_TO_NOTE[positionY];
  return NOTE_NAMES_VI[note] || note;
};

let audioCtx = null;
let audioIframe = null;
let audioReady = false;

/**
 * Get the real AudioContext constructor from a clean iframe
 * (bypasses overrides from libraries like iJewel3d)
 */
function getRealAudioContextClass() {
  if (!audioIframe) {
    audioIframe = document.createElement('iframe');
    audioIframe.style.cssText = 'position:absolute;width:0;height:0;border:0;pointer-events:none;';
    audioIframe.setAttribute('aria-hidden', 'true');
    document.body.appendChild(audioIframe);
  }

  const iframeWindow = audioIframe.contentWindow;
  if (!iframeWindow) return null;

  return iframeWindow.AudioContext || iframeWindow.webkitAudioContext;
}

// Pre-create iframe on module load for faster first interaction
if (typeof document !== 'undefined') {
  setTimeout(() => getRealAudioContextClass(), 0);
}

/**
 * Get or create AudioContext
 */
function getAudioContext() {
  if (audioCtx && typeof audioCtx.createGain === 'function' && audioCtx.state !== 'closed') {
    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
    return audioCtx;
  }

  const AC = getRealAudioContextClass();
  if (!AC) return null;

  try {
    audioCtx = new AC();
    if (typeof audioCtx.createGain !== 'function') {
      audioCtx = null;
      return null;
    }
    return audioCtx;
  } catch (e) {
    return null;
  }
}

/**
 * Initialize audio - call from user gesture (click/touch)
 * Returns a promise that resolves when audio is ready to play
 */
export async function initAudio() {
  const ctx = getAudioContext();
  if (!ctx) return false;

  // If already ready, return immediately
  if (audioReady && ctx.state === 'running') {
    return true;
  }

  // Resume if suspended (Safari requires this on user gesture)
  if (ctx.state === 'suspended') {
    try {
      await ctx.resume();
    } catch (e) {
      // Ignore resume errors
    }
  }

  // Play silent buffer to unlock on iOS Safari
  try {
    const buffer = ctx.createBuffer(1, 1, 22050);
    const source = ctx.createBufferSource();
    source.buffer = buffer;
    source.connect(ctx.destination);
    source.start(0);
  } catch (e) {
    // Ignore
  }

  // Wait a frame to ensure Safari has processed the unlock
  await new Promise(resolve => setTimeout(resolve, 50));

  audioReady = true;
  return true;
}

/**
 * Check if audio is initialized
 */
export function isAudioInitialized() {
  return audioCtx && audioCtx.state === 'running';
}

// Current instrument type - default to Harp
let currentInstrument = 'harp';

/**
 * Set the instrument type for playback
 */
export function setInstrument(type) {
  currentInstrument = type;
}

/**
 * Get current instrument type
 */
export function getInstrument() {
  return currentInstrument;
}

/**
 * Play a single note with the selected instrument
 */
export async function playNote(pitch, duration = 1.5) {
  if (!audioReady) {
    await initAudio();
  }

  const ctx = getAudioContext();
  if (!ctx || typeof ctx.createGain !== 'function') return;

  try {
    if (ctx.state === 'suspended') {
      await ctx.resume();
    }

    const frequency = NOTE_FREQUENCIES[pitch] || 440;

    switch (currentInstrument) {
      case 'harp':
        playHarp(ctx, frequency, duration);
        break;
      case 'violin':
        playViolin(ctx, frequency, duration);
        break;
      case 'celesta':
      default:
        playCelesta(ctx, frequency, duration);
        break;
    }
  } catch (error) {
    // Silent fail
  }
}

/**
 * CELESTA - Crystal clear, sparkly like diamonds ✨
 * Bright, magical, bell-like tone
 */
function playCelesta(ctx, frequency, duration) {
  const now = ctx.currentTime;
  const masterGain = ctx.createGain();
  masterGain.connect(ctx.destination);

  // Celesta envelope - quick attack, shimmering decay
  masterGain.gain.setValueAtTime(0, now);
  masterGain.gain.linearRampToValueAtTime(0.5, now + 0.005);
  masterGain.gain.exponentialRampToValueAtTime(0.3, now + 0.1);
  masterGain.gain.exponentialRampToValueAtTime(0.001, now + duration);

  // Celesta harmonics - bell-like with strong higher partials
  const harmonics = [
    { ratio: 1, gain: 1.0 },      // Fundamental
    { ratio: 2, gain: 0.6 },      // Octave - bright
    { ratio: 3, gain: 0.3 },      // 5th
    { ratio: 4, gain: 0.25 },     // 2nd octave
    { ratio: 5.5, gain: 0.15 },   // Sparkle
    { ratio: 8, gain: 0.08 },     // High shimmer
  ];

  const oscillators = harmonics.map(({ ratio, gain: g }) => {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(frequency * ratio, now);

    // Higher harmonics decay faster
    const decayTime = duration * (1 - ratio * 0.08);
    gain.gain.setValueAtTime(0, now);
    gain.gain.linearRampToValueAtTime(g * 0.2, now + 0.005);
    gain.gain.exponentialRampToValueAtTime(0.001, now + Math.max(0.1, decayTime));

    osc.connect(gain);
    gain.connect(masterGain);
    osc.start(now);
    osc.stop(now + duration + 0.1);

    return { osc, gain };
  });

  // Add sparkle/shimmer effect
  const sparkleOsc = ctx.createOscillator();
  const sparkleGain = ctx.createGain();
  sparkleOsc.type = 'sine';
  sparkleOsc.frequency.setValueAtTime(frequency * 6, now);
  sparkleGain.gain.setValueAtTime(0, now);
  sparkleGain.gain.linearRampToValueAtTime(0.1, now + 0.002);
  sparkleGain.gain.exponentialRampToValueAtTime(0.001, now + 0.15);
  sparkleOsc.connect(sparkleGain);
  sparkleGain.connect(masterGain);
  sparkleOsc.start(now);
  sparkleOsc.stop(now + 0.2);

  // Cleanup
  setTimeout(() => {
    oscillators.forEach(({ osc, gain }) => {
      try { osc.disconnect(); gain.disconnect(); } catch (e) {}
    });
    try { sparkleOsc.disconnect(); sparkleGain.disconnect(); masterGain.disconnect(); } catch (e) {}
  }, (duration + 0.2) * 1000);
}

/**
 * HARP - Elegant, ethereal, gentle plucked strings 🎵
 * Warm, flowing, romantic tone
 */
function playHarp(ctx, frequency, duration) {
  const now = ctx.currentTime;
  const masterGain = ctx.createGain();
  masterGain.connect(ctx.destination);

  // Harp envelope - soft pluck attack, long gentle decay
  masterGain.gain.setValueAtTime(0, now);
  masterGain.gain.linearRampToValueAtTime(0.45, now + 0.015);
  masterGain.gain.exponentialRampToValueAtTime(0.25, now + 0.2);
  masterGain.gain.exponentialRampToValueAtTime(0.001, now + duration * 1.2);

  // Harp harmonics - warm, rich but not harsh
  const harmonics = [
    { ratio: 1, gain: 1.0 },      // Fundamental - strong
    { ratio: 2, gain: 0.35 },     // Octave
    { ratio: 3, gain: 0.15 },     // 5th - warmth
    { ratio: 4, gain: 0.08 },     // 2nd octave
    { ratio: 5, gain: 0.03 },     // Subtle brightness
  ];

  const oscillators = harmonics.map(({ ratio, gain: g }) => {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(frequency * ratio, now);

    const decayTime = duration * (1.2 - ratio * 0.1);
    gain.gain.setValueAtTime(0, now);
    gain.gain.linearRampToValueAtTime(g * 0.18, now + 0.015);
    gain.gain.exponentialRampToValueAtTime(g * 0.08, now + 0.3);
    gain.gain.exponentialRampToValueAtTime(0.001, now + Math.max(0.2, decayTime));

    osc.connect(gain);
    gain.connect(masterGain);
    osc.start(now);
    osc.stop(now + duration * 1.3);

    return { osc, gain };
  });

  // Pluck transient - soft "thump"
  const pluckOsc = ctx.createOscillator();
  const pluckGain = ctx.createGain();
  const pluckFilter = ctx.createBiquadFilter();

  pluckOsc.type = 'triangle';
  pluckOsc.frequency.setValueAtTime(frequency * 2, now);
  pluckFilter.type = 'lowpass';
  pluckFilter.frequency.value = frequency * 4;

  pluckGain.gain.setValueAtTime(0, now);
  pluckGain.gain.linearRampToValueAtTime(0.12, now + 0.005);
  pluckGain.gain.exponentialRampToValueAtTime(0.001, now + 0.08);

  pluckOsc.connect(pluckFilter);
  pluckFilter.connect(pluckGain);
  pluckGain.connect(masterGain);
  pluckOsc.start(now);
  pluckOsc.stop(now + 0.1);

  // Cleanup
  setTimeout(() => {
    oscillators.forEach(({ osc, gain }) => {
      try { osc.disconnect(); gain.disconnect(); } catch (e) {}
    });
    try { pluckOsc.disconnect(); pluckFilter.disconnect(); pluckGain.disconnect(); masterGain.disconnect(); } catch (e) {}
  }, (duration * 1.3 + 0.2) * 1000);
}

/**
 * VIOLIN - Rich, expressive, singing tone 🎻
 * Warm, emotional, with vibrato
 */
function playViolin(ctx, frequency, duration) {
  const now = ctx.currentTime;
  const masterGain = ctx.createGain();
  masterGain.connect(ctx.destination);

  // Violin envelope - bow attack, sustained, gentle release
  masterGain.gain.setValueAtTime(0, now);
  masterGain.gain.linearRampToValueAtTime(0.35, now + 0.08);    // Bow attack
  masterGain.gain.linearRampToValueAtTime(0.4, now + 0.2);      // Slight swell
  masterGain.gain.linearRampToValueAtTime(0.35, now + duration * 0.7);
  masterGain.gain.exponentialRampToValueAtTime(0.001, now + duration);

  // Violin harmonics - sawtooth-like but smoother
  const harmonics = [
    { ratio: 1, gain: 1.0 },
    { ratio: 2, gain: 0.5 },
    { ratio: 3, gain: 0.35 },
    { ratio: 4, gain: 0.25 },
    { ratio: 5, gain: 0.15 },
    { ratio: 6, gain: 0.1 },
    { ratio: 7, gain: 0.06 },
  ];

  // Vibrato LFO
  const vibratoOsc = ctx.createOscillator();
  const vibratoGain = ctx.createGain();
  vibratoOsc.type = 'sine';
  vibratoOsc.frequency.value = 5.5; // Vibrato rate
  vibratoGain.gain.value = frequency * 0.008; // Vibrato depth

  vibratoOsc.connect(vibratoGain);
  vibratoOsc.start(now + 0.15); // Vibrato starts after attack
  vibratoOsc.stop(now + duration);

  const oscillators = harmonics.map(({ ratio, gain: g }) => {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(frequency * ratio, now);

    // Connect vibrato to frequency
    vibratoGain.connect(osc.frequency);

    gain.gain.setValueAtTime(0, now);
    gain.gain.linearRampToValueAtTime(g * 0.12, now + 0.08);
    gain.gain.linearRampToValueAtTime(g * 0.1, now + duration * 0.7);
    gain.gain.exponentialRampToValueAtTime(0.001, now + duration);

    osc.connect(gain);
    gain.connect(masterGain);
    osc.start(now);
    osc.stop(now + duration + 0.1);

    return { osc, gain };
  });

  // Bow noise - adds realism
  const noiseBuffer = ctx.createBuffer(1, ctx.sampleRate * 0.1, ctx.sampleRate);
  const noiseData = noiseBuffer.getChannelData(0);
  for (let i = 0; i < noiseData.length; i++) {
    noiseData[i] = (Math.random() * 2 - 1) * 0.3;
  }

  const noiseSource = ctx.createBufferSource();
  const noiseGain = ctx.createGain();
  const noiseFilter = ctx.createBiquadFilter();

  noiseSource.buffer = noiseBuffer;
  noiseFilter.type = 'bandpass';
  noiseFilter.frequency.value = frequency * 3;
  noiseFilter.Q.value = 2;

  noiseGain.gain.setValueAtTime(0, now);
  noiseGain.gain.linearRampToValueAtTime(0.03, now + 0.02);
  noiseGain.gain.exponentialRampToValueAtTime(0.001, now + 0.1);

  noiseSource.connect(noiseFilter);
  noiseFilter.connect(noiseGain);
  noiseGain.connect(masterGain);
  noiseSource.start(now);

  // Cleanup
  setTimeout(() => {
    oscillators.forEach(({ osc, gain }) => {
      try { osc.disconnect(); gain.disconnect(); } catch (e) {}
    });
    try {
      vibratoOsc.disconnect();
      vibratoGain.disconnect();
      noiseSource.disconnect();
      noiseFilter.disconnect();
      noiseGain.disconnect();
      masterGain.disconnect();
    } catch (e) {}
  }, (duration + 0.2) * 1000);
}

/**
 * Play note by name directly (e.g., 'C4', 'D5')
 */
export async function playNoteByName(noteName) {
  if (NOTE_FREQUENCIES[noteName]) {
    await playNote(noteName);
  }
}

/**
 * Play note by Y position on staff
 */
export async function playNoteByPosition(positionY) {
  const note = POSITION_TO_NOTE[positionY];
  if (note) await playNote(note);
}

/**
 * Play note by diamond shape
 */
export async function playDiamondNote(shape) {
  const config = getDiamondConfig(shape);
  if (config) await playNote(config.pitch);
}

/**
 * Play a sequence of notes
 */
export async function playNoteSequence(pitches, tempo = 120) {
  await initAudio();
  const noteDuration = 60 / tempo;

  for (let i = 0; i < pitches.length; i++) {
    await playNote(pitches[i], 0.3);
    await new Promise((resolve) => setTimeout(resolve, noteDuration * 500));
  }
}

/**
 * Play all diamond notes in sequence
 */
export function playScale() {
  const pitches = DIAMOND_CONFIGS.map((d) => d.pitch);
  playNoteSequence(pitches, 120);
}

/**
 * Play notes from an array of Note objects
 */
export function playNotesFromArray(notes, tempo = 100) {
  if (!notes || notes.length === 0) return;

  const sortedNotes = [...notes].sort((a, b) => a.positionX - b.positionX);
  const pitches = sortedNotes.map((note) => {
    const config = getDiamondConfig(note.diamondShape);
    return config?.pitch || 'C4';
  });

  playNoteSequence(pitches, tempo);
}

/**
 * Stop all sounds
 */
export function stopAll() {
  // Web Audio oscillators auto-stop
}

/**
 * Dispose audio resources
 */
export function disposeAudio() {
  if (audioCtx && typeof audioCtx.close === 'function') {
    audioCtx.close();
  }
  audioCtx = null;
  audioReady = false;

  if (audioIframe && audioIframe.parentNode) {
    audioIframe.parentNode.removeChild(audioIframe);
  }
  audioIframe = null;
}
