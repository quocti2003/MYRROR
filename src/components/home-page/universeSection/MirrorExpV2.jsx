import { useState, useCallback, useMemo, useRef, useEffect } from 'react';
import ArrowButton from '@components/common/button/ArrowButton';
import StarlightEffect from './StarlightEffect';
import './MirrorExpV2.css';

// Planet SVG component with unique IDs for each instance
const PlanetOrb = ({ size = 40 }) => {
  const uniqueId = useMemo(() => Math.random().toString(36).substr(2, 9), []);

  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 94 95"
      fill="none"
      className="mirror-exp-v2__planet-orb"
    >
      {/* Background fill to cover the hole */}
      <ellipse cx="47" cy="47.5" rx="43" ry="43.5" fill="#000000"/>
      <ellipse cx="47" cy="47.5" rx="43" ry="43.5" fill={`url(#radial_${uniqueId})`}/>
      <path
        d="M47 4.75C70.3259 4.75 89.25 23.8816 89.25 47.5C89.25 71.1184 70.3259 90.25 47 90.25C23.6741 90.25 4.75 71.1184 4.75 47.5C4.75 23.8816 23.6741 4.75 47 4.75Z"
        stroke={`url(#linear_${uniqueId})`}
        strokeWidth="1.5"
      />
      <defs>
        <filter id={`filter_${uniqueId}`} x="0" y="0" width="94" height="95" filterUnits="userSpaceOnUse" colorInterpolationFilters="sRGB">
          <feFlood floodOpacity="0" result="BackgroundImageFix"/>
          <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" result="shape"/>
          <feTurbulence type="fractalNoise" baseFrequency="2 2" numOctaves="3" seed="8287"/>
          <feDisplacementMap in="shape" scale="8" xChannelSelector="R" yChannelSelector="G" result="displacedImage" width="100%" height="100%"/>
          <feMerge result={`effect_${uniqueId}`}>
            <feMergeNode in="displacedImage"/>
          </feMerge>
        </filter>
        <radialGradient id={`radial_${uniqueId}`} cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(47 47.5) rotate(90) scale(43.5 43)">
          <stop offset="0.725962" stopOpacity="0"/>
          <stop offset="1" stopColor="#FFD9E3" stopOpacity="0.5"/>
        </radialGradient>
        <linearGradient id={`linear_${uniqueId}`} x1="93.9199" y1="50.5893" x2="-1.59243" y2="47.7166" gradientUnits="userSpaceOnUse">
          <stop stopColor="#BC224C"/>
          <stop offset="0.0961538" stopColor="white"/>
          <stop offset="0.293269"/>
          <stop offset="0.716346"/>
          <stop offset="0.875" stopColor="#BC224C"/>
          <stop offset="1" stopColor="white"/>
        </linearGradient>
      </defs>
    </svg>
  );
};

// Small dot SVG component for orbit decoration
const SmallDot = () => {
  const uniqueId = useMemo(() => Math.random().toString(36).substr(2, 9), []);

  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="27"
      height="25"
      viewBox="0 0 27 25"
      fill="none"
      className="mirror-exp-v2__small-dot"
    >
      <g clipPath={`url(#clip_${uniqueId})`}>
        <g transform="matrix(0 0.0123786 -0.0126846 0 13.1288 12.3786)">
          <rect x="0" y="0" width="1080.78" height="1113.86" fill={`url(#grad_${uniqueId})`} opacity="1" shapeRendering="crispEdges"/>
          <rect x="0" y="0" width="1080.78" height="1113.86" transform="scale(1 -1)" fill={`url(#grad_${uniqueId})`} opacity="1" shapeRendering="crispEdges"/>
          <rect x="0" y="0" width="1080.78" height="1113.86" transform="scale(-1 1)" fill={`url(#grad_${uniqueId})`} opacity="1" shapeRendering="crispEdges"/>
          <rect x="0" y="0" width="1080.78" height="1113.86" transform="scale(-1)" fill={`url(#grad_${uniqueId})`} opacity="1" shapeRendering="crispEdges"/>
        </g>
      </g>
      <defs>
        <clipPath id={`clip_${uniqueId}`}>
          <ellipse cx="13.1288" cy="12.3786" rx="13.1288" ry="12.3786"/>
        </clipPath>
        <linearGradient id={`grad_${uniqueId}`} x1="0" y1="0" x2="500" y2="500" gradientUnits="userSpaceOnUse">
          <stop stopColor="white"/>
          <stop offset="0.144231" stopColor="#BC224C"/>
          <stop offset="0.567308" stopColor="#1F1C1B" stopOpacity="0"/>
        </linearGradient>
      </defs>
    </svg>
  );
};

