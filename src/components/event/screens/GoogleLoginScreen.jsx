/**
 * GoogleLoginScreen - Login with Google OAuth
 * Replaces TicketScreen + NameScreen
 */
import React, { useState, useEffect } from 'react';
import { TEXT } from '@/constants/eventConstants';
import { signInWithGoogle, getCurrentUser, onAuthStateChange } from '@services/event/authService';
import { registerGoogleUser, checkExistingGoogleUser } from '@services/event/eventApi';
import useEventStore from '@/store/useEventStore';
import Logo from '../ui/Logo';

const GoogleLoginScreen = () => {
  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(true);
  const [error, setError] = useState('');

  const {
    setCurrentStep,
    setUser,
    setIsDemo,
    setUserNote,
    setSelectedDiamond,
    isDemo
  } = useEventStore();

  // Check if user is already logged in on mount
  useEffect(() => {
    const checkExistingUser = async () => {
      const user = await getCurrentUser();
      if (user) {
        await handleUserLogin(user);
      }
      setChecking(false);
    };

    checkExistingUser();

    // Subscribe to auth changes (for OAuth redirect callback)
    const unsubscribe = onAuthStateChange(async (user) => {
      if (user) {
        await handleUserLogin(user);
      }
    });

    return () => unsubscribe();
  }, []);

  // Handle user login (register in database)
  const handleUserLogin = async (googleUser) => {
    setLoading(true);
    setError('');

    try {
      // First check if user already exists and has a note
      const existingCheck = await checkExistingGoogleUser(googleUser.id);

      if (existingCheck.exists && existingCheck.hasNote) {
        // User already has a note - restore state and go to result
        setUser(existingCheck.user);
        setSelectedDiamond(existingCheck.note.diamondShape);
        setUserNote({
          ...existingCheck.note,
          orderId: existingCheck.user.lightNumber,
          userDisplayName: existingCheck.user.displayName,
        });
        setCurrentStep('result');
        return;
      }

      if (existingCheck.exists) {
        // User exists but no note - go to diamond selection
        setUser(existingCheck.user);
        setCurrentStep('diamond');
        return;
      }

      // New user - register
      const result = await registerGoogleUser({
        googleId: googleUser.id,
        email: googleUser.email,
        displayName: googleUser.displayName,
      });

      if (result.success) {
        if (result.isDemo) {
          setIsDemo(true);
        }
        setUser(result.user);
        setCurrentStep('diamond');
      } else {
        setError(result.error || 'Không thể đăng ký. Vui lòng thử lại.');
      }
    } catch (err) {
      console.error('Login error:', err);
      setError('Đã có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setLoading(true);
    setError('');

    const result = await signInWithGoogle();

    if (!result.success) {
      setError(result.error || 'Đăng nhập thất bại');
      setLoading(false);
    }
    // If success, the page will redirect to Google OAuth
    // After redirect back, onAuthStateChange will handle the login
  };

  // Demo mode login
  const handleDemoLogin = () => {
    setIsDemo(true);
    const demoUser = {
      id: 'demo-user-' + Date.now(),
      email: 'demo@example.com',
      displayName: 'Demo User',
      lightNumber: Math.floor(Math.random() * 1000) + 1,
      createdAt: new Date(),
    };
    setUser(demoUser);
    setCurrentStep('diamond');
  };

  if (checking) {
    return (
      <div className="event-screen login-screen">
        <Logo size="lg" />
        <div className="event-screen__content">
          <p className="event-loading">{TEXT.loading}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="event-screen login-screen">
      <Logo size="lg" />

      <div className="event-screen__content">
        <p className="event-tagline">{TEXT.tagline}</p>

        <div className="event-form">
          <h2 className="event-form__title">Tham gia sự kiện</h2>
          <p className="event-form__subtitle">
            Đăng nhập để đặt viên kim cương của bạn
          </p>

          {error && <p className="event-form__error">{error}</p>}

          <button
            type="button"
            className="event-btn event-btn--google"
            onClick={handleGoogleLogin}
            disabled={loading}
          >
            {loading ? (
              TEXT.loading
            ) : (
              <>
                <GoogleIcon />
                <span>Đăng nhập với Google</span>
              </>
            )}
          </button>

          {/* Demo mode button - only show if Supabase not configured */}
          {isDemo && (
            <button
              type="button"
              className="event-btn event-btn--secondary"
              onClick={handleDemoLogin}
              disabled={loading}
            >
              Chế độ Demo
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

// Google Icon SVG
const GoogleIcon = () => (
  <svg
    className="google-icon"
    width="20"
    height="20"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
      fill="#4285F4"
    />
    <path
      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
      fill="#34A853"
    />
    <path
      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
      fill="#FBBC05"
    />
    <path
      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
      fill="#EA4335"
    />
  </svg>
);

export default GoogleLoginScreen;
