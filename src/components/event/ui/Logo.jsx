/**
 * Event Logo Component
 */
import React from 'react';
import { TEXT } from '@/constants/eventConstants';

const Logo = ({ size = 'md', className = '' }) => {
  const sizes = {
    sm: { fontSize: '1.2rem', subSize: '0.7rem' },
    md: { fontSize: '1.8rem', subSize: '0.9rem' },
    lg: { fontSize: '2.5rem', subSize: '1.1rem' },
  };

  const { fontSize, subSize } = sizes[size] || sizes.md;

  return (
    <div className={`event-logo ${className}`} style={{ textAlign: 'center' }}>
      <h1
        style={{
          fontSize,
          fontFamily: '"Playfair Display", serif',
          fontWeight: 600,
          color: '#fff',
          margin: 0,
          letterSpacing: '0.05em',
        }}
      >
        MIRROR <span style={{ color: '#E91E63' }}>×</span> Doc Mong Mo
      </h1>
      <p
        style={{
          fontSize: subSize,
          color: 'rgba(255,255,255,0.7)',
          margin: '0.5rem 0 0',
          fontStyle: 'italic',
        }}
      >
        {TEXT.eventName}
      </p>
    </div>
  );
};

export default Logo;
