import "./NavbarV4.css";
import { useState, useRef, useEffect, useCallback } from "react";
import MirrorLogo from "@assets/images/Mirror_Logo_new.svg";
import MirrorLogoEvent from "@assets/Mirror_Logo.svg";
import DmmLogo from "@assets/LOGO DOC MONG MO.svg";
import { useAuth } from "@/context/AuthContext";
import { useNavigate, useLocation } from "react-router-dom";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";
import UnderlineButton from "@/components/common/button/UnderlineButton";
import GlassThemeButton from "@/components/common/button/GlassThemeButton";
import ShineGlassButton from "@/components/common/button/ShineGlassButton";
import BookingModalV3 from "@/components/booking/BookingModalV3";
import { ROUTES } from "@/constants/routes";
import { useNavbarTheme } from "@/hooks/useNavbarTheme";
import useEventStore from "@/store/useEventStore";
import { clearUserSession } from "@/pages/Event/EventLoginPage";

export default function NavbarV4({ logoOnly = false, showDmmLogo = false }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 425);
  const [isTablet, setIsTablet] = useState(
    window.innerWidth > 425 && window.innerWidth <= 1023
  );
  const [isInScrollContainer, setIsInScrollContainer] = useState(false);
  const [isInIntroSubmitSection, setIsInIntroSubmitSection] = useState(false);
  const [isAtTop, setIsAtTop] = useState(true); // Track if at top of page
  const [isMenuClosing, setIsMenuClosing] = useState(false); // Track when overlay is animating out
  const [isMenuOpening, setIsMenuOpening] = useState(false); // Track when menu is opening (for animation)
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false); // Track booking modal
  const logoRef = useRef(null);
  const { isAuthenticated, user, logout } = useAuth();

  // Event store for event logout
  const { user: eventUser, reset: resetEventStore } = useEventStore();

  // Get current navbar theme from hook
  const { theme: navbarTheme } = useNavbarTheme();

  // Check if current page is home, welcome, or immersive showroom (pages with white navbar)
  const isHomePage =
    location.pathname === ROUTES.HOME_PAGE ||
    location.pathname === ROUTES.HOME ||
    location.pathname === ROUTES.WELCOME ||
    location.pathname === ROUTES.IMMERSIVE_SHOWROOM;

  // Check if current page is immersive showroom (for always white logo without blend)
  const isImmersiveShowroomPage = location.pathname === ROUTES.IMMERSIVE_SHOWROOM;

  // Check if current page is Milan submission page or Submit Success page
  const isMilanPage = location.pathname.startsWith(ROUTES.MILAN_SUBMIT);

  // Check if current page is submit page (not success page)
  const isSubmitPage = location.pathname === ROUTES.MILAN_SUBMIT;

  // Check if should hide menu, account, and immersive button (Milan, Immersive Showroom, or logoOnly mode)
  const shouldHideButtons =
    logoOnly || isMilanPage || location.pathname === ROUTES.IMMERSIVE_SHOWROOM;

  // Check if on event pages (the-muse-of-love-grown routes)
  const isEventPage = location.pathname.startsWith(ROUTES.EVENT_GUIDE);

  // Check if on event pages after login (not guide or login page)
  const isEventPageAfterLogin = isEventPage &&
    location.pathname !== ROUTES.EVENT_GUIDE &&
    location.pathname !== ROUTES.EVENT_LOGIN;

  // Show event logout button if on event pages after login and user is logged in
  const showEventLogout = isEventPageAfterLogin && !!eventUser;

  // Check if logo click should be disabled (Milan, Immersive Showroom, but NOT event pages)
  const shouldDisableLogoClick = shouldHideButtons && !isEventPage;

  // Helper function to close menu with fade out animation
  const closeMenuWithAnimation = useCallback(() => {
    setIsMenuClosing(true);
    setIsMenuOpening(false); // Stop opening animation
    setTimeout(() => {
      setIsMenuOpen(false);
      setIsMenuClosing(false);
    }, 300); // Match animation duration
  }, []);

  useEffect(() => {
    // Initialize optimized transition system
    optimizedTransitionUtils.init();
  }, []);

  // Set initial state for homepage on mount
  useEffect(() => {
    if (isHomePage) {
      // On homepage, when at top of page, we should be in scroll-container
      setIsInScrollContainer(true);
    } else {
      setIsInScrollContainer(false);
    }
  }, [isHomePage]);

  // Add scroll detection to properly detect when user is within scroll-container section
  // Track mobile and tablet state
  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth <= 425);
      setIsTablet(window.innerWidth > 425 && window.innerWidth <= 1023);
    };

    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  // Track scroll position to show horizontal menu at top
  useEffect(() => {
    const handleScroll = () => {
      const scrollY = window.scrollY || window.pageYOffset;
      setIsAtTop(scrollY < 50); // Consider "at top" if scrolled less than 50px
    };

    // Initial check
    handleScroll();

    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Close menu when clicking outside
  useEffect(() => {
    // Only add listener when menu is open
    if (!isMenuOpen) return;

    const handleClickOutside = (event) => {
      if (
        !event.target.closest(".menu-v4-container") &&
        !event.target.closest(".menu-v4-popup") &&
        !event.target.closest(".mobile-v4-menu-overlay")
      ) {
        if (!isMenuClosing) {
          closeMenuWithAnimation();
        }
      }
    };

    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, [isMenuOpen, isMenuClosing, closeMenuWithAnimation]);

  // Reset menu state when route changes
  useEffect(() => {
    setIsMenuOpen(false);
    setIsMenuClosing(false);
    setIsMenuOpening(false);
  }, [location.pathname]);

  // Clear menu opening state after animation completes
  useEffect(() => {
    if (isMenuOpening) {
      // Clear opening state after animation completes (2s)
      const timer = setTimeout(() => {
        setIsMenuOpening(false);
      }, 2000);

      return () => clearTimeout(timer);
    }
  }, [isMenuOpening]);

  // Prevent body scroll when menu is open (all devices)
  useEffect(() => {
    if (isMenuOpen) {
      // Save current scroll position
      const scrollY = window.scrollY;
      document.body.style.position = "fixed";
      document.body.style.top = `-${scrollY}px`;
      document.body.style.width = "100%";
      document.body.style.overflow = "hidden";
    } else {
      // Restore scroll position
      const scrollY = document.body.style.top;
      document.body.style.position = "";
      document.body.style.top = "";
      document.body.style.width = "";
      document.body.style.overflow = "";
      if (scrollY) {
        window.scrollTo(0, parseInt(scrollY || "0") * -1);
      }
    }
  }, [isMenuOpen]);

  useEffect(() => {
    if (!isHomePage) return;

    const handleScroll = () => {
      const scrollContainer = document.querySelector(".scroll-container");
      if (!scrollContainer) return;

      const rect = scrollContainer.getBoundingClientRect();
      // Check if we're within the scroll-container bounds:
      // - At top of page: rect.top = 0, should be true
      // - Scrolling within: rect.top < 0 but rect.bottom > 0, should be true
      // - Past scroll-container: rect.bottom < 0, should be false
      const isViewportInScrollContainer = rect.top <= 50 && rect.bottom >= 0;

      // Debug log

      setIsInScrollContainer(isViewportInScrollContainer);
    };

    // Initial check with delay to ensure DOM is ready
    const initialCheck = () => {
      setTimeout(() => {
        handleScroll();
      }, 100);
    };

    window.addEventListener("scroll", handleScroll);
    // Run multiple times to catch initial state
    handleScroll();
    initialCheck();

    // Also check on window load
    window.addEventListener("load", handleScroll);

    return () => {
      window.removeEventListener("scroll", handleScroll);
      window.removeEventListener("load", handleScroll);
    };
  }, [isHomePage]);

  // Detect when IntroSubmit section is in view (for immersive showroom page)
  useEffect(() => {
    const isImmersiveShowroom = location.pathname === ROUTES.IMMERSIVE_SHOWROOM;
    if (!isImmersiveShowroom) {
      setIsInIntroSubmitSection(false);
      return;
    }

    const handleScroll = () => {
      const introSubmitSection = document.querySelector(
        ".intro-submit-section"
      );
      if (!introSubmitSection) return;

      const rect = introSubmitSection.getBoundingClientRect();
      const windowHeight = window.innerHeight;

      // Only trigger when section top is very close to or past the top of viewport
      // This ensures logo only changes when user has actually scrolled to the section
      const isInView = rect.top <= 50 && rect.bottom > windowHeight * 0.2;

      setIsInIntroSubmitSection(isInView);
    };

    window.addEventListener("scroll", handleScroll);
    handleScroll(); // Initial check

    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  }, [location.pathname]);

  const performTransition = async (route, options = {}) => {
    // Close menu immediately before transition to prevent animation loop
    // Set all states to false instantly (no animation)
    setIsMenuOpen(false);
    setIsMenuClosing(false);
    setIsMenuOpening(false);

    // Wait for DOM to update and remove all menu classes before capturing originalContent
    await new Promise((resolve) => setTimeout(resolve, 50));

    // Sử dụng optimized transition system
    await optimizedTransitionUtils.transitionToRoute(navigate, route, options);
  };

  const handleLogoClick = async () => {
    // Disable logo click on Milan and Immersive Showroom pages
    if (shouldDisableLogoClick) {
      return;
    }

    // On event pages, navigate to event guide
    if (isEventPage) {
      if (location.pathname === ROUTES.EVENT_GUIDE) {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      } else {
        navigate(ROUTES.EVENT_GUIDE);
      }
      return;
    }

    if (window.location.pathname === ROUTES.HOME_PAGE) {
      window.scrollTo(0, 0);
      setTimeout(() => {
        window.location.reload();
      }, 0);
    } else {
      sessionStorage.setItem("scrollToTop", "true");
      await performTransition(ROUTES.HOME_PAGE);
    }
  };

  const handleProductsClick = async () => {
    if (window.location.pathname === ROUTES.COLLECTIONS) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.COLLECTIONS);
  };

  const handleServicesClick = async () => {
    if (window.location.pathname === ROUTES.SERVICES) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.SERVICES);
  };

  const handleSupportClick = async () => {
    if (window.location.pathname === ROUTES.SUPPORT) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.SUPPORT);
  };

  const handleAboutClick = async () => {
    if (window.location.pathname === ROUTES.ABOUT) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.ABOUT);
  };

  const handleNewsClick = async () => {
    if (window.location.pathname === ROUTES.NEWS) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.NEWS);
  };

  const handleImmersiveShowroomClick = async () => {
    if (window.location.pathname === ROUTES.IMMERSIVE_SHOWROOM) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.IMMERSIVE_SHOWROOM);
  };

  const handleContactClick = async () => {
    if (window.location.pathname === ROUTES.CONTACT) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.CONTACT);
  };

  // Helper function to check if user is admin
  const isUserAdmin = () => {
    return user && user.roles && user.roles.includes("ADMIN");
  };

  // Helper function to check if user is vendor
  const isUserVendor = () => {
    return user && user.roles && user.roles.includes("VENDOR");
  };

  // Helper function to check if user is designer
  const isUserDesigner = () => {
    return user && user.roles && user.roles.includes("DESIGNER");
  };

  const handleProfileClick = async (tab = "My Passport") => {
    // If already on profile page, force navigation with replace to update state
    if (location.pathname === ROUTES.USER_PROFILE) {
      navigate(ROUTES.USER_PROFILE, {
        state: { activeTab: tab, timestamp: Date.now() },
        replace: true,
      });
    } else {
      await performTransition(ROUTES.USER_PROFILE, {
        state: { activeTab: tab, timestamp: Date.now() },
      });
    }
  };

  const handleAdminDashboardClick = async () => {
    await performTransition(ROUTES.DASHBOARD_ADMIN);
  };

  const handleVendorDashboardClick = async () => {
    await performTransition(ROUTES.DASHBOARD_VENDOR);
  };

  const handleDesignerDashboardClick = async () => {
    await performTransition(ROUTES.DASHBOARD_DESIGNER);
  };

  const handleLogoutClick = () => {
    logout();
  };

  // Event logout - only resets event store, not Mirror account
  const handleEventLogoutClick = () => {
    clearUserSession(); // Clear localStorage + sign out from Google
    resetEventStore();  // Reset Zustand store
    navigate(ROUTES.EVENT_LOGIN);
  };

  const handleLoginClick = async () => {
    navigate(ROUTES.AUTH_LOGIN);
    if (window.location.pathname === ROUTES.AUTH_LOGIN) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.AUTH_LOGIN);
  };

  const handleLocationClick = async () => {
    if (window.location.pathname === ROUTES.LOCATIONS) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.LOCATIONS);
  };

  const handleAccountMenuClick = async () => {
    // Check if user is authenticated
    const hasToken = localStorage.getItem("accessToken");
    const isLoggedIn = (isAuthenticated && user) || hasToken;

    if (isLoggedIn) {
      // If logged in, navigate to profile
      if (window.location.pathname === ROUTES.USER_PROFILE) {
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
      }
      sessionStorage.setItem("scrollToTop", "true");
      await performTransition(ROUTES.USER_PROFILE);
    } else {
      // If not logged in, navigate to login
      if (window.location.pathname === ROUTES.AUTH_LOGIN) {
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
      }
      sessionStorage.setItem("scrollToTop", "true");
      await performTransition(ROUTES.AUTH_LOGIN);
    }
  };

  return (
    <>
      {/* Navbar Liquid Glass Background */}
      <div
        className={`navbar-v4-glass-background navbar-v4-theme-${navbarTheme} ${
          isMenuOpen ? "expanded" : ""
        } ${isMenuOpen && !isMobile && !isTablet ? "menu-expanded" : ""}`}
      >
        <div className="liquidGlass-v4-effect"></div>
        <div className="liquidGlass-v4-tint"></div>
        <div className="liquidGlass-v4-shine"></div>
      </div>

      {/* SVG Filter for Liquid Glass Distortion */}
      <svg style={{ display: "none" }}>
        <filter
          id="navbar-glass-distortion"
          x="0%"
          y="0%"
          width="100%"
          height="100%"
          filterUnits="objectBoundingBox"
        >
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.000 0.000"
            numOctaves="1"
            seed="1"
            result="turbulence"
          />

          <feComponentTransfer in="turbulence" result="mapped">
            <feFuncR type="gamma" amplitude="1" exponent="10" offset="0.5" />
            <feFuncG type="gamma" amplitude="0" exponent="1" offset="0" />
            <feFuncB type="gamma" amplitude="0" exponent="1" offset="0.5" />
          </feComponentTransfer>

          <feGaussianBlur in="turbulence" stdDeviation="4" result="softMap" />

          <feSpecularLighting
            in="softMap"
            surfaceScale="3"
            specularConstant="0.9"
            specularExponent="100"
            lightingColor="white"
            result="specLight"
          >
            <fePointLight x="-200" y="-200" z="300" />
          </feSpecularLighting>

          <feComposite
            in="specLight"
            operator="arithmetic"
            k1="0"
            k2="1"
            k3="1"
            k4="0"
            result="litImage"
          />

          <feDisplacementMap
            in="SourceGraphic"
            in2="softMap"
            scale="60"
            xChannelSelector="R"
            yChannelSelector="G"
          />
        </filter>
      </svg>

      {/* Mobile/Tablet menu overlay - only on mobile/tablet, not desktop */}
      {(isMobile || isTablet) && (isMenuOpen || isMenuClosing) && (
        <div
          className={`mobile-v4-menu-overlay ${isMenuClosing ? "closing" : ""}`}
          onClick={closeMenuWithAnimation}
        />
      )}

      {/* DIV RIÊNG CHỈ DÀNH CHO LOGO BLEND */}
      <div
        className={`logo-v4-fixed-container navbar-v4-theme-${navbarTheme} ${
          isHomePage && !isMenuOpen ? "no-blend" : ""
        } ${
          isHomePage && isInScrollContainer && !isMenuOpen ? "scrolled" : ""
        } ${shouldDisableLogoClick ? "no-click" : ""} ${
          isSubmitPage ? "submit-page-logo" : ""
        } ${isInIntroSubmitSection ? "intro-submit-logo" : ""} ${
          isImmersiveShowroomPage ? "immersive-showroom-logo" : ""
        } ${isBookingModalOpen ? "above-modal" : ""} ${
          showDmmLogo ? "collab-mode" : ""
        } ${logoOnly && !showDmmLogo ? "event-logo" : ""}`}
        onClick={handleLogoClick}
      >
        <img
          ref={logoRef}
          src={logoOnly && !showDmmLogo ? MirrorLogoEvent : MirrorLogo}
          alt="Mirror Logo"
          className="navbar-v4-logo-svg"
        />
        {showDmmLogo && (
          <>
            <svg className="navbar-v4-collab-x" xmlns="http://www.w3.org/2000/svg" width="9" height="9" viewBox="0 0 9 9" fill="none">
              <path d="M0.5 0.5L8.5 8.5" stroke="currentColor" strokeLinecap="round"/>
              <path d="M8.5 0.5L0.5 8.5" stroke="currentColor" strokeLinecap="round"/>
            </svg>
            <img
              src={DmmLogo}
              alt="Dốc Mộng Mơ Logo"
              className="navbar-v4-dmm-logo"
            />
          </>
        )}
      </div>

      {/* Event Logout Button - Fixed top right on event pages after login */}
      {showEventLogout && (
        <div className={`event-logout-v4-container navbar-v4-theme-${navbarTheme}`}>
          <UnderlineButton onClick={handleEventLogoutClick}>
            <span className="event-logout-content">
              Đăng xuất
              <svg className="event-logout-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M5.8511 2.3999H3.38051C3.00609 2.3999 2.647 2.5474 2.38225 2.80995C2.11749 3.0725 1.96875 3.4286 1.96875 3.7999V12.1999C1.96875 12.5712 2.11749 12.9273 2.38225 13.1899C2.647 13.4524 3.00609 13.5999 3.38051 13.5999H5.8511M6.02656 7.9999H14.0266M14.0266 7.9999L10.9698 4.7999M14.0266 7.9999L10.9698 11.1999" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </span>
          </UnderlineButton>
        </div>
      )}

      {/* MENU VÀ ACCOUNT LINK VỚI BLEND MODE */}
      {!shouldHideButtons && (
        <div
          className={`menu-v4-fixed-container navbar-v4-theme-${navbarTheme} ${
            isHomePage && !isMenuOpen ? "no-blend" : ""
          } ${
            isHomePage && isInScrollContainer && !isMenuOpen ? "scrolled" : ""
          } ${isAtTop && !isMobile && !isTablet ? "at-top" : ""}`}
        >
          {/* Menu button always visible */}
          <div
            className={`menu-v4-container ${isMenuOpen ? "menu-v4-open" : ""} ${
              isMenuClosing ? "menu-v4-closing" : ""
            } ${!isAtTop && !isMobile && !isTablet ? "scrolled-mode" : ""}`}
          >
            <div
              className="menu-v4-button"
              onClick={(e) => {
                // Allow click on all devices and all scroll positions
                e.stopPropagation();
                if (isMenuOpen) {
                  closeMenuWithAnimation();
                } else {
                  setIsMenuOpen(true);
                  setIsMenuOpening(true);
                }
              }}
            >
              <div className="menu-v4-icon-container">
                <svg className="menu-v4-icon" viewBox="0 0 16 22" fill="none">
                  <path d="M16 8L0 8" stroke="currentColor" />
                  <path d="M16 14L0 14" stroke="currentColor" />
                </svg>
                <svg
                  className="menu-v4-icon-close"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                >
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </div>
              <span className="menu-v4-text">
                <UnderlineButton>Menu</UnderlineButton>
              </span>
            </div>
            {/* Popup menu */}
            <div
              className={`menu-v4-popup ${
                isMenuOpen || isMenuClosing ? "active" : ""
              } ${isMenuClosing ? "closing" : ""} ${
                isMenuOpening ? "opening" : ""
              }`}
            >
              <div className="menu-v4-groups">
                <ul className="menu-v4-list">
                  <li
                    className={
                      location.pathname === ROUTES.HOME_PAGE ||
                      location.pathname === ROUTES.HOME
                        ? "active"
                        : ""
                    }
                    onMouseEnter={() =>
                      optimizedTransitionUtils.prefetch(ROUTES.HOME_PAGE)
                    }
                  >
                    <UnderlineButton
                      textClassName="bodytext-6--no-margin"
                      onClick={handleLogoClick}
                    >
                      Home
                    </UnderlineButton>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.COLLECTIONS ? "active" : ""
                    }
                    onMouseEnter={() =>
                      optimizedTransitionUtils.prefetch(ROUTES.COLLECTIONS)
                    }
                  >
                    <UnderlineButton
                      textClassName="bodytext-6--no-margin"
                      onClick={handleProductsClick}
                    >
                      Products
                    </UnderlineButton>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.SERVICES ? "active" : ""
                    }
                    onMouseEnter={() =>
                      optimizedTransitionUtils.prefetch(ROUTES.SERVICES)
                    }
                  >
                    <UnderlineButton
                      textClassName="bodytext-6--no-margin"
                      onClick={handleServicesClick}
                    >
                      Services
                    </UnderlineButton>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.SUPPORT ? "active" : ""
                    }
                    onMouseEnter={() =>
                      optimizedTransitionUtils.prefetch(ROUTES.SUPPORT)
                    }
                  >
                    <UnderlineButton
                      textClassName="bodytext-6--no-margin"
                      onClick={handleSupportClick}
                    >
                      Support
                    </UnderlineButton>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.ABOUT ? "active" : ""
                    }
                    onMouseEnter={() =>
                      optimizedTransitionUtils.prefetch(ROUTES.ABOUT)
                    }
                  >
                    <UnderlineButton
                      textClassName="bodytext-6--no-margin"
                      onClick={handleAboutClick}
                    >
                      About
                    </UnderlineButton>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.NEWS ? "active" : ""
                    }
                    onMouseEnter={() =>
                      optimizedTransitionUtils.prefetch(ROUTES.NEWS)
                    }
                  >
                    <UnderlineButton
                      textClassName="bodytext-6--no-margin"
                      onClick={handleNewsClick}
                    >
                      News
                    </UnderlineButton>
                  </li>
                  <li
                    className={`immersive-v4-menu-item ${
                      location.pathname === ROUTES.IMMERSIVE_SHOWROOM
                        ? "active"
                        : ""
                    }`}
                    onMouseEnter={() =>
                      optimizedTransitionUtils.prefetch(
                        ROUTES.IMMERSIVE_SHOWROOM
                      )
                    }
                  >
                    <UnderlineButton
                      textClassName="bodytext-6--no-margin"
                      onClick={handleImmersiveShowroomClick}
                    >
                      Immersive Showroom
                    </UnderlineButton>
                  </li>
                </ul>
                <div className="menu-v4-divider"></div>
                <ul className="menu-v4-list">
                  <li
                    className={
                      location.pathname === ROUTES.CONTACT ? "active" : ""
                    }
                    onMouseEnter={() =>
                      optimizedTransitionUtils.prefetch(ROUTES.CONTACT)
                    }
                  >
                    <div className="menu-v4-button-with-icon">
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        width="14"
                        height="14"
                        viewBox="0 0 14 14"
                        fill="none"
                      >
                        <path
                          d="M10.7327 8.69192L12.6352 10.5944C12.9122 10.8714 12.9122 11.3204 12.6352 11.5974C11.1378 13.0948 8.76726 13.2633 7.07315 11.9927L6.21159 11.3465C4.61423 10.1485 3.19525 8.72952 1.99723 7.13216L1.35106 6.27059C0.0804796 4.57649 0.248952 2.20591 1.74634 0.708518C2.02331 0.431555 2.47235 0.431554 2.74932 0.708518L4.65183 2.61104C5.04236 3.00156 5.04236 3.63473 4.65183 4.02525L4.01474 4.66234C3.88852 4.78856 3.85723 4.98139 3.93706 5.14105C4.86002 6.98696 6.35679 8.48373 8.2027 9.40669C8.36236 9.48652 8.55519 9.45523 8.68141 9.32901L9.3185 8.69192C9.70903 8.30139 10.3422 8.30139 10.7327 8.69192Z"
                          stroke="currentColor"
                          strokeLinejoin="round"
                        />
                      </svg>
                      <UnderlineButton
                        textClassName="bodytext-6--no-margin"
                        onClick={handleContactClick}
                      >
                        Contact
                      </UnderlineButton>
                    </div>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.LOCATIONS ? "active" : ""
                    }
                    onMouseEnter={() =>
                      optimizedTransitionUtils.prefetch(ROUTES.LOCATIONS)
                    }
                  >
                    <div className="menu-v4-button-with-icon">
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        width="12"
                        height="16"
                        viewBox="0 0 12 16"
                        fill="none"
                      >
                        <path
                          fillRule="evenodd"
                          clipRule="evenodd"
                          d="M2.08725 2.0493C3.17003 1.01643 4.47427 0.5 6 0.5C7.52573 0.5 8.82382 1.01056 9.8943 2.03169C10.9648 3.05282 11.5 4.29107 11.5 5.74648C11.5 6.47418 11.3093 7.30751 10.9279 8.24648C10.5464 9.18545 10.085 10.0657 9.54362 10.8873C9.00224 11.7089 8.46701 12.4777 7.93792 13.1937C7.40883 13.9096 6.95973 14.4789 6.5906 14.9014L6 15.5C5.85235 15.3357 5.65548 15.1185 5.4094 14.8486C5.16331 14.5786 4.72036 14.0387 4.08054 13.2289C3.44071 12.419 2.88087 11.6326 2.40101 10.8697C1.92114 10.1068 1.48434 9.24414 1.0906 8.28169C0.696866 7.31924 0.5 6.47418 0.5 5.74648C0.5 4.29107 1.02908 3.05869 2.08725 2.0493Z"
                          stroke="currentColor"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                        <path
                          d="M6.84615 5.91667C6.84615 6.3769 6.46732 6.75 6 6.75C5.53268 6.75 5.15385 6.3769 5.15385 5.91667C5.15385 5.45643 5.53268 5.08333 6 5.08333C6.46732 5.08333 6.84615 5.45643 6.84615 5.91667Z"
                          stroke="currentColor"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                      <UnderlineButton
                        textClassName="bodytext-6--no-margin"
                        onClick={handleLocationClick}
                      >
                        Find your nearest location
                      </UnderlineButton>
                    </div>
                  </li>
                </ul>

                {/* PROFILE section - only show when authenticated */}
                {isAuthenticated && (
                  <>
                    <div className="menu-v4-divider"></div>
                    <ul className="menu-v4-list menu-v4-list-profile">
                      <li>
                        <UnderlineButton
                          textClassName="bodytext-6--no-margin"
                          onClick={() => handleProfileClick("My Passport")}
                        >
                          My Passport
                        </UnderlineButton>
                      </li>
                      <li>
                        <UnderlineButton
                          textClassName="bodytext-6--no-margin"
                          onClick={() => handleProfileClick("Orders")}
                        >
                          My Orders
                        </UnderlineButton>
                      </li>
                      <li>
                        <UnderlineButton
                          textClassName="bodytext-6--no-margin"
                          onClick={() => handleProfileClick("Services")}
                        >
                          My Services
                        </UnderlineButton>
                      </li>
                      <li>
                        <UnderlineButton
                          textClassName="bodytext-6--no-margin"
                          onClick={() => handleProfileClick("Wishlist")}
                        >
                          My Wishlist
                        </UnderlineButton>
                      </li>

                      {isUserAdmin() && (
                        <li>
                          <UnderlineButton
                            textClassName="bodytext-6--no-margin"
                            onClick={handleAdminDashboardClick}
                          >
                            Admin Dashboard
                          </UnderlineButton>
                        </li>
                      )}

                      {isUserVendor() && (
                        <li>
                          <UnderlineButton
                            textClassName="bodytext-6--no-margin"
                            onClick={handleVendorDashboardClick}
                          >
                            Vendor Dashboard
                          </UnderlineButton>
                        </li>
                      )}

                      {isUserDesigner() && (
                        <li>
                          <UnderlineButton
                            textClassName="bodytext-6--no-margin"
                            onClick={handleDesignerDashboardClick}
                          >
                            Designer Dashboard
                          </UnderlineButton>
                        </li>
                      )}

                      <li className="logout-v4-item">
                        <UnderlineButton
                          textClassName="bodytext-6--no-margin"
                          onClick={handleLogoutClick}
                        >
                          Log out
                        </UnderlineButton>
                      </li>
                    </ul>
                  </>
                )}
              </div>

              {/* Glass Theme Buttons at bottom - outside of groups */}
              <div
                className="menu-v4-bottom-buttons"
                style={{ mixBlendMode: "normal", isolation: "isolate" }}
              >
                {/* Mirror Partners Gate - shows Staff Portal for staff, Vendor/Designer Portal for partners, or default text */}
                {isAuthenticated && user?.roles?.some(role =>
                  ["ADMIN", "SUPER_ADMIN", "IT_ADMIN", "PRODUCTION_OPS", "SALES_CUSTOMER_OPS", "FINANCE", "MARKETING", "CREATIVE_DESIGN", "LEGAL"].includes(role)
                ) ? (
                  <GlassThemeButton
                    theme="light"
                    onClick={() => {
                      setIsMenuOpen(false);
                      navigate(ROUTES.DASHBOARD_ADMIN);
                    }}
                  >
                    <span className="bodytext-6--no-margin">Staff Portal</span>
                  </GlassThemeButton>
                ) : isAuthenticated && user?.roles?.includes("VENDOR") ? (
                  <GlassThemeButton
                    theme="light"
                    onClick={() => {
                      setIsMenuOpen(false);
                      navigate(ROUTES.DASHBOARD_VENDOR);
                    }}
                  >
                    <span className="bodytext-6--no-margin">Vendor Portal</span>
                  </GlassThemeButton>
                ) : isAuthenticated && user?.roles?.includes("DESIGNER") ? (
                  <GlassThemeButton
                    theme="light"
                    onClick={() => {
                      setIsMenuOpen(false);
                      navigate(ROUTES.DASHBOARD_DESIGNER);
                    }}
                  >
                    <span className="bodytext-6--no-margin">Designer Portal</span>
                  </GlassThemeButton>
                ) : (
                  <ShineGlassButton
                    theme="light"
                    onClick={() => {
                      // TODO: Add Mirror Partners Gate navigation
                      console.log("Enter Mirror Partners Gate clicked");
                    }}
                  >
                    Enter Mirror Partners Gate
                  </ShineGlassButton>
                )}
                <GlassThemeButton
                  theme="spec_light"
                  onClick={() => {
                    setIsBookingModalOpen(true);
                  }}
                >
                  <span className="bodytext-6--no-margin">Explore Mirror Passport</span>
                </GlassThemeButton>
              </div>
            </div>
          </div>

          {/* Horizontal menu items shown at top on desktop */}
          {!isMobile && !isTablet && (
            <ul
              className={`menu-v4-horizontal-list ${
                isMenuOpen ? "menu-open" : ""
              } ${!isAtTop ? "scrolled" : ""}`}
            >
              <li
                className={
                  location.pathname === ROUTES.HOME_PAGE ||
                  location.pathname === ROUTES.HOME
                    ? "active"
                    : ""
                }
                onMouseEnter={() =>
                  optimizedTransitionUtils.prefetch(ROUTES.HOME_PAGE)
                }
              >
                <UnderlineButton onClick={handleLogoClick}>
                  Home
                </UnderlineButton>
              </li>
              <li
                className={
                  location.pathname === ROUTES.COLLECTIONS ? "active" : ""
                }
                onMouseEnter={() =>
                  optimizedTransitionUtils.prefetch(ROUTES.COLLECTIONS)
                }
              >
                <UnderlineButton onClick={handleProductsClick}>
                  Products
                </UnderlineButton>
              </li>
              <li
                className={
                  location.pathname === ROUTES.SERVICES ? "active" : ""
                }
                onMouseEnter={() =>
                  optimizedTransitionUtils.prefetch(ROUTES.SERVICES)
                }
              >
                <UnderlineButton onClick={handleServicesClick}>
                  Services
                </UnderlineButton>
              </li>
              <li
                className={location.pathname === ROUTES.SUPPORT ? "active" : ""}
                onMouseEnter={() =>
                  optimizedTransitionUtils.prefetch(ROUTES.SUPPORT)
                }
              >
                <UnderlineButton onClick={handleSupportClick}>
                  Support
                </UnderlineButton>
              </li>
              <li
                className={location.pathname === ROUTES.ABOUT ? "active" : ""}
                onMouseEnter={() =>
                  optimizedTransitionUtils.prefetch(ROUTES.ABOUT)
                }
              >
                <UnderlineButton onClick={handleAboutClick}>
                  About
                </UnderlineButton>
              </li>
              <li
                className={location.pathname === ROUTES.NEWS ? "active" : ""}
                onMouseEnter={() =>
                  optimizedTransitionUtils.prefetch(ROUTES.NEWS)
                }
              >
                <UnderlineButton onClick={handleNewsClick}>
                  News
                </UnderlineButton>
              </li>
            </ul>
          )}
        </div>
      )}

      {!shouldHideButtons && (
        <div
          className={`account-v4-fixed-container navbar-v4-theme-${navbarTheme} ${
            isHomePage && !isMenuOpen ? "no-blend" : ""
          } ${
            isHomePage && isInScrollContainer && !isMenuOpen ? "scrolled" : ""
          }`}
        >
          <div className="account-v4-container">
            {isAuthenticated ? (
              // Logged in: Show "My Passport" without dropdown
              <div className="account-v4-button-wrapper">
                <UnderlineButton onClick={handleAccountMenuClick}>
                  My Passport
                </UnderlineButton>
              </div>
            ) : (
              // Not logged in: Show "Login" button
              <div className="account-v4-button-wrapper">
                <UnderlineButton onClick={handleLoginClick}>
                  Login
                </UnderlineButton>
              </div>
            )}
          </div>
        </div>
      )}

      {/* IMMERSIVE BUTTON - chỉ glassmorphism */}
      {!shouldHideButtons && (
        <>
          <div className="immersive-v4-fixed-container">
            <button
              className="immersive-v4-button"
              onClick={() => setIsBookingModalOpen(true)}
            ></button>
          </div>

          {/* BORDER RIÊNG BIỆT - chỉ mix-blend-mode */}
          <div
            className={`immersive-v4-border-container navbar-v4-theme-${navbarTheme} ${
              isHomePage && !isMenuOpen ? "no-blend" : ""
            } ${
              isHomePage && isInScrollContainer && !isMenuOpen ? "scrolled" : ""
            }`}
          >
            <div className="immersive-v4-border"></div>
          </div>

          {/* TEXT RIÊNG BIỆT - chỉ mix-blend-mode */}
          <div
            className={`immersive-v4-text-container navbar-v4-theme-${navbarTheme} ${
              isHomePage && !isMenuOpen ? "no-blend" : ""
            } ${
              isHomePage && isInScrollContainer && !isMenuOpen ? "scrolled" : ""
            }`}
            onClick={() => setIsBookingModalOpen(true)}
            style={{ cursor: "pointer" }}
          >
            <span className="immersive-v4-text bodytext-6--no-margin">
              Book an Appointment
            </span>
          </div>
        </>
      )}

      {/* Booking Modal */}
      <BookingModalV3
        isOpen={isBookingModalOpen}
        onClose={() => setIsBookingModalOpen(false)}
      />
    </>
  );
}
