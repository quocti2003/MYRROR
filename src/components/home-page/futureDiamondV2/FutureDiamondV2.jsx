import { useRef, useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import Lumex91 from "@components/home-page/lumex91/Lumex91";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import ShinyText from "@components/common/shiny-text/ShinyText";
import { getNewsDetailRoute } from "@/constants/routes";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";
import "./FutureDiamondV2.css";

const FutureDiamondV2 = () => {
  const containerRef = useRef(null);
  const stickyWrapperRef = useRef(null);
  const navigate = useNavigate();
  const [titleState, setTitleState] = useState("hidden"); // hidden, visible, faded
  const [lumexState, setLumexState] = useState("hidden"); // hidden, visible, faded
  const [videoBoxState, setVideoBoxState] = useState("hidden"); // hidden, visible
  const [bgPhase, setBgPhase] = useState("light"); // light, dark
  const [frameProgress, setFrameProgress] = useState(0);

  // Throttle ref for scroll performance
  const lastScrollTime = useRef(0);
  const SCROLL_THROTTLE_MS = 16; // ~60fps

  const handleExploreClick = async () => {
    await optimizedTransitionUtils.transitionToRoute(
      navigate,
      getNewsDetailRoute("milan")
    );
  };

  // Core scroll logic (extracted for reuse)
  const updateScrollState = useCallback(() => {
    if (!stickyWrapperRef.current) return;

    const rect = stickyWrapperRef.current.getBoundingClientRect();
    const windowHeight = window.innerHeight;
    const wrapperHeight = stickyWrapperRef.current.offsetHeight;

    // Calculate scroll progress within the sticky wrapper (0 to 1)
    const scrollProgress = (windowHeight - rect.top) / wrapperHeight;

    // Frame progress for Lumex91 - starts at 18% scroll, maps 18%-100% to 0-1
    const lumexStartProgress = 0.18;
    const lumexProgress =
      (scrollProgress - lumexStartProgress) / (1 - lumexStartProgress);
    const clampedFrameProgress = Math.max(0, Math.min(1, lumexProgress));
    setFrameProgress(clampedFrameProgress);

    // Video box: Fade in when section enters (18%)
    if (scrollProgress < 0.18) {
      setVideoBoxState("hidden");
    } else {
      setVideoBoxState("visible");
    }

    // Title: Fade in 18%, hold, fade out 40%
    if (scrollProgress < 0.18) {
      setTitleState("hidden");
    } else if (scrollProgress >= 0.18 && scrollProgress < 0.4) {
      setTitleState("visible");
    } else {
      setTitleState("faded");
    }

    // Lumex content: Fade in at 45%, stay visible (no fade out)
    if (scrollProgress < 0.45) {
      setLumexState("hidden");
    } else {
      setLumexState("visible");
    }

    // Background phase: light initially, fade to dark at 40%
    if (scrollProgress < 0.4) {
      setBgPhase("light");
    } else {
      setBgPhase("dark");
    }
  }, []);

  // Scroll-based animation - fade in, hold, fade out sequence (with throttle)
  useEffect(() => {
    const handleScroll = () => {
      const now = performance.now();
      if (now - lastScrollTime.current < SCROLL_THROTTLE_MS) {
        return; // Skip if throttled
      }
      lastScrollTime.current = now;
      updateScrollState();
    };

    window.addEventListener("scroll", handleScroll, { passive: true });

    // Delay initial check to ensure CSS is applied first
    const timer = setTimeout(() => {
      updateScrollState();
    }, 100);

    return () => {
      window.removeEventListener("scroll", handleScroll);
      clearTimeout(timer);
    };
  }, [updateScrollState]);

  return (
    <section
      className={`future-diamond-v2 future-diamond-v2--${bgPhase}`}
      ref={containerRef}
      data-navbar-theme={bgPhase === "light" ? "black" : "white"}
    >
      {/* Sticky scroll section - all content inside */}
      <div className="future-diamond-v2-sticky-wrapper" ref={stickyWrapperRef}>
        <div className="future-diamond-v2-sticky-content">
          {/* Lumex box - stays sticky */}
          <div
            className={`future-diamond-v2-lumex-container future-diamond-v2-lumex--${lumexState} future-diamond-v2-videobox--${videoBoxState}`}
          >
            <Lumex91 externalProgress={frameProgress} />
          </div>

          {/* Title - centered, fades in/out */}
          <div
            className={`future-diamond-v2-title-overlay future-diamond-v2-title-overlay--${titleState}`}
          >
            <h1 className="future-diamond-v2-title">Love-Grown Diamond™</h1>
          </div>

          {/* Lumex Content - fades in */}
          <div
            className={`future-diamond-v2-lumex-content-overlay future-diamond-v2-lumex-content-overlay--${lumexState}`}
          >
            <p className="bodytext-6--no-margin future-diamond-v2-lumex-subtitle">
              THE WORLD'S NEWEST DIAMOND CUT, A NEW STAR IS BORN
            </p>
            <h1 className="heading-2--no-margin">
              <ShinyText text="Mirror Lumex - 91™" speed={2} />
            </h1>
            <ShineGlassButton theme="footer" onClick={handleExploreClick}>
              Explore more
            </ShineGlassButton>
          </div>
        </div>
      </div>
    </section>
  );
};

export default FutureDiamondV2;
