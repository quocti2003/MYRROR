/**
 * Diamond Component - SVG-based diamond shape renderer
 */
import React from 'react';
import { getDiamondConfig } from '@/constants/eventConstants';

// SVG paths for different diamond shapes
const DIAMOND_PATHS = {
  round: 'M50,5 A45,45 0 1,1 50,95 A45,45 0 1,1 50,5',
  oval: 'M50,5 Q85,25 85,50 Q85,75 50,95 Q15,75 15,50 Q15,25 50,5',
  pear: 'M50,5 Q80,30 75,60 Q65,95 50,95 Q35,95 25,60 Q20,30 50,5',
  heart: 'M50,90 L15,50 Q5,30 25,15 Q45,5 50,25 Q55,5 75,15 Q95,30 85,50 Z',
  asscher: 'M50,5 L95,50 L50,95 L5,50 Z',
  emerald: 'M20,15 L80,15 L95,35 L95,65 L80,85 L20,85 L5,65 L5,35 Z',
  marquise: 'M50,5 Q85,25 85,50 Q85,75 50,95 Q15,75 15,50 Q15,25 50,5',
};

// Facet lines for each shape
const FACET_LINES = {
  round: [
    'M50,5 L50,50',
    'M50,95 L50,50',
    'M5,50 L50,50',
    'M95,50 L50,50',
    'M20,20 L50,50',
    'M80,20 L50,50',
    'M20,80 L50,50',
    'M80,80 L50,50',
  ],
  oval: [
    'M50,5 L50,50',
    'M50,95 L50,50',
    'M15,50 L50,50',
    'M85,50 L50,50',
  ],
  pear: [
    'M50,5 L50,50',
    'M50,95 L50,50',
    'M25,60 L50,50',
    'M75,60 L50,50',
  ],
  heart: [
    'M50,90 L50,50',
    'M25,15 L50,50',
    'M75,15 L50,50',
    'M15,50 L50,50',
    'M85,50 L50,50',
  ],
  asscher: [
    'M50,5 L50,95',
    'M5,50 L95,50',
    'M50,5 L5,50',
    'M50,5 L95,50',
    'M50,95 L5,50',
    'M50,95 L95,50',
  ],
  emerald: [
    'M20,15 L80,85',
    'M80,15 L20,85',
    'M50,15 L50,85',
    'M5,50 L95,50',
  ],
  marquise: [
    'M50,5 L50,95',
    'M15,50 L85,50',
    'M30,20 L70,80',
    'M70,20 L30,80',
  ],
};

const Diamond = ({
  shape = 'round',
  size = 60,
  color,
  isSelected = false,
  isHighlighted = false,
  showGlow = false,
  onClick,
  className = '',
}) => {
  const config = getDiamondConfig(shape);
  const diamondColor = color || config?.color || '#E91E63';
  const path = DIAMOND_PATHS[shape] || DIAMOND_PATHS.round;
  const facets = FACET_LINES[shape] || FACET_LINES.round;

  const glowSize = isHighlighted ? 20 : 10;
  const scale = isHighlighted ? 1.2 : 1;

  return (
    <svg
      width={size * scale}
      height={size * scale}
      viewBox="0 0 100 100"
      className={`diamond ${isSelected ? 'diamond--selected' : ''} ${className}`}
      onClick={onClick}
      style={{ cursor: onClick ? 'pointer' : 'default' }}
    >
      <defs>
        {/* Gradient for 3D effect */}
        <linearGradient id={`grad-${shape}-${size}`} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#ffffff" stopOpacity="0.8" />
          <stop offset="30%" stopColor={diamondColor} stopOpacity="0.9" />
          <stop offset="70%" stopColor={diamondColor} stopOpacity="1" />
          <stop offset="100%" stopColor="#000000" stopOpacity="0.3" />
        </linearGradient>

        {/* Glow filter */}
        {showGlow && (
          <filter id={`glow-${shape}-${size}`} x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation={glowSize} result="coloredBlur" />
            <feMerge>
              <feMergeNode in="coloredBlur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        )}
      </defs>

      {/* Main diamond shape */}
      <path
        d={path}
        fill={`url(#grad-${shape}-${size})`}
        stroke={diamondColor}
        strokeWidth="2"
        filter={showGlow ? `url(#glow-${shape}-${size})` : undefined}
      />

      {/* Facet lines */}
      {facets.map((facet, index) => (
        <path
          key={index}
          d={facet}
          stroke="rgba(255,255,255,0.3)"
          strokeWidth="0.5"
          fill="none"
        />
      ))}

      {/* Sparkle highlights */}
      <circle cx="30" cy="30" r="3" fill="rgba(255,255,255,0.8)" />
      <circle cx="35" cy="25" r="1.5" fill="rgba(255,255,255,0.6)" />

      {/* Selection ring */}
      {isSelected && (
        <circle
          cx="50"
          cy="50"
          r="48"
          fill="none"
          stroke={diamondColor}
          strokeWidth="3"
          strokeDasharray="5,3"
          className="diamond__selection-ring"
        />
      )}
    </svg>
  );
};

export default Diamond;
