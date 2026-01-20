/**
 * ResultScreen - Final step: Show result and options
 */
import React, { useEffect, useMemo, useState } from 'react';
import { TEXT, getDiamondConfig, calculateNotePositionByOrder } from '@/constants/eventConstants';
import { fetchAllNotes } from '@services/event/eventApi';
import { initAudio, isAudioInitialized, playDiamondNote, playNotesFromArray } from '@services/event/audio';
import useEventStore from '@/store/useEventStore';
import Diamond from '../ui/Diamond';
import MusicStaff from '../ui/MusicStaff';
import Logo from '../ui/Logo';
import AvatarGenerator, { downloadAvatar } from '../ui/AvatarGenerator';

const MAX_VISIBLE_NOTES = 8; // Show 7 notes before + user's note

const ResultScreen = () => {
  const { user, userNote, allNotes, setAllNotes, reset, isDemo } = useEventStore();
  const [avatarDataUrl, setAvatarDataUrl] = useState('');

  // Fetch all notes on mount to show the full music staff
  useEffect(() => {
    const loadNotes = async () => {
      const notes = await fetchAllNotes();
      setAllNotes(notes);
    };
    loadNotes();
  }, [setAllNotes]);

  // Get last 8 notes (or less) for user's journey view
  // Position them so the last note (user's note) is always at the far right
  // Filter out duplicates by note id
  const userJourneyNotes = useMemo(() => {
    if (!allNotes || allNotes.length === 0) return [];

    // Remove duplicates by id
    const uniqueNotes = allNotes.filter((note, index, self) =>
      index === self.findIndex((n) => n.id === note.id)
    );

    // Get the last N notes (most recent)
    const recentNotes = uniqueNotes.slice(-MAX_VISIBLE_NOTES);
    const count = recentNotes.length;

    // Calculate positions from right to left
    // Last note (user's) at 92%, spread others to the left
    const spacing = 12; // % between each note
    return recentNotes.map((note, index) => ({
      ...note,
      // Position from right: last note at 92%, previous at 92-12=80%, etc.
      positionX: 92 - ((count - 1 - index) * spacing),
    }));
  }, [allNotes]);

  const config = userNote ? getDiamondConfig(userNote.diamondShape) : null;

  const handlePlayMyNote = async () => {
    if (userNote) {
      if (!isAudioInitialized()) {
        await initAudio();
      }
      playDiamondNote(userNote.diamondShape);
    }
  };

  const handlePlayAll = async () => {
    // Only play the visible notes (last 8)
    const visibleNotes = allNotes.slice(-MAX_VISIBLE_NOTES);
    if (visibleNotes.length > 0) {
      if (!isAudioInitialized()) {
        await initAudio();
      }
      playNotesFromArray(visibleNotes);
    }
  };

  const handleDownloadAvatar = () => {
    if (avatarDataUrl && user) {
      downloadAvatar(avatarDataUrl, `mirror-diamond-${user.displayName}-${user.lightNumber}.png`);
    }
  };

  const handleRestart = () => {
    sessionStorage.removeItem('event_ticket_code');
    reset();
  };

  if (!userNote || !user) {
    return (
      <div className="event-screen result-screen">
        <Logo size="md" />
        <p>{TEXT.error}</p>
        <button className="event-btn event-btn--secondary" onClick={handleRestart}>
          {TEXT.resultRestart}
        </button>
      </div>
    );
  }

  return (
    <div className="event-screen result-screen">
      <Logo size="md" />

      <div className="event-screen__content">
        <h2 className="result-title">{TEXT.resultTitle}</h2>
        <p className="result-subtitle">{TEXT.resultSubtitle}</p>

        {/* Light number */}
        <div className="result-light-number">
          <span className="result-light-label">{TEXT.resultLightNumber}</span>
          <span className="result-light-value">#{user.lightNumber}</span>
        </div>

        {/* User's diamond */}
        <div className="result-diamond">
          <Diamond
            shape={userNote.diamondShape}
            size={120}
            color={config?.color}
            showGlow
            isHighlighted
          />
          <p className="result-diamond-name">{config?.displayNameVi}</p>
          <p className="result-diamond-pitch">{config?.pitch}</p>
        </div>

        {/* Display name */}
        <p className="result-username">{user.displayName}</p>

        {/* Action buttons */}
        <div className="result-actions">
          <button className="event-btn event-btn--secondary" onClick={handlePlayMyNote}>
            🎵 {TEXT.resultPlayNote}
          </button>

          <button className="event-btn event-btn--secondary" onClick={handlePlayAll}>
            🎶 {TEXT.resultPlayAll}
          </button>
        </div>

        {/* Download Avatar Button */}
        <button
          className="event-btn event-btn--primary result-download-btn"
          onClick={handleDownloadAvatar}
          disabled={!avatarDataUrl}
        >
          📥 {TEXT.resultDownload}
        </button>

        {/* Journey view - mini staff showing last 7 notes */}
        <div className="result-journey">
          <h3>Hành trình âm nhạc</h3>
          <MusicStaff
            notes={userJourneyNotes}
            interactive={false}
            highlightedNoteId={userNote.id}
            showTrebleClef={false}
          />
          <p className="result-journey__count">
            Nốt của bạn là nốt thứ <strong>{allNotes.length}</strong> trong bản giao hưởng
          </p>
        </div>

        {/* Lucky draw notice */}
        <div className="result-notice">
          <p>Bạn là ánh sáng số <strong>{user.lightNumber}</strong> và đã tham gia vào chương trình quay số may mắn!</p>
          <p>Hãy theo dõi màn hình lớn để biết kết quả.</p>
        </div>

        {/* Demo mode indicator */}
        {isDemo && <div className="demo-badge">{TEXT.demoMode}</div>}

        {/* Restart button (for testing) */}
        <button className="event-btn event-btn--text" onClick={handleRestart}>
          {TEXT.resultRestart}
        </button>
      </div>

      {/* Hidden Avatar Generator - auto-generates background and scenery */}
      <AvatarGenerator
        displayName={user.displayName}
        lightNumber={user.lightNumber}
        diamondShape={userNote.diamondShape}
        onGenerated={setAvatarDataUrl}
      />
    </div>
  );
};

export default ResultScreen;
