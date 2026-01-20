/**
 * "We Wish You A Merry Christmas" - Sheet Music Data
 * Based on traditional arrangement with treble and bass clefs
 */

// Note frequencies (extended range)
export const NOTE_FREQUENCIES = {
  // Octave 3 (bass)
  C3: 130.81,
  D3: 146.83,
  E3: 164.81,
  F3: 174.61,
  G3: 196.00,
  A3: 220.00,
  B3: 246.94,
  // Octave 4
  C4: 261.63,
  D4: 293.66,
  E4: 329.63,
  F4: 349.23,
  G4: 392.00,
  A4: 440.00,
  B4: 493.88,
  // Octave 5
  C5: 523.25,
  D5: 587.33,
  E5: 659.25,
  F5: 698.46,
  G5: 783.99,
  A5: 880.00,
  B5: 987.77,
};

// Staff line positions (from bottom to top)
// Treble clef: E4=0, F4=0.5, G4=1, A4=1.5, B4=2, C5=2.5, D5=3, E5=3.5, F5=4
// Bass clef: G2=0, A2=0.5, B2=1, C3=1.5, D3=2, E3=2.5, F3=3, G3=3.5, A3=4

export const TREBLE_POSITIONS = {
  C4: -1,    // Below staff
  D4: -0.5,
  E4: 0,     // First line
  F4: 0.5,
  G4: 1,     // Second line
  A4: 1.5,
  B4: 2,     // Third line
  C5: 2.5,
  D5: 3,     // Fourth line
  E5: 3.5,
  F5: 4,     // Fifth line
  G5: 4.5,
  A5: 5,
};

export const BASS_POSITIONS = {
  C3: 1.5,
  D3: 2,
  E3: 2.5,
  F3: 3,
  G3: 3.5,
  A3: 4,
  B3: 4.5,
  C4: 5,
};

// Song tempo (BPM) - faster for more lively feel
export const SONG_TEMPO = 150;

// Time signature: 3/4
export const TIME_SIGNATURE = { beats: 3, noteValue: 4 };

/**
 * Note data structure:
 * {
 *   pitch: string,      // Note name (e.g., "G4")
 *   duration: number,   // In beats (1 = quarter note, 0.5 = eighth note)
 *   startBeat: number,  // When the note starts
 *   clef: 'treble' | 'bass',
 *   isChord: boolean,   // If part of a chord (plays simultaneously)
 * }
 */

