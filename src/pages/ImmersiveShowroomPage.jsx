import { useEffect, useRef, lazy, Suspense, useState } from "react";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import { useScrollToNextSection } from "@/hooks/useScrollToNextSection";
import { useBottomTheme } from "@/hooks/useBottomTheme";
import "@components/home-page/scrollEffect/ScrollEffect.css";
import Logo from "@assets/images/Logo.svg";
import SoundIcon from "@assets/images/button/sound.svg";
import ArrowButton from "@assets/images/button/arrow-button.svg";

// Lazy load heavy components
const ScrollEffect = lazy(() =>
  import("@components/home-page/scrollEffect/ScrollEffect")
);
const BrandPillars = lazy(() =>
  import("@components/home-page/brandPillars/BrandPillars")
);
const UniverseSection = lazy(() =>
  import("@components/home-page/universeSection/MirrorExp")
);
const FutureDiamondV2 = lazy(() =>
  import("@components/home-page/futureDiamondV2/FutureDiamondV2")
);
const IntroSubmit = lazy(() =>
  import("@components/submit/intro-submit/IntroSubmit")
);
const ImmersiveShowroom = lazy(() =>
  import("@components/home-page/immersiveShowroom/ImmersiveShowroom")
);

const ImmersiveShowroomPage = () => {
  const finalGradientRef = useRef(null);
  const elementsToFadeRef = useRef(null);

  const { isArrowVisible, handleArrowClick } = useScrollToNextSection({
    footerSelector: '.footer',
  });
  const { theme: arrowTheme } = useBottomTheme();
  const [isImmersiveCollapsed, setIsImmersiveCollapsed] = useState(false);
  const [showScrollTop, setShowScrollTop] = useState(false);

  useEffect(() => {
    const handleScrollButtons = () => {
      const scrollY = window.scrollY;
      setIsImmersiveCollapsed(scrollY > 100);
      setShowScrollTop(scrollY > 500);
    };
    window.addEventListener("scroll", handleScrollButtons, { passive: true });
    handleScrollButtons();
    return () => window.removeEventListener("scroll", handleScrollButtons);
  }, []);

  useEffect(() => {
    // Check if we need to scroll to top
    if (sessionStorage.getItem("scrollToTop") === "true") {
      window.scrollTo(0, 0);
      sessionStorage.removeItem("scrollToTop");
    }
  }, []);

  useEffect(() => {
    const handleScroll = () => {
      const scrollY = window.scrollY;
      const animationStart = 0;
      const animationEnd = window.innerHeight;

      let progress = 0;
      if (scrollY >= animationStart && scrollY <= animationEnd) {
        progress = (scrollY - animationStart) / (animationEnd - animationStart);
      } else if (scrollY > animationEnd) {
        progress = 1;
      }

      if (finalGradientRef.current) {
        // Calculation MUST go from POSITIVE to NEGATIVE (120 -> -20)
        const wipePosition = 400 - progress * 350;
        finalGradientRef.current.style.setProperty(
          "--wipe-progress",
          `${wipePosition}%`
        );
      }

      if (elementsToFadeRef.current) {
        const fadeProgress = Math.min(1, Math.max(0, (progress - 0.5) * 2));
        elementsToFadeRef.current.style.opacity = 1 - fadeProgress;
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <>
      {/* <div className="scroll-container">
        <div className="homepage">
          <div className="gradient-initial">
            <div className="gradient-top"></div>
            <div className="gradient-bottom"></div>
          </div>

          <div className="gradient-final" ref={finalGradientRef}></div>

          <div className="logo-center">
            <img src={Logo} alt="Mirror Logo" className="main-logo" />
          </div>

          <div className="elements-to-fade" ref={elementsToFadeRef}>
            <div className="scroll-down">
              <button>
                <img src={ArrowButton} alt="Arrow Button" />
              </button>
            </div>
            <div className="vetor-button">
              <button>
                <img src={SoundIcon} alt="Sound" />
              </button>
            </div>
          </div>
        </div>
      </div> */}

      {/* <MirrorIntroduce /> */}

      <Suspense
        fallback={
          <div style={{ minHeight: "100vh", background: "#000" }}>
            Loading...
          </div>
        }
      >
        <ScrollEffect />

        <div data-section="future-diamond" data-navbar-theme="white">
          <FutureDiamondV2 />
        </div>

        <BrandPillars />

        <UniverseSection />

        <ImmersiveShowroom />

        <IntroSubmit />
      </Suspense>

      {/* Fixed Immersive Button */}
      <div className="fixed-immersive-container">
        <GlassThemeButton
          theme={arrowTheme === "white" ? "dark" : "light"}
          icon="globe"
          isCollapsed={isImmersiveCollapsed}
        >
          Immersive Showroom
        </GlassThemeButton>
      </div>

      {/* Fixed Arrow Button */}
      {isArrowVisible && (
        <div className="fixed-arrow-container">
          <GlassThemeButton
            theme={arrowTheme === "white" ? "dark" : "light"}
            icon="arrow"
            onClick={handleArrowClick}
          />
        </div>
      )}

      {/* Fixed Scroll to Top Button - only show when scroll-down arrow is hidden */}
      <div className={`fixed-scroll-top-container no-immersive-btn ${showScrollTop ? 'visible' : ''}`}>
        <GlassThemeButton
          theme={arrowTheme === "white" ? "dark" : "light"}
          icon="arrow-up"
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
        />
      </div>
    </>
  );
};

export default ImmersiveShowroomPage;
