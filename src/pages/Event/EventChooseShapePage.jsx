/**
 * EventChooseShapePage - Choose diamond shape for Mirror Diamond Event
 * Click on shapes on the orbit to select them
 */
import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import NavbarV4 from '@/components/navbar/NavbarV4';
import EventBackButton from '@/components/event/EventBackButton';
import EventNextButton from '@/components/event/EventNextButton';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';
import useEventStore from '@/store/useEventStore';
import { placeNote } from '@services/event/eventApi';

import './EventChooseShapePage.css';

// GIF mapping for iOS (WebM alpha not supported, frames too slow)
const shapeGifs = {
  h1: 'mirror_DMM/HEART.gif',
  h2: 'mirror_DMM/OVAL.gif',
  h3: 'mirror_DMM/ROUND.gif',
  h4: 'mirror_DMM/PEAR.gif',
  h5: 'mirror_DMM/ASSCHER.gif',
  h6: 'mirror_DMM/EMERALD.gif',
  h7: 'mirror_DMM/MARQUISE.gif',
};

// Frame animation config for macOS Safari (WebM alpha not supported)
const SHAPE_FRAME_COUNT = 163;
const SHAPE_FPS = 30;

// Frame folder mapping for each shape
const shapeFrameFolders = {
  h1: 'heart-frames',
  h2: 'oval-frames',
  h3: 'round-frames',
  h4: 'pear-frames',
  h5: 'asscher-frames',
  h6: 'emerald-frames',
  h7: 'marquise-frames',
};

// Fixed mapping: Shape ID → Note position (each shape has one specific note)
// Must match PlaceNoteScreenNew.jsx
const SHAPE_TO_POSITION = {
  h1: 0,  // Heart → Đô (C4)
  h2: 1,  // Oval → Rê (D4)
  h3: 2,  // Round → Mi (E4)
  h4: 3,  // Pear → Pha (F4)
  h5: 4,  // Asscher → Son (G4)
  h6: 5,  // Emerald → La (A4)
  h7: 6,  // Marquise → Si (B4)
};

// Mapping from position Y to pitch name (must match SHAPE_TO_POSITION)
const POSITION_TO_PITCH = {
  0: 'C4',  // Đô
  1: 'D4',  // Rê
  2: 'E4',  // Mi
  3: 'F4',  // Pha
  4: 'G4',  // Son
  5: 'A4',  // La
  6: 'B4',  // Si
};

// Generate random X position (30-70 for better visual)
const getRandomPositionX = () => 30 + Math.floor(Math.random() * 41);

// Video mapping for center display
const shapeVideos = {
  h1: 'mirror_DMM/HEART.webm',
  h2: 'mirror_DMM/OVAL.webm',
  h3: 'mirror_DMM/ROUND.webm',
  h4: 'mirror_DMM/PEAR.webm',
  h5: 'mirror_DMM/ASSCHER.webm',
  h6: 'mirror_DMM/EMERALD.webm',
  h7: 'mirror_DMM/MARQUISE.webm',
};

// Get video URL for shape
const getVideoUrl = (shape) => getMediaUrl(shapeVideos[shape.id]);

// Get GIF URL for shape (iOS/in-app browsers)
const getGifUrl = (shape) => getMediaUrl(shapeGifs[shape.id]);

// Diamond shapes data - H1 to H7 (scale: 1 = default, >1 = larger)
// HEART-01 = Heart shape
// HEART-02 = Oval shape
// HEART-03 = Round shape
// HEART-04 = Pear shape
// HEART-05 = Asscher shape
// HEART-06 = Emerald shape
// HEART-07 = Marquise shape
const shapes = [
  {
    id: 'h1',
    name: 'Heart shape',
    image: 'mirror_DMM/HEART-01.webp', // Heart shape
    scale: 1.2,
  },
  {
    id: 'h2',
    name: 'Oval shape',
    image: 'mirror_DMM/HEART-02.webp', // Oval shape
    scale: 1,
  },
  {
    id: 'h3',
    name: 'Round shape',
    image: 'mirror_DMM/HEART-03.webp', // Round shape
    scale: 1.2,
  },
  {
    id: 'h4',
    name: 'Pear shape',
    image: 'mirror_DMM/HEART-04.webp', // Pear shape
    scale: 1.2,
  },
  {
    id: 'h5',
    name: 'Asscher shape',
    image: 'mirror_DMM/HEART-05.webp', // Asscher shape
    scale: 1.3,
  },
  {
    id: 'h6',
    name: 'Emerald shape',
    image: 'mirror_DMM/HEART-06.webp', // Emerald shape
    scale: 1.1,
  },
  {
    id: 'h7',
    name: 'Marquise shape',
    image: 'mirror_DMM/HEART-07.webp', // Marquise shape
    scale: 0.8,
  },
];