// Melody (treble clef) - "We Wish You A Merry Christmas"
export const MELODY_NOTES = [
  // Bar 1: "We"
  { pitch: 'G4', duration: 1, startBeat: 0, clef: 'treble' },

  // Bar 2: "wish you a"
  { pitch: 'C5', duration: 1, startBeat: 3, clef: 'treble' },
  { pitch: 'C5', duration: 0.5, startBeat: 4, clef: 'treble' },
  { pitch: 'D5', duration: 0.5, startBeat: 4.5, clef: 'treble' },
  { pitch: 'C5', duration: 0.5, startBeat: 5, clef: 'treble' },
  { pitch: 'B4', duration: 0.5, startBeat: 5.5, clef: 'treble' },

  // Bar 3: "mer-ry"
  { pitch: 'A4', duration: 1, startBeat: 6, clef: 'treble' },
  { pitch: 'A4', duration: 1, startBeat: 7, clef: 'treble' },
  { pitch: 'A4', duration: 1, startBeat: 8, clef: 'treble' },

  // Bar 4: "Christ-mas, We"
  { pitch: 'D5', duration: 1, startBeat: 9, clef: 'treble' },
  { pitch: 'D5', duration: 0.5, startBeat: 10, clef: 'treble' },
  { pitch: 'E5', duration: 0.5, startBeat: 10.5, clef: 'treble' },
  { pitch: 'D5', duration: 0.5, startBeat: 11, clef: 'treble' },
  { pitch: 'C5', duration: 0.5, startBeat: 11.5, clef: 'treble' },

  // Bar 5: "wish you a"
  { pitch: 'B4', duration: 1, startBeat: 12, clef: 'treble' },
  { pitch: 'G4', duration: 1, startBeat: 13, clef: 'treble' },
  { pitch: 'G4', duration: 1, startBeat: 14, clef: 'treble' },

  // Bar 6: "mer-ry"
  { pitch: 'E5', duration: 1, startBeat: 15, clef: 'treble' },
  { pitch: 'E5', duration: 0.5, startBeat: 16, clef: 'treble' },
  { pitch: 'F5', duration: 0.5, startBeat: 16.5, clef: 'treble' },
  { pitch: 'E5', duration: 0.5, startBeat: 17, clef: 'treble' },
  { pitch: 'D5', duration: 0.5, startBeat: 17.5, clef: 'treble' },

  // Bar 7: "Christ-mas, We"
  { pitch: 'C5', duration: 1, startBeat: 18, clef: 'treble' },
  { pitch: 'A4', duration: 1, startBeat: 19, clef: 'treble' },
  { pitch: 'G4', duration: 0.5, startBeat: 20, clef: 'treble' },
  { pitch: 'G4', duration: 0.5, startBeat: 20.5, clef: 'treble' },

  // Bar 8: "wish you a"
  { pitch: 'A4', duration: 1, startBeat: 21, clef: 'treble' },
  { pitch: 'D5', duration: 1, startBeat: 22, clef: 'treble' },
  { pitch: 'B4', duration: 1, startBeat: 23, clef: 'treble' },

  // Bar 9: "mer-ry"
  { pitch: 'C5', duration: 2, startBeat: 24, clef: 'treble' },
  { pitch: 'G4', duration: 1, startBeat: 26, clef: 'treble' },

  // Bar 10-11: "Christ-mas and a"
  { pitch: 'C5', duration: 1, startBeat: 27, clef: 'treble' },
  { pitch: 'C5', duration: 1, startBeat: 28, clef: 'treble' },
  { pitch: 'C5', duration: 1, startBeat: 29, clef: 'treble' },
  { pitch: 'B4', duration: 1, startBeat: 30, clef: 'treble' },
  { pitch: 'B4', duration: 1, startBeat: 31, clef: 'treble' },

  // Bar 12: "hap-py New"
  { pitch: 'C5', duration: 1, startBeat: 32, clef: 'treble' },
  { pitch: 'B4', duration: 1, startBeat: 33, clef: 'treble' },
  { pitch: 'A4', duration: 1, startBeat: 34, clef: 'treble' },

  // Bar 13: "Year!"
  { pitch: 'G4', duration: 2, startBeat: 35, clef: 'treble' },

  // ============ CHORUS: "Good tidings we bring" ============
  // Bar 14: "Good"
  { pitch: 'D5', duration: 1, startBeat: 38, clef: 'treble' },

  // Bar 15: "ti-dings we"
  { pitch: 'E5', duration: 1, startBeat: 39, clef: 'treble' },
  { pitch: 'E5', duration: 0.5, startBeat: 40, clef: 'treble' },
  { pitch: 'F5', duration: 0.5, startBeat: 40.5, clef: 'treble' },
  { pitch: 'E5', duration: 0.5, startBeat: 41, clef: 'treble' },
  { pitch: 'D5', duration: 0.5, startBeat: 41.5, clef: 'treble' },

  // Bar 16: "bring to"
  { pitch: 'C5', duration: 1, startBeat: 42, clef: 'treble' },
  { pitch: 'A4', duration: 1, startBeat: 43, clef: 'treble' },
  { pitch: 'A4', duration: 1, startBeat: 44, clef: 'treble' },

  // Bar 17: "you and your"
  { pitch: 'A4', duration: 1, startBeat: 45, clef: 'treble' },
  { pitch: 'B4', duration: 1, startBeat: 46, clef: 'treble' },
  { pitch: 'A4', duration: 0.5, startBeat: 47, clef: 'treble' },
  { pitch: 'G4', duration: 0.5, startBeat: 47.5, clef: 'treble' },

  // Bar 18: "kin, Good"
  { pitch: 'D5', duration: 2, startBeat: 48, clef: 'treble' },
  { pitch: 'D5', duration: 1, startBeat: 50, clef: 'treble' },

  // Bar 19: "ti-dings for"
  { pitch: 'E5', duration: 1, startBeat: 51, clef: 'treble' },
  { pitch: 'E5', duration: 0.5, startBeat: 52, clef: 'treble' },
  { pitch: 'F5', duration: 0.5, startBeat: 52.5, clef: 'treble' },
  { pitch: 'E5', duration: 0.5, startBeat: 53, clef: 'treble' },
  { pitch: 'D5', duration: 0.5, startBeat: 53.5, clef: 'treble' },

  // Bar 20: "Christ-mas and a"
  { pitch: 'C5', duration: 1, startBeat: 54, clef: 'treble' },
  { pitch: 'G4', duration: 0.5, startBeat: 55, clef: 'treble' },
  { pitch: 'G4', duration: 0.5, startBeat: 55.5, clef: 'treble' },
  { pitch: 'A4', duration: 0.5, startBeat: 56, clef: 'treble' },
  { pitch: 'D5', duration: 0.5, startBeat: 56.5, clef: 'treble' },

  // Bar 21: "hap-py New"
  { pitch: 'B4', duration: 1, startBeat: 57, clef: 'treble' },
  { pitch: 'C5', duration: 1, startBeat: 58, clef: 'treble' },
  { pitch: 'B4', duration: 0.5, startBeat: 59, clef: 'treble' },
  { pitch: 'A4', duration: 0.5, startBeat: 59.5, clef: 'treble' },

  // Bar 22: "Year!"
  { pitch: 'G4', duration: 3, startBeat: 60, clef: 'treble' },
];

