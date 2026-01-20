/**
 * NotePreviewScreen - Step 4: Preview note position and play sound
 * Shows user their note on the staff before customizing avatar
 */
import React, { useEffect, useMemo, useState } from 'react';
import { TEXT, getDiamondConfig, calculateNotePositionByOrder } from '@/constants/eventConstants';
import { fetchAllNotes } from '@services/event/eventApi';
import { initAudio, isAudioInitialized, playNote } from '@services/event/audio';
import useEventStore from '@/store/useEventStore';
import Diamond from '../ui/Diamond';
import MusicStaff from '../ui/MusicStaff';
import Logo from '../ui/Logo';

const MAX_VISIBLE_NOTES = 8; // 7 notes before + user's note

const NotePreviewScreen = () => {
  const { user, userNote, allNotes, setAllNotes, setCurrentStep } = useEventStore();
  const [hasPlayedNote, setHasPlayedNote] = useState(false);

  const config = userNote ? getDiamondConfig(userNote.diamondShape) : null;

  // Fetch all notes on mount
  useEffect(() => {
    const loadNotes = async () => {
      const notes = await fetchAllNotes();
      setAllNotes(notes);
    };
    loadNotes();
  }, [setAllNotes]);

  // Auto play note on first load
  useEffect(() => {
    const playUserNote = async () => {
      if (userNote && !hasPlayedNote) {
        if (!isAudioInitialized()) {
          await initAudio();
        }
        playNote(userNote.pitch);
        setHasPlayedNote(true);
      }
    };

    // Small delay to let the screen render first
    const timer = setTimeout(playUserNote, 500);
    return () => clearTimeout(timer);
  }, [userNote, hasPlayedNote]);

  // Get last 7 notes for display, with user's note at the right
  const displayNotes = useMemo(() => {
    if (!allNotes || allNotes.length === 0) return [];

    const recentNotes = allNotes.slice(-MAX_VISIBLE_NOTES);
    const count = recentNotes.length;
    const spacing = 12;

    return recentNotes.map((note, index) => ({
      ...note,
      positionX: 92 - ((count - 1 - index) * spacing),
    }));
  }, [allNotes]);

  const handlePlayNote = async () => {
    if (userNote) {
      if (!isAudioInitialized()) {
        await initAudio();
      }
      playNote(userNote.pitch);
    }
  };

  const handleContinue = () => {
    setCurrentStep('result');
  };

  if (!userNote || !user) {
    return (
      <div className="event-screen preview-screen">
        <Logo size="md" />
        <p>{TEXT.error}</p>
      </div>
    );
  }

  return (
    <div className="event-screen preview-screen">
      <Logo size="sm" />

      <div className="event-screen__content">
        <h2 className="event-form__title">Nốt nhạc của bạn!</h2>

        {/* Light number */}
        <div className="preview-light-number">
          <span className="preview-light-label">Ánh sáng số</span>
          <span className="preview-light-value">#{user.lightNumber}</span>
        </div>

        {/* User's diamond and note info */}
        <div className="preview-note-info">
          <Diamond
            shape={userNote.diamondShape}
            size={80}
            color={config?.color}
            showGlow
            isHighlighted
          />
          <div className="preview-note-details">
            <p className="preview-note-name">{config?.displayNameVi}</p>
            <p className="preview-note-pitch">Nốt {userNote.pitch}</p>
          </div>
        </div>

        {/* Play button */}
        <button
          className="event-btn event-btn--secondary preview-play-btn"
          onClick={handlePlayNote}
        >
          🎵 Nghe nốt của bạn
        </button>

        {/* Music staff showing recent notes */}
        <div className="preview-staff">
          <p className="preview-staff-label">Vị trí trên khuông nhạc</p>
          <MusicStaff
            notes={displayNotes}
            interactive={false}
            highlightedNoteId={userNote.id}
            showTrebleClef={false}
          />
          <p className="preview-staff-count">
            Bạn là người thứ <strong>{allNotes.length}</strong> tham gia bản giao hưởng
          </p>
        </div>

        {/* Continue to result */}
        <button
          className="event-btn event-btn--primary"
          onClick={handleContinue}
        >
          Tiếp tục
        </button>
      </div>
    </div>
  );
};

export default NotePreviewScreen;
