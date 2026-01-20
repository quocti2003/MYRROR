/**
 * Event Store - Zustand state management for Mirror Diamond Event
 * Flow: ticket -> name -> diamond -> result
 */
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useEventStore = create(
  persist(
    (set, get) => ({
      // Current step in the flow
      currentStep: 'login', // 'login' | 'diamond' | 'result'

      // User data (Google login)
      user: null, // { id, googleId, email, displayName, lightNumber, createdAt }

      // Selected diamond shape
      selectedDiamond: null,

      // Initial note position (randomized once per user, then persisted)
      initialNotePosition: null, // { x: number, y: number }

      // Melody notes (7 random notes, generated once and persisted)
      melodyNotes: null, // Array of { id, positionX, positionY, shape }

      // Selected avatar background
      selectedBackground: 'pink',

      // Selected avatar scenery
      selectedScenery: 'mountains',

      // User's placed note (persisted)
      userNote: null,

      // All notes (not persisted, refreshed on load)
      allNotes: [],

      // Stats
      totalParticipants: 0,
      totalNotes: 0,

      // Audio state
      audioInitialized: false,
      isSoundActive: false,

      // Demo mode
      isDemo: false,

      // Pre-generated avatar (NOT persisted - too large for localStorage, kept in memory only)
      generatedAvatarUrl: null,
      generatedForShape: null,
      generatedForName: null,

      // Actions
      setCurrentStep: (step) => set({ currentStep: step }),

      setUser: (user) => {
        const currentUser = get().user;
        // If user changed, reset user-specific data (melody, note position, etc.)
        if (currentUser && user && currentUser.id !== user.id) {
          set({
            user,
            melodyNotes: null,
            initialNotePosition: null,
            userNote: null,
            selectedDiamond: null,
          });
        } else {
          set({ user });
        }
      },

      setSelectedDiamond: (diamond) => set({ selectedDiamond: diamond }),

      setInitialNotePosition: (position) => set({ initialNotePosition: position }),

      setMelodyNotes: (notes) => set({ melodyNotes: notes }),

      setSelectedBackground: (background) => set({ selectedBackground: background }),

      setSelectedScenery: (scenery) => set({ selectedScenery: scenery }),

      setUserNote: (note) => set({ userNote: note }),

      addNote: (note) =>
        set((state) => ({
          allNotes: [...state.allNotes, note],
          totalNotes: state.totalNotes + 1,
        })),

      setAllNotes: (notes) => set({ allNotes: notes, totalNotes: notes.length }),

      setTotalParticipants: (count) => set({ totalParticipants: count }),

      setAudioInitialized: (initialized) => set({ audioInitialized: initialized }),

      setIsSoundActive: (active) => set({ isSoundActive: active }),

      toggleSound: () => set((state) => ({ isSoundActive: !state.isSoundActive })),

      setIsDemo: (isDemo) => set({ isDemo }),

      // Pre-generated avatar actions
      setGeneratedAvatar: (url, shape, name) => set({
        generatedAvatarUrl: url,
        generatedForShape: shape,
        generatedForName: name,
      }),
      clearGeneratedAvatar: () => set({
        generatedAvatarUrl: null,
        generatedForShape: null,
        generatedForName: null,
      }),

      // Reset all state (logout)
      reset: () =>
        set({
          currentStep: 'login',
          user: null,
          selectedDiamond: null,
          initialNotePosition: null,
          melodyNotes: null,
          selectedBackground: 'pink',
          selectedScenery: 'mountains',
          userNote: null,
          allNotes: [],
          totalParticipants: 0,
          totalNotes: 0,
          audioInitialized: false,
          isSoundActive: false,
          isDemo: false,
          generatedAvatarUrl: null,
          generatedForShape: null,
          generatedForName: null,
        }),

      // Check if user has completed the flow
      hasCompletedFlow: () => {
        const { userNote } = get();
        return !!userNote;
      },

      // Resume from persisted state
      resumeFromPersistedState: () => {
        const { userNote } = get();
        if (userNote) {
          set({ currentStep: 'result' });
          return true;
        }
        return false;
      },
    }),
    {
      name: 'mirror-diamond-event',
      // Persist userNote, user, selectedDiamond, initialNotePosition, melodyNotes to localStorage
      // NOTE: Avatar URL is NOT persisted (too large ~2-5MB, causes lag and quota issues)
      partialize: (state) => ({
        userNote: state.userNote,
        user: state.user,
        selectedDiamond: state.selectedDiamond,
        initialNotePosition: state.initialNotePosition,
        melodyNotes: state.melodyNotes,
      }),
    }
  )
);

export default useEventStore;
