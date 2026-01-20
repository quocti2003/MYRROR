/**
 * PlaceNoteScreen - Step 4: Place note on music staff
 */
import React, { useState, useEffect } from 'react';
import { TEXT, getDiamondConfig } from '@/constants/eventConstants';
import { placeNote, fetchAllNotes } from '@services/event/eventApi';
import { initAudio, playNote, isAudioInitialized } from '@services/event/audio';
import { broadcastNoteAdded } from '@services/event/ably';
import useEventStore from '@/store/useEventStore';
import MusicStaff from '../ui/MusicStaff';
import Diamond from '../ui/Diamond';
import Logo from '../ui/Logo';

const PlaceNoteScreen = () => {
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const { user, selectedDiamond, allNotes, setAllNotes, setCurrentStep, setUserNote, addNote } =
    useEventStore();

  // Fetch existing notes on mount
  useEffect(() => {
    const loadNotes = async () => {
      const notes = await fetchAllNotes();
      setAllNotes(notes);
    };
    loadNotes();
  }, [setAllNotes]);

  const handlePlaceNote = async (position) => {
    const config = getDiamondConfig(selectedDiamond);
    setPreview({
      diamondShape: selectedDiamond,
      positionX: position.positionX,
      positionY: position.positionY,
      pitch: config?.pitch || position.pitch,
    });

    // Ensure audio is initialized before playing
    if (!isAudioInitialized()) {
      await initAudio();
    }

    // Play the note
    playNote(config?.pitch || position.pitch);
  };

  const handleConfirm = async () => {
    if (!preview || !user) return;

    setLoading(true);
    setError('');

    const noteData = {
      userId: user.id,
      userDisplayName: user.displayName,
      diamondShape: preview.diamondShape,
      pitch: preview.pitch,
      positionX: preview.positionX,
      positionY: preview.positionY,
    };

    const result = await placeNote(noteData);

    if (result.success) {
      // Add to local state
      addNote(result.note);

      // Broadcast via WebSocket
      broadcastNoteAdded(result.note);

      // Save user's note
      setUserNote(result.note);

      // Move to result screen
      setCurrentStep('result');
    } else {
      setError(result.error || TEXT.error);
    }

    setLoading(false);
  };

  const config = getDiamondConfig(selectedDiamond);

  return (
    <div className="event-screen place-screen">
      <Logo size="sm" />

      <div className="event-screen__content">
        <h2 className="event-form__title">{TEXT.placeTitle}</h2>
        <p className="event-subtitle">{TEXT.placeSubtitle}</p>

        {/* Selected diamond info */}
        <div className="place-diamond-info">
          <Diamond shape={selectedDiamond} size={50} color={config?.color} showGlow />
          <span>{config?.displayNameVi}</span>
        </div>

        {/* Music staff */}
        <div className="place-staff-container">
          <MusicStaff
            notes={allNotes}
            previewNote={preview}
            interactive={true}
            onPlaceNote={handlePlaceNote}
          />
        </div>

        <p className="place-note-count">
          Đã có <strong>{allNotes.length}</strong> nốt nhạc trên khuông
        </p>

        {error && <p className="event-form__error">{error}</p>}

        <button
          className="event-btn event-btn--primary"
          disabled={!preview || loading}
          onClick={handleConfirm}
        >
          {loading ? TEXT.loading : TEXT.placeButton}
        </button>
      </div>
    </div>
  );
};

export default PlaceNoteScreen;
