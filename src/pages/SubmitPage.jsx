import { useEffect, useRef, useState } from "react";
import ScrollDownArrow from "@components/common/button/ScrollDownArrow";
import ScrollToTopArrow from "@components/common/button/ScrollToTopArrow";
import ImmersiveButton from "@components/common/button/ImmersiveButton";
import { useScrollToNextSection } from "@/hooks/useScrollToNextSection";
import { useBottomTheme } from "@/hooks/useBottomTheme";
import "@components/home-page/scrollEffect/ScrollEffect.css";
import SmoothScroll from "@utils/smoothScroll";
import IntroSubmit from "@components/submit/intro-submit/IntroSubmit";
import GuideStep1 from "@components/submit/guide-step-1/GuideStep1";
import GuideStep2 from "@components/submit/guide-step-2/GuideStep2";
import GuideStep3 from "@components/submit/guide-step-3/GuideStep3";
import SubmitForm from "@components/submit/submit-form/SubmitForm";
import "./SubmitPage.css";

const SubmitPage = () => {
  const progressBarRef = useRef(null);
  const smoothScrollRef = useRef(null);

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
    // Initialize smooth scroll only on desktop
    const isMobileOrTablet = window.innerWidth <= 1023;

    if (!isMobileOrTablet) {
      smoothScrollRef.current = new SmoothScroll({
        wrapper: document.querySelector("[data-smooth-wrapper]"),
        content: document.querySelector("[data-smooth-content]"),
        ease: 0.08, // Adjust smoothness (0.05-0.1 recommended)
      });
    }

    let ticking = false;

    const updateProgressBar = () => {
      if (progressBarRef.current) {
        const scrollTop = window.scrollY;
        const docHeight =
          document.documentElement.scrollHeight - window.innerHeight;
        const scrollPercent = (scrollTop / docHeight) * 100;
        progressBarRef.current.style.width = `${scrollPercent}%`;
      }
      ticking = false;
    };

    const handleScroll = () => {
      if (!ticking) {
        window.requestAnimationFrame(updateProgressBar);
        ticking = true;
      }
    };

    window.addEventListener("scroll", handleScroll, { passive: true });

    // Initial update
    updateProgressBar();

    return () => {
      // Cleanup smooth scroll
      if (smoothScrollRef.current) {
        smoothScrollRef.current.destroy();
      }
      window.removeEventListener("scroll", handleScroll);
    };
  }, []);

  return (
    <div className="submit-page">
      <div className="scroll-progress-bar" ref={progressBarRef}></div>

      <div data-smooth-wrapper>
        <div data-smooth-content>
          <section data-parallax="0.5">
            <IntroSubmit />
          </section>

          <GuideStep1 />

          <GuideStep2 />

          <GuideStep3 />

          <SubmitForm />
        </div>
      </div>

      {/* Fixed Immersive Button */}
      <div className="fixed-immersive-container">
        <ImmersiveButton
          theme={arrowTheme}
          isCollapsed={isImmersiveCollapsed}
        />
      </div>

      {/* Fixed Arrow Button */}
      {isArrowVisible && (
        <div className="fixed-arrow-container">
          <ScrollDownArrow theme={arrowTheme} onClick={handleArrowClick} />
        </div>
      )}

      {/* Fixed Scroll to Top Button - only show when scroll-down arrow is hidden */}
      <div className={`fixed-scroll-top-container ${showScrollTop ? 'visible' : ''}`}>
        <ScrollToTopArrow theme={arrowTheme} />
      </div>
    </div>
  );
};

export default SubmitPage;
