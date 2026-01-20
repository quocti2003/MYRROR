import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import "./Footer.css";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import GlassThemeButton from "@components/common/button/GlassThemeButton";
import UnderlineButton from "@components/common/button/UnderlineButton";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";
import { ROUTES } from "@/constants/routes";
import fbIcon from "@assets/images/icons/fb_icon.svg";
import instaIcon from "@assets/images/icons/insta_icon.svg";
import tiktokIcon from "@assets/images/icons/tiktok_icon.svg";
import PrismaticBurst from "@components/common/prismatic-burst/PrismaticBurst";
import BookingModalV3 from "@components/booking/BookingModalV3";

const Footer = () => {
  const navigate = useNavigate();
  const [isFooterVisible, setIsFooterVisible] = useState(false);
  const [isFooterDeepVisible, setIsFooterDeepVisible] = useState(false); // For footer-content at 80%
  const [shouldRenderBurst, setShouldRenderBurst] = useState(false);
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);
  const footerRef = useRef(null);

  // Refs for scroll-linked animation elements
  const footerContentRef = useRef(null);
  const footerCenterRef = useRef(null);
  const footerBottomRef = useRef(null);

  const handleHomeClick = async (e) => {
    e.preventDefault();
    if (window.location.pathname === ROUTES.HOME) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    sessionStorage.setItem("scrollToTop", "true");
    await optimizedTransitionUtils.transitionToRoute(navigate, ROUTES.HOME);
  };

  const handleProductsClick = async (e) => {
    e.preventDefault();
    if (window.location.pathname === ROUTES.COLLECTIONS) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    sessionStorage.setItem("scrollToTop", "true");
    await optimizedTransitionUtils.transitionToRoute(
      navigate,
      ROUTES.COLLECTIONS
    );
  };

  const handleServicesClick = async (e) => {
    e.preventDefault();
    if (window.location.pathname === ROUTES.SERVICES) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    sessionStorage.setItem("scrollToTop", "true");
    await optimizedTransitionUtils.transitionToRoute(navigate, ROUTES.SERVICES);
  };

  const handleSupportClick = async (e) => {
    e.preventDefault();
    if (window.location.pathname === ROUTES.SUPPORT) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    sessionStorage.setItem("scrollToTop", "true");
    await optimizedTransitionUtils.transitionToRoute(navigate, ROUTES.SUPPORT);
  };

  const handleAboutClick = async (e) => {
    e.preventDefault();
    if (window.location.pathname === ROUTES.ABOUT) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    sessionStorage.setItem("scrollToTop", "true");
    await optimizedTransitionUtils.transitionToRoute(navigate, ROUTES.ABOUT);
  };

  const handleNewsClick = async (e) => {
    e.preventDefault();
    if (window.location.pathname === ROUTES.NEWS) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    sessionStorage.setItem("scrollToTop", "true");
    await optimizedTransitionUtils.transitionToRoute(navigate, ROUTES.NEWS);
  };

  const handleContactClick = async (e) => {
    e.preventDefault();
    if (window.location.pathname === ROUTES.CONTACT) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    sessionStorage.setItem("scrollToTop", "true");
    await optimizedTransitionUtils.transitionToRoute(navigate, ROUTES.CONTACT);
  };

  const handleLocationClick = async (e) => {
    e.preventDefault();
    if (window.location.pathname === ROUTES.LOCATIONS) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    sessionStorage.setItem("scrollToTop", "true");
    await optimizedTransitionUtils.transitionToRoute(
      navigate,
      ROUTES.LOCATIONS
    );
  };

  const handleBookAppointmentClick = (e) => {
    e.preventDefault();
    setIsBookingModalOpen(true);
  };

  // Use IntersectionObserver on the spacer element (not fixed footer)
  useEffect(() => {
    // Find the spacer element that reveals the footer
    const spacer = document.querySelector(".footer-reveal-spacer");
    if (!spacer) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        const ratio = entry.intersectionRatio;
        // footer-center & footer-bottom: trigger at 50%
        setIsFooterVisible(entry.isIntersecting && ratio >= 0.5);
        // footer-content: trigger at 80%
        setIsFooterDeepVisible(entry.isIntersecting && ratio >= 0.8);
      },
      { threshold: [0, 0.25, 0.5, 0.75, 0.8, 0.9, 1] }
    );

    observer.observe(spacer);

    return () => observer.disconnect();
  }, []);

  // Trigger animation for footer-center & footer-bottom at 50%
  useEffect(() => {
    if (isFooterVisible) {
      footerCenterRef.current?.classList.add('visible');
      footerBottomRef.current?.classList.add('visible');
    } else {
      footerCenterRef.current?.classList.remove('visible');
      footerBottomRef.current?.classList.remove('visible');
    }
  }, [isFooterVisible]);

  // Trigger animation for footer-content at 80%
  useEffect(() => {
    if (isFooterDeepVisible) {
      footerContentRef.current?.classList.add('visible');
    } else {
      footerContentRef.current?.classList.remove('visible');
    }
  }, [isFooterDeepVisible]);

  // Handle mount/unmount - fade in only
  useEffect(() => {
    if (isFooterVisible) {
      // Mount component immediately for fade-in
      setShouldRenderBurst(true);
    } else {
      // Unmount immediately (no fade-out)
      setShouldRenderBurst(false);
    }
  }, [isFooterVisible]);

  return (
    <footer className="footer" data-navbar-theme="white" ref={footerRef}>
      {/* Prismatic Burst Background Effect */}
      <div
        className={`footer-background-effect ${
          isFooterVisible ? "active" : ""
        }`}
      >
        {shouldRenderBurst && (
          <PrismaticBurst
            animationType="hover"
            intensity={3.1}
            speed={0.15}
            distort={10}
            paused={false}
            offset={{ x: 0, y: 0 }}
            hoverDampness={0.6}
            rayCount={0}
            mixBlendMode="lighten"
            colors={["#bc224c", "#140039", "#0B0B0B"]}
          />
        )}
      </div>

      <div className="footer-container">
        <div className="footer-content" ref={footerContentRef}>
          <div className="footer-left">
            <div className="footer-section">
              <ul className="footer-links">
                <li>
                  <UnderlineButton
                    onClick={handleHomeClick}
                    className="footer-link-button"
                  >
                    Home
                  </UnderlineButton>
                </li>
                <li>
                  <UnderlineButton
                    onClick={handleProductsClick}
                    className="footer-link-button"
                  >
                    Products
                  </UnderlineButton>
                </li>
                <li>
                  <UnderlineButton
                    onClick={handleServicesClick}
                    className="footer-link-button"
                  >
                    Services
                  </UnderlineButton>
                </li>
                <li>
                  <UnderlineButton
                    onClick={handleSupportClick}
                    className="footer-link-button"
                  >
                    Support
                  </UnderlineButton>
                </li>
                <li>
                  <UnderlineButton
                    onClick={handleAboutClick}
                    className="footer-link-button"
                  >
                    About
                  </UnderlineButton>
                </li>
                <li>
                  <UnderlineButton
                    onClick={handleNewsClick}
                    className="footer-link-button"
                  >
                    News
                  </UnderlineButton>
                </li>
              </ul>
            </div>
          </div>

          <div className="footer-right">
            <div className="footer-section">
              <ul className="contact-info">
                <li>
                  <UnderlineButton
                    onClick={handleLocationClick}
                    className="contact-link-button"
                    textClassName="bodytext-6--no-margin"
                  >
                    Location
                  </UnderlineButton>
                </li>
                <li>
                  <UnderlineButton
                    onClick={handleContactClick}
                    className="contact-link-button"
                    textClassName="bodytext-6--no-margin"
                  >
                    Contact us
                  </UnderlineButton>
                </li>
                <li>
                  <UnderlineButton
                    onClick={handleBookAppointmentClick}
                    className="contact-link-button"
                    textClassName="bodytext-6--no-margin"
                  >
                    Book an appointment
                  </UnderlineButton>
                </li>
                <li>
                  <a href="mailto:support@mirrorfuturediamond.com">
                    <UnderlineButton
                      className="contact-link-button"
                      textClassName="bodytext-6--no-margin"
                    >
                      support@mirrorfuturediamond.com
                    </UnderlineButton>
                  </a>
                </li>
                <li>
                  <a href="tel:+97.130.0938">
                    <UnderlineButton
                      className="contact-link-button"
                      textClassName="bodytext-6--no-margin"
                    >
                      +97.130.0938
                    </UnderlineButton>
                  </a>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <div className="footer-center" ref={footerCenterRef}>
          <div className="newsletter-section">
            <p className="bodytext-4--no-margin newsletter-tagline">
              COLLECT. REFLECT. AWAKEN
            </p>
            <h2 className="heading-1--no-margin newsletter-title">
              JOIN OUR
              <br />
              MIRROR PASSPORT
            </h2>
            <p className="bodytext-4--no-margin newsletter-subtitle">
              Your gateway into the Mirrorverse where your journey, your taste,
              and your stories shape the luxury you receive.
            </p>
            <div className="newsletter-buttons">
              <ShineGlassButton
                onClick={() => {}}
                className="newsletter-btn-explore"
                theme="footer"
              >
                Explore more
              </ShineGlassButton>
              <GlassThemeButton
                onClick={() => {}}
                className="newsletter-btn-signin"
                theme="spec_dark"
              >
                <span className="bodytext-6--no-margin">Sign in</span>
              </GlassThemeButton>
            </div>
          </div>
        </div>

        <div className="footer-bottom" ref={footerBottomRef}>
          <div className="footer-bottom-center">
            <div className="social-icons">
              <a
                href="https://www.facebook.com/mirrorfuturediamond"
                className="social-icon facebook"
                target="_blank"
                rel="noopener noreferrer"
              >
                <img src={fbIcon} alt="Facebook" />
              </a>
              <a
                href="https://www.instagram.com/mirrorfuturediamond/"
                className="social-icon instagram"
                target="_blank"
                rel="noopener noreferrer"
              >
                <img src={instaIcon} alt="Instagram" />
              </a>
              <a
                href="https://www.tiktok.com/@mirrorfuturediamond?lang=vi-VN"
                className="social-icon tiktok"
                target="_blank"
                rel="noopener noreferrer"
              >
                <img src={tiktokIcon} alt="TikTok" />
              </a>
            </div>
          </div>

          {/* Footer Bottom Left & Right - Inside footer-bottom */}
          <div className="footer-bottom-row">
            <div className="footer-bottom-left">
              <p className="bodytext-6--no-margin">
                All rights reserved © 2025
              </p>
            </div>

            <div className="footer-bottom-right">
              <a className="bodytext-6--no-margin legal-link">Legal mentions</a>
            </div>
          </div>
        </div>
      </div>

      {/* Booking Modal */}
      <BookingModalV3
        isOpen={isBookingModalOpen}
        onClose={() => setIsBookingModalOpen(false)}
      />
    </footer>
  );
};

export default Footer;
