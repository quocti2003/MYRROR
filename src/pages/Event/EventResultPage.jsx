/**
 * EventResultPage - Result page after avatar generation
 * Shows generated avatar with download and share options
 */
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import NavbarV4 from '@/components/navbar/NavbarV4';
import ShineGlassButton from '@components/common/button/ShineGlassButton';
import EventBackButton from '@/components/event/EventBackButton';

import './EventResultPage.css';

const EventResultPage = () => {
  const navigate = useNavigate();
  const [showShareModal, setShowShareModal] = useState(false);

  const shareData = {
    title: 'My Diamond Avatar - Mirror',
    text: 'Check out my avatar from Mirror Diamond Event!',
    url: window.location.href,
  };

  const handleBack = () => {
    navigate(ROUTES.EVENT_PREVIEW);
  };

  const handleRegenerate = () => {
    // TODO: Regenerate avatar
    navigate(ROUTES.EVENT_PREVIEW);
  };

  const handleDownload = () => {
    // TODO: Download avatar image
    console.log('Download avatar');
  };

  const handleShare = async () => {
    // Try native Web Share API first (mobile)
    if (navigator.share) {
      try {
        await navigator.share(shareData);
      } catch (err) {
        // User cancelled or error
        if (err.name !== 'AbortError') {
          setShowShareModal(true);
        }
      }
    } else {
      // Fallback to custom modal (desktop)
      setShowShareModal(true);
    }
  };

  const handleShareTo = (platform) => {
    const { url, text, title } = shareData;
    const encodedUrl = encodeURIComponent(url);
    const encodedText = encodeURIComponent(text);
    const encodedTitle = encodeURIComponent(title);

    const shareUrls = {
      facebook: `https://www.facebook.com/sharer/sharer.php?u=${encodedUrl}`,
      twitter: `https://twitter.com/intent/tweet?url=${encodedUrl}&text=${encodedText}`,
      linkedin: `https://www.linkedin.com/sharing/share-offsite/?url=${encodedUrl}`,
      whatsapp: `https://wa.me/?text=${encodedText}%20${encodedUrl}`,
      telegram: `https://t.me/share/url?url=${encodedUrl}&text=${encodedText}`,
    };

    window.open(shareUrls[platform], '_blank', 'width=600,height=400');
    setShowShareModal(false);
  };

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(shareData.url);
      alert('Link copied!');
      setShowShareModal(false);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  return (
    <>
      <NavbarV4 logoOnly />
      <div className="event-result" data-navbar-theme="black">
      {/* Background */}
      <div className="event-result__bg" />

      {/* Main Content */}
      <div className="event-result__main">
        {/* Back Button */}
        <EventBackButton onClick={handleBack} />

        {/* Avatar Image */}
        <div className="event-result__avatar">
          <div className="event-result__avatar-placeholder">
            {/* Generated avatar will be displayed here */}
          </div>
        </div>

        {/* Regenerate Button */}
        <ShineGlassButton theme="light" onClick={handleRegenerate} className="event-result__regenerate">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M1 4V10H7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M23 20V14H17" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10M23 14L18.36 18.36A9 9 0 0 1 3.51 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </ShineGlassButton>
      </div>

      {/* Bottom Content */}
      <div className="event-result__bottom">
        <div className="event-result__info">
          <h3 className="heading-3--no-margin event-result__info-title">
            Choose the note shape
          </h3>
          <p className="event-result__info-text">
            cinq elit, sed diam nonummy nibut laoreet dolore
            magna aliquam erat volutpat. cinq elit, sed diam
            nonummy nibut nibut laoreet dolore magna aliquam
            erat volutpat.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="event-result__actions">
          <ShineGlassButton theme="light" onClick={handleDownload}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" style={{ marginRight: '8px' }}>
              <path d="M21 15V19C21 19.5304 20.7893 20.0391 20.4142 20.4142C20.0391 20.7893 19.5304 21 19 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M7 10L12 15L17 10" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M12 15V3" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            Download
          </ShineGlassButton>

          <ShineGlassButton theme="light" onClick={handleShare} className="event-result__share">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M18 8C19.6569 8 21 6.65685 21 5C21 3.34315 19.6569 2 18 2C16.3431 2 15 3.34315 15 5C15 6.65685 16.3431 8 18 8Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M6 15C7.65685 15 9 13.6569 9 12C9 10.3431 7.65685 9 6 9C4.34315 9 3 10.3431 3 12C3 13.6569 4.34315 15 6 15Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M18 22C19.6569 22 21 20.6569 21 19C21 17.3431 19.6569 16 18 16C16.3431 16 15 17.3431 15 19C15 20.6569 16.3431 22 18 22Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M8.59 13.51L15.42 17.49" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M15.41 6.51L8.59 10.49" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </ShineGlassButton>
        </div>
      </div>

      {/* Share Modal */}
      {showShareModal && (
        <div className="event-result__modal-overlay" onClick={() => setShowShareModal(false)}>
          <div className="event-result__modal" onClick={(e) => e.stopPropagation()}>
            <h3 className="event-result__modal-title">Share to</h3>

            <div className="event-result__modal-buttons">
              <button
                className="event-result__modal-btn event-result__modal-btn--facebook"
                onClick={() => handleShareTo('facebook')}
              >
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                </svg>
                Facebook
              </button>

              <button
                className="event-result__modal-btn event-result__modal-btn--twitter"
                onClick={() => handleShareTo('twitter')}
              >
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>
                </svg>
                Twitter
              </button>

              <button
                className="event-result__modal-btn event-result__modal-btn--whatsapp"
                onClick={() => handleShareTo('whatsapp')}
              >
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                </svg>
                WhatsApp
              </button>

              <button
                className="event-result__modal-btn event-result__modal-btn--telegram"
                onClick={() => handleShareTo('telegram')}
              >
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z"/>
                </svg>
                Telegram
              </button>

              <button
                className="event-result__modal-btn event-result__modal-btn--copy"
                onClick={handleCopyLink}
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                </svg>
                Copy Link
              </button>
            </div>

            <button
              className="event-result__modal-close"
              onClick={() => setShowShareModal(false)}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
    </>
  );
};

export default EventResultPage;
