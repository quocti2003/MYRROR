import React, { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";
import { MediaImage } from "@components/common/media";
import { ROUTES } from "@/constants/routes";
import "./Section2.css";
import ShineGlassButton from "@components/common/button/ShineGlassButton";

const Section2 = () => {
  const navigate = useNavigate();
  const sectionRef = useRef(null);

  const handleProductCareClick = async () => {
    await optimizedTransitionUtils.transitionToRoute(
      navigate,
      `${ROUTES.SERVICES_DETAIL}?tab=product-care-repair`
    );
  };

  const handleTradeInClick = async () => {
    await optimizedTransitionUtils.transitionToRoute(
      navigate,
      `${ROUTES.SERVICES_DETAIL}?tab=trade-in-upgrade`
    );
  };

  useEffect(() => {
    const handleScroll = () => {
      const windowHeight = window.innerHeight;

      // Desktop parallax effect (>1024px) - same as mobile background
      if (window.innerWidth > 1024) {
        const desktopScreens = document.querySelectorAll(".section2-desktop-screen");

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
        const mobileScreen1 = document.querySelector(
          ".section2-mobile-screen-1"
        );
        const mobileScreen2 = document.querySelector(
          ".section2-mobile-screen-2"
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
      }
    };

    window.addEventListener("scroll", handleScroll);
    handleScroll(); // Initial calculation

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <div ref={sectionRef} className="section2-wrapper">
      {/* Tablet content areas - only visible on tablet */}
      <div className="section2-tablet-content-1">
        <div className="section2-content">
          <div className="section2-full-content">
            <div className="section2-header-description">
              <div className="section2-header">
                <span className="section2-subtitle bodytext-4--no-margin">
                  EXPLORE OUR
                </span>
                <h2 className="section2-title heading-1--no-margin">
                  PRODUCT CARE & REPAIR
                </h2>
              </div>
              <p className="section2-description bodytext-4--no-margin">
                Extend the life and brilliance of your jewelry with Mirror's
                professional care and repair services, tailored to keep each
                piece as radiant as the day you received it.
              </p>
            </div>
            <ShineGlassButton
              className="section2-cta"
              theme="light"
              onClick={handleProductCareClick}
            >
              See more
            </ShineGlassButton>
          </div>
        </div>
      </div>

      <div className="section2-tablet-content-2">
        <div className="section2-content">
          <div className="section2-full-content">
            <div className="section2-header-description">
              <div className="section2-header">
                <span className="section2-subtitle bodytext-4--no-margin">
                  EXPLORE OUR
                </span>
                <h2 className="section2-title heading-1--no-margin">
                  TRADE IN
                </h2>
              </div>
              <p className="section2-description bodytext-4--no-margin">
                Mirror's "Reclaim & Renew" program allows you to trade in
                eligible pieces in the future toward upgraded designs - a
                promise of continuous evolution in your jewelry journey.
              </p>
            </div>
            <ShineGlassButton
              className="section2-cta"
              theme="light"
              onClick={handleTradeInClick}
            >
              See more
            </ShineGlassButton>
          </div>
        </div>
      </div>

      {/* Mobile Layout - only visible on mobile */}
      <div className="section2-mobile-screen-1">
        <div className="section2-content-layer">
          <div className="section2-content">
            <div className="section2-full-content">
              <div className="section2-header-description">
                <div className="section2-header">
                  <span className="section2-subtitle bodytext-4--no-margin">
                    EXPLORE OUR
                  </span>
                  <h2 className="section2-title heading-1--no-margin">
                    PRODUCT CARE & REPAIR
                  </h2>
                </div>
                <p className="section2-description bodytext-4--no-margin">
                  Extend the life and brilliance of your jewelry with Mirror's
                  professional care and repair services, tailored to keep each
                  piece as radiant as the day you received it.
                </p>
              </div>
              <ShineGlassButton
                className="section2-cta"
                theme="footer"
                onClick={handleProductCareClick}
              >
                See more
              </ShineGlassButton>
            </div>
          </div>
        </div>
      </div>

      <div className="section2-mobile-screen-2">
        <div className="section2-content-layer">
          <div className="section2-content">
            <div className="section2-full-content">
              <div className="section2-header-description">
                <div className="section2-header">
                  <span className="section2-subtitle bodytext-4--no-margin">
                    EXPLORE OUR
                  </span>
                  <h2 className="section2-title heading-1--no-margin">
                    TRADE IN
                  </h2>
                </div>
                <p className="section2-description bodytext-4--no-margin">
                  Mirror's "Reclaim & Renew" program allows you to trade in
                  eligible pieces in the future toward upgraded designs - a
                  promise of continuous evolution in your jewelry journey.
                </p>
              </div>
              <ShineGlassButton
                className="section2-cta"
                theme="footer"
                onClick={handleTradeInClick}
              >
                See more
              </ShineGlassButton>
            </div>
          </div>
        </div>
      </div>

      {/* Desktop/Tablet Layout - Simple Screen Layout */}
      <div className="section2-desktop-container">
        {/* Screen 1 - Product Care */}
        <div className="section2-desktop-screen">
          <div className="section2-desktop-image">
            <MediaImage
              src="services/Product care & Repair_1.webp"
              alt="Product Care"
            />
          </div>
          <div className="section2-desktop-content">
            <div className="section2-content">
              <div className="section2-full-content">
                <div className="section2-header-description">
                  <div className="section2-header">
                    <span className="section2-subtitle bodytext-4--no-margin">
                      EXPLORE OUR
                    </span>
                    <h2 className="section2-title heading-2--no-margin">
                      Product Care and Repair
                    </h2>
                  </div>
                  <p className="section2-description bodytext-4--no-margin">
                    Extend the life and brilliance of your jewelry with Mirror's
                    professional care and repair services, tailored to keep each
                    piece as radiant as the day you received it.
                  </p>
                </div>
                <ShineGlassButton
                  className="section2-cta"
                  theme="light"
                  onClick={handleProductCareClick}
                >
                  See more
                </ShineGlassButton>
              </div>
            </div>
          </div>
        </div>

        {/* Screen 2 - Trade In */}
        <div className="section2-desktop-screen">
          <div className="section2-desktop-image">
            <MediaImage src="services/Trade in.webp" alt="Trade In" />
          </div>
          <div className="section2-desktop-content">
            <div className="section2-content">
              <div className="section2-full-content">
                <div className="section2-header-description">
                  <div className="section2-header">
                    <span className="section2-subtitle bodytext-4--no-margin">
                      EXPLORE OUR
                    </span>
                    <h2 className="section2-title heading-2--no-margin">
                      Trade in
                    </h2>
                  </div>
                  <p className="section2-description bodytext-4--no-margin">
                    Mirror's "Reclaim & Renew" program allows you to trade in
                    eligible pieces in the future toward upgraded designs - a
                    promise of continuous evolution in your jewelry journey.
                  </p>
                </div>
                <ShineGlassButton
                  className="section2-cta"
                  theme="light"
                  onClick={handleTradeInClick}
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

export default Section2;
