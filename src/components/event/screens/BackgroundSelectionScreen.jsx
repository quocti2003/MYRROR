/**
 * BackgroundSelectionScreen - Step 4: Select avatar background and scenery
 */
import React, { useState } from 'react';
import { TEXT } from '@/constants/eventConstants';
import { AVATAR_BACKGROUNDS, AVATAR_SCENERIES } from '../ui/AvatarGenerator';
import useEventStore from '@/store/useEventStore';
import Logo from '../ui/Logo';

const BackgroundSelectionScreen = () => {
  const [selectedBg, setSelectedBg] = useState(null);
  const [selectedScenery, setSelectedScenery] = useState(null);

  const { setCurrentStep, setSelectedBackground, setSelectedScenery: setStoreScenery } = useEventStore();

  const handleContinue = () => {
    if (selectedBg && selectedScenery) {
      setSelectedBackground(selectedBg);
      setStoreScenery(selectedScenery);
      setCurrentStep('result');
    }
  };

  const canContinue = selectedBg && selectedScenery;

  return (
    <div className="event-screen background-screen">
      <Logo size="sm" />

      <div className="event-screen__content">
        <h2 className="event-form__title">Tùy chỉnh ảnh lưu niệm</h2>

        {/* Background Selection */}
        <div className="selection-section">
          <p className="selection-label">Chọn màu nền</p>
          <div className="background-grid">
            {AVATAR_BACKGROUNDS.map((bg) => (
              <button
                key={bg.id}
                className={`background-card ${selectedBg === bg.id ? 'background-card--selected' : ''}`}
                onClick={() => setSelectedBg(bg.id)}
                style={{
                  background: `linear-gradient(135deg, ${bg.start} 0%, ${bg.mid} 50%, ${bg.end} 100%)`,
                }}
              >
                <span className="background-card__name">{bg.name}</span>
                {selectedBg === bg.id && (
                  <span className="background-card__check">✓</span>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Scenery Selection */}
        <div className="selection-section">
          <p className="selection-label">Chọn phong cảnh</p>
          <div className="scenery-grid">
            {AVATAR_SCENERIES.map((scenery) => (
              <button
                key={scenery.id}
                className={`scenery-card ${selectedScenery === scenery.id ? 'scenery-card--selected' : ''}`}
                onClick={() => setSelectedScenery(scenery.id)}
              >
                <span className="scenery-card__icon">{scenery.icon}</span>
                <span className="scenery-card__name">{scenery.name}</span>
                {selectedScenery === scenery.id && (
                  <span className="scenery-card__check">✓</span>
                )}
              </button>
            ))}
          </div>
        </div>

        <button
          className="event-btn event-btn--primary"
          disabled={!canContinue}
          onClick={handleContinue}
        >
          Tiếp tục
        </button>
      </div>
    </div>
  );
};

export default BackgroundSelectionScreen;
