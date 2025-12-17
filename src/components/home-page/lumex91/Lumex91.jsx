import "./Lumex91.css";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import ShinyText from "@components/common/shiny-text/ShinyText";
import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { getNewsDetailRoute } from "@/constants/routes";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";

const TOTAL_FRAMES = 456;
const FRAME_PATH = "/home-page/lumex91-ani-frames/frame_";

const Lumex91 = ({ externalProgress = null }) => {
  const [framesLoaded, setFramesLoaded] = useState(false);
  const [shouldLoadFrames, setShouldLoadFrames] = useState(false);
  const [currentFrame, setCurrentFrame] = useState(1);
  const videoBoxRef = useRef(null);
  const imagesRef = useRef([]);
  const rafRef = useRef(null);
  const navigate = useNavigate();

  // Preload all frames when approaching viewport (earlier trigger)
  useEffect(() => {
    if (!videoBoxRef.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setShouldLoadFrames(true);
            observer.disconnect();
          }
        });
      },
      {
        rootMargin: "1500px", // Load much earlier - when ~1500px away from viewport
        threshold: 0,
      }
    );

    observer.observe(videoBoxRef.current);

    return () => observer.disconnect();
  }, []);

  // Preload all frame images in parallel using Promise.all
  useEffect(() => {
    if (!shouldLoadFrames) return;

    const loadImage = (index) => {
      return new Promise((resolve) => {
        const img = new Image();
        const frameNum = String(index).padStart(3, "0");
        img.src = `${FRAME_PATH}${frameNum}.webp`;
        img.onload = () => resolve(img);
        img.onerror = () => resolve(img); // Still resolve to not block others
      });
    };

    // Load all frames in parallel
    const imagePromises = [];
    for (let i = 1; i <= TOTAL_FRAMES; i++) {
      imagePromises.push(loadImage(i));
    }

    Promise.all(imagePromises).then((loadedImages) => {
      imagesRef.current = loadedImages;
      setFramesLoaded(true);
    });
  }, [shouldLoadFrames]);

  // Scroll-controlled frame display
  const updateFrame = useCallback(() => {
    if (!videoBoxRef.current || !framesLoaded) return;

    // Use external progress if provided
    if (externalProgress !== null) {
      const frameIndex = Math.min(
        TOTAL_FRAMES,
        Math.max(1, Math.ceil(externalProgress * TOTAL_FRAMES))
      );
      setCurrentFrame(frameIndex);
      rafRef.current = requestAnimationFrame(updateFrame);
      return;
    }

    const rect = videoBoxRef.current.getBoundingClientRect();
    const windowHeight = window.innerHeight;

    // Start: top of box enters bottom of viewport
    // End: box scrolled 30% past top of viewport (slower animation)
    const scrollStart = windowHeight;
    const scrollEnd = -rect.height * 0.3;
    const scrollRange = scrollStart - scrollEnd;
    const currentPosition = rect.top;

    const progress = Math.max(
      0,
      Math.min(1, (scrollStart - currentPosition) / scrollRange)
    );
    const frameIndex = Math.min(
      TOTAL_FRAMES,
      Math.max(1, Math.ceil(progress * TOTAL_FRAMES))
    );

    setCurrentFrame(frameIndex);
    rafRef.current = requestAnimationFrame(updateFrame);
  }, [framesLoaded, externalProgress]);

  // Start animation loop when frames ready
  useEffect(() => {
    if (!framesLoaded) return;

    rafRef.current = requestAnimationFrame(updateFrame);

    return () => {
      if (rafRef.current) {
        cancelAnimationFrame(rafRef.current);
      }
    };
  }, [framesLoaded, updateFrame]);

  const handleExploreClick = async () => {
    await optimizedTransitionUtils.transitionToRoute(
      navigate,
      getNewsDetailRoute("milan")
    );
  };

  // Generate current frame src
  const currentFrameSrc = `${FRAME_PATH}${String(currentFrame).padStart(
    3,
    "0"
  )}.webp`;

  return (
    <section className="lumex91">
      <div className="lumex91-container">
        <div className="lumex91-video-box" ref={videoBoxRef}>
          {/* Placeholder image - shown while frames loading */}
          <img
            className={`lumex91-video lumex91-placeholder ${
              framesLoaded ? "lumex91-placeholder-hide" : ""
            }`}
            src={`${FRAME_PATH}001.webp`}
            alt="Mirror-Lumex 91"
          />

          {/* Scroll-controlled frame display */}
          {framesLoaded && (
            <img
              className="lumex91-video lumex91-frame"
              src={currentFrameSrc}
              alt="Mirror-Lumex 91"
            />
          )}
        </div>

        <div className="lumex91-content">
          <h1 className="heading-1--no-margin">
            <ShinyText text="Mirror Lumex - 91™" speed={2} />
          </h1>
          <GlassThemeButton theme="dark" onClick={handleExploreClick}>
            <span className="bodytext-6--no-margin">Explore more</span>
          </GlassThemeButton>
        </div>
      </div>
    </section>
  );
};

export default Lumex91;
