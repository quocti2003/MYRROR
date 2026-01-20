/**
 * ChristmasMusicSheet - 3D Music Sheet Display with Glowing Notes Animation
 * Inspired by TikTok music visualization
 */
import React, { useEffect, useState, useRef, useCallback } from 'react';
import {
  ALL_NOTES,
  MELODY_NOTES,
  BASS_NOTES,
  SONG_INFO,
  SONG_TEMPO,
  BAR_LINES,
  NOTE_FREQUENCIES,
  calculateNotePosition,
  getNoteColor,
} from '@/constants/christmasSong';

const ChristmasMusicSheet = ({
  autoPlay = false,
  showTitle = true,
  onComplete,
  className = '',
}) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentBeat, setCurrentBeat] = useState(-1);
  const [activeNotes, setActiveNotes] = useState(new Set());
  const [glowingNotes, setGlowingNotes] = useState([]);
  const containerRef = useRef(null);
  const animationRef = useRef(null);
  const startTimeRef = useRef(null);
  const audioCtxRef = useRef(null);

  // Sheet dimensions
  const SHEET_WIDTH = 2400;
  const SHEET_HEIGHT = 400;
  const STAFF_TOP = 60;
  const STAFF_HEIGHT = 120;
  const LINE_SPACING = 30;
  const BEAT_WIDTH = 50;
  const LEFT_MARGIN = 120;

  // Initialize audio context
  const initAudio = useCallback(() => {
    try {
      if (!audioCtxRef.current) {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (AudioContext) {
          audioCtxRef.current = new AudioContext();
        }
      }
      if (audioCtxRef.current && audioCtxRef.current.state === 'suspended') {
        audioCtxRef.current.resume();
      }
      return audioCtxRef.current;
    } catch (error) {
      console.error('Failed to initialize audio:', error);
      return null;
    }
  }, []);

  // Play a note
  const playNote = useCallback((pitch, duration = 0.3) => {
    try {
      const ctx = initAudio();
      if (!ctx || typeof ctx.createOscillator !== 'function') {
        return;
      }

      const frequency = NOTE_FREQUENCIES[pitch];
      if (!frequency) return;

      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(frequency, now);

      gain.gain.setValueAtTime(0, now);
      gain.gain.linearRampToValueAtTime(0.25, now + 0.02);
      gain.gain.linearRampToValueAtTime(0.15, now + 0.1);
      gain.gain.linearRampToValueAtTime(0, now + duration);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now);
      osc.stop(now + duration + 0.05);
    } catch (error) {
      console.error('Failed to play note:', error);
    }
  }, [initAudio]);

  // Animation frame
  const animate = useCallback((timestamp) => {
    if (!startTimeRef.current) {
      startTimeRef.current = timestamp;
    }

    const elapsed = timestamp - startTimeRef.current;
    const beatDuration = (60 / SONG_TEMPO) * 1000; // ms per beat
    const currentBeatFloat = elapsed / beatDuration;

    setCurrentBeat(currentBeatFloat);

    // Find notes that should be active/glowing
    const newActiveNotes = new Set();
    const newGlowingNotes = [];

    ALL_NOTES.forEach((note, index) => {
      const noteStart = note.startBeat;
      const noteEnd = note.startBeat + note.duration;

      // Note is currently being played
      if (currentBeatFloat >= noteStart && currentBeatFloat < noteEnd) {
        newActiveNotes.add(index);

        // Check if this note just started (within last frame)
        const prevBeat = (elapsed - 16) / beatDuration;
        if (prevBeat < noteStart && currentBeatFloat >= noteStart) {
          playNote(note.pitch, note.duration * (60 / SONG_TEMPO));
        }
      }

      // Add to glowing if recently played (for trail effect)
      if (currentBeatFloat >= noteStart && currentBeatFloat < noteEnd + 2) {
        const intensity = currentBeatFloat < noteEnd
          ? 1
          : Math.max(0, 1 - (currentBeatFloat - noteEnd) / 2);
        newGlowingNotes.push({ index, intensity, note });
      }
    });

    setActiveNotes(newActiveNotes);
    setGlowingNotes(newGlowingNotes);

    // Continue animation if not finished
    if (currentBeatFloat < SONG_INFO.totalBeats + 2) {
      animationRef.current = requestAnimationFrame(animate);
    } else {
      setIsPlaying(false);
      setCurrentBeat(-1);
      setActiveNotes(new Set());
      setGlowingNotes([]);
      startTimeRef.current = null;
      onComplete?.();
    }
  }, [playNote, onComplete]);

  // Start playback
  const startPlayback = useCallback(() => {
    initAudio();
    setIsPlaying(true);
    startTimeRef.current = null;
    animationRef.current = requestAnimationFrame(animate);
  }, [animate, initAudio]);

  // Stop playback
  const stopPlayback = useCallback(() => {
    if (animationRef.current) {
      cancelAnimationFrame(animationRef.current);
    }
    setIsPlaying(false);
    setCurrentBeat(-1);
    setActiveNotes(new Set());
    setGlowingNotes([]);
    startTimeRef.current = null;
  }, []);

  // Auto-play on mount
  useEffect(() => {
    if (autoPlay) {
      const timer = setTimeout(startPlayback, 1000);
      return () => clearTimeout(timer);
    }
  }, [autoPlay, startPlayback]);

  // Cleanup
  useEffect(() => {
    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
      // Close audio context on unmount
      if (audioCtxRef.current && audioCtxRef.current.state !== 'closed') {
        audioCtxRef.current.close().catch(() => {});
      }
    };
  }, []);

  // Calculate note X position
  const getNoteX = (startBeat) => {
    return LEFT_MARGIN + startBeat * BEAT_WIDTH;
  };

  // Calculate note Y position on staff
  const getNoteY = (pitch, clef, staffTop) => {
    const position = calculateNotePosition(pitch, clef);
    // Position 0 is the bottom line, position 4 is the top line
    const yOffset = (4 - position) * (LINE_SPACING / 2);
    return staffTop + yOffset;
  };

  // Render treble clef symbol
  const renderTrebleClef = (x, y) => (
    <text
      x={x}
      y={y + 70}
      fontSize="80"
      fontFamily="serif"
      fill="#333"
      className="clef-symbol"
    >
      𝄞
    </text>
  );

  // Render bass clef symbol
  const renderBassClef = (x, y) => (
    <text
      x={x}
      y={y + 50}
      fontSize="60"
      fontFamily="serif"
      fill="#333"
      className="clef-symbol"
    >
      𝄢
    </text>
  );

  // Render a single note
  const renderNote = (note, index, staffTop, isActive, glowIntensity) => {
    const x = getNoteX(note.startBeat);
    const y = getNoteY(note.pitch, note.clef, staffTop);
    const color = getNoteColor(note.clef);
    const baseColor = '#333';

    const isGlowing = glowIntensity > 0;

    return (
      <g key={`note-${index}`} className={`note ${isActive ? 'note--active' : ''}`}>
        {/* Glow effect */}
        {isGlowing && (
          <>
            <circle
              cx={x}
              cy={y}
              r={20 + glowIntensity * 10}
              fill={color}
              opacity={glowIntensity * 0.3}
              className="note-glow-outer"
            />
            <circle
              cx={x}
              cy={y}
              r={12 + glowIntensity * 5}
              fill={color}
              opacity={glowIntensity * 0.5}
              className="note-glow-inner"
            />
          </>
        )}

        {/* Note head */}
        <ellipse
          cx={x}
          cy={y}
          rx={10}
          ry={8}
          fill={isGlowing ? color : baseColor}
          stroke={isGlowing ? color : 'none'}
          strokeWidth={isGlowing ? 2 : 0}
          className="note-head"
          style={{
            filter: isGlowing ? `drop-shadow(0 0 ${8 * glowIntensity}px ${color})` : 'none',
          }}
        />

        {/* Note stem (for quarter notes) */}
        {note.duration <= 1 && (
          <line
            x1={x + 9}
            y1={y}
            x2={x + 9}
            y2={y - 40}
            stroke={isGlowing ? color : baseColor}
            strokeWidth={2}
            style={{
              filter: isGlowing ? `drop-shadow(0 0 ${4 * glowIntensity}px ${color})` : 'none',
            }}
          />
        )}
      </g>
    );
  };

  // Render staff lines
  const renderStaffLines = (startX, endX, staffTop) => {
    const lines = [];
    for (let i = 0; i < 5; i++) {
      lines.push(
        <line
          key={`line-${staffTop}-${i}`}
          x1={startX}
          y1={staffTop + i * LINE_SPACING}
          x2={endX}
          y2={staffTop + i * LINE_SPACING}
          stroke="#555"
          strokeWidth={1}
        />
      );
    }
    return lines;
  };

  // Render bar lines
  const renderBarLines = (staffTop, staffBottom) => {
    return BAR_LINES.map((beat, index) => {
      const x = getNoteX(beat);
      return (
        <line
          key={`bar-${index}`}
          x1={x}
          y1={staffTop}
          x2={x}
          y2={staffBottom}
          stroke="#666"
          strokeWidth={index === 0 || index === BAR_LINES.length - 1 ? 3 : 1}
        />
      );
    });
  };

  // Playhead position
  const playheadX = currentBeat >= 0 ? getNoteX(currentBeat) : -100;

  return (
    <div
      ref={containerRef}
      className={`christmas-music-sheet ${className}`}
      onClick={() => !isPlaying && startPlayback()}
    >
      {/* Title */}
      {showTitle && (
        <div className="sheet-title">
          <h2>{SONG_INFO.title}</h2>
        </div>
      )}

      {/* 3D Container */}
      <div className="sheet-3d-container">
        <div className="sheet-perspective">
          <svg
            viewBox={`0 0 ${SHEET_WIDTH} ${SHEET_HEIGHT}`}
            className="sheet-svg"
            preserveAspectRatio="xMidYMid meet"
          >
            {/* Background */}
            <rect
              x={0}
              y={0}
              width={SHEET_WIDTH}
              height={SHEET_HEIGHT}
              fill="#f5f0e6"
              rx={4}
            />

            {/* Treble staff */}
            {renderStaffLines(50, SHEET_WIDTH - 50, STAFF_TOP)}
            {renderTrebleClef(55, STAFF_TOP)}

            {/* Bass staff */}
            {renderStaffLines(50, SHEET_WIDTH - 50, STAFF_TOP + STAFF_HEIGHT + 40)}
            {renderBassClef(55, STAFF_TOP + STAFF_HEIGHT + 40)}

            {/* Bar lines */}
            {renderBarLines(STAFF_TOP, STAFF_TOP + STAFF_HEIGHT + 40 + STAFF_HEIGHT)}

            {/* Time signature */}
            <text x={100} y={STAFF_TOP + 45} fontSize="30" fontFamily="serif" fill="#333">3</text>
            <text x={100} y={STAFF_TOP + 75} fontSize="30" fontFamily="serif" fill="#333">4</text>
            <text x={100} y={STAFF_TOP + STAFF_HEIGHT + 85} fontSize="30" fontFamily="serif" fill="#333">3</text>
            <text x={100} y={STAFF_TOP + STAFF_HEIGHT + 115} fontSize="30" fontFamily="serif" fill="#333">4</text>

            {/* Melody notes (treble) */}
            {MELODY_NOTES.map((note, index) => {
              const globalIndex = ALL_NOTES.findIndex(
                (n) => n.startBeat === note.startBeat && n.clef === note.clef && n.pitch === note.pitch
              );
              const isActive = activeNotes.has(globalIndex);
              const glowData = glowingNotes.find((g) => g.index === globalIndex);
              const glowIntensity = glowData?.intensity || 0;

              return renderNote(note, `melody-${index}`, STAFF_TOP, isActive, glowIntensity);
            })}

            {/* Bass notes */}
            {BASS_NOTES.map((note, index) => {
              const globalIndex = ALL_NOTES.findIndex(
                (n) => n.startBeat === note.startBeat && n.clef === note.clef && n.pitch === note.pitch
              );
              const isActive = activeNotes.has(globalIndex);
              const glowData = glowingNotes.find((g) => g.index === globalIndex);
              const glowIntensity = glowData?.intensity || 0;

              return renderNote(note, `bass-${index}`, STAFF_TOP + STAFF_HEIGHT + 40, isActive, glowIntensity);
            })}

            {/* Playhead */}
            {isPlaying && currentBeat >= 0 && (
              <g className="playhead">
                <line
                  x1={playheadX}
                  y1={STAFF_TOP - 20}
                  x2={playheadX}
                  y2={STAFF_TOP + STAFF_HEIGHT + 40 + STAFF_HEIGHT + 20}
                  stroke="rgba(255, 255, 255, 0.5)"
                  strokeWidth={3}
                />
                <circle
                  cx={playheadX}
                  cy={STAFF_TOP - 30}
                  r={8}
                  fill="#FFD700"
                  className="playhead-dot"
                />
              </g>
            )}
          </svg>
        </div>
      </div>

      {/* Play button overlay */}
      {!isPlaying && (
        <div className="sheet-play-overlay">
          <button className="sheet-play-btn" onClick={startPlayback}>
            <svg viewBox="0 0 24 24" width="48" height="48">
              <polygon points="5,3 19,12 5,21" fill="currentColor" />
            </svg>
            <span>Phát nhạc</span>
          </button>
        </div>
      )}

      {/* Controls */}
      {isPlaying && (
        <div className="sheet-controls">
          <button className="sheet-stop-btn" onClick={stopPlayback}>
            Dừng
          </button>
        </div>
      )}
    </div>
  );
};

export default ChristmasMusicSheet;
