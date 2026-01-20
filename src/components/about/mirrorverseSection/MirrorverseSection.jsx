import React, { useEffect, useRef } from "react";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import { MediaVideo } from "@components/common/media";
import "./MirrorverseSection.css";

const MirrorverseSection = () => {
  const containerRef = useRef(null);
  const stickyRef = useRef(null);
  const contentRef = useRef(null);
  const headerRef = useRef(null);
  const titleRef = useRef(null);
  const descriptionRef = useRef(null);
  const buttonsRef = useRef(null);
  const videoRef = useRef(null);

  const headerText = "ENTER THE MIRRORVERSE";
  const titleText = "WHERE LUXURY BECOMES AN EXPERIENCE";
  const descriptionText =
    "Mirror is more than a brand. It's a world.\n\nThrough AR try-ons, immersive showrooms, sensorial storytelling, we invite you to experience jewelry in a way that awakens every sense - from the flicker of a candlelight to the touch of velvet trays, from curated soundscapes to mirror illusions. This is luxury you don't just wear - you step into it.";

  useEffect(() => {
    let ticking = false;
    let lastProgress = -1;

    const updateScroll = () => {
      if (!containerRef.current) {
        ticking = false;
        return;
      }

      const container = containerRef.current;
      const rect = container.getBoundingClientRect();
      const viewportHeight = window.innerHeight;

      // Start animation when section top enters viewport from bottom
      const scrolled = viewportHeight - rect.top;
      const totalScrollDistance = container.offsetHeight;
      const progress = Math.max(0, Math.min(1, scrolled / totalScrollDistance));

      if (Math.abs(progress - lastProgress) < 0.001) {
        ticking = false;
        return;
      }
      lastProgress = progress;

      // Phase 1 (0-20%): Move entire content to fixed position
      const moveProgress =
        progress <= 0.2 ? Math.max(0, Math.min(1, progress / 0.2)) : 1;

      if (contentRef.current) {
        const viewportHeight = window.innerHeight;
        const initialTranslateY = viewportHeight * 0.5;
        const currentTranslateY =
          initialTranslateY - moveProgress * initialTranslateY;
        contentRef.current.style.transform = `translate3d(0, ${currentTranslateY}px, 0)`;
        contentRef.current.style.opacity = moveProgress;
      }

      // Phase 2 (20-35%): Header fade in + move up
      if (headerRef.current) {
        let headerY = 30,
          headerOpacity = 0;
        if (progress < 0.2) {
          headerY = 30;
          headerOpacity = 0;
        } else if (progress <= 0.35) {
          const fadeProgress = (progress - 0.2) / 0.15;
          headerY = (1 - fadeProgress) * 30;
          headerOpacity = fadeProgress;
        } else {
          headerY = 0;
          headerOpacity = 1;
        }
        headerRef.current.style.transform = `translate3d(0, ${headerY}%, 0)`;
        headerRef.current.style.opacity = headerOpacity;
      }

      // Phase 3 (35-50%): Title fade in + move up
      if (titleRef.current) {
        let titleY = 30,
          titleOpacity = 0;
        if (progress < 0.35) {
          titleY = 30;
          titleOpacity = 0;
        } else if (progress <= 0.5) {
          const fadeProgress = (progress - 0.35) / 0.15;
          titleY = (1 - fadeProgress) * 30;
          titleOpacity = fadeProgress;
        } else {
          titleY = 0;
          titleOpacity = 1;
        }
        titleRef.current.style.transform = `translate3d(0, ${titleY}%, 0)`;
        titleRef.current.style.opacity = titleOpacity;
      }

      // Phase 4 (50-65%): Description fade in + move up
      if (descriptionRef.current) {
        let descY = 30,
          descOpacity = 0;
        if (progress < 0.5) {
          descY = 30;
          descOpacity = 0;
        } else if (progress <= 0.65) {
          const fadeProgress = (progress - 0.5) / 0.15;
          descY = (1 - fadeProgress) * 30;
          descOpacity = fadeProgress;
        } else {
          descY = 0;
          descOpacity = 1;
        }
        descriptionRef.current.style.transform = `translate3d(0, ${descY}%, 0)`;
        descriptionRef.current.style.opacity = descOpacity;
      }

      // Phase 5 (65-75%): Buttons fade in + move up
      if (buttonsRef.current) {
        let btnY = 30,
          btnOpacity = 0;
        if (progress < 0.65) {
          btnY = 30;
          btnOpacity = 0;
        } else if (progress <= 0.75) {
          const fadeProgress = (progress - 0.65) / 0.1;
          btnY = (1 - fadeProgress) * 30;
          btnOpacity = fadeProgress;
        } else {
          btnY = 0;
          btnOpacity = 1;
        }
        buttonsRef.current.style.transform = `translate3d(0, ${btnY}%, 0)`;
        buttonsRef.current.style.opacity = btnOpacity;
      }

      // Phase 6 (75-100%): Video fade in
      if (videoRef.current) {
        let videoOpacity = 0;
        if (progress < 0.75) {
          videoOpacity = 0;
        } else if (progress <= 1) {
          videoOpacity = (progress - 0.75) / 0.25;
        } else {
          videoOpacity = 1;
        }
        videoRef.current.style.opacity = videoOpacity;
      }

      ticking = false;
    };

    const handleScroll = () => {
      if (!ticking) {
        window.requestAnimationFrame(updateScroll);
        ticking = true;
      }
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    handleScroll();

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <div className="mirrorverse-section" ref={containerRef}>
      <div className="mirrorverse-sticky-wrapper" ref={stickyRef}>
        <div className="mirrorverse-content" ref={contentRef}>
          {/* Header */}
          <div className="mirrorverse-header" ref={headerRef}>
            <p className="bodytext-4--no-margin">{headerText}</p>
          </div>

          {/* Main Title */}
          <div className="mirrorverse-title" ref={titleRef}>
            <h1 className="heading-1--no-margin">{titleText}</h1>
          </div>

          {/* Description */}
          <div className="mirrorverse-description" ref={descriptionRef}>
            <p className="bodytext-4--no-margin">
              {descriptionText.split("\n").map((line, index) => (
                <React.Fragment key={index}>
                  {line}
                  {index < descriptionText.split("\n").length - 1 && <br />}
                </React.Fragment>
              ))}
            </p>
          </div>

          {/* Buttons */}
          <div className="mirrorverse-buttons" ref={buttonsRef}>
            <ShineGlassButton theme="footer">AR Try on</ShineGlassButton>
            <GlassThemeButton theme="spec_dark">
              Immersive Showroom
            </GlassThemeButton>
          </div>
        </div>

        {/* Video Section */}
        <div className="mirrorverse-video" ref={videoRef}>
          <MediaVideo
            src="about/immersive_showroom.mp4"
            autoPlay
            muted
            loop
            playsInline
            className="mirrorverse-video-element"
          />
        </div>
      </div>
    </div>
  );
};

export default MirrorverseSection;
