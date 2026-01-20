/**
 * DiamondSelectionScreen - Step 3: Select diamond shape
 * Note: Diamond selection is purely visual - the note/pitch is determined by lightNumber
 */
import React, { useState } from 'react';
import { TEXT, DIAMOND_CONFIGS, calculateNoteFromLightNumber } from '@/constants/eventConstants';
import { placeNote } from '@services/event/eventApi';
import { broadcastNoteAdded } from '@services/event/ably';
import useEventStore from '@/store/useEventStore';
import Diamond from '../ui/Diamond';
import Logo from '../ui/Logo';

const DiamondSelectionScreen = () => {
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const { user, setCurrentStep, setSelectedDiamond, setUserNote } = useEventStore();

  const handleDiamondClick = (config) => {
    // Just select, no audio - diamond is purely visual
    setSelected(config.shape);
  };

  const handleContinue = async () => {
    if (!selected || !user) return;

    setLoading(true);
    setError('');

    // Calculate note position based on user's lightNumber
    const { pitch, staffLine, positionX } = calculateNoteFromLightNumber(user.lightNumber);

    const noteData = {
      userId: user.id,
      userDisplayName: user.displayName,
      diamondShape: selected,
      pitch,
      positionX,
      positionY: staffLine,
    };

    const result = await placeNote(noteData);

    if (result.success) {
      // Save selected diamond
      setSelectedDiamond(selected);

      // Broadcast via WebSocket
      broadcastNoteAdded(result.note);

      // Save user's note
      setUserNote(result.note);

      // Go to result directly
      setCurrentStep('result');
    } else {
      setError(result.error || TEXT.error);
    }

    setLoading(false);
  };

  return (
    <div className="event-screen diamond-screen">
      <Logo size="sm" />

      <div className="event-screen__content">
        <h2 className="event-form__title">{TEXT.diamondTitle}</h2>
        <p className="event-subtitle">{TEXT.diamondSubtitle}</p>

        <div className="diamond-grid">
          {DIAMOND_CONFIGS.map((config) => (
            <div
              key={config.shape}
              className={`diamond-card ${selected === config.shape ? 'diamond-card--selected' : ''}`}
              onClick={() => handleDiamondClick(config)}
            >
              <Diamond
                shape={config.shape}
                size={70}
                color={config.color}
                isSelected={selected === config.shape}
                showGlow={selected === config.shape}
              />
              <span className="diamond-card__name">{config.displayNameVi}</span>
              <span className="diamond-card__pitch">{config.pitch}</span>
            </div>
          ))}
        </div>

        {selected && (
          <div className="diamond-selected-info">
            <p>
              Bạn đã chọn:{' '}
              <strong>{DIAMOND_CONFIGS.find((d) => d.shape === selected)?.displayNameVi}</strong>
            </p>
          </div>
        )}

        {error && <p className="event-form__error">{error}</p>}

        <button
          className="event-btn event-btn--primary"
          disabled={!selected || loading}
          onClick={handleContinue}
        >
          {loading ? TEXT.loading : TEXT.diamondButton}
        </button>
      </div>
    </div>
  );
};

export default DiamondSelectionScreen;
