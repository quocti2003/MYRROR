import React, { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";
import { MediaImage } from "@components/common/media";
import { ROUTES } from "@/constants/routes";
import "./Section4.css";
import ShineGlassButton from "@components/common/button/ShineGlassButton";

const Section4 = () => {
  const navigate = useNavigate();
  const sectionRef = useRef(null);

  const handleReturnExchangeClick = async () => {
    await optimizedTransitionUtils.transitionToRoute(
      navigate,
      `${ROUTES.SUPPORT_DETAIL}?tab=return-exchange`
    );
  };

  const handleSizingGuideClick = async () => {
    await optimizedTransitionUtils.transitionToRoute(
      navigate,
      `${ROUTES.SUPPORT_DETAIL}?tab=sizing-guide`
    );
  };

  const handleWarrantyInfoClick = async () => {
    await optimizedTransitionUtils.transitionToRoute(
      navigate,
      `${ROUTES.SUPPORT_DETAIL}?tab=warranty-info`
    );
  };

  useEffect(() => {
    const handleScroll = () => {
      const windowHeight = window.innerHeight;

      // Desktop parallax effect (>1024px) - same as mobile background
      if (window.innerWidth > 1024) {
        const desktopScreens = document.querySelectorAll(".section4-desktop-screen");

        desktopScreens.forEach((screen) => {
          const rect = screen.getBoundingClientRect();
          const scrollDistance = windowHeight - rect.top;
          const totalDistance = windowHeight + rect.height;
          const scrollRatio = Math.max(0, Math.min(1, scrollDistance / totalDistance));

          // Image moves DOWN slowly (0% → 30%)
          const imgParallax = scrollRatio * 30;

          screen.style.setProperty("--parallax-img-offset", `${imgParallax}%`);
        });
      }

      // Mobile parallax effect - only on mobile screens (<=480px)
      if (window.innerWidth <= 480) {
        const windowHeight = window.innerHeight;
        const mobileScreen1 = document.querySelector(
          ".section4-mobile-screen-1"
        );
        const mobileScreen2 = document.querySelector(
          ".section4-mobile-screen-2"
        );
        const mobileScreen3 = document.querySelector(
          ".section4-mobile-screen-3"
        );

        if (mobileScreen1) {
          const rect1 = mobileScreen1.getBoundingClientRect();
          const scrollDistance1 = windowHeight - rect1.top;
          const totalDistance1 = windowHeight + rect1.height;
          const scrollRatio1 = Math.max(
            0,
            Math.min(1, scrollDistance1 / totalDistance1)
          );

          // Dual-layer parallax: background and text move at different speeds (EXTREME)
          const bgParallax1 = scrollRatio1 * 100; // Background moves DOWN slowly (100px)
          const textParallax1 = scrollRatio1 * -80; // Text moves UP fast (80px) - opposite direction

          mobileScreen1.style.setProperty(
            "--parallax-bg-offset",
            `${bgParallax1}px`
          );
          mobileScreen1.style.setProperty(
            "--parallax-text-offset",
            `${textParallax1}px`
          );
        }

        if (mobileScreen2) {
          const rect2 = mobileScreen2.getBoundingClientRect();
          const scrollDistance2 = windowHeight - rect2.top;
          const totalDistance2 = windowHeight + rect2.height;
          const scrollRatio2 = Math.max(
            0,
            Math.min(1, scrollDistance2 / totalDistance2)
          );

          // Dual-layer parallax: background and text move at different speeds (EXTREME)
          const bgParallax2 = scrollRatio2 * 100; // Background moves DOWN slowly (100px)
          const textParallax2 = scrollRatio2 * -80; // Text moves UP fast (80px) - opposite direction

          mobileScreen2.style.setProperty(
            "--parallax-bg-offset",
            `${bgParallax2}px`
          );
          mobileScreen2.style.setProperty(
            "--parallax-text-offset",
            `${textParallax2}px`
          );
        }

        if (mobileScreen3) {
          const rect3 = mobileScreen3.getBoundingClientRect();
          const scrollDistance3 = windowHeight - rect3.top;
          const totalDistance3 = windowHeight + rect3.height;
          const scrollRatio3 = Math.max(
            0,
            Math.min(1, scrollDistance3 / totalDistance3)
          );

          // Dual-layer parallax: background and text move at different speeds (EXTREME)
          const bgParallax3 = scrollRatio3 * 100; // Background moves DOWN slowly (100px)
          const textParallax3 = scrollRatio3 * -80; // Text moves UP fast (80px) - opposite direction

          mobileScreen3.style.setProperty(
            "--parallax-bg-offset",
            `${bgParallax3}px`
          );
          mobileScreen3.style.setProperty(
            "--parallax-text-offset",
            `${textParallax3}px`
          );
        }
      }
    };

    window.addEventListener("scroll", handleScroll);
    handleScroll(); // Initial calculation

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <div ref={sectionRef} className="section4-wrapper">
      {/* Tablet content areas - only visible on tablet */}
      <div className="section4-tablet-content-1">
        <div className="section4-content">
          <div className="section4-full-content">
            <div className="section4-header-description">
              <div className="section4-header">
                <span className="section4-subtitle bodytext-4--no-margin">
                  EXPLORE OUR
                </span>
                <h2 className="section4-title heading-1--no-margin">
                  RETURN & EXCHANGE
                </h2>
              </div>
              <p className="section4-description bodytext-4--no-margin">
                Extend the life and brilliance of your jewelry with Mirror's
                professional care and repair services, tailored to keep each
                piece as radiant as the day you received it.
              </p>
            </div>
            <ShineGlassButton
              className="section4-cta"
              theme="light"
              onClick={handleReturnExchangeClick}
            >
              See more
            </ShineGlassButton>
          </div>
        </div>
      </div>

      <div className="section4-tablet-content-2">
        <div className="section4-content">
          <div className="section4-full-content">
            <div className="section4-header-description">
              <div className="section4-header">
                <span className="section4-subtitle bodytext-4--no-margin">
                  EXPLORE OUR
                </span>
                <h2 className="section4-title heading-1--no-margin">
                  SIZING GUIDE
                </h2>
              </div>
              <p className="section4-description bodytext-4--no-margin">
                Elevate your gift with our signature wrapping - a carefully
                crafted presentation that captures the essence of refined
                giving.
              </p>
            </div>
            <ShineGlassButton
              className="section4-cta"
              theme="light"
              onClick={handleSizingGuideClick}
            >
              See more
            </ShineGlassButton>
          </div>
        </div>
      </div>

      <div className="section4-tablet-content-3">
        <div className="section4-content">
          <div className="section4-full-content">
            <div className="section4-header-description">
              <div className="section4-header">
                <span className="section4-subtitle bodytext-4--no-margin">
                  EXPLORE OUR
                </span>
                <h2 className="section4-title heading-1--no-margin">
                  WARRANTY & INFO
                </h2>
              </div>
              <p className="section4-description bodytext-4--no-margin">
                All jewelry purchased from Mirror Future Diamond is covered by a
                12-month limited warranty
              </p>
            </div>
            <ShineGlassButton
              className="section4-cta"
              theme="light"
              onClick={handleWarrantyInfoClick}
            >
              See more
            </ShineGlassButton>
          </div>
        </div>
      </div>

      {/* Mobile Layout - only visible on mobile */}
      <div className="section4-mobile-screen-1">
        <div className="section4-content-layer">
          <div className="section4-content">
            <div className="section4-full-content">
              <div className="section4-header-description">
                <div className="section4-header">
                  <span className="section4-subtitle bodytext-4--no-margin">
                    EXPLORE OUR
                  </span>
                  <h2 className="section4-title heading-1--no-margin">
                    RETURN & EXCHANGE
                  </h2>
                </div>
                <p className="section4-description bodytext-4--no-margin">
                  Extend the life and brilliance of your jewelry with Mirror's
                  professional care and repair services, tailored to keep each
                  piece as radiant as the day you received it.
                </p>
              </div>
              <ShineGlassButton
                className="section4-cta"
                theme="footer"
                onClick={handleReturnExchangeClick}
              >
                See more
              </ShineGlassButton>
            </div>
          </div>
        </div>
      </div>

      <div className="section4-mobile-screen-2">
        <div className="section4-content-layer">
          <div className="section4-content">
            <div className="section4-full-content">
              <div className="section4-header-description">
                <div className="section4-header">
                  <span className="section4-subtitle bodytext-4--no-margin">
                    EXPLORE OUR
                  </span>
                  <h2 className="section4-title heading-1--no-margin">
                    SIZING GUIDE
                  </h2>
                </div>
                <p className="section4-description bodytext-4--no-margin">
                  Elevate your gift with our signature wrapping - a carefully
                  crafted presentation that captures the essence of refined
                  giving.
                </p>
              </div>
              <ShineGlassButton
                className="section4-cta"
                theme="footer"
                onClick={handleSizingGuideClick}
              >
                See more
              </ShineGlassButton>
            </div>
          </div>
        </div>
      </div>

      <div className="section4-mobile-screen-3">
        <div className="section4-content-layer">
          <div className="section4-content">
            <div className="section4-full-content">
              <div className="section4-header-description">
                <div className="section4-header">
                  <span className="section4-subtitle bodytext-4--no-margin">
                    EXPLORE OUR
                  </span>
                  <h2 className="section4-title heading-1--no-margin">
                    WARRANTY & INFO
                  </h2>
                </div>
                <p className="section4-description bodytext-4--no-margin">
                  All jewelry purchased from Mirror Future Diamond is covered by
                  a 12-month limited warranty
                </p>
              </div>
              <ShineGlassButton
                className="section4-cta"
                theme="footer"
                onClick={handleWarrantyInfoClick}
              >
                See more
              </ShineGlassButton>
            </div>
          </div>
        </div>
      </div>

      {/* Desktop Layout - Simple Screen Layout */}
      <div className="section4-desktop-container">
        {/* Screen 1 - Return & Exchange */}
        <div className="section4-desktop-screen">
          <div className="section4-desktop-image">
            <MediaImage
              src="services/Return & Exchange_960x1080.webp"
              alt="Return & Exchange"
            />
          </div>
          <div className="section4-desktop-content">
            <div className="section4-content">
              <div className="section4-full-content">
                <div className="section4-header-description">
                  <div className="section4-header">
                    <span className="section4-subtitle bodytext-4--no-margin">
                      EXPLORE OUR
                    </span>
                    <h2 className="section4-title heading-2--no-margin">
                      Return and Exchange
                    </h2>
                  </div>
                  <p className="section4-description bodytext-4--no-margin">
                    Extend the life and brilliance of your jewelry with Mirror's
                    professional care and repair services, tailored to keep each
                    piece as radiant as the day you received it.
                  </p>
                </div>
                <ShineGlassButton
                  className="section4-cta"
                  theme="light"
                  onClick={handleReturnExchangeClick}
                >
                  See more
                </ShineGlassButton>
              </div>
            </div>
          </div>
        </div>

        {/* Screen 2 - Sizing Guide */}
        <div className="section4-desktop-screen">
          <div className="section4-desktop-image">
            <MediaImage src="services/Sizing Guide.webp" alt="Sizing Guide" />
          </div>
          <div className="section4-desktop-content">
            <div className="section4-content">
              <div className="section4-full-content">
                <div className="section4-header-description">
                  <div className="section4-header">
                    <span className="section4-subtitle bodytext-4--no-margin">
                      EXPLORE OUR
                    </span>
                    <h2 className="section4-title heading-2--no-margin">
                      Sizing Guide
                    </h2>
                  </div>
                  <p className="section4-description bodytext-4--no-margin">
                    Elevate your gift with our signature wrapping - a carefully
                    crafted presentation that captures the essence of refined
                    giving.
                  </p>
                </div>
                <ShineGlassButton
                  className="section4-cta"
                  theme="light"
                  onClick={handleSizingGuideClick}
                >
                  See more
                </ShineGlassButton>
              </div>
            </div>
          </div>
        </div>

        {/* Screen 3 - Warranty & Info */}
        <div className="section4-desktop-screen">
          <div className="section4-desktop-image">
            <MediaImage
              src="services/Warranty Legacy.webp"
              alt="Warranty & Info"
            />
          </div>
          <div className="section4-desktop-content">
            <div className="section4-content">
              <div className="section4-full-content">
                <div className="section4-header-description">
                  <div className="section4-header">
                    <span className="section4-subtitle bodytext-4--no-margin">
                      EXPLORE OUR
                    </span>
                    <h2 className="section4-title heading-2--no-margin">
                      Warranty Legacy
                    </h2>
                  </div>
                  <p className="section4-description bodytext-4--no-margin">
                    All jewelry purchased from Mirror Future Diamond is covered
                    by a 12-month limited warranty
                  </p>
                </div>
                <ShineGlassButton
                  className="section4-cta"
                  theme="light"
                  onClick={handleWarrantyInfoClick}
                >
                  See more
                </ShineGlassButton>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Section4;
