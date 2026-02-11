import "./NavbarV3.css";
import { useState, useRef, useEffect, useCallback } from "react";
import MirrorLogo from "@assets/images/Mirror_Logo_new.svg";
import { useAuth } from "@/context/AuthContext";
import { useNavigate, useLocation } from "react-router-dom";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";
import UnderlineButton from "@/components/common/button/UnderlineButton";
import { ROUTES } from "@/constants/routes";
import { useImmersiveModal } from "@/contexts/ImmersiveModalContext";
import { useNavbarTheme } from "@/hooks/useNavbarTheme";
import DiamondFallingEffect from "@/utils/diamondFallingEffect";

export default function NavbarV3() {
  const navigate = useNavigate();
  const location = useLocation();
  const { openModal } = useImmersiveModal();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isMenuClosing, setIsMenuClosing] = useState(false);

  const [isMobile, setIsMobile] = useState(window.innerWidth <= 425);
  const [isTablet, setIsTablet] = useState(
    window.innerWidth > 425 && window.innerWidth <= 1023
  );
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false);
  const [isInScrollContainer, setIsInScrollContainer] = useState(false);
  const logoRef = useRef(null);
  const diamondEffectRef = useRef(null);
  const { isAuthenticated, user, logout } = useAuth();

  // Helper function to close menu with animation
  const closeMenuWithAnimation = useCallback(() => {
    setIsMenuClosing(true);
    setTimeout(() => {
      setIsMenuOpen(false);
      setIsMenuClosing(false);
    }, 300); // Match animation duration
  }, []);

  // Get current navbar theme from hook
  const { theme: navbarTheme } = useNavbarTheme();

  // Check if current page is home, welcome, or immersive showroom (pages with white navbar)
  const isHomePage =
    location.pathname === ROUTES.HOME_PAGE ||
    location.pathname === ROUTES.HOME ||
    location.pathname === ROUTES.WELCOME;

  // Check if should hide menu, account, and immersive button
  const shouldHideButtons = false;

  // Check if logo click should be disabled (Milan and Immersive Showroom)
  const shouldDisableLogoClick = shouldHideButtons;

  useEffect(() => {
    // Initialize optimized transition system
    optimizedTransitionUtils.init();

    // Initialize diamond falling effect
    diamondEffectRef.current = new DiamondFallingEffect("navbar-diamond-container", {
      count: 20, // Reduced from 40 to 20
      speed: 2.5,
      size: 12,
      sway: 6,
      rotation: 4,
      fallAreaWidth: 0.26, // 26% of screen width (matching gradient background)
      fallAreaAlign: 'left', // Align to left side
    });

    // Cleanup on unmount
    return () => {
      if (diamondEffectRef.current) {
        diamondEffectRef.current.stop();
      }
    };
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

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (!event.target.closest(".menu-v3-container")) {
        if (isMenuOpen && !isMenuClosing) {
          closeMenuWithAnimation();
        }
      }
    };

    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, [isMenuOpen, isMenuClosing, closeMenuWithAnimation]);


  // Prevent body scroll when menu or account is open on all devices
  useEffect(() => {
    if (isMenuOpen || isAccountMenuOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
  }, [isMenuOpen, isAccountMenuOpen]);

  // Control diamond falling effect based on menu state
  // Delay diamond effect until gradient background finishes sweeping (0.8s)
  useEffect(() => {
    let diamondTimeout;

    if (isMenuOpen && diamondEffectRef.current) {
      // Wait for gradient sweep animation to complete (0.8s) before starting diamonds
      diamondTimeout = setTimeout(() => {
        if (diamondEffectRef.current) {
          diamondEffectRef.current.start();
        }
      }, 800); // Match gradient transition duration
    } else if (!isMenuOpen && diamondEffectRef.current) {
      // Stop immediately when menu closes
      diamondEffectRef.current.stop();
    }

    // Cleanup timeout if menu closes before diamond effect starts
    return () => {
      if (diamondTimeout) {
        clearTimeout(diamondTimeout);
      }
    };
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

  const performTransition = async (route, options = {}) => {
    // Close menu immediately before transition to prevent animation loop
    if (isMenuOpen || isMenuClosing) {
      setIsMenuOpen(false);
      setIsMenuClosing(false);
      // Wait for DOM to update before capturing originalContent
      await new Promise(resolve => setTimeout(resolve, 50));
    }

    // Use optimized transition system
    await optimizedTransitionUtils.transitionToRoute(navigate, route, options);
  };

  const handleLogoClick = async () => {
    // Disable logo click on Milan and Immersive Showroom pages
    if (shouldDisableLogoClick) {
      return;
    }

    // Close menu immediately if open
    if (isMenuOpen || isMenuClosing) {
      setIsMenuOpen(false);
      setIsMenuClosing(false);
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

  const handleHomeClick = async () => {
    if (
      window.location.pathname === ROUTES.HOME ||
      window.location.pathname === ROUTES.HOME_PAGE
    ) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.HOME_PAGE);
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

  // Helper function to get current page name for display
  const getCurrentPageName = () => {
    const path = location.pathname;
    if (path === ROUTES.HOME || path === ROUTES.HOME_PAGE) return "Home";
    // User profile pages - check active tab from location state
    if (path === ROUTES.USER_PROFILE) {
      const activeTab = location.state?.activeTab;
      if (activeTab === "Orders") return "Orders";
      if (activeTab === "Services") return "Services";
      if (activeTab === "Wishlist") return "Wishlist";
      return "My Passport"; // Default to My Passport if no tab specified
    }
    // Use startsWith for pages with detail/sub-pages
    if (path.startsWith(ROUTES.COLLECTIONS)) return "Products";
    if (path.startsWith(ROUTES.SERVICES)) return "Services";
    if (path.startsWith(ROUTES.SUPPORT)) return "Support";
    if (path.startsWith(ROUTES.NEWS)) return "News";
    // Exact match for pages without sub-pages
    if (path === ROUTES.ABOUT) return "About Mirror";
    if (path === ROUTES.LOCATIONS) return "Location";
    if (path === ROUTES.CONTACT) return "Contact us";
    return null; // No page name for other routes
  };

  const handleProfileClick = async (tab = "My Passport") => {
    setIsAccountMenuOpen(false);

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
    setIsAccountMenuOpen(false);
    await performTransition(ROUTES.DASHBOARD_ADMIN);
  };

  const handleVendorDashboardClick = async () => {
    setIsAccountMenuOpen(false);
    await performTransition(ROUTES.DASHBOARD_VENDOR);
  };

  const handleDesignerDashboardClick = async () => {
    setIsAccountMenuOpen(false);
    await performTransition(ROUTES.DASHBOARD_DESIGNER);
  };

  const handleLogoutClick = () => {
    setIsAccountMenuOpen(false);
    logout();
  };

  const handleLoginClick = async () => {
    setIsAccountMenuOpen(false);
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

  const handleImmersiveShowroomClick = () => {
    openModal();
  };

  const handleBookAppointmentClick = async () => {
    if (window.location.pathname === ROUTES.CONTACT) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    sessionStorage.setItem("scrollToTop", "true");
    await performTransition(ROUTES.CONTACT);
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
        className={`navbar-v3-glass-background navbar-v3-theme-${navbarTheme} ${
          isMenuOpen || isAccountMenuOpen ? "expanded" : ""
        } ${isMenuOpen ? "menu-expanded" : ""}`}
      >
        <div className="liquidGlass-v3-effect"></div>
        <div className="liquidGlass-v3-tint"></div>
        <div className="liquidGlass-v3-shine"></div>
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

      {/* Mobile/Tablet menu overlay */}
      {(isMobile || isTablet) && isMenuOpen && (
        <div
          className="mobile-v3-menu-overlay"
          onClick={() => setIsMenuOpen(false)}
        />
      )}

      {/* SEPARATE DIV FOR LOGO BLEND ONLY */}
      <div
        className={`logo-v3-fixed-container navbar-v3-theme-${navbarTheme} ${
          isHomePage && !isMenuOpen ? "no-blend" : ""
        } ${
          isHomePage && isInScrollContainer && !isMenuOpen ? "scrolled" : ""
        } ${shouldDisableLogoClick ? "no-click" : ""}`}
        onClick={handleLogoClick}
      >
        <img
          ref={logoRef}
          src={MirrorLogo}
          alt="Mirror Logo"
          className="navbar-v3-logo-svg"
        />
      </div>

      {/* MENU AND ACCOUNT LINK WITH BLEND MODE */}
      {!shouldHideButtons && (
        <div
          className={`menu-v3-fixed-container navbar-v3-theme-${navbarTheme} ${
            isHomePage && !isMenuOpen ? "no-blend" : ""
          } ${
            isHomePage && isInScrollContainer && !isMenuOpen ? "scrolled" : ""
          }`}
        >
          <div
            className={`menu-v3-container ${isMenuOpen ? "menu-v3-open" : ""}`}
          >
            <div
              className="menu-v3-button"
              onClick={(e) => {
                e.stopPropagation();
                if (isMenuOpen) {
                  closeMenuWithAnimation();
                } else {
                  setIsMenuOpen(true);
                }
              }}
            >
              <div className="menu-v3-icon-container">
                <svg
                  className="menu-v3-icon menu-v3-icon-desktop"
                  xmlns="http://www.w3.org/2000/svg"
                  width="32"
                  height="26"
                  viewBox="0 0 32 26"
                  fill="none"
                >
                  <path d="M32 18H0" stroke="white" strokeWidth="1.5" />
                  <path
                    d="M20 8.00001L0 8.00001"
                    stroke="white"
                    strokeWidth="1.5"
                  />
                </svg>
                <svg
                  className="menu-v3-icon-close"
                  xmlns="http://www.w3.org/2000/svg"
                  width="37"
                  height="26"
                  viewBox="0 0 37 26"
                  fill="none"
                >
                  <path
                    d="M18.915 22.3848L0.530273 4.00001"
                    stroke="white"
                    strokeWidth="1.5"
                  />
                  <path
                    d="M18.915 3.61523L0.530273 22"
                    stroke="white"
                    strokeWidth="1.5"
                  />
                </svg>
              </div>
              <span className="menu-v3-text">
                <UnderlineButton>Menu</UnderlineButton>
              </span>
              {/* Show separator and page name for all pages */}
              {getCurrentPageName() && !isMenuOpen && (
                <>
                  <svg
                    className="menu-v3-page-separator"
                    xmlns="http://www.w3.org/2000/svg"
                    width="7"
                    height="20"
                    viewBox="0 0 7 20"
                    fill="none"
                  >
                    <path
                      d="M5.90039 0.195312L0.72401 19.5138"
                      stroke="white"
                      strokeOpacity="0.5"
                      strokeWidth="1.5"
                    />
                  </svg>
                  <span className="menu-v3-page-name bodytext-6--no-margin">
                    {getCurrentPageName()}
                  </span>
                </>
              )}
            </div>
            <div
              className={`menu-v3-popup ${
                isMenuOpen || isMenuClosing ? "active" : ""
              } ${isMenuClosing ? "closing" : ""}`}
            >
              <div className="menu-v3-groups">
                {/* First group - Main navigation */}
                <ul className="menu-v3-list">
                  <li>
                    <UnderlineButton onClick={handleHomeClick}>
                      Home
                    </UnderlineButton>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.COLLECTIONS ? "active" : ""
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
                  >
                    <UnderlineButton onClick={handleServicesClick}>
                      Services
                    </UnderlineButton>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.SUPPORT ? "active" : ""
                    }
                  >
                    <UnderlineButton onClick={handleSupportClick}>
                      Support
                    </UnderlineButton>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.ABOUT ? "active" : ""
                    }
                  >
                    <UnderlineButton onClick={handleAboutClick}>
                      About
                    </UnderlineButton>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.NEWS ? "active" : ""
                    }
                  >
                    <UnderlineButton onClick={handleNewsClick}>
                      News
                    </UnderlineButton>
                  </li>
                </ul>
                <div className="menu-v3-divider-wrapper">
                  <hr className="menu-v3-divider" />
                </div>

                {/* FIND US section */}
                <ul className="menu-v3-list">
                  <li
                    className={
                      location.pathname === ROUTES.LOCATIONS ? "active" : ""
                    }
                  >
                    <UnderlineButton onClick={handleLocationClick}>
                      Location
                    </UnderlineButton>
                  </li>
                  <li
                    className={
                      location.pathname === ROUTES.CONTACT ? "active" : ""
                    }
                  >
                    <UnderlineButton onClick={handleContactClick}>
                      Contact
                    </UnderlineButton>
                  </li>
                </ul>
                <div className="menu-v3-divider-wrapper menu-v3-profile-divider">
                  <hr className="menu-v3-divider" />
                </div>

                {/* PROFILE section */}
                {!isAuthenticated ? (
                  <>
                    <ul className="menu-v3-list menu-v3-profile-section-guest">
                      <li>
                        <UnderlineButton onClick={handleAccountMenuClick}>
                          Mirror Passport
                        </UnderlineButton>
                      </li>
                      <li>
                        <UnderlineButton onClick={handleLoginClick}>
                          Log in
                        </UnderlineButton>
                      </li>
                    </ul>
                  </>
                ) : (
                  <>
                    <ul className="menu-v3-list">
                      <li>
                        <UnderlineButton
                          onClick={() => handleProfileClick("My Passport")}
                        >
                          My Passport
                        </UnderlineButton>
                      </li>
                      <li>
                        <UnderlineButton
                          onClick={() => handleProfileClick("Orders")}
                        >
                          My Orders
                        </UnderlineButton>
                      </li>
                      <li>
                        <UnderlineButton
                          onClick={() => handleProfileClick("Services")}
                        >
                          My Services
                        </UnderlineButton>
                      </li>
                      <li>
                        <UnderlineButton
                          onClick={() => handleProfileClick("Wishlist")}
                        >
                          My Wishlist
                        </UnderlineButton>
                      </li>

                      {isUserAdmin() && (
                        <li>
                          <UnderlineButton onClick={handleAdminDashboardClick}>
                            Admin Dashboard
                          </UnderlineButton>
                        </li>
                      )}

                      {isUserVendor() && (
                        <li>
                          <UnderlineButton onClick={handleVendorDashboardClick}>
                            Vendor Dashboard
                          </UnderlineButton>
                        </li>
                      )}

                      {isUserDesigner() && (
                        <li>
                          <UnderlineButton
                            onClick={handleDesignerDashboardClick}
                          >
                            Designer Dashboard
                          </UnderlineButton>
                        </li>
                      )}

                      <li className="logout-v3-item">
                        <UnderlineButton onClick={handleLogoutClick}>
                          Log out
                        </UnderlineButton>
                      </li>
                    </ul>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {!shouldHideButtons && (
        <div
          className={`account-v3-fixed-container navbar-v3-theme-${navbarTheme} ${
            isHomePage && !isMenuOpen ? "no-blend" : ""
          } ${
            isHomePage && isInScrollContainer && !isMenuOpen ? "scrolled" : ""
          }`}
        >
          <div className="account-v3-container">
            {isAuthenticated ? (
              // Logged in: Show "My Passport" without dropdown
              <div className="account-v3-button-wrapper">
                <UnderlineButton onClick={handleAccountMenuClick}>
                  My Passport
                </UnderlineButton>
              </div>
            ) : (
              // Not logged in: Show "Log in" button
              <div className="account-v3-button-wrapper">
                <UnderlineButton onClick={handleLoginClick}>
                  Log in
                </UnderlineButton>
              </div>
            )}
          </div>
        </div>
      )}

      {/* IMMERSIVE BUTTON - glassmorphism only */}
      {!shouldHideButtons && (
        <>
          <div className="immersive-v3-fixed-container">
            <button className="immersive-v3-button" onClick={handleBookAppointmentClick}></button>
          </div>

          {/* SEPARATE BORDER - mix-blend-mode only */}
          <div
            className={`immersive-v3-border-container navbar-v3-theme-${navbarTheme} ${
              isHomePage && !isMenuOpen ? "no-blend" : ""
            } ${
              isHomePage && isInScrollContainer && !isMenuOpen ? "scrolled" : ""
            }`}
          >
            <div className="immersive-v3-border"></div>
          </div>

          {/* SEPARATE TEXT - mix-blend-mode only */}
          <div
            className={`immersive-v3-text-container navbar-v3-theme-${navbarTheme} ${
              isHomePage && !isMenuOpen ? "no-blend" : ""
            } ${
              isHomePage && isInScrollContainer && !isMenuOpen ? "scrolled" : ""
            }`}
          >
            <span className="immersive-v3-text bodytext-4--no-margin">
              Book an Appointment
            </span>
          </div>
        </>
      )}

      {/* Diamond Falling Effect Container */}
      <div id="navbar-diamond-container"></div>

      {/* Bottom button on gradient sweep - Desktop only */}
      {isMenuOpen && (
        <div className="menu-v3-gradient-bottom-button">
          <UnderlineButton onClick={handleImmersiveShowroomClick}>
            Explore Immersive showroom
          </UnderlineButton>
        </div>
      )}
    </>
  );
}