const slides = [
  {
    id: 'sight',
    title: 'Sight',
    description: 'is the first point of reflection.',
    details: 'It establishes the emotional tone that guides all other senses, ensuring that every Mirror experience feels composed, intentional, and emotionally legible from the first moment.'
  },
  {
    id: 'sound',
    title: 'Sound',
    description: 'Sound shapes atmosphere and emotional pacing.',
    details: 'In the Mirror ecosystem, sound is not an accessory. It is a structural element of experience, shaping how moments unfold over time.'
  },
  {
    id: 'touch',
    title: 'Touch',
    description: 'Touch confirms what the eyes believe.',
    details: 'Every texture, weight, and surface in Mirror is designed to communicate quality and care through physical interaction.'
  },
  {
    id: 'scent',
    title: 'Scent',
    description: 'Scent creates invisible architecture.',
    details: 'It defines spaces and moments, leaving lasting impressions that transcend visual memory.'
  },
  {
    id: 'taste',
    title: 'Taste',
    description: 'Taste completes the sensory journey.',
    details: 'It transforms experience into memory, creating moments that linger long after they end.'
  },
  {
    id: 'mind',
    title: 'Mind',
    description: 'Mind unifies all sensory experience.',
    details: 'It is the space where sight, sound, touch, scent, and taste converge into consciousness, creating meaning from sensation.'
  }
];

// Orbit radii (rx, ry) - 10 orbits scaled 80% to match SVG ellipses
const orbitRadii = {
  1: { rx: 163, ry: 34 },      // Innermost
  2: { rx: 332, ry: 70 },
  3: { rx: 351, ry: 70 },
  4: { rx: 554, ry: 114 },
  5: { rx: 594, ry: 114 },
  6: { rx: 725, ry: 158 },
  7: { rx: 771, ry: 158 },
  8: { rx: 1036, ry: 213 },
  9: { rx: 1132, ry: 213 },
  10: { rx: 1340, ry: 234 },   // Outermost
};

// Planet/icon data for 6 planets (matching design image)
const planets = [
  { id: 'planet1', orbit: 7, angle: 155, icon: 'rect', size: 87 },       // orbit 5, left
  { id: 'planet2', orbit: 5, angle: 290, icon: 'circle', size: 55 },  // orbit 5, bottom
  { id: 'planet3', orbit: 6, angle: 250, icon: 'droplet' , size: 45},    // orbit 6
  { id: 'planet4', orbit: 9, angle: 290, icon: 'star' ,size: 55},        // orbit 7
  { id: 'planet5', orbit: 8, angle: 220, icon: 'diamond', size: 45 },     // orbit 8
  { id: 'planet6', orbit: 9, angle: 70, icon: 'heart', size: 86 },  // orbit 9, bottom
];

// Small dots data for decorative points on orbits (matching design)
const smallDots = [
  { id: 'dot1', orbit: 3, angle: 125 },
  { id: 'dot2', orbit: 2, angle: 20 },
  { id: 'dot3', orbit: 6, angle: 0 },
  { id: 'dot4', orbit: 7, angle: 85 },
  { id: 'dot5', orbit: 7, angle: 230 },
  { id: 'dot6', orbit: 8, angle: 140 },
  { id: 'dot7', orbit: 8, angle: 0 },
  { id: 'dot8', orbit: 8, angle: 330 },
  { id: 'dot9', orbit: 8, angle: 250 },
];

// Calculate planet position on ellipse
const getPlanetPosition = (orbit, angleDeg) => {
  const { rx, ry } = orbitRadii[orbit];
  const angleRad = (angleDeg * Math.PI) / 180;
  const x = rx * Math.cos(angleRad);
  const y = ry * Math.sin(angleRad);
  return { x, y };
};

