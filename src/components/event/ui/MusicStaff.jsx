/**
 * MusicStaff Component - Interactive music staff for note placement
 */
import React, { useRef, useCallback } from 'react';
import Diamond from './Diamond';
import { STAFF_LINE_TO_PITCH, getDiamondConfig } from '@/constants/eventConstants';

const STAFF_LINES = 5;
const STAFF_PADDING = 40; // Padding top/bottom for notes outside staff lines
const STAFF_LINES_HEIGHT = 200; // Height of the 5 staff lines area
const TOTAL_HEIGHT = STAFF_LINES_HEIGHT + (STAFF_PADDING * 2); // Total container height
const LINE_SPACING = STAFF_LINES_HEIGHT / (STAFF_LINES - 1);

const MusicStaff = ({
  notes = [],
  previewNote = null,
  interactive = true,
  onPlaceNote,
  highlightedNoteId = null,
  className = '',
  showTrebleClef = true,
  width = '100%',
}) => {
  const staffRef = useRef(null);

  // Convert Y position to staff line (0-6)
  const yToStaffLine = (y, height) => {
    const lineHeight = height / 6;
    return Math.round(y / lineHeight);
  };

  // Convert staff line to Y position (with padding offset)
  const staffLineToY = (staffLine) => {
    // staffLine 0 = top of staff lines, staffLine 6 = below staff
    // Map 0-6 range to fit within the padded area
    return STAFF_PADDING + (staffLine / 6) * STAFF_LINES_HEIGHT;
  };

  // Handle click on staff
  const handleClick = useCallback(
    (e) => {
      if (!interactive || !onPlaceNote) return;

      const rect = staffRef.current.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width) * 100;
      const y = e.clientY - rect.top;
      const staffLine = yToStaffLine(y, rect.height);
      const pitch = STAFF_LINE_TO_PITCH[Math.min(6, Math.max(0, staffLine))];

      onPlaceNote({
        positionX: Math.max(5, Math.min(95, x)),
        positionY: staffLine,
        pitch,
      });
    },
    [interactive, onPlaceNote]
  );

  return (
    <div
      ref={staffRef}
      className={`music-staff ${interactive ? 'music-staff--interactive' : ''} ${className}`}
      onClick={handleClick}
      style={{ width, height: TOTAL_HEIGHT, position: 'relative' }}
    >
      {/* Staff background */}
      <svg
        width="100%"
        height="100%"
        style={{ position: 'absolute', top: 0, left: 0 }}
        preserveAspectRatio="none"
      >
        {/* Staff lines - positioned with padding offset */}
        {Array.from({ length: STAFF_LINES }).map((_, i) => (
          <line
            key={i}
            x1="0"
            y1={STAFF_PADDING + i * LINE_SPACING}
            x2="100%"
            y2={STAFF_PADDING + i * LINE_SPACING}
            stroke="rgba(255,255,255,0.3)"
            strokeWidth="1"
          />
        ))}
      </svg>

      {/* Treble clef */}
      {showTrebleClef && (
        <div
          className="music-staff__clef"
          style={{
            position: 'absolute',
            left: 10,
            top: STAFF_PADDING + STAFF_LINES_HEIGHT / 2,
            transform: 'translateY(-50%)',
            fontSize: '4rem',
            color: 'rgba(255,255,255,0.5)',
            fontFamily: 'serif',
            userSelect: 'none',
          }}
        >
          𝄞
        </div>
      )}

      {/* Existing notes */}
      {notes.map((note) => {
        const config = getDiamondConfig(note.diamondShape);
        const isHighlighted = note.id === highlightedNoteId;

        return (
          <div
            key={note.id}
            className="music-staff__note"
            style={{
              position: 'absolute',
              left: `${note.positionX}%`,
              top: staffLineToY(note.positionY),
              transform: 'translate(-50%, -50%)',
              transition: 'all 0.3s ease',
            }}
          >
            <Diamond
              shape={note.diamondShape}
              size={isHighlighted ? 50 : 35}
              color={config?.color}
              isHighlighted={isHighlighted}
              showGlow={isHighlighted}
            />
          </div>
        );
      })}

      {/* Preview note */}
      {previewNote && (
        <div
          className="music-staff__preview"
          style={{
            position: 'absolute',
            left: `${previewNote.positionX}%`,
            top: staffLineToY(previewNote.positionY),
            transform: 'translate(-50%, -50%)',
            opacity: 0.7,
            animation: 'pulse 1s ease-in-out infinite',
          }}
        >
          <Diamond
            shape={previewNote.diamondShape}
            size={45}
            color={getDiamondConfig(previewNote.diamondShape)?.color}
            showGlow
          />
        </div>
      )}

      {/* Click hint for empty staff */}
      {interactive && notes.length === 0 && !previewNote && (
        <div
          className="music-staff__hint"
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            color: 'rgba(255,255,255,0.5)',
            fontSize: '0.9rem',
            textAlign: 'center',
            pointerEvents: 'none',
          }}
        >
          Chạm vào khuông nhạc để đặt kim cương
        </div>
      )}
    </div>
  );
};

export default MusicStaff;