// Orbit configuration - 3 elliptical orbits (rx = width/2, ry = height/2)
const orbitRadii = {
  1: { rx: 333, ry: 56 },    // Innermost: 666x112
  2: { rx: 539, ry: 80 },    // Middle: 1078x160
  3: { rx: 821, ry: 113.5 }, // Outermost: 1642x227
};

// Fixed positions for shapes on innermost orbit (angles in degrees)
const orbitAngles = [0, 51, 103, 154, 206, 257, 309];

// ViewBox dimensions for percentage calculation
const VIEWBOX_WIDTH = 1700;
const VIEWBOX_HEIGHT = 280;
const CENTER_X = 850;
const CENTER_Y = 140;

// Calculate position on ellipse as percentage (to scale with SVG)
const getPositionOnOrbit = (orbit, angleDeg) => {
  const { rx, ry } = orbitRadii[orbit];
  const angleRad = (angleDeg * Math.PI) / 180;
  const x = rx * Math.cos(angleRad);
  const y = ry * Math.sin(angleRad);
  // Convert to percentage of viewBox
  const percentX = ((CENTER_X + x) / VIEWBOX_WIDTH) * 100;
  const percentY = ((CENTER_Y + y) / VIEWBOX_HEIGHT) * 100;
  return { percentX, percentY };
};

