/**
 * EventPage - Main event page with flow management
 * Flow: login (Google) -> diamond -> result
 */
import React, { useEffect } from 'react';
import useEventStore from '@/store/useEventStore';
import { isSupabaseConfigured } from '@services/event/supabase';
import { subscribeToNotesChannel } from '@services/event/ably';

// Screens
import GoogleLoginScreen from '@components/event/screens/GoogleLoginScreen';
import DiamondSelectionScreen from '@components/event/screens/DiamondSelectionScreen';
import ResultScreen from '@components/event/screens/ResultScreen';

import '@styles/event.css';

const EventPage = () => {
  const { currentStep, resumeFromPersistedState, setIsDemo, addNote } = useEventStore();

  // Check for persisted state on mount
  useEffect(() => {
    // Resume if user has completed flow before
    resumeFromPersistedState();

    // Set demo mode if Supabase not configured
    if (!isSupabaseConfigured()) {
      setIsDemo(true);
    }
  }, [resumeFromPersistedState, setIsDemo]);

  // Subscribe to real-time notes
  useEffect(() => {
    const unsubscribe = subscribeToNotesChannel((newNote) => {
      addNote(newNote);
    });

    return () => unsubscribe();
  }, [addNote]);

  // Render current screen based on step
  const renderScreen = () => {
    switch (currentStep) {
      case 'login':
        return <GoogleLoginScreen />;
      case 'diamond':
        return <DiamondSelectionScreen />;
      case 'result':
        return <ResultScreen />;
      default:
        return <GoogleLoginScreen />;
    }
  };

  return <div className="event-page">{renderScreen()}</div>;
};

export default EventPage;
