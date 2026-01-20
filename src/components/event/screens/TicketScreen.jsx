/**
 * TicketScreen - Step 1: Enter ticket code
 */
import React, { useState } from 'react';
import { TEXT } from '@/constants/eventConstants';
import { validateTicket } from '@services/event/eventApi';
import useEventStore from '@/store/useEventStore';
import Logo from '../ui/Logo';

const TicketScreen = () => {
  const [ticketCode, setTicketCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { setCurrentStep, setIsDemo } = useEventStore();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!ticketCode.trim()) return;

    setLoading(true);
    setError('');

    const result = await validateTicket(ticketCode.trim());

    if (result.valid) {
      if (result.isDemo) {
        setIsDemo(true);
      }
      // Store ticket code temporarily in sessionStorage
      sessionStorage.setItem('event_ticket_code', ticketCode.trim().toUpperCase());
      setCurrentStep('name');
    } else {
      setError(result.error || TEXT.ticketError);
    }

    setLoading(false);
  };

  return (
    <div className="event-screen ticket-screen">
      <Logo size="lg" />

      <div className="event-screen__content">
        <p className="event-tagline">{TEXT.tagline}</p>

        <form onSubmit={handleSubmit} className="event-form">
          <h2 className="event-form__title">{TEXT.ticketTitle}</h2>

          <div className="event-form__field">
            <input
              type="text"
              value={ticketCode}
              onChange={(e) => setTicketCode(e.target.value.toUpperCase())}
              placeholder={TEXT.ticketPlaceholder}
              className="event-input"
              autoFocus
              disabled={loading}
            />
          </div>

          {error && <p className="event-form__error">{error}</p>}

          <button
            type="submit"
            className="event-btn event-btn--primary"
            disabled={!ticketCode.trim() || loading}
          >
            {loading ? TEXT.loading : TEXT.ticketButton}
          </button>
        </form>
      </div>
    </div>
  );
};

export default TicketScreen;
