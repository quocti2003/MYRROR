/**
 * EventSoundButton - Shared sound toggle button for all Event pages
 * Uses useEventStore for shared state across pages
 */
import GlassThemeButton from '@/components/common/button/GlassThemeButton';
import { useBottomTheme } from '@/hooks/useBottomTheme';
import useEventStore from '@/store/useEventStore';

import './EventSoundButton.css';

// Sound wave icon component
const SoundWaveIcon = ({ isActive }) => (
  <div className="sound-wave-container">
    <svg
      className={`sound-wave-svg ${isActive ? 'active' : ''}`}
      width="120"
      height="24"
      viewBox="0 0 120 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* Straight line - visible when inactive */}
      <path
        className={`wave-path-straight ${!isActive ? 'visible' : ''}`}
        d="M0 12 L120 12"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
      />
      {/* Long flowing wave - visible when active */}
      <g className={`wave-group ${isActive ? 'visible' : ''}`}>
        <path
          className="wave-path-flowing"
          d="M-20 12 Q-15 4, -10 12 Q-5 20, 0 12 Q5 4, 10 12 Q15 20, 20 12 Q25 4, 30 12 Q35 20, 40 12 Q45 4, 50 12 Q55 20, 60 12 Q65 4, 70 12 Q75 20, 80 12 Q85 4, 90 12 Q95 20, 100 12 Q105 4, 110 12 Q115 20, 120 12 Q125 4, 130 12 Q135 20, 140 12"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          fill="none"
        />
      </g>
    </svg>
  </div>
);

const EventSoundButton = ({ className = '' }) => {
  const { theme: arrowTheme } = useBottomTheme();
  const { isSoundActive, toggleSound } = useEventStore();

  return (
    <div className={`event-sound-button ${className}`}>
      <GlassThemeButton
        theme={arrowTheme === 'white' ? 'dark' : 'light'}
        icon={<SoundWaveIcon isActive={isSoundActive} />}
        isCollapsed={true}
        onClick={toggleSound}
      >
        {isSoundActive ? 'Background music on' : 'Background music off'}
      </GlassThemeButton>
    </div>
  );
};

export default EventSoundButton;