const EventChooseShapePage = () => {
  const navigate = useNavigate();
  const {
    user,
    selectedDiamond,
    setSelectedDiamond,
    setUserNote,
    setInitialNotePosition,
  } = useEventStore();

  // Check if user has already selected a shape (locked mode)
  const isLocked = !!selectedDiamond;

  // Find initial index based on previously selected diamond (or default to 0)
  const getInitialIndex = () => {
    if (!selectedDiamond) return 0;
    const index = shapes.findIndex(shape => shape.id === selectedDiamond);
    return index >= 0 ? index : 0;
  };

  const [currentIndex, setCurrentIndex] = useState(getInitialIndex);
  const [prevIndex, setPrevIndex] = useState(null);
  const [isAnimating, setIsAnimating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [lockedMessage, setLockedMessage] = useState(''); // Message when trying to change locked selection
  // Initialize rotation offset to show selected shape at center
  const [rotationOffset, setRotationOffset] = useState(() => orbitAngles[getInitialIndex()]);
  const [isEntering, setIsEntering] = useState(true); // For entrance animation
  const [showIdleRipple, setShowIdleRipple] = useState(false); // For idle ripple effect
  const animationRef = useRef(null);
  const idleTimerRef = useRef(null);

  // Platform detection: iOS uses GIF, macOS Safari uses WebP frames, others use WebM
  const [isIOS, setIsIOS] = useState(false);
  const [isMacSafari, setIsMacSafari] = useState(false);

  // Frame animation state for macOS Safari
  const [shapeFrames, setShapeFrames] = useState({});
  const [currentShapeFrame, setCurrentShapeFrame] = useState(0);
  const [loadedShapes, setLoadedShapes] = useState({});
  const shapeAnimationRef = useRef(null);
  const lastShapeFrameTimeRef = useRef(0);

  // Detect platform on mount
  // iOS/in-app browsers → GIF (simple, fast)
  // macOS Safari → WebP frames (smooth animation)
  // Other browsers → WebM video (best quality with alpha)
  useEffect(() => {
    const ua = navigator.userAgent;
    const uaLower = ua.toLowerCase();

    const isSafari = /^((?!chrome|android).)*safari/i.test(ua);
    const isIOSDevice = /iPad|iPhone|iPod/.test(ua) ||
      (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

    // In-app browsers on Android should use GIF too (WebM may not work)
    const isInAppBrowser = /zalo|fbav|fban|instagram|line|kakaotalk|wechat|micromessenger/.test(uaLower);
    const isAndroidInApp = isInAppBrowser && /android/i.test(ua);

    // iOS or Android in-app browsers → use GIF
    setIsIOS(isIOSDevice || isAndroidInApp);
    // macOS Safari (not iOS) → use WebP frames
    setIsMacSafari(isSafari && !isIOSDevice && !isAndroidInApp);
  }, []);

  // Preload frames for current shape (macOS Safari only)
  useEffect(() => {
    if (!isMacSafari) {
      console.log('[Frame Loading] Skipped - not macOS Safari');
      return;
    }

    const currentShapeId = shapes[currentIndex].id;

    // Skip if already loaded
    if (loadedShapes[currentShapeId]) {
      console.log('[Frame Loading] Skipped - already loaded:', currentShapeId);
      return;
    }

    console.log('[Frame Loading] Starting for:', currentShapeId);

    const loadFrames = async () => {
      const frameFolder = shapeFrameFolders[currentShapeId];
      if (!frameFolder) return;

      // Build frame URLs
      const frameUrls = [];
      for (let i = 1; i <= SHAPE_FRAME_COUNT; i++) {
        const frameNum = String(i).padStart(3, '0');
        frameUrls.push(`/dmm-event/${frameFolder}/frame_${frameNum}.webp`);
      }

      // Load all frames in parallel for faster loading
      await Promise.all(frameUrls.map(src =>
        new Promise((resolve) => {
          const img = new Image();
          img.onload = resolve;
          img.onerror = resolve;
          img.src = src;
        })
      ));

      console.log('[Frame Loading] Completed:', currentShapeId, frameUrls.length, 'frames');

      setShapeFrames(prev => ({
        ...prev,
        [currentShapeId]: frameUrls,
      }));
      setLoadedShapes(prev => ({
        ...prev,
        [currentShapeId]: true,
      }));
    };

    loadFrames();
  }, [isMacSafari, currentIndex, loadedShapes]);

  // Animation loop for shape frames (macOS Safari only)
  const animateShapeFrames = useCallback((timestamp) => {
    const frameInterval = 1000 / SHAPE_FPS;
    if (timestamp - lastShapeFrameTimeRef.current >= frameInterval) {
      setCurrentShapeFrame((prev) => (prev + 1) % SHAPE_FRAME_COUNT);
      lastShapeFrameTimeRef.current = timestamp;
    }
    shapeAnimationRef.current = requestAnimationFrame(animateShapeFrames);
  }, []);

  // Start/stop shape animation based on macOS Safari and loaded state
  useEffect(() => {
    const currentShapeId = shapes[currentIndex].id;
    if (!isMacSafari || !loadedShapes[currentShapeId]) {
      console.log('[Animation] Not starting:', { isMacSafari, loaded: loadedShapes[currentShapeId] });
      return;
    }

    console.log('[Animation] Starting for:', currentShapeId);

    setCurrentShapeFrame(0);
    lastShapeFrameTimeRef.current = 0;

    shapeAnimationRef.current = requestAnimationFrame(animateShapeFrames);

    return () => {
      if (shapeAnimationRef.current) {
        cancelAnimationFrame(shapeAnimationRef.current);
      }
    };
  }, [isMacSafari, currentIndex, loadedShapes, animateShapeFrames]);

  // Don't remove entrance class - just let animation complete naturally
  // The class stays but animation only runs once
  useEffect(() => {
    // Animation will complete naturally without removing the class
    // This prevents the "snap" when class is removed
  }, []);

  // Idle ripple effect - show after 5 seconds of inactivity
  useEffect(() => {
    const startIdleTimer = () => {
      // Clear existing timer
      if (idleTimerRef.current) {
        clearTimeout(idleTimerRef.current);
      }
      setShowIdleRipple(false);

      // Start new timer - show ripple after 5 seconds
      idleTimerRef.current = setTimeout(() => {
        setShowIdleRipple(true);
      }, 5000);
    };

    // Start timer on mount and when currentIndex changes
    startIdleTimer();

    return () => {
      if (idleTimerRef.current) {
        clearTimeout(idleTimerRef.current);
      }
    };
  }, [currentIndex]);

  // Animate rotation when currentIndex changes
  useEffect(() => {
    const targetOffset = orbitAngles[currentIndex];

    // Normalize startOffset to 0-360 range for calculation
    let normalizedStart = rotationOffset % 360;
    if (normalizedStart < 0) normalizedStart += 360;

    // Calculate shortest rotation path
    let diff = targetOffset - normalizedStart;
    if (diff > 180) diff -= 360;
    if (diff < -180) diff += 360;

    const duration = 1200; // ms
    const startTime = performance.now();
    // Use the actual rotationOffset as start (not normalized) to prevent jumps
    const animStartOffset = rotationOffset;
    // Calculate actual target to maintain continuity
    const animTargetOffset = animStartOffset + diff;

    const animate = (currentTime) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);

      // Easing function with minimal bounce effect
      const easeOutBounce = (t) => {
        if (t < 0.9) {
          // Ease out to tiny overshoot
          return 1.008 * (1 - Math.pow(1 - t / 0.9, 3));
        } else {
          // Bounce back to target
          const bounceProgress = (t - 0.9) / 0.1;
          return 1.008 - 0.008 * (1 - Math.pow(1 - bounceProgress, 2));
        }
      };

      const eased = easeOutBounce(progress);
      const currentOffset = animStartOffset + (animTargetOffset - animStartOffset) * eased;
      setRotationOffset(currentOffset);

      if (progress < 1) {
        animationRef.current = requestAnimationFrame(animate);
      }
    };

    animationRef.current = requestAnimationFrame(animate);

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [currentIndex]);

  const handleShapeClick = (index) => {
    if (index === currentIndex || isAnimating) return;

    // If locked and trying to select different shape, show message
    if (isLocked) {
      setLockedMessage('Bạn đã chọn Nốt sáng.');
      // Auto hide message after 2 seconds
      setTimeout(() => setLockedMessage(''), 2000);
      return;
    }

    setPrevIndex(currentIndex);
    setIsAnimating(true);
    setCurrentIndex(index);

    // Reset animation state after animation completes
    setTimeout(() => {
      setIsAnimating(false);
      setPrevIndex(null);
    }, 600);
  };

  const handleBack = () => {
    navigate(ROUTES.EVENT_NAME);
  };

  const handleNext = async () => {
    // If already locked (shape selected), just navigate to next step
    if (isLocked) {
      navigate(ROUTES.EVENT_PLACE_NOTE);
      return;
    }

    if (saving) return;

    setSaving(true);
    setError('');

    try {
      const selectedShape = shapes[currentIndex].id;

      // Position determined by shape (not random)
      const positionX = 50; // Always centered
      const positionY = SHAPE_TO_POSITION[selectedShape];
      const pitch = POSITION_TO_PITCH[positionY];

      // Save note to database
      const noteData = {
        userId: user.id,
        userDisplayName: user.displayName,
        diamondShape: selectedShape,
        pitch,
        positionX,
        positionY,
      };

      const result = await placeNote(noteData);

      if (result.success) {
        // Save to store
        setSelectedDiamond(selectedShape);
        setInitialNotePosition({ x: positionX, y: positionY });
        setUserNote({
          ...result.note,
          diamondShape: selectedShape,
          positionX,
          positionY,
          pitch,
        });

        // Navigate to place note screen
        navigate(ROUTES.EVENT_PLACE_NOTE);
      } else {
        setError(result.error || 'Không thể lưu. Vui lòng thử lại.');
      }
    } catch (err) {
      console.error('Save note error:', err);
      setError('Đã có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
      setSaving(false);
    }
  };

  const currentShape = shapes[currentIndex];

  // Calculate angle for each shape - animate along the orbit
  const getShapeAngle = (index) => {
    const baseAngle = orbitAngles[index];
    // Offset by current rotation to move selected shape to center (90 degrees = bottom)
    let angle = baseAngle - rotationOffset + 90;
    // Normalize to 0-360 for positioning
    angle = ((angle % 360) + 360) % 360;
    return angle;
  };

  return (
    <>
      <NavbarV4 logoOnly />
      <div className="event-choose-shape" data-navbar-theme="black">
        {/* Title */}
        <h1 className="event-choose-shape__title heading-3--no-margin">
          {currentShape.name}
        </h1>

        {/* Main Content Area */}
        <div className={`event-choose-shape__main ${isEntering ? 'event-choose-shape__main--entering' : ''}`}>
            {/* Center Diamond Display */}
            <div className="event-choose-shape__center">
              {/* Old shape - animating out */}
              {isAnimating && prevIndex !== null && (
                <div className="event-choose-shape__diamond event-choose-shape__diamond--exit">
                  {isIOS ? (
                    // iOS: Use GIF
                    <img
                      src={getGifUrl(shapes[prevIndex])}
                      alt={shapes[prevIndex].name}
                      className="event-choose-shape__diamond-img"
                    />
                  ) : isMacSafari ? (
                    // macOS Safari: Use WebP frames
                    <img
                      src={shapeFrames[shapes[prevIndex].id]?.[currentShapeFrame] || `/dmm-event/${shapeFrameFolders[shapes[prevIndex].id]}/frame_001.webp`}
                      alt={shapes[prevIndex].name}
                      className="event-choose-shape__diamond-img"
                    />
                  ) : (
                    // Other browsers: Use WebM video
                    <video
                      src={getVideoUrl(shapes[prevIndex])}
                      autoPlay
                      loop
                      muted
                      playsInline
                      className="event-choose-shape__diamond-img"
                    />
                  )}
                </div>
              )}
              {/* Current shape - animating in or static */}
              <div className={`event-choose-shape__diamond ${isAnimating ? 'event-choose-shape__diamond--enter' : ''}`}>
                <div style={{ transform: `scale(${currentShape.scale || 1})`, transition: 'transform 0.6s ease' }}>
                  {isIOS ? (
                    // iOS: Use GIF
                    <img
                      src={getGifUrl(currentShape)}
                      alt={currentShape.name}
                      className="event-choose-shape__diamond-img"
                    />
                  ) : isMacSafari ? (
                    // macOS Safari: Use WebP frames
                    <img
                      src={shapeFrames[currentShape.id]?.[currentShapeFrame] || `/dmm-event/${shapeFrameFolders[currentShape.id]}/frame_001.webp`}
                      alt={currentShape.name}
                      className="event-choose-shape__diamond-img"
                    />
                  ) : (
                    // Other browsers: Use WebM video
                    <video
                      src={getVideoUrl(currentShape)}
                      autoPlay
                      loop
                      muted
                      playsInline
                      className="event-choose-shape__diamond-img"
                    />
                  )}
                </div>
              </div>
            </div>
        </div>

        {/* Orbits Section */}
        <div className={`event-choose-shape__orbits-container ${isEntering ? 'event-choose-shape__orbits-container--entering' : ''}`}>
          {/* SVG Orbits */}
          <svg
            className="event-choose-shape__orbits-svg"
            viewBox="0 0 1700 280"
            preserveAspectRatio="xMidYMid meet"
          >
            <defs>
              {/* Radial gradients for orbit glow fill */}
              <radialGradient id="chooseOrbitGlow1" cx="0.5" cy="0.5" r="0.5">
                <stop offset="0%" stopColor="#FFFFFF"/>
                <stop offset="33.65%" stopColor="#FCE8EE" stopOpacity="0.6"/>
                <stop offset="54.33%" stopColor="#F6EDEF"/>
                <stop offset="93.27%" stopColor="#FFFFFF" stopOpacity="0.3"/>
                <stop offset="100%" stopColor="#FFFFFF"/>
              </radialGradient>
              <radialGradient id="chooseOrbitGlow2" cx="0.5" cy="0.5" r="0.5">
                <stop offset="0%" stopColor="#EFBCCA"/>
                <stop offset="54.33%" stopColor="#FFE3EA"/>
                <stop offset="82.21%" stopColor="#FFF7F9" stopOpacity="0.3"/>
                <stop offset="100%" stopColor="#FFFFFF"/>
              </radialGradient>
              <radialGradient id="chooseOrbitGlow3" cx="0.5" cy="0.5" r="0.5">
                <stop offset="77.88%" stopColor="#FFFDFE"/>
                <stop offset="87.5%" stopColor="#FFF5F8"/>
                <stop offset="96.63%" stopColor="#BC224C"/>
                <stop offset="100%" stopColor="#FFFFFF"/>
              </radialGradient>

              {/* Linear gradients for orbit stroke */}
              <linearGradient id="chooseOrbitStroke2" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#FFFFFF" stopOpacity="0.8"/>
                <stop offset="29.3%" stopColor="#FFD9E3" stopOpacity="0.2"/>
                <stop offset="71.6%" stopColor="#FFD9E3" stopOpacity="0.2"/>
                <stop offset="100%" stopColor="#FFFFFF" stopOpacity="0.8"/>
              </linearGradient>
              <linearGradient id="chooseOrbitStroke3" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#FFFFFF" stopOpacity="0.6"/>
                <stop offset="29.3%" stopColor="#FFD9E3" stopOpacity="0.1"/>
                <stop offset="71.6%" stopColor="#FFD9E3" stopOpacity="0.1"/>
                <stop offset="100%" stopColor="#FFFFFF" stopOpacity="0.6"/>
              </linearGradient>
            </defs>

            {/* 3 Orbits - from outermost to innermost */}
            <ellipse
              cx="850" cy="140"
              rx={orbitRadii[3].rx} ry={orbitRadii[3].ry}
              fill="url(#chooseOrbitGlow3)"
              stroke="url(#chooseOrbitStroke3)"
              strokeWidth="1.5"
              opacity="0.1"
            />
            <ellipse
              cx="850" cy="140"
              rx={orbitRadii[2].rx} ry={orbitRadii[2].ry}
              fill="url(#chooseOrbitGlow2)"
              stroke="url(#chooseOrbitStroke2)"
              strokeWidth="1.5"
              opacity="0.5"
            />
            <ellipse
              cx="850" cy="140"
              rx={orbitRadii[1].rx} ry={orbitRadii[1].ry}
              fill="url(#chooseOrbitGlow1)"
              stroke="none"
            />
          </svg>

          {/* Shapes on orbit - click to select */}
          <div className="event-choose-shape__orbit-shapes">
            {shapes.map((shape, index) => {
              const angle = getShapeAngle(index);
              const pos = getPositionOnOrbit(1, angle);
              const isSelected = index === currentIndex;
              // Show ripple on the shape to the right of selected (previous index visually)
              const rightShapeIndex = (currentIndex - 1 + shapes.length) % shapes.length;
              const showRippleOnThis = index === rightShapeIndex && showIdleRipple;

              return (
                <div
                  key={shape.id}
                  className={`event-choose-shape__orbit-shape ${isSelected ? 'event-choose-shape__orbit-shape--selected' : 'event-choose-shape__orbit-shape--blurred'}`}
                  style={{
                    left: `${pos.percentX}%`,
                    top: `${pos.percentY}%`,
                    transform: 'translate(-50%, -100%)'
                  }}
                  onClick={() => handleShapeClick(index)}
                >
                  <img
                    src={getMediaUrl(shape.image)}
                    alt={shape.name}
                    className="event-choose-shape__orbit-shape-img"
                    draggable="false"
                  />
                  {/* Idle ripple indicator - shows on shape next to selected after 3s */}
                  {showRippleOnThis && (
                    <div className="event-choose-shape__idle-ripple">
                      <span className="event-choose-shape__idle-ripple-ring"></span>
                      <span className="event-choose-shape__idle-ripple-ring"></span>
                      <span className="event-choose-shape__idle-ripple-ring"></span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Locked Message Toast */}
        {lockedMessage && (
          <div className="event-choose-shape__toast bodytext-4--no-margin">
            {lockedMessage}
          </div>
        )}

        {/* Bottom Section */}
        <div className="event-choose-shape__bottom">
          {/* Place your note - Left (Desktop only) */}
          <div className="event-choose-shape__note event-choose-shape__note--desktop">
            <h3 className="event-choose-shape__note-title heading-3--no-margin">Lựa chọn Nốt sáng</h3>
            <p className="event-choose-shape__note-text bodytext-6--no-margin">
              Hãy chọn hình dáng Nốt sáng phản chiếu cảm xúc và phong cách của bạn. Mỗi dáng hình là cách bạn để lại dấu ấn riêng trong bản nhạc Love-Grown.
            </p>
          </div>

          {/* Error Message */}
          {error && <p className="event-choose-shape__error">{error}</p>}

          {/* Choose the shape text */}
          <p className="event-choose-shape__select-text bodytext-6--no-margin">
            {isLocked ? 'Bạn đã chọn Nốt sáng' : 'Chọn Nốt sáng của riêng bạn'}
          </p>

          {/* Empty space - Right (for balance, Desktop only) */}
          <div className="event-choose-shape__spacer"></div>
        </div>

        {/* Back Button - Fixed bottom left */}
        <EventBackButton onClick={handleBack} />

        {/* Next Button - Fixed bottom right */}
        <EventNextButton onClick={handleNext} disabled={saving}>
          {saving ? 'Đang lưu...' : 'Tiếp tục'}
        </EventNextButton>
      </div>
    </>
  );
};

export default EventChooseShapePage;