// Map each slide to its featured/active planet
// Slide index → Planet ID
const SLIDE_TO_PLANET = {
  0: 'planet6', // Sight → Heart
  1: 'planet1', // Sound → Rect
  2: 'planet2', // Touch → Circle
  3: 'planet3', // Scent → Droplet
  4: 'planet4', // Taste → Star
  5: 'planet5', // Mind → Diamond
};

// Target size when active (same as heart)
const ACTIVE_SIZE = 86;

// Sparkle component - generates random sparkles for active planet
const Sparkles = ({ count = 12 }) => {
  // Generate random sparkle positions once on mount
  const sparkles = useMemo(() => {
    return Array.from({ length: count }, (_, i) => ({
      id: i,
      // Random position in a circle around the planet
      top: `${15 + Math.random() * 70}%`,
      left: `${15 + Math.random() * 70}%`,
      // Random size between 1-2.5px
      size: 1 + Math.random() * 1.5,
      // Random animation delay (0-3s)
      delay: Math.random() * 3,
      // Random animation duration (3-6s)
      duration: 3 + Math.random() * 3,
      // Random color - mostly white, some pink
      color: Math.random() > 0.7 ? 'rgba(255, 200, 220, 1)' : 'rgba(255, 255, 255, 1)',
    }));
  }, [count]);

  return (
    <div className="mirror-exp-v2__sparkles">
      {sparkles.map((sparkle) => (
        <span
          key={sparkle.id}
          className="mirror-exp-v2__sparkle"
          style={{
            top: sparkle.top,
            left: sparkle.left,
            width: `${sparkle.size}px`,
            height: `${sparkle.size}px`,
            backgroundColor: sparkle.color,
            animationDelay: `${sparkle.delay}s`,
            animationDuration: `${sparkle.duration}s`,
          }}
        />
      ))}
    </div>
  );
};

