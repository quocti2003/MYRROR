/**
 * EventBackButton - Reusable back button for event pages
 * Shows icon only on mobile, icon + text on tablet/desktop
 */
import { useState, useEffect } from 'react';
import GlassThemeButton from '@/components/common/button/GlassThemeButton';

import './EventBackButton.css';

export default function EventBackButton({ onClick, className = '', theme = 'event_dark' }) {
  const [isTabletOrDesktop, setIsTabletOrDesktop] = useState(window.innerWidth > 480);

  useEffect(() => {
    const handleResize = () => {
      setIsTabletOrDesktop(window.innerWidth > 480);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return (
    <div className={`event-back-button ${className}`}>
      <GlassThemeButton
        theme={theme}
        onClick={onClick}
        textClassName="bodytext-6--no-margin"
        expandable={false}
        icon={
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="16" viewBox="0 0 20 16" fill="none">
            <path d="M19.3008 7.6001L1 7.6001M1 7.6001L6.14286 2.6001M1 7.6001L6.14286 12.6001" stroke="currentColor" strokeLinecap="square"/>
          </svg>
        }
      >
        {isTabletOrDesktop && 'Quay lại'}
      </GlassThemeButton>
    </div>
  );
}
