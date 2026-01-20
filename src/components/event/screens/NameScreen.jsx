/**
 * NameScreen - Step 2: Enter display name
 */
import React, { useState } from 'react';
import { TEXT } from '@/constants/eventConstants';
import { registerUser } from '@services/event/eventApi';
import useEventStore from '@/store/useEventStore';
import Logo from '../ui/Logo';

const NameScreen = () => {
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { setCurrentStep, setUser, setIsDemo } = useEventStore();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!displayName.trim()) return;

    setLoading(true);
    setError('');

    const ticketCode = sessionStorage.getItem('event_ticket_code');

    if (!ticketCode) {
      setCurrentStep('ticket');
      return;
    }

    const result = await registerUser(ticketCode, displayName.trim());

    if (result.success) {
      if (result.isDemo) {
        setIsDemo(true);
      }
      setUser(result.user);
      setCurrentStep('diamond');
    } else {
      setError(result.error || TEXT.error);
    }

    setLoading(false);
  };

  return (
    <div className="event-screen name-screen">
      <Logo size="md" />

      <div className="event-screen__content">
        <form onSubmit={handleSubmit} className="event-form">
          <h2 className="event-form__title">{TEXT.nameTitle}</h2>

          <div className="event-form__field">
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value.slice(0, 30))}
              placeholder={TEXT.namePlaceholder}
              className="event-input"
              autoFocus
              disabled={loading}
              maxLength={30}
            />
            <span className="event-form__counter">{displayName.length}/30</span>
          </div>

          {error && <p className="event-form__error">{error}</p>}

          <button
            type="submit"
            className="event-btn event-btn--primary"
            disabled={!displayName.trim() || loading}
          >
            {loading ? TEXT.loading : TEXT.nameButton}
          </button>
        </form>
      </div>
    </div>
  );
};

export default NameScreen;
