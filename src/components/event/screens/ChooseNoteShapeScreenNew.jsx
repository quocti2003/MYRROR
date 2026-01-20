/**
 * ChooseNoteShapeScreenNew - Step 3: Preview card with 3D flip effect
 * Simplified version based on working card-flip-test.html (Safari compatible)
 */
import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import useEventStore from '@/store/useEventStore';
import AvatarGenerator, { downloadAvatar } from '@/components/event/ui/AvatarGenerator';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';
import NavbarV4 from '@/components/navbar/NavbarV4';
import GlassThemeButton from '@/components/common/button/GlassThemeButton';
import EventBackButton from '@/components/event/EventBackButton';
import EventNextButton from '@/components/event/EventNextButton';
import EdgeNextButton from '@/components/event/EdgeNextButton';

// Mapping from shape ID (h1, h2...) to diamondShape name for AvatarGenerator
// Must match keys in AvatarGenerator's DIAMONDS object:
// heart, round, emerald, marquise, oval, pear, asscher
const SHAPE_NAME_MAP = {
  // Shape IDs
  h1: 'heart',
  h2: 'oval',
  h3: 'round',
  h4: 'pear',
  h5: 'asscher',
  h6: 'emerald',
  h7: 'marquise',
  // Legacy names (passthrough)
  heart: 'heart',
  oval: 'oval',
  round: 'round',
  pear: 'pear',
  asscher: 'asscher',
  emerald: 'emerald',
  marquise: 'marquise',
};

// Card back image URL (precomputed to avoid issues)
const CARD_BACK_TITLE_URL = getMediaUrl('dmm/card_back_title.png');

