/**
 * usePreloadShapeFrames - Preload diamond shape frames for Safari/iOS
 *
 * WebM alpha transparency is not supported on Safari/iOS/in-app browsers,
 * so we need to use frame-by-frame WebP animation instead.
 * This hook preloads all frames in the background to ensure smooth animation
 * when the user reaches the EventChooseShapePage.
 */
import { useEffect, useRef } from 'react';
import useEventStore from '@/store/useEventStore';

// Frame animation config (must match EventChooseShapePage)
const SHAPE_FRAME_COUNT = 163;

// Frame folder mapping for each shape
const SHAPE_FRAME_FOLDERS = {
  h1: 'heart-frames',
  h2: 'oval-frames',
  h3: 'round-frames',
  h4: 'pear-frames',
  h5: 'asscher-frames',
  h6: 'emerald-frames',
  h7: 'marquise-frames',
};

// All shape IDs in order of priority (most popular first)
const SHAPE_IDS = ['h1', 'h3', 'h2', 'h4', 'h5', 'h6', 'h7'];

/**
 * Detect if the current browser needs frame animation fallback
 * (Safari, iOS, in-app browsers don't support WebM alpha)
 */
const needsFrameAnimation = () => {
  const ua = navigator.userAgent.toLowerCase();

  // Safari desktop
  const isSafariBrowser = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);

  // iOS (ALL browsers on iOS use WebKit which doesn't support WebM alpha)
  const isIOS = /ipad|iphone|ipod/.test(ua) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

  // In-app browsers that may not support WebM properly
  const isInAppBrowser = /zalo|fbav|fban|instagram|line|kakaotalk|wechat|micromessenger/.test(ua);

  return isSafariBrowser || isIOS || isInAppBrowser;
};

/**
 * Preload frames for a single shape
 * @param {string} shapeId - Shape ID (h1-h7)
 * @returns {Promise<string[]>} Array of frame URLs
 */
const preloadShapeFrames = async (shapeId) => {
  const frameFolder = SHAPE_FRAME_FOLDERS[shapeId];
  if (!frameFolder) return [];

  const loadedFrames = [];

  for (let i = 1; i <= SHAPE_FRAME_COUNT; i++) {
    const frameNum = String(i).padStart(3, '0');
    const src = `/dmm-event/${frameFolder}/frame_${frameNum}.webp`;

    await new Promise((resolve) => {
      const img = new Image();
      img.onload = resolve;
      img.onerror = resolve; // Continue even if one frame fails
      img.src = src;
    });
    loadedFrames.push(src);
  }

  return loadedFrames;
};

/**
 * Hook to preload all shape frames in the background
 * Call this on pages before EventChooseShapePage (e.g., EventLoginPage, EventNamePage)
 */
const usePreloadShapeFrames = () => {
  const {
    shapeFramesLoaded,
    isPreloadingFrames,
    setShapeFrames,
    setIsPreloadingFrames,
  } = useEventStore();

  const hasStartedRef = useRef(false);

  useEffect(() => {
    // Skip if not needed (Chrome/Firefox support WebM)
    if (!needsFrameAnimation()) return;

    // Skip if already started or completed
    if (hasStartedRef.current || isPreloadingFrames) return;

    // Check if all shapes already loaded
    const allLoaded = SHAPE_IDS.every((id) => shapeFramesLoaded[id]);
    if (allLoaded) return;

    hasStartedRef.current = true;
    setIsPreloadingFrames(true);

    // Preload shapes sequentially (to avoid overloading)
    const preloadAllShapes = async () => {
      for (const shapeId of SHAPE_IDS) {
        // Skip if already loaded
        if (shapeFramesLoaded[shapeId]) continue;

        try {
          const frames = await preloadShapeFrames(shapeId);
          setShapeFrames(shapeId, frames);
        } catch (error) {
          console.error(`Failed to preload frames for ${shapeId}:`, error);
        }
      }

      setIsPreloadingFrames(false);
    };

    // Start preloading after a short delay (let the page render first)
    const timeoutId = setTimeout(preloadAllShapes, 500);

    return () => clearTimeout(timeoutId);
  }, [shapeFramesLoaded, isPreloadingFrames, setShapeFrames, setIsPreloadingFrames]);
};

export default usePreloadShapeFrames;
export { needsFrameAnimation, SHAPE_FRAME_COUNT, SHAPE_FRAME_FOLDERS, SHAPE_IDS };
