/**
 * EventDisplayPage - Large screen display for venue projection
 */
import React, { useEffect, useState, useRef } from 'react';
import { fetchAllNotes } from '@services/event/eventApi';
import {
  subscribeToNotesChannel,
  subscribeToLuckyDrawChannel,
} from '@services/event/ably';
import MusicStaff from '@components/event/ui/MusicStaff';
import Diamond from '@components/event/ui/Diamond';
import Logo from '@components/event/ui/Logo';
import ChristmasMusicSheet from '@components/event/ui/ChristmasMusicSheet';
import { getDiamondConfig, TEXT } from '@/constants/eventConstants';

import '@styles/event.css';

const EventDisplayPage = () => {
  const [notes, setNotes] = useState([]);
  const [latestNote, setLatestNote] = useState(null);
  const [showLuckyDraw, setShowLuckyDraw] = useState(false);
  const [winner, setWinner] = useState(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [christmasMode, setChristmasMode] = useState(false);

  const staffContainerRef = useRef(null);

  // Load initial notes
  useEffect(() => {
    const loadNotes = async () => {
      const allNotes = await fetchAllNotes();
      setNotes(allNotes);
    };
    loadNotes();
  }, []);

  // Subscribe to real-time updates
  useEffect(() => {
    // Notes channel
    const unsubscribeNotes = subscribeToNotesChannel((newNote) => {
      setNotes((prev) => [...prev, newNote]);
      setLatestNote(newNote);

      // Clear latest note banner after 5 seconds
      setTimeout(() => {
        setLatestNote((current) => (current?.id === newNote.id ? null : current));
      }, 5000);

      // Auto-scroll to show new note
      if (staffContainerRef.current) {
        staffContainerRef.current.scrollLeft = staffContainerRef.current.scrollWidth;
      }
    });

    // Lucky draw channel
    const unsubscribeLuckyDraw = subscribeToLuckyDrawChannel({
      onDrawStart: () => {
        setShowLuckyDraw(true);
        setIsDrawing(true);
        setWinner(null);
      },
      onWinnerSelected: (winnerData) => {
        setIsDrawing(false);
        setWinner(winnerData);
      },
    });

    return () => {
      unsubscribeNotes();
      unsubscribeLuckyDraw();
    };
  }, []);

  const closeLuckyDraw = () => {
    setShowLuckyDraw(false);
    setWinner(null);
    setIsDrawing(false);
  };

  // Toggle Christmas mode
  const toggleChristmasMode = () => {
    setChristmasMode((prev) => !prev);
  };

  // Christmas mode - full screen music sheet
  if (christmasMode) {
    return (
      <div className="event-display event-display--christmas">
        <ChristmasMusicSheet
          autoPlay={false}
          showTitle={true}
          onComplete={() => {}}
        />

        {/* Toggle button */}
        <button
          className="christmas-mode-toggle"
          onClick={toggleChristmasMode}
        >
          Quay lại
        </button>

        {/* Lucky draw overlay */}
        {showLuckyDraw && (
          <div className="lucky-draw-overlay" onClick={winner ? closeLuckyDraw : undefined}>
            <div className="lucky-draw-content">
              <h2>{TEXT.luckyDrawTitle}</h2>

              {isDrawing && (
                <div className="lucky-draw-animation">
                  <div className="spinning-diamond">
                    <Diamond shape="heart" size={100} showGlow isHighlighted />
                  </div>
                  <p>Đang quay số...</p>
                </div>
              )}

              {winner && (
                <div className="lucky-draw-winner">
                  <h3>{TEXT.luckyDrawWinner}</h3>
                  <div className="winner-info">
                    <Diamond
                      shape={winner.diamondShape || 'heart'}
                      size={120}
                      showGlow
                      isHighlighted
                    />
                    <p className="winner-light">Ánh sáng #{winner.lightNumber}</p>
                    <p className="winner-name">{winner.displayName}</p>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="event-display">
      {/* Header */}
      <div className="event-display__header">
        <Logo size="lg" />
        <div className="event-display__stats">
          <span className="event-stat-item">
            <span className="event-stat-value">{notes.length}</span>
            <span className="event-stat-label">Nốt nhạc</span>
          </span>
        </div>
      </div>

      {/* Main music staff */}
      <div className="event-display__staff" ref={staffContainerRef}>
        <MusicStaff
          notes={notes}
          interactive={false}
          highlightedNoteId={latestNote?.id}
          width="200%"
        />
      </div>

      {/* Latest note banner */}
      {latestNote && (
        <div className="event-display__latest">
          <Diamond
            shape={latestNote.diamondShape}
            size={40}
            color={getDiamondConfig(latestNote.diamondShape)?.color}
            showGlow
          />
          <span className="latest-info">
            <strong>{latestNote.userDisplayName}</strong> vừa đặt một{' '}
            {getDiamondConfig(latestNote.diamondShape)?.displayNameVi}
          </span>
        </div>
      )}

      {/* Lucky draw overlay */}
      {showLuckyDraw && (
        <div className="lucky-draw-overlay" onClick={winner ? closeLuckyDraw : undefined}>
          <div className="lucky-draw-content">
            <h2>{TEXT.luckyDrawTitle}</h2>

            {isDrawing && (
              <div className="lucky-draw-animation">
                <div className="spinning-diamond">
                  <Diamond shape="heart" size={100} showGlow isHighlighted />
                </div>
                <p>Đang quay số...</p>
              </div>
            )}

            {winner && (
              <div className="lucky-draw-winner">
                <h3>{TEXT.luckyDrawWinner}</h3>
                <div className="winner-info">
                  <Diamond
                    shape={winner.diamondShape || 'heart'}
                    size={120}
                    showGlow
                    isHighlighted
                  />
                  <p className="winner-light">Ánh sáng #{winner.lightNumber}</p>
                  <p className="winner-name">{winner.displayName}</p>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Footer */}
      <div className="event-display__footer">
        <p>{TEXT.tagline}</p>
      </div>

      {/* Christmas mode toggle button */}
      <button
        className="christmas-mode-toggle"
        onClick={toggleChristmasMode}
      >
        Nhạc Giáng Sinh
      </button>
    </div>
  );
};

export default EventDisplayPage;
