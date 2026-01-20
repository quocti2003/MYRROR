/**
 * EdgeNextButton - Reusable edge button for event pages
 * Shows on left edge, vertically centered on tablet/mobile only
 * Optionally shows a sliding panel with title/description
 * Click outside to close panel
 */
import { useState, useRef, useEffect } from 'react';
import './EdgeNextButton.css';

export default function EdgeNextButton({ onClick, className = '', title, description }) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef(null);

  // Click outside to close panel
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (isOpen && containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const handleButtonClick = () => {
    if (title || description) {
      // Toggle panel open/close
      setIsOpen(!isOpen);
    } else {
      // No panel, just navigate
      onClick?.();
    }
  };

  return (
    <div ref={containerRef} className="edge-next-button-container">
      <button
        className={`edge-next-button ${className} ${isOpen ? 'edge-next-button--open' : ''}`}
        onClick={handleButtonClick}
      >
        <svg className="edge-next-button__arrow" xmlns="http://www.w3.org/2000/svg" width="6" height="11" viewBox="0 0 6 11" fill="none">
          <path d="M1.0625 9.06067L5.0625 5.06067L1.0625 1.06067" stroke="#0B0B0B" strokeWidth="1.5" strokeLinecap="square" strokeLinejoin="round"/>
        </svg>
      </button>

      {(title || description) && (
        <div
          className={`edge-panel ${isOpen ? 'edge-panel--open' : ''}`}
        >
          <div className="edge-panel__content">
            {title && <h3 className="edge-panel__title">{title}</h3>}
            {description && <p className="edge-panel__description">{description}</p>}
          </div>
        </div>
      )}
    </div>
  );
}
