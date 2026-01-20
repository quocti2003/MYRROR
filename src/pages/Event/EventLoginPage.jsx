/**
 * EventLoginPage - Login page for Mirror Diamond Event
 * Uses direct Google Sign-In (không qua Supabase Auth)
 * OAuth redirect về domain của bạn, chỉ dùng Supabase để lưu data
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ROUTES } from '@/constants/routes';
import NavbarV4 from '@/components/navbar/NavbarV4';
import GlassThemeButton from '@/components/common/button/GlassThemeButton';
import EventBackButton from '@/components/event/EventBackButton';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';
import { initGoogleSignIn, signOutGoogle, signInWithGoogle, signInWithGoogleRedirect, handleOAuthRedirectCallback } from '@services/event/googleAuthService';
import { registerGoogleUser, checkExistingGoogleUser } from '@services/event/eventApi';
import useEventStore from '@/store/useEventStore';

import './EventLoginPage.css';

// LocalStorage key for session
const SESSION_KEY = 'mirror_event_user';

/**
 * Detect if user is in a problematic in-app browser where Google OAuth doesn't work
 * - Zalo on iOS: Show warning (Google blocks WebViews)
 * - Facebook/Messenger on iOS: Show warning (Google blocks WebViews with 403 disallowed_useragent)
 * - Facebook/Messenger on Android: Use redirect flow (works)
 */
const detectInAppBrowser = () => {
  if (typeof window === 'undefined') return { isInApp: false, app: null, useRedirectFlow: false };

  const ua = navigator.userAgent || navigator.vendor || window.opera || '';
  const isMobile = /android|iphone|ipad|ipod|mobile/i.test(ua);

  if (!isMobile) return { isInApp: false, app: null, useRedirectFlow: false };

  const isIOS = /iphone|ipad|ipod/i.test(ua);
  const isAndroid = /android/i.test(ua);

  // Zalo on iOS - show warning (Google blocks WebViews)
  if (isIOS && /Zalo/i.test(ua)) {
    return { isInApp: true, app: 'Zalo', useRedirectFlow: false };
  }

  // Facebook/Messenger detection
  const isFacebookApp = /Messenger|FBAN|FBAV/i.test(ua) && /FB_IAB|FBAN|FBAV/i.test(ua);

  if (isFacebookApp) {
    if (isIOS) {
      // iOS: Google blocks with 403 disallowed_useragent - show warning
      return { isInApp: true, app: 'Messenger', useRedirectFlow: false };
    } else if (isAndroid) {
      // Android: redirect flow works
      return { isInApp: false, app: 'Messenger', useRedirectFlow: true };
    }
  }

  return { isInApp: false, app: null, useRedirectFlow: false };
};

// Get saved user from localStorage
const getSavedUser = () => {
  try {
    const saved = localStorage.getItem(SESSION_KEY);
    return saved ? JSON.parse(saved) : null;
  } catch {
    return null;
  }
};

// Save user to localStorage
const saveUserSession = (user) => {
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify(user));
  } catch (e) {
    console.error('Failed to save session:', e);
  }
};

// Clear user session
export const clearUserSession = () => {
  try {
    localStorage.removeItem(SESSION_KEY);
    signOutGoogle();
  } catch (e) {
    console.error('Failed to clear session:', e);
  }
};

const EventLoginPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(true);
  const [error, setError] = useState('');
  const googleButtonRef = useRef(null);

  // Detect in-app browser
  const [inAppBrowser] = useState(() => detectInAppBrowser());

  // Detect if navigated from guide page or name page (desktop only)
  const [isDesktop, setIsDesktop] = useState(window.innerWidth > 1024);
  const fromGuide = (location.state?.fromGuide || location.state?.fromName) && isDesktop;

  // Update isDesktop on window resize
  useEffect(() => {
    const handleResize = () => {
      setIsDesktop(window.innerWidth > 1024);
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const {
    setUser,
    setIsDemo,
    setUserNote,
    setSelectedDiamond,
  } = useEventStore();

  // Handle user login (register in database) - works for Google
  const handleUserLogin = useCallback(async (oauthUser) => {
    setLoading(true);
    setError('');

    try {
      // First check if user already exists (pass provider to check correct column)
      const existingCheck = await checkExistingGoogleUser(oauthUser.id, oauthUser.provider);

      if (existingCheck.exists) {
        // Add provider info to user
        const userWithProvider = { ...existingCheck.user, provider: oauthUser.provider };
        setUser(userWithProvider);
        saveUserSession(userWithProvider);

        // Check if user already has a note
        if (existingCheck.hasNote && existingCheck.note) {
          // Restore note data to store
          setSelectedDiamond(existingCheck.note.diamondShape);
          setUserNote({
            ...existingCheck.note,
            orderId: existingCheck.user.lightNumber,
            userDisplayName: existingCheck.user.displayName,
          });
          // Skip to YOUR-NOTE page
          navigate(ROUTES.EVENT_PLACE_NOTE);
          return;
        }

        // Check if user has already confirmed their name
        if (existingCheck.nameConfirmed) {
          // Skip name page, go directly to choose shape
          navigate(ROUTES.EVENT_CHOOSE_SHAPE);
        } else {
          // User hasn't confirmed name yet, go to name page
          navigate(ROUTES.EVENT_NAME, { state: { fromLogin: true } });
        }
        return;
      }

      // New user - register
      const result = await registerGoogleUser({
        authId: oauthUser.id,
        googleId: oauthUser.id, // Legacy support
        email: oauthUser.email,
        displayName: oauthUser.displayName,
        provider: oauthUser.provider,
      });

      if (result.success) {
        if (result.isDemo) {
          setIsDemo(true);
        }
        setUser(result.user);
        saveUserSession(result.user);
        navigate(ROUTES.EVENT_NAME, { state: { fromLogin: true } });
      } else {
        setError(result.error || 'Không thể đăng ký. Vui lòng thử lại.');
      }
    } catch (err) {
      console.error('Login error:', err);
      setError('Đã có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, [navigate, setUser, setIsDemo, setUserNote, setSelectedDiamond]);

  // Check if user is already logged in on mount
  useEffect(() => {
    const checkExistingUser = async () => {
      const savedUser = getSavedUser();
      if (savedUser) {
        // User already logged in, restore session
        setUser(savedUser);

        // Check current status from database
        const existingCheck = await checkExistingGoogleUser(
          savedUser.googleId || savedUser.authId || savedUser.id,
          savedUser.provider || 'google'
        );

        if (existingCheck.exists) {
          if (existingCheck.hasNote && existingCheck.note) {
            setSelectedDiamond(existingCheck.note.diamondShape);
            setUserNote({
              ...existingCheck.note,
              orderId: existingCheck.user.lightNumber,
              userDisplayName: existingCheck.user.displayName,
            });
            navigate(ROUTES.EVENT_PLACE_NOTE);
          } else if (existingCheck.nameConfirmed) {
            navigate(ROUTES.EVENT_CHOOSE_SHAPE);
          } else {
            navigate(ROUTES.EVENT_NAME, { state: { fromLogin: true } });
          }
        }
      }
      setChecking(false);
    };

    checkExistingUser();
  }, [navigate, setUser, setUserNote, setSelectedDiamond]);

  // Initialize Google Sign-In
  useEffect(() => {
    if (checking) return;

    const initGoogle = async () => {
      try {
        await initGoogleSignIn(
          (userData) => handleUserLogin(userData),
          (errorMsg) => {
            setError(errorMsg);
            setLoading(false);
          }
        );
      } catch (err) {
        console.error('Failed to init Google Sign-In:', err);
      }
    };

    initGoogle();
  }, [checking, handleUserLogin]);

  // Handle OAuth redirect callback (for Messenger in-app browser)
  useEffect(() => {
    const checkOAuthCallback = async () => {
      const userData = await handleOAuthRedirectCallback();
      if (userData) {
        // User returned from Google OAuth redirect
        await handleUserLogin(userData);
      }
    };

    checkOAuthCallback();
  }, [handleUserLogin]);

  // Handle Google login button click
  const handleGoogleLogin = () => {
    setLoading(true);
    setError('');

    // For Messenger in-app browser: use redirect flow (works in WebViews)
    // For normal browsers: use Google Identity Services popup (better UX)
    if (inAppBrowser.useRedirectFlow) {
      signInWithGoogleRedirect();
    } else {
      signInWithGoogle();
    }
  };

  // Handle back to guide page
  const handleBack = () => {
    navigate(ROUTES.EVENT_GUIDE);
  };

  // Animation variants
  const contentVariants = isDesktop
    ? {
        // Desktop: slide in from right
        hidden: {
          opacity: 0,
          x: 100,
        },
        visible: {
          opacity: 1,
          x: 0,
          transition: {
            duration: 0.6,
            delay: 0.3,
            ease: 'easeOut',
          },
        },
      }
    : {
        // Mobile/Tablet: simple fade in
        hidden: {
          opacity: 0,
        },
        visible: {
          opacity: 1,
          transition: {
            duration: 0.6,
            ease: 'easeOut',
          },
        },
      };

  const cardChildrenVariants = isDesktop
    ? {
        // Desktop: stagger with y movement
        hidden: { opacity: 0, y: 20 },
        visible: (i) => ({
          opacity: 1,
          y: 0,
          transition: {
            delay: 0.5 + i * 0.1,
            duration: 0.4,
            ease: 'easeOut',
          },
        }),
      }
    : {
        // Mobile/Tablet: simple fade in
        hidden: { opacity: 0 },
        visible: (i) => ({
          opacity: 1,
          transition: {
            delay: 0.3 + i * 0.1,
            duration: 0.4,
            ease: 'easeOut',
          },
        }),
      };

  // Background container animation - desktop only
  const backgroundContainerVariants = isDesktop
    ? {
        hidden: fromGuide
          ? {
              width: '100vw',
              left: '0',
            }
          : {
              opacity: 0,
            },
        visible: {
          width: '50%',
          left: '0',
          opacity: 1,
          transition: {
            duration: 0.8,
            ease: [0.4, 0, 0.2, 1],
          },
        },
      }
    : {
        // Mobile/Tablet: simple fade in, no width animation
        hidden: { opacity: 0 },
        visible: {
          opacity: 1,
          transition: { duration: 0.6 },
        },
      };

  // Background image - desktop only animation
  const backgroundImageVariants = isDesktop
    ? {
        hidden: fromGuide
          ? {
              objectPosition: 'center center',
            }
          : {
              objectPosition: 'right center',
            },
        visible: {
          objectPosition: 'right center',
          transition: {
            duration: 0.8,
            ease: [0.4, 0, 0.2, 1],
          },
        },
      }
    : {
        // Mobile/Tablet: no animation
        hidden: { opacity: 1 },
        visible: { opacity: 1 },
      };

  // Gradient overlay animation - desktop only (hidden on mobile/tablet by CSS)
  const gradientVariants = isDesktop
    ? {
        hidden: fromGuide
          ? {
              left: '200%',
              opacity: 0.3,
            }
          : {
              left: '140%',
              opacity: 0,
            },
        visible: {
          left: '140%',
          opacity: 1,
          transition: {
            duration: 0.8,
            ease: [0.4, 0, 0.2, 1],
          },
        },
      }
    : {
        // Mobile/Tablet: no animation (hidden by CSS anyway)
        hidden: { opacity: 0 },
        visible: { opacity: 0 },
      };

  return (
    <div className="event-login-wrapper">
      <NavbarV4 logoOnly />
      <div className="event-login" data-navbar-theme="black">
        {/* Gradient overlay - animated */}
        <motion.div
          className="event-login__gradient"
          variants={gradientVariants}
          initial="hidden"
          animate="visible"
        />

        {/* Background - Container stays at 50% left, only image animates */}
        <motion.div
          className="event-login__bg"
          variants={backgroundContainerVariants}
          initial="hidden"
          animate="visible"
        >
          <motion.img
            src={getMediaUrl('mirror_DMM/Artboard-6.webp')}
            alt="Background"
            className="event-login__bg-img"
            variants={backgroundImageVariants}
            initial="hidden"
            animate="visible"
          />
        </motion.div>

        {/* Content - Right side (Desktop) / Glass Card (Mobile) - Slide in from right */}
        <motion.div
          className="event-login__content"
          variants={contentVariants}
          initial="hidden"
          animate="visible"
        >
          {/* Glass Card for Mobile/Tablet */}
          <div className="event-login__card">
            <motion.h1
              className="event-login__title heading-2--no-margin"
              custom={0}
              variants={cardChildrenVariants}
              initial="hidden"
              animate="visible"
            >
              Tham gia trải nghiệm
            </motion.h1>

            <motion.p
              className="event-login__subtitle bodytext-6--no-margin"
              custom={1}
              variants={cardChildrenVariants}
              initial="hidden"
              animate="visible"
            >
              Đăng nhập để tiếp tục
            </motion.p>

            {/* Error Message */}
            {error && <p className="event-login__error">{error}</p>}

            {/* Social Login Buttons or In-app disclaimer */}
            <motion.div
              className="event-login__social"
              custom={2}
              variants={cardChildrenVariants}
              initial="hidden"
              animate="visible"
            >
              {inAppBrowser.isInApp ? (
                /* In-app browser: show disclaimer instead of login button */
                <div className="event-login__inapp-disclaimer event-login__inapp-disclaimer--inline">
                  <p className="event-login__inapp-disclaimer-text bodytext-6--no-margin">
                    Chọn{' '}
                    <span className="event-login__dots event-login__dots--vertical">
                      <svg xmlns="http://www.w3.org/2000/svg" width="2" height="2" viewBox="0 0 2 2" fill="none"><circle cx="1" cy="1" r="1" fill="#BC224C"/></svg>
                      <svg xmlns="http://www.w3.org/2000/svg" width="2" height="2" viewBox="0 0 2 2" fill="none"><circle cx="1" cy="1" r="1" fill="#BC224C"/></svg>
                      <svg xmlns="http://www.w3.org/2000/svg" width="2" height="2" viewBox="0 0 2 2" fill="none"><circle cx="1" cy="1" r="1" fill="#BC224C"/></svg>
                    </span>
                    {' '}hoặc{' '}
                    <span className="event-login__dots event-login__dots--horizontal">
                      <svg xmlns="http://www.w3.org/2000/svg" width="2" height="2" viewBox="0 0 2 2" fill="none"><circle cx="1" cy="1" r="1" fill="#BC224C"/></svg>
                      <svg xmlns="http://www.w3.org/2000/svg" width="2" height="2" viewBox="0 0 2 2" fill="none"><circle cx="1" cy="1" r="1" fill="#BC224C"/></svg>
                      <svg xmlns="http://www.w3.org/2000/svg" width="2" height="2" viewBox="0 0 2 2" fill="none"><circle cx="1" cy="1" r="1" fill="#BC224C"/></svg>
                    </span>
                    {' '}ở góc màn hình
                  </p>
                  <p className="event-login__inapp-disclaimer-text bodytext-6--no-margin">
                    Để mở bằng trình duyệt
                  </p>
                </div>
              ) : (
                /* Normal browser: show Google login button */
                <>
                  <GlassThemeButton
                    theme="event_dark"
                    onClick={handleGoogleLogin}
                    className="event-login__social-btn"
                    textClassName="bodytext-6--no-margin"
                    expandable={false}
                    icon={<img src="/google-icon.svg" alt="Google" width="15" height="15" />}
                  >
                    {loading ? 'Đang đăng nhập...' : 'Tiếp tục với Google'}
                  </GlassThemeButton>

                  {/* Hidden div for Google's native button (optional fallback) */}
                  <div ref={googleButtonRef} style={{ display: 'none' }} />
                </>
              )}
            </motion.div>
          </div>
        </motion.div>

        {/* Back Button - Fixed bottom left */}
        <EventBackButton onClick={handleBack} />
      </div>
    </div>
  );
};

export default EventLoginPage;