const MirrorExpV2 = () => {
  const [currentSlide, setCurrentSlide] = useState(0);
  const [rotationStep, setRotationStep] = useState(0); // Separate state for planet rotation
  const [animatedRotation, setAnimatedRotation] = useState(0); // Smoothly animated rotation angle
  const [isAnimating, setIsAnimating] = useState(false);
  const [direction, setDirection] = useState(null); // 'prev' or 'next'
  const [noTransition, setNoTransition] = useState(false);
  const animationTimeoutRef = useRef(null);
  const orbitAnimationRef = useRef(null);
  const animatedRotationRef = useRef(0); // Track current rotation for animation

  // Keep ref in sync with state
  useEffect(() => {
    animatedRotationRef.current = animatedRotation;
  }, [animatedRotation]);

  // Animate rotation smoothly along the orbit
  useEffect(() => {
    const targetRotation = rotationStep * 60;
    const startRotation = animatedRotationRef.current;
    const startTime = performance.now();
    const duration = 700; // Match slide transition duration

    const animate = (currentTime) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);

      // Ease-in-out curve for smooth motion
      const easeProgress = progress < 0.5
        ? 2 * progress * progress
        : 1 - Math.pow(-2 * progress + 2, 2) / 2;

      const currentRotation = startRotation + (targetRotation - startRotation) * easeProgress;
      setAnimatedRotation(currentRotation);

      if (progress < 1) {
        orbitAnimationRef.current = requestAnimationFrame(animate);
      }
    };

    orbitAnimationRef.current = requestAnimationFrame(animate);

    return () => {
      if (orbitAnimationRef.current) {
        cancelAnimationFrame(orbitAnimationRef.current);
      }
    };
  }, [rotationStep]);

  const handlePrev = useCallback((e) => {
    if (isAnimating) return;

    e?.currentTarget?.blur();
    document.activeElement?.blur();

    setDirection('prev');
    setIsAnimating(true);

    // Update planet rotation immediately for smooth animation
    setRotationStep((prev) => prev - 1);

    if (animationTimeoutRef.current) {
      clearTimeout(animationTimeoutRef.current);
    }

    // After slide animation: update content instantly
    animationTimeoutRef.current = setTimeout(() => {
      setNoTransition(true);
      setCurrentSlide((prev) => (prev === 0 ? slides.length - 1 : prev - 1));
      setDirection(null);

      // Re-enable transition after content swap
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          setNoTransition(false);
          setIsAnimating(false);
        });
      });
    }, 700); // Match CSS opacity transition duration
  }, [isAnimating]);

  const handleNext = useCallback((e) => {
    if (isAnimating) return;

    e?.currentTarget?.blur();
    document.activeElement?.blur();

    setDirection('next');
    setIsAnimating(true);

    // Update planet rotation immediately for smooth animation
    setRotationStep((prev) => prev + 1);

    if (animationTimeoutRef.current) {
      clearTimeout(animationTimeoutRef.current);
    }

    // After slide animation: update content instantly
    animationTimeoutRef.current = setTimeout(() => {
      setNoTransition(true);
      setCurrentSlide((prev) => (prev === slides.length - 1 ? 0 : prev + 1));
      setDirection(null);

      // Re-enable transition after content swap
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          setNoTransition(false);
          setIsAnimating(false);
        });
      });
    }, 700); // Match CSS opacity transition duration
  }, [isAnimating]);

  const currentContent = slides[currentSlide];
  const nextSlide = slides[(currentSlide + 1) % slides.length];
  const nextNextSlide = slides[(currentSlide + 2) % slides.length];
  const prevSlide = slides[(currentSlide - 1 + slides.length) % slides.length];
  const prevPrevSlide = slides[(currentSlide - 2 + slides.length) % slides.length];

  return (
    <section className="mirror-exp-v2">
      {/* Header */}
      <div className="mirror-exp-v2__header bodytext-4--no-margin">
        AND AWAKENING LUXURY THROUGH YOUR SENSES
      </div>

      {/* Content Slider */}
      <div className="mirror-exp-v2__slider">
        {/* Slides Track */}
        <div className={`mirror-exp-v2__track ${direction ? `mirror-exp-v2__track--${direction}` : ''}`}>
          {/* Prev-Prev Slide - appears when going prev */}
          <div className={`mirror-exp-v2__slide mirror-exp-v2__slide--prev-prev ${noTransition ? 'mirror-exp-v2__slide--no-transition' : ''}`}>
            <h2 className="mirror-exp-v2__title mirror-exp-v2__title--faded heading-2--no-margin">{prevPrevSlide.title}</h2>
            <div className="mirror-exp-v2__starlight mirror-exp-v2__starlight--faded">
              <StarlightEffect direction="falling" height={40} />
            </div>
            <p className="mirror-exp-v2__description mirror-exp-v2__description--faded bodytext-6--no-margin">
              {prevPrevSlide.description}
            </p>
            <p className="mirror-exp-v2__details mirror-exp-v2__details--faded bodytext-6--no-margin">
              {prevPrevSlide.details}
            </p>
          </div>

          {/* Previous Slide */}
          <div className={`mirror-exp-v2__slide mirror-exp-v2__slide--prev ${noTransition ? 'mirror-exp-v2__slide--no-transition' : ''}`}>
            <h2 className="mirror-exp-v2__title mirror-exp-v2__title--faded heading-2--no-margin">{prevSlide.title}</h2>
            <div className="mirror-exp-v2__starlight mirror-exp-v2__starlight--faded">
              <StarlightEffect direction="falling" height={40} />
            </div>
            <p className="mirror-exp-v2__description mirror-exp-v2__description--faded bodytext-6--no-margin">
              {prevSlide.description}
            </p>
            <p className="mirror-exp-v2__details mirror-exp-v2__details--faded bodytext-6--no-margin">
              {prevSlide.details}
            </p>
          </div>

          {/* Current Slide */}
          <div className={`mirror-exp-v2__slide mirror-exp-v2__slide--current ${noTransition ? 'mirror-exp-v2__slide--no-transition' : ''}`}>
            <h2 className="mirror-exp-v2__title heading-2--no-margin">{currentContent.title}</h2>
            <div className="mirror-exp-v2__starlight">
              <StarlightEffect direction="falling" height={40} />
            </div>
            <p className="mirror-exp-v2__description bodytext-6--no-margin">
              {currentContent.description}
            </p>
            <p className="mirror-exp-v2__details bodytext-6--no-margin">
              {currentContent.details}
            </p>
          </div>

          {/* Next Slide */}
          <div className={`mirror-exp-v2__slide mirror-exp-v2__slide--next ${noTransition ? 'mirror-exp-v2__slide--no-transition' : ''}`}>
            <h2 className="mirror-exp-v2__title mirror-exp-v2__title--faded heading-2--no-margin">{nextSlide.title}</h2>
            <div className="mirror-exp-v2__starlight mirror-exp-v2__starlight--faded">
              <StarlightEffect direction="falling" height={40} />
            </div>
            <p className="mirror-exp-v2__description mirror-exp-v2__description--faded bodytext-6--no-margin">
              {nextSlide.description}
            </p>
            <p className="mirror-exp-v2__details mirror-exp-v2__details--faded bodytext-6--no-margin">
              {nextSlide.details}
            </p>
          </div>

          {/* Next-Next Slide - appears when going next */}
          <div className={`mirror-exp-v2__slide mirror-exp-v2__slide--next-next ${noTransition ? 'mirror-exp-v2__slide--no-transition' : ''}`}>
            <h2 className="mirror-exp-v2__title mirror-exp-v2__title--faded heading-2--no-margin">{nextNextSlide.title}</h2>
            <div className="mirror-exp-v2__starlight mirror-exp-v2__starlight--faded">
              <StarlightEffect direction="falling" height={40} />
            </div>
            <p className="mirror-exp-v2__description mirror-exp-v2__description--faded bodytext-6--no-margin">
              {nextNextSlide.description}
            </p>
            <p className="mirror-exp-v2__details mirror-exp-v2__details--faded bodytext-6--no-margin">
              {nextNextSlide.details}
            </p>
          </div>
        </div>

        {/* Navigation Buttons - outside track */}
        <ArrowButton
          direction="left"
          className="mirror-exp-v2__nav-btn mirror-exp-v2__nav-btn--left"
          onClick={handlePrev}
          ariaLabel="Previous slide"
        />
        <ArrowButton
          direction="right"
          className="mirror-exp-v2__nav-btn mirror-exp-v2__nav-btn--right"
          onClick={handleNext}
          ariaLabel="Next slide"
        />
      </div>

      {/* Universe Visualization */}
      <div className="mirror-exp-v2__universe">
        {/* Elliptical Orbits - SVG based - 10 orbits with non-uniform spacing */}
        <svg className="mirror-exp-v2__orbits-svg" viewBox="0 0 1400 450" preserveAspectRatio="xMidYMid meet">
          <defs>
            {/* Radial gradients for orbit glow fill - 10 orbits (from orbit files 1-10) */}
            <radialGradient id="orbitGlow1" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0.788462" stopColor="#060606" stopOpacity="0"/>
              <stop offset="1" stopColor="#7422BC" stopOpacity="0.4"/>
            </radialGradient>
            <radialGradient id="orbitGlow2" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0.903846" stopColor="#060606" stopOpacity="0"/>
              <stop offset="1" stopColor="#7422BC" stopOpacity="0.2"/>
            </radialGradient>
            <radialGradient id="orbitGlow3" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0.903846" stopColor="#060606" stopOpacity="0"/>
              <stop offset="1" stopColor="#BC224C" stopOpacity="0.6"/>
            </radialGradient>
            <radialGradient id="orbitGlow4" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0.903846" stopColor="#060606" stopOpacity="0"/>
              <stop offset="1" stopColor="#7422BC" stopOpacity="0.15"/>
            </radialGradient>
            <radialGradient id="orbitGlow5" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0.903846" stopColor="#060606" stopOpacity="0"/>
              <stop offset="1" stopColor="#2255BC" stopOpacity="0.25"/>
            </radialGradient>
            <radialGradient id="orbitGlow6" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0.903846" stopColor="#060606" stopOpacity="0"/>
              <stop offset="1" stopColor="#2249BC" stopOpacity="0.15"/>
            </radialGradient>
            <radialGradient id="orbitGlow7" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0.903846" stopColor="#060606" stopOpacity="0"/>
              <stop offset="1" stopColor="#BC224C" stopOpacity="0.25"/>
            </radialGradient>
            <radialGradient id="orbitGlow8" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0.903846" stopColor="#060606" stopOpacity="0"/>
              <stop offset="1" stopColor="#BC224C" stopOpacity="0.25"/>
            </radialGradient>
            <radialGradient id="orbitGlow9" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0.903846" stopColor="#060606" stopOpacity="0"/>
              <stop offset="1" stopColor="#7422BC" stopOpacity="0.1"/>
            </radialGradient>
            <radialGradient id="orbitGlow10" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0.903846" stopColor="#060606" stopOpacity="0"/>
              <stop offset="1" stopColor="#7422BC" stopOpacity="0.08"/>
            </radialGradient>

            {/* Linear gradients for orbit stroke - 10 orbits (from orbit files 1-10) */}
            {/* Orbits 1-3: #BC224C → white → black → black → #BC224C → white */}
            <linearGradient id="orbitStroke1" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#BC224C"/>
              <stop offset="9.61538%" stopColor="#FFFFFF"/>
              <stop offset="29.3269%" stopColor="#000000"/>
              <stop offset="71.6346%" stopColor="#000000"/>
              <stop offset="87.5%" stopColor="#BC224C"/>
              <stop offset="100%" stopColor="#FFFFFF"/>
            </linearGradient>
            <linearGradient id="orbitStroke2" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#BC224C"/>
              <stop offset="9.61538%" stopColor="#FFFFFF"/>
              <stop offset="29.3269%" stopColor="#000000"/>
              <stop offset="71.6346%" stopColor="#000000"/>
              <stop offset="87.5%" stopColor="#BC224C"/>
              <stop offset="100%" stopColor="#FFFFFF"/>
            </linearGradient>
            <linearGradient id="orbitStroke3" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#BC224C"/>
              <stop offset="9.61538%" stopColor="#FFFFFF"/>
              <stop offset="29.3269%" stopColor="#000000"/>
              <stop offset="71.6346%" stopColor="#000000"/>
              <stop offset="87.5%" stopColor="#BC224C"/>
              <stop offset="100%" stopColor="#FFFFFF"/>
            </linearGradient>
            {/* Orbits 4-7: white → black → black → white */}
            <linearGradient id="orbitStroke4" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#FFFFFF"/>
              <stop offset="29.3269%" stopColor="#000000"/>
              <stop offset="71.6346%" stopColor="#000000"/>
              <stop offset="100%" stopColor="#FFFFFF"/>
            </linearGradient>
            <linearGradient id="orbitStroke5" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#FFFFFF"/>
              <stop offset="29.3269%" stopColor="#000000"/>
              <stop offset="71.6346%" stopColor="#000000"/>
              <stop offset="100%" stopColor="#FFFFFF"/>
            </linearGradient>
            <linearGradient id="orbitStroke6" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#FFFFFF"/>
              <stop offset="29.3269%" stopColor="#000000"/>
              <stop offset="71.6346%" stopColor="#000000"/>
              <stop offset="100%" stopColor="#FFFFFF"/>
            </linearGradient>
            <linearGradient id="orbitStroke7" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#FFFFFF"/>
              <stop offset="29.3269%" stopColor="#000000"/>
              <stop offset="71.6346%" stopColor="#000000"/>
              <stop offset="100%" stopColor="#FFFFFF"/>
            </linearGradient>
            {/* Orbit 8: #BC224C → white → black → black → #BC224C → white */}
            <linearGradient id="orbitStroke8" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#BC224C"/>
              <stop offset="9.61538%" stopColor="#FFFFFF"/>
              <stop offset="29.3269%" stopColor="#000000"/>
              <stop offset="71.6346%" stopColor="#000000"/>
              <stop offset="87.5%" stopColor="#BC224C"/>
              <stop offset="100%" stopColor="#FFFFFF"/>
            </linearGradient>
            {/* Orbits 9-10: gradient with varying opacity */}
            <linearGradient id="orbitStroke9" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#8E22BC" stopOpacity="0.12"/>
              <stop offset="15%" stopColor="#FFFFFF" stopOpacity="0.08"/>
              <stop offset="35%" stopColor="#8E22BC" stopOpacity="0.02"/>
              <stop offset="50%" stopColor="#000000" stopOpacity="0.01"/>
              <stop offset="65%" stopColor="#8E22BC" stopOpacity="0.02"/>
              <stop offset="85%" stopColor="#FFFFFF" stopOpacity="0.08"/>
              <stop offset="100%" stopColor="#8E22BC" stopOpacity="0.12"/>
            </linearGradient>
            <linearGradient id="orbitStroke10" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#8E22BC" stopOpacity="0.08"/>
              <stop offset="20%" stopColor="#FFFFFF" stopOpacity="0.05"/>
              <stop offset="40%" stopColor="#8E22BC" stopOpacity="0.01"/>
              <stop offset="50%" stopColor="#000000" stopOpacity="0.005"/>
              <stop offset="60%" stopColor="#8E22BC" stopOpacity="0.01"/>
              <stop offset="80%" stopColor="#FFFFFF" stopOpacity="0.05"/>
              <stop offset="100%" stopColor="#8E22BC" stopOpacity="0.08"/>
            </linearGradient>
          </defs>

          {/* 10 Orbits - from outermost to innermost, scaled 80% */}
          <ellipse cx="700" cy="225" rx="1340" ry="234" fill="url(#orbitGlow10)" stroke="url(#orbitStroke10)" strokeWidth="1.5"/>
          <ellipse cx="700" cy="225" rx="1132" ry="213" fill="url(#orbitGlow9)" stroke="url(#orbitStroke9)" strokeWidth="1.5"/>
          <ellipse cx="700" cy="225" rx="1036" ry="213" fill="url(#orbitGlow8)" stroke="#BC224C" strokeWidth="1.5"/>
          <ellipse cx="700" cy="225" rx="771" ry="158" fill="url(#orbitGlow7)" stroke="url(#orbitStroke7)" strokeWidth="1.5"/>
          <ellipse cx="700" cy="225" rx="725" ry="158" fill="url(#orbitGlow6)" stroke="url(#orbitStroke6)" strokeWidth="1.5"/>
          <ellipse cx="700" cy="225" rx="594" ry="114" fill="url(#orbitGlow5)" stroke="url(#orbitStroke5)" strokeWidth="1.5"/>
          <ellipse cx="700" cy="225" rx="554" ry="114" fill="url(#orbitGlow4)" stroke="url(#orbitStroke4)" strokeWidth="1"/>
          <ellipse cx="700" cy="225" rx="351" ry="70" fill="url(#orbitGlow3)" stroke="#BC224C" strokeWidth="1.5"/>
          <ellipse cx="700" cy="225" rx="332" ry="70" fill="url(#orbitGlow2)" stroke="#BC224C" strokeWidth="1.5"/>
          <ellipse cx="700" cy="225" rx="163" ry="34" fill="url(#orbitGlow1)" stroke="#BC224C" strokeWidth="1.5"/>
        </svg>

        {/* Center Element */}
        <div className="mirror-exp-v2__center">
          <svg xmlns="http://www.w3.org/2000/svg" width="71" height="70" viewBox="0 0 71 70" fill="none" className="mirror-exp-v2__center-diamond">
            <g filter="url(#filter0_center)">
              <ellipse cx="35.4666" cy="34.5333" rx="26.1333" ry="25.2" fill="url(#paint0_radial_center)"/>
              <path d="M35.467 10.0833C49.5116 10.0835 60.8499 21.0557 60.8499 34.5334C60.8497 48.0111 49.5115 58.9834 35.467 58.9836C21.4223 58.9836 10.0834 48.0112 10.0833 34.5334C10.0833 21.0556 21.4223 10.0833 35.467 10.0833Z" stroke="url(#paint1_linear_center)" strokeWidth="1.5"/>
            </g>
            <g clipPath="url(#paint2_diamond_clip)">
              <g transform="matrix(0 0.035 -0.0342667 0 35.4667 35)">
                <rect x="0" y="0" width="1028.57" height="1064.2" fill="url(#paint2_diamond_center)" opacity="1" shapeRendering="crispEdges"/>
                <rect x="0" y="0" width="1028.57" height="1064.2" transform="scale(1 -1)" fill="url(#paint2_diamond_center)" opacity="1" shapeRendering="crispEdges"/>
                <rect x="0" y="0" width="1028.57" height="1064.2" transform="scale(-1 1)" fill="url(#paint2_diamond_center)" opacity="1" shapeRendering="crispEdges"/>
                <rect x="0" y="0" width="1028.57" height="1064.2" transform="scale(-1)" fill="url(#paint2_diamond_center)" opacity="1" shapeRendering="crispEdges"/>
              </g>
            </g>
            <defs>
              <filter id="filter0_center" x="5.33325" y="5.33325" width="60.2666" height="58.3999" filterUnits="userSpaceOnUse" colorInterpolationFilters="sRGB">
                <feFlood floodOpacity="0" result="BackgroundImageFix"/>
                <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" result="shape"/>
                <feTurbulence type="fractalNoise" baseFrequency="2 2" numOctaves="3" seed="8287"/>
                <feDisplacementMap in="shape" scale="8" xChannelSelector="R" yChannelSelector="G" result="displacedImage" width="100%" height="100%"/>
                <feMerge result="effect1_texture">
                  <feMergeNode in="displacedImage"/>
                </feMerge>
              </filter>
              <clipPath id="paint2_diamond_clip">
                <ellipse cx="35.4667" cy="35" rx="35.4667" ry="35"/>
              </clipPath>
              <radialGradient id="paint0_radial_center" cx="0" cy="0" r="1" gradientUnits="userSpaceOnUse" gradientTransform="translate(35.4666 34.5333) rotate(90) scale(25.2 26.1333)">
                <stop offset="0.9" stopOpacity="0"/>
                <stop offset="1" stopColor="#FDFDFB" stopOpacity="0.5"/>
              </radialGradient>
              <linearGradient id="paint1_linear_center" x1="63.9823" y1="36.3229" x2="5.93972" y2="34.4915" gradientUnits="userSpaceOnUse">
                <stop stopColor="#BC224C"/>
                <stop offset="0.0961538" stopColor="white"/>
                <stop offset="0.293269"/>
                <stop offset="0.716346"/>
                <stop offset="0.875" stopColor="#BC224C"/>
                <stop offset="1" stopColor="white"/>
              </linearGradient>
              <linearGradient id="paint2_diamond_center" x1="0" y1="0" x2="500" y2="500" gradientUnits="userSpaceOnUse">
                <stop offset="0.144231" stopColor="white"/>
                <stop offset="0.567308" stopColor="#1F1C1B" stopOpacity="0"/>
              </linearGradient>
            </defs>
          </svg>
        </div>

        {/* Planets/Icons on orbits */}
        <div className="mirror-exp-v2__planets">
          {planets.map((planet) => {
            // Use animatedRotation for smooth orbit movement
            const pos = getPlanetPosition(planet.orbit, planet.angle + animatedRotation);

            // Calculate which slide is visually active (accounts for animation direction)
            let visualSlide = currentSlide;
            if (direction === 'next') {
              visualSlide = (currentSlide + 1) % slides.length;
            } else if (direction === 'prev') {
              visualSlide = (currentSlide - 1 + slides.length) % slides.length;
            }

            // Check if this planet is active for the visual slide
            const isActive = SLIDE_TO_PLANET[visualSlide] === planet.id;

            // Calculate scale: when active, scale up to match ACTIVE_SIZE
            const baseSize = planet.size || 60;
            const activeScale = ACTIVE_SIZE / baseSize;

            return (
              <div
                key={planet.id}
                className={`mirror-exp-v2__planet mirror-exp-v2__planet--orbit${planet.orbit}`}
                style={{
                  transform: `translate(calc(-50% + ${pos.x}px), calc(-50% + ${pos.y}px))`
                }}
              >
                <div
                  className={`mirror-exp-v2__planet-inner${isActive ? ' mirror-exp-v2__planet-inner--active' : ''}`}
                  style={isActive ? { transform: `scale(${activeScale})` } : undefined}
                >
                  {isActive && <Sparkles count={14} />}
                  <PlanetOrb size={baseSize} />
                  <span className="mirror-exp-v2__icon-glow"></span>
                  <span className={`mirror-exp-v2__planet-icon mirror-exp-v2__planet-icon--${planet.icon}`}></span>
                </div>
              </div>
            );
          })}

          {/* Small decorative dots on orbits */}
          {smallDots.map((dot) => {
            // Use animatedRotation for smooth orbit movement
            const pos = getPlanetPosition(dot.orbit, dot.angle + animatedRotation);
            return (
              <div
                key={dot.id}
                className="mirror-exp-v2__small-dot-wrapper"
                style={{
                  transform: `translate(calc(-50% + ${pos.x}px), calc(-50% + ${pos.y}px))`
                }}
              >
                <SmallDot />
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};

export default MirrorExpV2;
