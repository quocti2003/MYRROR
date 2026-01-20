/**
 * ThankYouScreen - Final thank you screen after card generation
 * Tablet layout for /the-muse-of-love-grown/mirror-thankyou
 */
import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import NavbarV4 from '@/components/navbar/NavbarV4';
import GlassThemeButton from '@/components/common/button/GlassThemeButton';
import EventBackButton from '@/components/event/EventBackButton';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';

// Frame animation config for Safari (WebM alpha not supported)
const RING_FRAME_COUNT = 90;
const RING_FPS = 20; // Smooth animation

const ThankYouScreen = () => {
  const navigate = useNavigate();

  // Safari detection and frame animation states
  const [isSafari, setIsSafari] = useState(false);
  const [ringFrames, setRingFrames] = useState([]);
  const [currentRingFrame, setCurrentRingFrame] = useState(0);
  const [isRingLoaded, setIsRingLoaded] = useState(false);
  const ringAnimationRef = useRef(null);
  const lastRingFrameTimeRef = useRef(0);

  // Detect browsers that don't support WebM alpha (need frame animation fallback)
  // Includes: Safari, all iOS browsers (WebKit), in-app browsers (Zalo, Facebook, etc.)
  useEffect(() => {
    const ua = navigator.userAgent.toLowerCase();

    // Check for Safari desktop
    const isSafariBrowser = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);

    // Check for iOS (ALL browsers on iOS use WebKit which doesn't support WebM alpha)
    const isIOS = /ipad|iphone|ipod/.test(ua) ||
      (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

    // Check for in-app browsers that may not support WebM properly
    const isInAppBrowser = /zalo|fbav|fban|instagram|line|kakaotalk|wechat|micromessenger/.test(ua);

    // Use frame animation for Safari, iOS, or any in-app browser
    const needsFrameAnimation = isSafariBrowser || isIOS || isInAppBrowser;

    setIsSafari(needsFrameAnimation);
  }, []);

  // Preload WebP frames for Safari
  useEffect(() => {
    if (!isSafari) return;

    const loadFrames = async () => {
      const loadedFrames = [];
      for (let i = 1; i <= RING_FRAME_COUNT; i++) {
        const frameNum = String(i).padStart(3, '0');
        const src = `/dmm-event/ring-webp-frames/frame_${frameNum}.webp`;
        loadedFrames.push(src);

        // Preload image
        const img = new Image();
        img.src = src;
      }
      setRingFrames(loadedFrames);
      setIsRingLoaded(true);
    };

    loadFrames();
  }, [isSafari]);

  // Animation loop for Safari
  useEffect(() => {
    if (!isSafari || !isRingLoaded || ringFrames.length === 0) return;

    const frameInterval = 1000 / RING_FPS;

    const animate = (timestamp) => {
      if (timestamp - lastRingFrameTimeRef.current >= frameInterval) {
        setCurrentRingFrame(prev => (prev + 1) % ringFrames.length);
        lastRingFrameTimeRef.current = timestamp;
      }
      ringAnimationRef.current = requestAnimationFrame(animate);
    };

    ringAnimationRef.current = requestAnimationFrame(animate);

    return () => {
      if (ringAnimationRef.current) {
        cancelAnimationFrame(ringAnimationRef.current);
      }
    };
  }, [isSafari, isRingLoaded, ringFrames.length]);

  const handleGoBack = () => {
    navigate(ROUTES.EVENT_CHOOSE_NOTE);
  };

  const handleBuyTicket = () => {
    // External link to buy ticket (same as lucky draw in EventGuidePage)
    window.open('https://ticketbox.vn/doc-mong-mo-her-concert-25556?utm_medium=sr-her+concert_all-dates_all-prices&utm_source=tkb-search-results', '_blank');
  };

  const handleTryAR = () => {
    window.open(ROUTES.EVENT_RING_VIEWER, '_blank');
  };

  return (
    <>
      <NavbarV4 logoOnly />
      <div className="mirror-thankyou" data-navbar-theme="white">
        {/* Background */}
        <div className="mirror-thankyou__bg" />

        {/* Left Side - Social Icons */}
        <div className="mirror-thankyou__left">
          <div className="mirror-thankyou__social">
            <GlassThemeButton
              theme="event_light"
              onClick={() => window.open('https://www.instagram.com/mirrorfuturediamond/', '_blank')}
              icon={
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z" fill="currentColor"/>
                </svg>
              }
            />
            <GlassThemeButton
              theme="event_light"
              onClick={() => window.open('https://www.facebook.com/mirrorfuturediamond', '_blank')}
              icon={
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" fill="currentColor"/>
                </svg>
              }
            />
            <GlassThemeButton
              theme="event_light"
              onClick={() => window.open('https://www.tiktok.com/@mirrorfuturediamond?lang=vi-VN', '_blank')}
              icon={
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M19.59 6.69a4.83 4.83 0 01-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 01-5.2 1.74 2.89 2.89 0 012.31-4.64 2.93 2.93 0 01.88.13V9.4a6.84 6.84 0 00-1-.05A6.33 6.33 0 005 20.1a6.34 6.34 0 0010.86-4.43v-7a8.16 8.16 0 004.77 1.52v-3.4a4.85 4.85 0 01-1-.1z" fill="currentColor"/>
                </svg>
              }
            />
          </div>
        </div>

        {/* Back Button */}
        <EventBackButton onClick={handleGoBack} theme="event_light" />

        {/* Right Side - Navigation Icons */}
        <div className="mirror-thankyou__right">
          <GlassThemeButton
            theme="event_light"
            onClick={() => navigate(ROUTES.EVENT_PLACE_NOTE)}
            icon={
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M11.5 7.71875C10.7598 9.91458 8.64001 11.5 6.14044 11.5C3.02531 11.5 0.5 9.03757 0.5 6C0.5 2.96243 3.02531 0.5 6.14044 0.5C8.2282 0.5 10.051 1.60605 11.0263 3.25M8.53763 3.9375H11.3578V1.1875" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            }
          />
          <GlassThemeButton
            theme="event_light"
            onClick={() => {/* TODO: Share functionality */}}
            icon={
              <svg width="18" height="14" viewBox="0 0 15 11" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M14.5 5.10798L7.03341 0.5L7.03341 3.1087C0.5 4.41304 0.5 10.5 0.5 10.5C0.5 10.5 3.29997 7.02174 7.03341 7.45652L7.03341 10.1522L14.5 5.10798Z" stroke="currentColor" strokeLinejoin="round"/>
              </svg>
            }
          />
          <GlassThemeButton
            theme="event_light"
            onClick={() => navigate(ROUTES.EVENT_GUIDE)}
            icon={
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M6 14V9.43522C6 9.01504 6.35817 8.67442 6.8 8.67442H9.2C9.64183 8.67442 10 9.01504 10 9.43522V14M7.53638 2.14078L2.33638 5.65735C2.12534 5.80007 2 6.0311 2 6.27737V12.8588C2 13.4891 2.53726 14 3.2 14H12.8C13.4627 14 14 13.4891 14 12.8588V6.27737C14 6.0311 13.8747 5.80007 13.6636 5.65735L8.46362 2.14078C8.18605 1.95307 7.81395 1.95307 7.53638 2.14078Z" stroke="currentColor" strokeLinecap="round"/>
              </svg>
            }
          />
        </div>

        {/* Main Content */}
        <main className="mirror-thankyou__main">
          {/* Title */}
          <h1 className="mirror-thankyou__title">
            Cảm ơn bạn đã đồng hành cùng The Muse of Love-Grown
          </h1>

          {/* Description */}
          <p className="mirror-thankyou__description bodytext-6--no-margin">
            Mỗi tấm vé tham dự HER CONCERT là một cơ hội nhận được chiếc nhẫn kim cương từ MIRROR.
          </p>

          {/* Action Buttons */}
          <div className="mirror-thankyou__actions">
            <GlassThemeButton theme="event_spec" onClick={handleBuyTicket}>
              Mua vé tham dự
            </GlassThemeButton>
            <GlassThemeButton theme="event_light" onClick={handleTryAR}>
              Xem nhẫn 3D
            </GlassThemeButton>
          </div>
        </main>

        {/* Lucky Ring Video */}
        <div className="mirror-thankyou__lucky-ring">
          {isSafari ? (
            // WebP frame animation for Safari (WebM alpha not supported)
            isRingLoaded && ringFrames.length > 0 && (
              <img
                src={ringFrames[currentRingFrame]}
                alt="Lucky Ring"
                className="mirror-thankyou__lucky-ring-img"
              />
            )
          ) : (
            // WebM video for other browsers
            <video
              src={getMediaUrl('mirror_DMM/nhan-lucky-draw.webm')}
              autoPlay
              loop
              muted
              playsInline
              className="mirror-thankyou__lucky-ring-img"
              onLoadedMetadata={(e) => { e.target.playbackRate = 0.15; }}
            />
          )}
        </div>
      </div>
    </>
  );
};

export default ThankYouScreen;
