/**
 * EventPreviewPage - Preview page before generating avatar
 * User can choose note shape and see avatar preview
 */
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import NavbarV4 from '@/components/navbar/NavbarV4';
import ShineGlassButton from '@components/common/button/ShineGlassButton';
import EventBackButton from '@/components/event/EventBackButton';

import './EventPreviewPage.css';

// Diamond shapes data
const DIAMOND_SHAPES = [
  { id: 'heart', name: 'Heart', icon: '/diamonds/heart.webp' },
  { id: 'round', name: 'Round', icon: '/diamonds/round.webp' },
  { id: 'oval', name: 'Oval', icon: '/diamonds/oval.webp' },
  { id: 'marquise', name: 'Marquise', icon: '/diamonds/marquise.webp' },
];

const EventPreviewPage = () => {
  const navigate = useNavigate();
  const [selectedShape, setSelectedShape] = useState('heart');

  const handleBack = () => {
    navigate(ROUTES.EVENT_NAME);
  };

  const handleGenerate = () => {
    // TODO: Store selected shape and generate avatar
    navigate(ROUTES.EVENT_RESULT);
  };

  return (
    <>
      <NavbarV4 logoOnly />
      <div className="event-preview" data-navbar-theme="black">
      {/* Background */}
      <div className="event-preview__bg" />

      {/* Main Content */}
      <div className="event-preview__main">
        {/* Back Button */}
        <EventBackButton onClick={handleBack} />

        {/* Diamond Shape */}
        <div className="event-preview__diamond">
          <img
            src="/diamonds/heart-ruby.webp"
            alt="Heart Diamond"
            className="event-preview__diamond-img"
          />
        </div>

        {/* Divider Line */}
        <div className="event-preview__divider" />

        {/* Avatar Preview Card */}
        <div className="event-preview__card">
          <div className="event-preview__card-content">
            <span className="event-preview__card-subtitle">THE SOUND OF</span>
            <span className="event-preview__card-title">Love</span>
            <span className="event-preview__card-script">Grown</span>
          </div>
        </div>

        {/* Generate Button */}
        <ShineGlassButton theme="light" onClick={handleGenerate}>
          Generate
        </ShineGlassButton>
      </div>

      {/* Bottom Content */}
      <div className="event-preview__bottom">
        <div className="event-preview__info">
          <h3 className="heading-3--no-margin event-preview__info-title">
            Chọn nốt sáng
          </h3>
          <p className="event-preview__info-text">
            cinq elit, sed diam nonummy nibut laoreet dolore
            magna aliquam erat volutpat. cinq elit, sed diam
            nonummy nibut nibut laoreet dolore magna aliquam
            erat volutpat.
          </p>
        </div>
      </div>
    </div>
    </>
  );
};

export default EventPreviewPage;
