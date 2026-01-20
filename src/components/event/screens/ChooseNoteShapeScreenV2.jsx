/**
 * ChooseNoteShapeScreenV2 - Step 3: Preview card with 3D flip effect
 * Simplified version based on working card-flip-test.html
 */
import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import useEventStore from '@/store/useEventStore';
import AvatarGenerator, { downloadAvatar } from '@/components/event/ui/AvatarGenerator';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';
import NavbarV4 from '@/components/navbar/NavbarV4';
import GlassThemeButton from '@/components/common/button/GlassThemeButton';

// Mapping from shape ID to diamondShape name
const SHAPE_NAME_MAP = {
  h1: 'heart', h2: 'oval', h3: 'round', h4: 'pear',
  h5: 'cushion', h6: 'emerald', h7: 'marquise',
  heart: 'heart', oval: 'oval', round: 'round', pear: 'pear',
  cushion: 'cushion', emerald: 'emerald', marquise: 'marquise',
};

const ChooseNoteShapeScreenV2 = () => {
  const navigate = useNavigate();
  const [downloading, setDownloading] = useState(false);
  const [isShowingFront, setIsShowingFront] = useState(false);
  const [entryComplete, setEntryComplete] = useState(false);
  const [isEntryAnimating, setIsEntryAnimating] = useState(true);

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
    setGeneratedAvatar,
  } = useEventStore();

  const currentShape = SHAPE_NAME_MAP[selectedDiamond] || 'heart';
  const [localAvatarUrl, setLocalAvatarUrl] = useState('');
  const avatarDataUrl = (generatedForShape === currentShape && generatedAvatarUrl)
    ? generatedAvatarUrl
    : localAvatarUrl;
  const shouldGenerateAvatar = !avatarDataUrl;

  const handleAvatarGenerated = (dataUrl) => {
    setLocalAvatarUrl(dataUrl);
    setGeneratedAvatar(dataUrl, currentShape);
  };

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

  // Entry animation complete
  useEffect(() => {
    const timer = setTimeout(() => {
      setIsEntryAnimating(false); // Remove animation class so JS can control transform
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
    downloadAvatar(avatarDataUrl, filename);
    setTimeout(() => setDownloading(false), 500);
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
          text: 'My avatar from Mirror Diamond',
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
      <div className="your-wallpaper your-wallpaper-v2">
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
            width: 250px;
            height: 446px;
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
            border-radius: 16px;
            display: flex;
            justify-content: center;
            align-items: center;
            overflow: hidden;
            box-shadow:
              0 20px 60px rgba(0, 0, 0, 0.4),
              0 8px 25px rgba(0, 0, 0, 0.3);
          }

          .your-wallpaper-v2__card-front {
            background: #fff;
          }

          .your-wallpaper-v2__card-back {
            background: #fff;
            transform: rotateY(180deg);
          }

          .your-wallpaper-v2__card-image {
            width: 100%;
            height: 100%;
            object-fit: cover;
            border-radius: 14px;
          }

          @media (max-width: 1024px) {
            .your-wallpaper-v2__card {
              width: 280px;
              height: 500px;
            }
          }

          @media (max-width: 480px) {
            .your-wallpaper-v2__card {
              width: 220px;
              height: 392px;
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
            Từ đây, MIRROR đã phát triển avatar phản chiếu Nốt sáng, giai điệu và dấu ấn cá nhân của bạn.
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
                    ) : (
                      <div style={{ color: '#999' }}>Loading...</div>
                    )}
                  </div>

                  {/* Back face - Card back */}
                  <div className="your-wallpaper-v2__card-face your-wallpaper-v2__card-back">
                    <img src={getMediaUrl('dmm/card_back.png')} alt="Card Back" className="your-wallpaper-v2__card-image" />
                  </div>
                </div>
              </div>
            </div>

            {/* Hint */}
            <p className="your-wallpaper__hint bodytext-6--no-margin">
              {isShowingFront ? 'Giữ và kéo để xoay avatar của bạn' : 'Chạm để khám phá avatar của bạn'}
            </p>
          </div>
        </main>

        {/* Footer Actions */}
        <div className="your-wallpaper__footer-actions">
          <GlassThemeButton
            theme="light"
            onClick={handleDownload}
            className={downloading || !avatarDataUrl ? 'disabled' : ''}
          >
            <svg width="15" height="16" viewBox="0 0 15 16" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M0.5 14.9856C0.854118 15.315 1.33441 15.5 1.83521 15.5H13.1648C13.6656 15.5 14.1459 15.315 14.5 14.9856M7.50105 0.5V10.4521M7.50105 10.4521L11.8171 6.64941M7.50105 10.4521L3.18502 6.64941" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            {downloading ? 'Đang tải...' : 'Tải xuống'}
          </GlassThemeButton>
          <GlassThemeButton
            theme="light"
            onClick={handleShare}
            icon={<img src={getMediaUrl('dmm/icons/share-icon.svg')} alt="Share" width={18} height={18} />}
          />
        </div>

        {/* Navigation arrows */}
        <div className="your-wallpaper__arrow your-wallpaper__arrow--left">
          <GlassThemeButton theme="light" onClick={handleGoBack} icon={
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="12" viewBox="0 0 14 12" fill="none">
              <path d="M12.7187 5.70703L0.71875 5.70703M0.71875 5.70703L5.86161 0.707031M0.71875 5.70703L5.86161 10.707" stroke="currentColor" strokeLinecap="square"/>
            </svg>
          } />
        </div>
        <div className="your-wallpaper__arrow your-wallpaper__arrow--right">
          <GlassThemeButton theme="light" onClick={() => navigate(ROUTES.EVENT_THANKYOU)} icon={
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="12" viewBox="0 0 14 12" fill="none">
              <path d="M0.5 5.70703L12.5 5.70703M12.5 5.70703L7.35714 10.707M12.5 5.70703L7.35714 0.707031" stroke="currentColor" strokeLinecap="square"/>
            </svg>
          } />
        </div>

        {/* Hidden Avatar Generator */}
        {shouldGenerateAvatar && (
          <AvatarGenerator
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

export default ChooseNoteShapeScreenV2;
