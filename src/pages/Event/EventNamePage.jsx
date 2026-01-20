/**
 * EventNamePage - Name input page for Mirror Diamond Event
 * User enters their name after login
 */
import { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ROUTES } from '@/constants/routes';
import NavbarV4 from '@/components/navbar/NavbarV4';
import GlassThemeButton from '@components/common/button/GlassThemeButton';
import EventBackButton from '@/components/event/EventBackButton';
import EventNextButton from '@/components/event/EventNextButton';
import useEventStore from '@/store/useEventStore';
import { updateUserDisplayName } from '@services/event/eventApi';

import './EventNamePage.css';

const EventNamePage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, setUser } = useEventStore();
  // Truncate name to 20 characters max
  const [name, setName] = useState((user?.displayName || '').slice(0, 20));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const inputRef = useRef(null);
  const measureRef = useRef(null);

  // Detect if navigated from login page (desktop only)
  const [isDesktop, setIsDesktop] = useState(window.innerWidth > 1024);
  const fromLogin = location.state?.fromLogin && isDesktop;

  useEffect(() => {
    const handleResize = () => {
      setIsDesktop(window.innerWidth > 1024);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Auto-resize input-wrap based on content, min = placeholder width (responsive)
  useEffect(() => {
    const updateWidth = () => {
      if (measureRef.current && inputRef.current) {
        // Measure placeholder width
        measureRef.current.textContent = 'tên của bạn';
        const placeholderWidth = measureRef.current.offsetWidth;

        // Measure current text width
        if (name) {
          measureRef.current.textContent = name;
        }
        const textWidth = measureRef.current.offsetWidth;

        // Wrapper width = max of placeholder and text
        const wrapperWidth = Math.max(placeholderWidth, textWidth);
        const wrapper = inputRef.current.parentElement;
        if (wrapper) {
          wrapper.style.width = `${wrapperWidth}px`;
        }
      }
    };

    updateWidth();

    // Re-calculate on window resize for responsive
    window.addEventListener('resize', updateWidth);
    return () => window.removeEventListener('resize', updateWidth);
  }, [name]);

  const handleBack = () => {
    navigate(ROUTES.EVENT_GUIDE);
  };

  const handleCreateNote = async () => {
    if (!name.trim()) return;

    setSaving(true);
    setError('');

    try {
      // Save name to database (use correct column based on provider)
      const authId = user.authId || user.googleId || user.facebookId;
      const provider = user.provider || 'google';
      const result = await updateUserDisplayName(authId, name.trim(), provider);

      if (result.success) {
        // Update store with new name
        setUser({ ...user, displayName: name.trim() });
        navigate(ROUTES.EVENT_CHOOSE_SHAPE);
      } else {
        setError(result.error || 'Không thể lưu tên. Vui lòng thử lại.');
      }
    } catch (err) {
      console.error('Save name error:', err);
      setError('Đã có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
      setSaving(false);
    }
  };

  // Background animation variants - slide from right (login position) to center
  const backgroundVariants = isDesktop
    ? {
        hidden: fromLogin
          ? {
              width: '50%',
              left: '50%',
              right: '0',
            }
          : {
              opacity: 1,
            },
        visible: {
          width: '100%',
          left: '0',
          right: '0',
          opacity: 1,
          transition: {
            duration: 0.8,
            ease: [0.4, 0, 0.2, 1],
          },
        },
      }
    : {
        hidden: { opacity: 1 },
        visible: { opacity: 1 },
      };

  return (
    <>
      <NavbarV4 logoOnly />
      <div className="event-name" data-navbar-theme="black">
      {/* Background */}
      <motion.div
        className="event-name__bg"
        variants={backgroundVariants}
        initial="hidden"
        animate="visible"
      />

      {/* Main Container */}
      <div className="event-name__main">
        {/* Main Content */}
        <div className="event-name__content">
          <div className="event-name__greeting">
            <span className="event-name__hi heading-2--no-margin">Dành cho</span>
            <div className="event-name__input-row">
              <div className="event-name__input-wrap">
                <span ref={measureRef} className="event-name__input-measure heading-2--no-margin" aria-hidden="true" />
                <input
                  ref={inputRef}
                  type="text"
                  className="event-name__input heading-2--no-margin"
                  placeholder="tên của bạn"
                  value={name}
                  onChange={(e) => setName(e.target.value.slice(0, 20))}
                  maxLength={20}
                  autoFocus
                />
                <div className="event-name__input-line" />
              </div>
              <span className="event-name__comma heading-2--no-margin">,</span>
            </div>
          </div>

          <h2 className="heading-2--no-margin event-name__subtitle">
            nguồn cảm hứng của giai điệu Love-Grown.
          </h2>

          {/* Error Message */}
          {error && <p className="event-name__error">{error}</p>}
        </div>

        {/* Create Note Button */}
        <div className="event-name__nav">
          <GlassThemeButton
            theme="event_spec"
            onClick={handleCreateNote}
            disabled={saving || !name.trim()}
          >
            {saving ? 'Đang sáng tạo...' : 'Sáng tạo Nốt sáng của riêng bạn'}
          </GlassThemeButton>
        </div>
      </div>

      {/* Back Button - Fixed bottom left */}
      <EventBackButton onClick={handleBack} />

      {/* Next Button - Fixed bottom right */}
      <EventNextButton onClick={handleCreateNote} disabled={saving || !name.trim()}>
        {saving ? 'Đang sáng tạo...' : 'Tiếp tục'}
      </EventNextButton>
    </div>
    </>
  );
};

export default EventNamePage;
