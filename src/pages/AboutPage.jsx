import { useEffect, useState } from "react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import SloganSection from "@components/about/sloganSection/SloganSection";
import StartingPlaceSection from "@components/about/startingPlaceSection/StartingPlaceSection";
import BODMemberV4 from "@components/about/BODMemberV4/BODMemberV4";
import MirrorNetworkDiscoverSection from "@components/about/mirrorNetworkDiscoverSection/MirrorNetworkDiscoverSection";
import MirrorverseSection from "@components/about/mirrorverseSection/MirrorverseSection";
import AtMirror from "@components/about/atMirror/AtMirror";
import MirrorPassportSection from "@components/about/mirrorPassportSection/MirrorPassportSection";
import MirrorNetworkV2 from "@components/about/mirrorNetworkV2/MirrorNetworkV2";
import SharedSection from "@components/about/sharedSection/SharedSection";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import { useScrollToNextSection } from "@/hooks/useScrollToNextSection";
import { useBottomTheme } from "@/hooks/useBottomTheme";
import "@components/home-page/scrollEffect/ScrollEffect.css"; // Import CSS for arrow styling
import "./AboutPage.css";

gsap.registerPlugin(ScrollTrigger);

const AboutPage = () => {
  const { isArrowVisible, handleArrowClick } = useScrollToNextSection({
    footerSelector: '[data-section="shared-section"], .footer', // Check both SharedSection and global footer
    scrollToEnd: true, // Scroll to end of each section instead of start
    scrollToStartSections: ["bod-member"], // BOD Member section should scroll to start instead of end
  });
  const { theme: arrowTheme } = useBottomTheme();
  const [isImmersiveCollapsed, setIsImmersiveCollapsed] = useState(false);
  const [showScrollTop, setShowScrollTop] = useState(false);

  // Detect scroll to collapse immersive button and show scroll-to-top
  useEffect(() => {
    const handleScroll = () => {
      const scrollY = window.scrollY;
      setIsImmersiveCollapsed(scrollY > 100);
      setShowScrollTop(scrollY > 500);
    };
    window.addEventListener("scroll", handleScroll, { passive: true });
    handleScroll();
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const setupScrollTriggers = () => {
    // Kill existing triggers first
    ScrollTrigger.getAll().forEach((trigger) => trigger.kill());

    let panels = gsap.utils.toArray(".panel");

    panels.forEach((panel) => {
      ScrollTrigger.create({
        trigger: panel,
        start: () =>
          panel.offsetHeight < window.innerHeight ? "top top" : "bottom bottom",
        pin: true,
        pinSpacing: false,
        anticipatePin: 1, // Smooth pin when scrolling fast
        invalidateOnRefresh: true, // Recalculate on refresh
        fastScrollEnd: true, // Better fast scroll handling
      });
    });
  };

  useEffect(() => {
    // Scroll to top on mount
    window.scrollTo(0, 0);

    // Small delay to ensure DOM is ready
    const initTimeout = setTimeout(() => {
      setupScrollTriggers();
    }, 100);

    const handlePageTransitionComplete = () => {
      setTimeout(() => {
        setupScrollTriggers();
      }, 150);
    };

    const handleResize = () => {
      ScrollTrigger.refresh();
    };

    window.addEventListener(
      "pageTransitionComplete",
      handlePageTransitionComplete
    );
    window.addEventListener("resize", handleResize);

    return () => {
      clearTimeout(initTimeout);
      ScrollTrigger.getAll().forEach((trigger) => trigger.kill());
      window.removeEventListener(
        "pageTransitionComplete",
        handlePageTransitionComplete
      );
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  return (
    <div className="about-page">
      <div data-section="slogan-section" data-navbar-theme="white">
        <SloganSection />
      </div>
      <div data-section="starting-place" data-navbar-theme="white">
        {/* <section className="panel" data-section="starting-place"> */}
        <StartingPlaceSection />
        {/* </section> */}
      </div>

      <div data-section="bod-member" data-navbar-theme="black">
        <BODMemberV4 />
      </div>

      {/* <section className="panel" data-section="at-mirror">
        <AtMirror />
      </section> */}

      <div data-section="mirrorverse" data-navbar-theme="white">
        <section className="panel" data-section="mirrorverse">
          <MirrorverseSection />
        </section>
      </div>

      {/* <section data-section="mirror-network-discover" data-navbar-theme="white">
        <MirrorNetworkDiscoverSection />
      </section> */}

      <div data-section="mirror-network-v2" data-navbar-theme="white">
        <MirrorNetworkV2 />
      </div>

      <div data-section="mirror-passport" data-navbar-theme="white">
        <MirrorPassportSection />
      </div>

      {/* <div data-section="shared-section" data-navbar-theme="white">
        <SharedSection />
      </div> */}

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
      <div className={`fixed-scroll-top-container ${showScrollTop ? 'visible' : ''}`}>
        <GlassThemeButton
          theme={arrowTheme === "white" ? "dark" : "light"}
          icon="arrow-up"
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
        />
      </div>
    </div>
  );
};

export default AboutPage;
