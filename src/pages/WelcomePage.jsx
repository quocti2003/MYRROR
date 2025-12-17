import React, { useEffect, useState } from "react";
import { getImageUrl } from "@utils/cloudflareMediaUtil";
import ScrollDownArrow from "@components/common/button/ScrollDownArrow";
import ScrollToTopArrow from "@components/common/button/ScrollToTopArrow";
import ImmersiveButton from "@components/common/button/ImmersiveButton";
import { useScrollToNextSection } from "@/hooks/useScrollToNextSection";
import { useBottomTheme } from "@/hooks/useBottomTheme";
import "@components/home-page/scrollEffect/ScrollEffect.css";
import "./WelcomePage.css";

const WelcomePage = () => {
  const [isLoaded, setIsLoaded] = useState(false);

  const { isArrowVisible, handleArrowClick } = useScrollToNextSection({
    footerSelector: '.footer',
  });
  const { theme: arrowTheme } = useBottomTheme();
  const [isImmersiveCollapsed, setIsImmersiveCollapsed] = useState(false);
  const [showScrollTop, setShowScrollTop] = useState(false);

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

  useEffect(() => {
    // Trigger animation after component mounts
    setTimeout(() => setIsLoaded(true), 100);
  }, []);

  return (
    <div className={`welcome-page ${isLoaded ? "loaded" : ""}`}>
      {/* Background Image */}
      <div className="welcome-background-image"></div>

      {/* Logo */}
      <div className="welcome-logo">
        <img src={getImageUrl("welcome/welcome_logo.svg")} alt="Welcome Logo" />
      </div>

      {/* Main content */}
      {/* <div className="welcome-content">
        <div className="welcome-title-section">
          <h1 className="welcome-title heading-1--no-margin">Future Diamond</h1>
        </div>

        <div className="welcome-subtitle-section">
          <h2 className="welcome-subtitle bodytext-1--no-margin">
            Is Coming Soon
          </h2>
        </div>
      </div> */}

      {/* Bottom text content */}
      <div className="welcome-bottom-text">
        <h2 className="bottom-text-line1 bodytext-1--no-margin">
          Awakening Luxury through Your Senses, in Every Time, Space, and
          Presence.
        </h2>
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

export default WelcomePage;