// Bass accompaniment (bass clef)
export const BASS_NOTES = [
  // Bar 1-2
  { pitch: 'C3', duration: 3, startBeat: 0, clef: 'bass' },
  { pitch: 'C3', duration: 3, startBeat: 3, clef: 'bass' },

  // Bar 3
  { pitch: 'F3', duration: 3, startBeat: 6, clef: 'bass' },

  // Bar 4-5
  { pitch: 'G3', duration: 3, startBeat: 9, clef: 'bass' },
  { pitch: 'G3', duration: 3, startBeat: 12, clef: 'bass' },

  // Bar 6-7
  { pitch: 'C3', duration: 3, startBeat: 15, clef: 'bass' },
  { pitch: 'F3', duration: 3, startBeat: 18, clef: 'bass' },

  // Bar 8-9
  { pitch: 'G3', duration: 3, startBeat: 21, clef: 'bass' },
  { pitch: 'C3', duration: 3, startBeat: 24, clef: 'bass' },

  // Bar 10-11
  { pitch: 'C3', duration: 3, startBeat: 27, clef: 'bass' },
  { pitch: 'G3', duration: 3, startBeat: 30, clef: 'bass' },

  // Bar 12-13
  { pitch: 'F3', duration: 3, startBeat: 33, clef: 'bass' },
  { pitch: 'C3', duration: 3, startBeat: 36, clef: 'bass' },

  // ============ CHORUS BASS ============
  // Bar 14-15: "Good tidings we bring"
  { pitch: 'G3', duration: 3, startBeat: 38, clef: 'bass' },
  { pitch: 'C3', duration: 3, startBeat: 41, clef: 'bass' },

  // Bar 16-17: "to you and your kin"
  { pitch: 'F3', duration: 3, startBeat: 44, clef: 'bass' },
  { pitch: 'G3', duration: 3, startBeat: 47, clef: 'bass' },

  // Bar 18-19: "Good tidings for"
  { pitch: 'G3', duration: 3, startBeat: 50, clef: 'bass' },
  { pitch: 'C3', duration: 3, startBeat: 53, clef: 'bass' },

  // Bar 20-21: "Christmas and a happy New"
  { pitch: 'F3', duration: 3, startBeat: 56, clef: 'bass' },
  { pitch: 'G3', duration: 3, startBeat: 59, clef: 'bass' },

  // Bar 22: "Year!"
  { pitch: 'C3', duration: 3, startBeat: 62, clef: 'bass' },
];

// Combine all notes
export const ALL_NOTES = [...MELODY_NOTES, ...BASS_NOTES].sort(
  (a, b) => a.startBeat - b.startBeat
);

// Song metadata
export const SONG_INFO = {
  title: 'We Wish You A Merry Christmas',
  titleShort: 'Merry Christmas',
  composer: 'Traditional English Carol',
  totalBars: 22,
  totalBeats: 65,
  durationSeconds: (65 / SONG_TEMPO) * 60,
};

// Calculate pixel position for a note on the staff
export const calculateNotePosition = (pitch, clef) => {
  if (clef === 'treble') {
    return TREBLE_POSITIONS[pitch] ?? 2;
  }
  return BASS_POSITIONS[pitch] ?? 2;
};

// Get color based on clef (like in the video)
export const getNoteColor = (clef) => {
  return clef === 'treble' ? '#FF6B6B' : '#4ECDC4'; // Pink/orange for treble, cyan for bass
};

// Bar line positions (in beats)
export const BAR_LINES = [0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45, 48, 51, 54, 57, 60, 63, 66];