const ChooseNoteShapeScreenNew = () => {
  const navigate = useNavigate();
  const [downloading, setDownloading] = useState(false);
  const [isShowingFront, setIsShowingFront] = useState(false);
  const [entryComplete, setEntryComplete] = useState(false);
  const [isEntryAnimating, setIsEntryAnimating] = useState(true);
  const [cardBackLoaded, setCardBackLoaded] = useState(false);

  const cardRef = useRef(null);
  const cardContainerRef = useRef(null);

  // Drag state
  const isDragging = useRef(false);
  const currentRotation = useRef(180);
  const velocity = useRef(0);
  const targetRotation = useRef(180);
  const lastX = useRef(0);
  const dragHistory = useRef([]);
  const mouseDownTime = useRef(0);
  const mouseDownX = useRef(0);

  // Physics
  const friction = 0.92;
  const snapStrength = 0.15;

  const {
    user,
    selectedDiamond,
    generatedAvatarUrl,
    generatedForShape,
    generatedForName,
    setGeneratedAvatar,
  } = useEventStore();

  const currentShape = SHAPE_NAME_MAP[selectedDiamond] || 'heart';
  const currentName = user?.displayName || 'Guest';
  const [localAvatarUrl, setLocalAvatarUrl] = useState('');
  const [generationFailed, setGenerationFailed] = useState(false);
  const [retryCount, setRetryCount] = useState(0);

  // Check if cached avatar matches both current shape AND current name
  const isCachedAvatarValid = generatedForShape === currentShape &&
                              generatedForName === currentName &&
                              generatedAvatarUrl;
  const avatarDataUrl = isCachedAvatarValid ? generatedAvatarUrl : localAvatarUrl;
  const shouldGenerateAvatar = !avatarDataUrl && !generationFailed;

  const handleAvatarGenerated = (dataUrl) => {
    if (dataUrl) {
      setLocalAvatarUrl(dataUrl);
      // Save with both shape and name for validation on reload
      setGeneratedAvatar(dataUrl, currentShape, currentName);
      setGenerationFailed(false);
    } else {
      // Generation failed
      setGenerationFailed(true);
    }
  };

  // Retry generation
  const handleRetryGeneration = () => {
    setGenerationFailed(false);
    setLocalAvatarUrl('');
    setRetryCount(prev => prev + 1);
  };

  // Timeout fallback - if generation takes too long (common on iOS), show retry
  // Only run when retryCount changes (new generation attempt)
  useEffect(() => {
    // Skip if we already have avatar or generation already failed
    if (avatarDataUrl || generationFailed) return;

    const timeout = setTimeout(() => {
      // Check state at timeout time
      setGenerationFailed((prev) => {
        if (!prev) {
          console.warn('Avatar generation timeout - showing retry button');
          return true;
        }
        return prev;
      });
    }, 15000); // 15 seconds timeout

    return () => clearTimeout(timeout);
  }, [retryCount]); // Only depend on retryCount to avoid loops

  // Update card rotation
  const updateCard = () => {
    if (cardRef.current) {
      cardRef.current.style.transform = `rotateY(${currentRotation.current}deg)`;
    }
    // Update showing front state
    const normalized = ((currentRotation.current % 360) + 360) % 360;
    const nowShowingFront = normalized < 90 || normalized > 270;
    if (nowShowingFront !== isShowingFront) {
      setIsShowingFront(nowShowingFront);
    }
  };

  // Find nearest snap angle (0 or 180)
  const findNearestSnapAngle = (angle) => Math.round(angle / 180) * 180;

  // Calculate release velocity
  const calculateReleaseVelocity = () => {
    if (dragHistory.current.length < 2) return 0;
    const recent = dragHistory.current.slice(-5);
    let totalDelta = 0;
    let totalTime = 0;
    for (let i = 1; i < recent.length; i++) {
      totalDelta += recent[i].x - recent[i - 1].x;
      totalTime += recent[i].time - recent[i - 1].time;
    }
    if (totalTime === 0) return 0;
    return (totalDelta / totalTime) * 16;
  };

  // Animation loop
  useEffect(() => {
    let animationId;
    const animate = () => {
      if (!isDragging.current && entryComplete) {
        if (Math.abs(velocity.current) > 0.1) {
          velocity.current *= friction;
          currentRotation.current += velocity.current;
          const remainingDistance = velocity.current / (1 - friction);
          const predictedEnd = currentRotation.current + remainingDistance;
          targetRotation.current = findNearestSnapAngle(predictedEnd);
        } else {
          velocity.current = 0;
          const diff = targetRotation.current - currentRotation.current;
          if (Math.abs(diff) > 0.5) {
            currentRotation.current += diff * snapStrength;
          } else {
            currentRotation.current = targetRotation.current;
          }
        }
        updateCard();
      }
      animationId = requestAnimationFrame(animate);
    };
    animate();
    return () => cancelAnimationFrame(animationId);
  }, [entryComplete]);

  // Preload card back title image
  useEffect(() => {
    const img = new Image();
    img.onload = () => setCardBackLoaded(true);
    img.onerror = () => {
      // Retry once on error
      setTimeout(() => {
        const retryImg = new Image();
        retryImg.onload = () => setCardBackLoaded(true);
        retryImg.src = `${CARD_BACK_TITLE_URL}?t=${Date.now()}`;
      }, 500);
    };
    img.src = CARD_BACK_TITLE_URL;
  }, []);

  // Entry animation complete
  useEffect(() => {
    const timer = setTimeout(() => {
      setIsEntryAnimating(false);
      setEntryComplete(true);
      currentRotation.current = 180;
      targetRotation.current = 180;
      if (cardRef.current) {
        cardRef.current.style.transform = `rotateY(180deg)`;
      }
    }, 2000);
    return () => clearTimeout(timer);
  }, []);

  // Drag handlers
  const startDrag = (clientX) => {
    if (!entryComplete) return;
    isDragging.current = true;
    lastX.current = clientX;
    dragHistory.current = [{ x: clientX, time: performance.now() }];
    mouseDownTime.current = Date.now();
    mouseDownX.current = clientX;
  };

  const moveDrag = (clientX) => {
    if (!isDragging.current) return;
    const deltaX = clientX - lastX.current;
    currentRotation.current += deltaX * 0.5;
    lastX.current = clientX;
    dragHistory.current.push({ x: clientX, time: performance.now() });
    if (dragHistory.current.length > 10) dragHistory.current.shift();
    updateCard();
  };

  const endDrag = (clientX) => {
    if (!isDragging.current) return;
    isDragging.current = false;

    velocity.current = calculateReleaseVelocity() * 0.5;
    const remainingDistance = velocity.current / (1 - friction);
    const predictedEnd = currentRotation.current + remainingDistance;
    targetRotation.current = findNearestSnapAngle(predictedEnd);

    // Check for click
    const clickDistance = Math.abs(clientX - mouseDownX.current);
    const clickDuration = Date.now() - mouseDownTime.current;
    if (clickDistance < 10 && clickDuration < 300) {
      const normalized = ((currentRotation.current % 360) + 360) % 360;
      targetRotation.current = normalized < 90 || normalized > 270 ? 180 : 0;
    }
  };

  // Mouse events
  const handleMouseDown = (e) => {
    e.preventDefault();
    startDrag(e.clientX);
  };

  useEffect(() => {
    const handleMouseMove = (e) => moveDrag(e.clientX);
    const handleMouseUp = (e) => endDrag(e.clientX);
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, []);

  // Touch events
  const handleTouchStart = (e) => {
    e.preventDefault();
    startDrag(e.touches[0].clientX);
  };

  useEffect(() => {
    const handleTouchMove = (e) => {
      if (isDragging.current && e.touches.length > 0) {
        moveDrag(e.touches[0].clientX);
      }
    };
    const handleTouchEnd = (e) => {
      const touch = e.changedTouches[0];
      endDrag(touch ? touch.clientX : lastX.current);
    };
    document.addEventListener('touchmove', handleTouchMove);
    document.addEventListener('touchend', handleTouchEnd);
    return () => {
      document.removeEventListener('touchmove', handleTouchMove);
      document.removeEventListener('touchend', handleTouchEnd);
    };
  }, []);

  // Navigation
  const handleGoBack = () => navigate(ROUTES.EVENT_WRITE_MESSAGE);

  // Download
  const handleDownload = async () => {
    if (!avatarDataUrl) return;
    setDownloading(true);
    const filename = `mirrorthankyou_${user?.displayName || 'guest'}_${Date.now()}.png`;
    await downloadAvatar(avatarDataUrl, filename);
    setDownloading(false);
  };

  // Share
  const handleShare = async () => {
    if (!avatarDataUrl) return;
    try {
      const response = await fetch(avatarDataUrl);
      const blob = await response.blob();
      const file = new File([blob], `mirrorthankyou_${user?.displayName || 'guest'}.png`, { type: 'image/png' });
      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        await navigator.share({
          files: [file],
          title: 'Mirror Diamond',
          text: 'Nốt sáng của tôi từ Mirror Diamond ✨',
        });
      } else {
        downloadAvatar(avatarDataUrl, `mirrorthankyou_${user?.displayName || 'guest'}.png`);
      }
    } catch (err) {
      console.log('Share cancelled or failed:', err);
    }
  };

  return (
    <>
      <NavbarV4 logoOnly />
      <div className="your-wallpaper your-wallpaper-v2" data-navbar-theme="black">
        <style>{`
          .your-wallpaper-v2 {
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            position: relative;
            overflow: hidden;
          }

          .your-wallpaper-v2__bg {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background:
              radial-gradient(121.12% 87.58% at 50% 50%, #FF87A8 0%, rgba(255, 255, 255, 0.50) 56.25%, rgba(246, 246, 246, 0.00) 100%),
              linear-gradient(180deg, #FFF6F6 0%, #FFDCE5 12.53%, #FFEFF6 25.06%, #FFF8F8 37.53%, rgba(255, 241, 241, 0.91) 50%, #FFE6EC 63.82%, #FFEDFE 77.64%, #F6F6F6 100%);
            z-index: 0;
          }

          .your-wallpaper-v2__ripple-container {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 100%;
            height: 100%;
            pointer-events: none;
            z-index: 1;
          }

          .your-wallpaper-v2__ripple-ring {
            position: absolute;
            left: 50%;
            top: 50%;
            border-radius: 50%;
            transform: translate(-50%, -50%);
            pointer-events: none;
            opacity: 0.7;
            background: radial-gradient(50% 50% at 50% 50%,
              #F4A5B8 0%,
              rgba(185, 185, 185, 0.00) 54.33%,
              #FFF 93.27%,
              rgba(255, 255, 255, 0.00) 100%
            );
            animation: ripplePulseV2 10s ease-out infinite;
          }

          .your-wallpaper-v2__ripple-ring:nth-child(1) { animation-delay: 1s; }
          .your-wallpaper-v2__ripple-ring:nth-child(2) { animation-delay: 3s; }
          .your-wallpaper-v2__ripple-ring:nth-child(3) { animation-delay: 5s; }
          .your-wallpaper-v2__ripple-ring:nth-child(4) { animation-delay: 7s; }
          .your-wallpaper-v2__ripple-ring:nth-child(5) { animation-delay: 9s; }

          @keyframes ripplePulseV2 {
            0% { width: 100px; height: 100px; opacity: 0; }
            10% { opacity: 0.7; }
            50% { opacity: 0.5; }
            80% { opacity: 0.25; }
            100% { width: 1200px; height: 1200px; opacity: 0; }
          }

          .your-wallpaper-v2__scene {
            perspective: 1000px;
            perspective-origin: 65% 35%;
            z-index: 2;
          }

          .your-wallpaper-v2__card-container {
            transform-style: preserve-3d;
            transform: rotateX(12deg) rotateY(-8deg);
            cursor: grab;
            animation: cardEntryV2 2s cubic-bezier(0.16, 1, 0.3, 1) forwards;
          }

          @keyframes cardEntryV2 {
            0% {
              transform: translateZ(2000px) scale(4) rotateX(12deg) rotateY(-8deg);
              opacity: 0;
            }
            20% { opacity: 0.6; }
            100% {
              transform: rotateX(12deg) rotateY(-8deg);
              opacity: 1;
            }
          }

          .your-wallpaper-v2__card-container.dragging {
            cursor: grabbing;
          }

          .your-wallpaper-v2__card {
            width: min(420px, calc((100vh - 350px) * 420 / 714));
            aspect-ratio: 420 / 714;
            position: relative;
            transform-style: preserve-3d;
          }

          .your-wallpaper-v2__card.entry-animating {
            animation: entryFlipV2 2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
          }

          @keyframes entryFlipV2 {
            0% { transform: rotateY(0deg); }
            100% { transform: rotateY(180deg); }
          }

          .your-wallpaper-v2__card-face {
            position: absolute;
            width: 100%;
            height: 100%;
            backface-visibility: hidden;
            -webkit-backface-visibility: hidden;
            border-radius: 16px;
            display: flex;
            justify-content: center;
            align-items: center;
            overflow: hidden;
            box-shadow: 0 4px 8px 0 rgba(188, 34, 76, 0.25);
          }

          .your-wallpaper-v2__card-front {
            background: #fff;
            transform: translateZ(1px);
          }

          .your-wallpaper-v2__card-back {
            background: linear-gradient(180deg, #4C0F0F 17.79%, #BC224C 55.29%, #F6F6F6 100%);
            transform: rotateY(180deg);
          }

          .your-wallpaper-v2__card-back-title {
            width: 60%;
            height: auto;
            object-fit: contain;
          }

          .your-wallpaper-v2__card-image {
            width: 100%;
            height: 100%;
            object-fit: cover;
            border-radius: 14px;
          }

          @media (max-width: 1024px) {
            .your-wallpaper__card-container {
              margin-top: -50px;
            }
            .your-wallpaper-v2__card {
              width: min(350px, calc((100vh - 350px) * 350 / 600));
              aspect-ratio: 350 / 600;
            }
          }

          @media (max-width: 480px) {
            .your-wallpaper__card-container {
              margin: -40px auto 0;
            }
            .your-wallpaper-v2__card {
              width: min(250px, calc((100vh - 350px) * 241 / 427));
              aspect-ratio: 241 / 427;
            }
          }
        `}</style>

        <div className="your-wallpaper-v2__bg" />

        {/* Ripples */}
        <div className="your-wallpaper-v2__ripple-container">
          <div className="your-wallpaper-v2__ripple-ring" />
          <div className="your-wallpaper-v2__ripple-ring" />
          <div className="your-wallpaper-v2__ripple-ring" />
          <div className="your-wallpaper-v2__ripple-ring" />
          <div className="your-wallpaper-v2__ripple-ring" />
        </div>

        {/* Header */}
        <header className="your-wallpaper__header">
          <h2 className="heading-2--no-margin your-wallpaper__title">Khi giai điệu thành hình</h2>
          <p className="bodytext-6--no-margin your-wallpaper__description">
            Từ đây, MIRROR đã phát triển avatar phản chiếu Nốt Sáng, giai điệu và dấu ấn cá nhân của bạn.
            Bạn có thể tải và chia sẻ khoảnh khắc hiện diện này.
          </p>
        </header>

        {/* Main content - Card preview */}
        <main className="your-wallpaper__main">
          <div className="your-wallpaper__card-container">
            {/* Card */}
            <div className="your-wallpaper-v2__scene">
              <div
                className="your-wallpaper-v2__card-container"
                ref={cardContainerRef}
                onMouseDown={handleMouseDown}
                onTouchStart={handleTouchStart}
              >
                <div className={`your-wallpaper-v2__card ${isEntryAnimating ? 'entry-animating' : ''}`} ref={cardRef}>
                  {/* Front face - Avatar */}
                  <div className="your-wallpaper-v2__card-face your-wallpaper-v2__card-front">
                    {avatarDataUrl ? (
                      <img src={avatarDataUrl} alt="Generated Card" className="your-wallpaper-v2__card-image" />
                    ) : generationFailed ? (
                      <div style={{
                        color: '#BC224C',
                        textAlign: 'center',
                        padding: '20px',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        gap: '12px'
                      }}>
                        <p style={{ margin: 0, fontSize: '14px' }}>Không thể tạo avatar</p>
                        <button
                          onClick={handleRetryGeneration}
                          style={{
                            background: '#BC224C',
                            color: '#fff',
                            border: 'none',
                            padding: '8px 16px',
                            borderRadius: '20px',
                            cursor: 'pointer',
                            fontSize: '14px'
                          }}
                        >
                          Thử lại
                        </button>
                      </div>
                    ) : (
                      <div style={{ color: '#999' }}>Đang tạo...</div>
                    )}
                  </div>

                  {/* Back face - Card back */}
                  <div className="your-wallpaper-v2__card-face your-wallpaper-v2__card-back">
                    <img src={CARD_BACK_TITLE_URL} alt="Card Back Title" className="your-wallpaper-v2__card-back-title" />
                  </div>
                </div>
              </div>
            </div>

            {/* Hint */}
            <p className="your-wallpaper__hint bodytext-6--no-margin">
              {isShowingFront ? 'Giữ và kéo để xoay avatar của bạn' : 'Chạm để khám phá avatar của bạn'}
            </p>

            {/* Action buttons below card - Download + Share (desktop) */}
            <div className="your-wallpaper__actions">
              <GlassThemeButton
                theme="event_spec"
                onClick={handleDownload}
                className={downloading || !avatarDataUrl ? 'disabled' : ''}
              >
                <svg width="15" height="16" viewBox="0 0 15 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M0.5 14.9856C0.854118 15.315 1.33441 15.5 1.83521 15.5H13.1648C13.6656 15.5 14.1459 15.315 14.5 14.9856M7.50105 0.5V10.4521M7.50105 10.4521L11.8171 6.64941M7.50105 10.4521L3.18502 6.64941" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                {downloading ? 'Đang tải...' : 'Tải xuống'}
              </GlassThemeButton>
              <GlassThemeButton
                theme="event_dark"
                onClick={handleShare}
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="11" viewBox="0 0 15 11" fill="none">
                  <path d="M14.5 5.10798L7.03341 0.5L7.03341 3.1087C0.5 4.41304 0.5 10.5 0.5 10.5C0.5 10.5 3.29997 7.02174 7.03341 7.45652L7.03341 10.1522L14.5 5.10798Z" stroke="currentColor" strokeLinejoin="round"/>
                </svg>
                Chia sẻ
              </GlassThemeButton>
            </div>
          </div>
        </main>

        {/* Back and Next buttons */}
        <EventBackButton onClick={handleGoBack} />
        <EventNextButton onClick={() => navigate(ROUTES.EVENT_THANKYOU)} />

        <EdgeNextButton
          onClick={() => navigate(ROUTES.EVENT_THANKYOU)}
          title="Khi giai điệu thành hình"
          description="Từ đây, MIRROR đã phát triển avatar phản chiếu Nốt Sáng, giai điệu và dấu ấn cá nhân của bạn. Bạn có thể tải và chia sẻ khoảnh khắc hiện diện này."
        />

        {/* Footer - actions on tablet/mobile */}
        <footer className="your-wallpaper__footer">
          <div className="your-wallpaper__footer-actions">
            <GlassThemeButton
              theme="event_spec"
              onClick={handleDownload}
              className={downloading || !avatarDataUrl ? 'disabled' : ''}
            >
              <svg width="15" height="16" viewBox="0 0 15 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M0.5 14.9856C0.854118 15.315 1.33441 15.5 1.83521 15.5H13.1648C13.6656 15.5 14.1459 15.315 14.5 14.9856M7.50105 0.5V10.4521M7.50105 10.4521L11.8171 6.64941M7.50105 10.4521L3.18502 6.64941" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              {downloading ? 'Đang tải...' : 'Tải xuống'}
            </GlassThemeButton>
            <GlassThemeButton
              theme="event_dark"
              onClick={handleShare}
              className="share-btn-circle"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="15" height="11" viewBox="0 0 15 11" fill="none">
                <path d="M14.5 5.10798L7.03341 0.5L7.03341 3.1087C0.5 4.41304 0.5 10.5 0.5 10.5C0.5 10.5 3.29997 7.02174 7.03341 7.45652L7.03341 10.1522L14.5 5.10798Z" stroke="currentColor" strokeLinejoin="round"/>
              </svg>
              <span className="share-btn-text">Chia sẻ</span>
            </GlassThemeButton>
          </div>
        </footer>

        {/* Hidden Avatar Generator */}
        {shouldGenerateAvatar && (
          <AvatarGenerator
            key={`avatar-gen-${retryCount}`}
            displayName={user?.displayName || 'Guest'}
            lightNumber={user?.lightNumber || 1}
            diamondShape={currentShape}
            onGenerated={handleAvatarGenerated}
            delay={500}
          />
        )}
      </div>
    </>
  );
};

export default ChooseNoteShapeScreenNew;
