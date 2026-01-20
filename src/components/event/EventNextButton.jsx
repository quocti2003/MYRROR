/**
 * EventNextButton - Reusable next button for event pages
 * Shows icon only on mobile, icon + text on tablet/desktop
 */
import { useState, useEffect } from 'react';
import GlassThemeButton from '@/components/common/button/GlassThemeButton';

import './EventNextButton.css';

export default function EventNextButton({ onClick, className = '', children = 'Tiếp tục', disabled = false }) {
  const [isTabletOrDesktop, setIsTabletOrDesktop] = useState(window.innerWidth > 480);

  useEffect(() => {
    const handleResize = () => {
      setIsTabletOrDesktop(window.innerWidth > 480);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return (
    <div className={`event-next-button ${className}`}>
      <GlassThemeButton
        theme="event_dark"
        onClick={onClick}
        textClassName="bodytext-6--no-margin"
        expandable={false}
        disabled={disabled}
        icon={
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="16" viewBox="0 0 20 16" fill="none">
            <path d="M0.699219 7.6001L19 7.6001M19 7.6001L13.8571 2.6001M19 7.6001L13.8571 12.6001" stroke="currentColor" strokeLinecap="square"/>
          </svg>
        }
      >
        {isTabletOrDesktop && children}
      </GlassThemeButton>
    </div>
  );
}
