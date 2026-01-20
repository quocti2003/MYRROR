/**
 * PlaceNoteScreenNew - New design for placing note on music staff
 * Pink gradient background with circular rings
 */
import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import EventBackButton from '@/components/event/EventBackButton';
import EventNextButton from '@/components/event/EventNextButton';
import GlassThemeButton from '@/components/common/button/GlassThemeButton';
import { fetchAllNotes } from '@services/event/eventApi';
import { initAudio, playNoteByPosition, isAudioInitialized, getNoteName, POSITION_TO_NOTE } from '@services/event/audio';
import useEventStore from '@/store/useEventStore';
import RippleEffect from '@/components/event/effects/ripple-effect';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';
import NavbarV4 from '@/components/navbar/NavbarV4';
import AvatarGenerator from '@/components/event/ui/AvatarGenerator';

// Desktop breakpoint for Phase 2 feature (>= 1025px)
const DESKTOP_BREAKPOINT = 1025;

// Melody version - must match WriteMessageScreenNew to share melodyNotes
const MELODY_VERSION = 4;
// Mapping from shape ID to Cloudflare image path - using optimized webp
// HEART-01 = Heart, HEART-02 = Oval, HEART-03 = Round, HEART-04 = Pear
// HEART-05 = Asscher, HEART-06 = Emerald, HEART-07 = Marquise
const DIAMOND_MAP = {
  // Shape IDs (from EventChooseShapePage)
  h1: 'mirror_DMM/HEART-01.webp',  // Heart shape
  h2: 'mirror_DMM/HEART-02.webp',  // Oval shape
  h3: 'mirror_DMM/HEART-03.webp',  // Round shape
  h4: 'mirror_DMM/HEART-04.webp',  // Pear shape
  h5: 'mirror_DMM/HEART-05.webp',  // Asscher shape
  h6: 'mirror_DMM/HEART-06.webp',  // Emerald shape
  h7: 'mirror_DMM/HEART-07.webp',  // Marquise shape
  // Legacy shape names (from old /event flow)
  heart: 'mirror_DMM/HEART-01.webp',
  oval: 'mirror_DMM/HEART-02.webp',
  round: 'mirror_DMM/HEART-03.webp',
  pear: 'mirror_DMM/HEART-04.webp',
  asscher: 'mirror_DMM/HEART-05.webp',
  emerald: 'mirror_DMM/HEART-06.webp',
  marquise: 'mirror_DMM/HEART-07.webp',
};

// Mapping from shape ID to AvatarGenerator diamondShape name
// Must match EventChooseShapePage: h1=Heart, h2=Oval, h3=Round, h4=Pear, h5=Asscher, h6=Emerald, h7=Marquise
// OLD (incorrect):
// h1: 'heart',
// h2: 'round',
// h3: 'emerald',
// h4: 'marquise',
// h5: 'pear',
// h6: 'oval',
// h7: 'princess',
const SHAPE_NAME_MAP = {
  h1: 'heart',
  h2: 'oval',
  h3: 'round',
  h4: 'pear',
  h5: 'asscher',
  h6: 'emerald',
  h7: 'marquise',
};

// Fixed mapping: Shape ID → Note position (each shape has one specific note)
const SHAPE_TO_POSITION = {
  h1: 0,  // Heart → Đô (C4)
  h2: 1,  // Oval → Rê (D4)
  h3: 2,  // Round → Mi (E4)
  h4: 3,  // Pear → Pha (F4)
  h5: 4,  // Asscher → Son (G4)
  h6: 5,  // Emerald → La (A4)
  h7: 6,  // Marquise → Si (B4)
};


// All possible Y positions for the note (7 positions for 7 notes)
// Following standard treble clef notation:
// - Đô (C4) = ledger line below staff
// - Rê (D4) = space below bottom line
// - Mi (E4) = Line 4 (bottom line)
// - Pha-Si on lines 2-3 and spaces 2-3
// Line centers: 4, 92, 180, 268, 356 (lines 0-4)
// Space centers: 48, 136, 224, 312 (spaces 0-3)
// Below staff: 400 (space below), 444 (ledger line)
const NOTE_POSITIONS_Y = [
  436,  // Position 0 - Ledger line (below staff) - Đô (C4) - SAME AS YOUR-MELODY
  404,  // Position 1 - Space below Line 4 - Rê (D4)
  360,  // Position 2 - Line 4 (bottom) - Mi (E4)
  316,  // Position 3 - Space 3 - Pha (F4)
  272,  // Position 4 - Line 3 - Son (G4)
  228,  // Position 5 - Space 2 - La (A4)
  184,  // Position 6 - Line 2 - Si (B4)
];

// Tablet: Staff positions in vh (based on 1194px design) - CENTERED
// Total height = 200 + 150 + 200 = 550, offset = (1194 - 550) / 2 = 322
// Staff 1: top = 322/1194, height = 200/1194
// Staff 2: top = 672/1194, height = 200/1194
const NOTE_POSITIONS_Y_TABLET_STAFF1 = [
  (322 + 241) / 1194 * 100,    // Position 0 - Ledger line - Đô (C4) +4px
  (322 + 224) / 1194 * 100,    // Position 1 - Space below Line 4 - Rê (D4)
  (322 + 200) / 1194 * 100,    // Position 2 - Line 4 (bottom) - Mi (E4)
  (322 + 176) / 1194 * 100,    // Position 3 - Space 3 - Pha (F4)
  (322 + 152) / 1194 * 100,    // Position 4 - Line 3 - Son (G4)
  (322 + 128) / 1194 * 100,    // Position 5 - Space 2 - La (A4)
  (322 + 104) / 1194 * 100,    // Position 6 - Line 2 - Si (B4)
];

const NOTE_POSITIONS_Y_TABLET_STAFF2 = [
  (672 + 241) / 1194 * 100,    // Position 0 - Ledger line - Đô (C4) +4px
  (672 + 224) / 1194 * 100,    // Position 1 - Space below Line 4 - Rê (D4)
  (672 + 200) / 1194 * 100,    // Position 2 - Line 4 (bottom) - Mi (E4)
  (672 + 176) / 1194 * 100,    // Position 3 - Space 3 - Pha (F4)
  (672 + 152) / 1194 * 100,    // Position 4 - Line 3 - Son (G4)
  (672 + 128) / 1194 * 100,    // Position 5 - Space 2 - La (A4)
  (672 + 104) / 1194 * 100,    // Position 6 - Line 2 - Si (B4)
];

// Mobile: Staff positions in vh (based on 844px design) - CENTERED
// Total height = 200 + 100 + 200 = 500, offset = (844 - 500) / 2 = 172
// Staff 1: top = 172/844, height = 200/844
// Staff 2: top = 472/844, height = 200/844
const NOTE_POSITIONS_Y_MOBILE_STAFF1 = [
  (172 + 242) / 844 * 100,     // Position 0 - Ledger line - Đô (C4) +4px
  (172 + 224) / 844 * 100,     // Position 1 - Space below Line 4 - Rê (D4)
  (172 + 200) / 844 * 100,     // Position 2 - Line 4 (bottom) - Mi (E4)
  (172 + 176) / 844 * 100,     // Position 3 - Space 3 - Pha (F4)
  (172 + 152) / 844 * 100,     // Position 4 - Line 3 - Son (G4)
  (172 + 128) / 844 * 100,     // Position 5 - Space 2 - La (A4)
  (172 + 104) / 844 * 100,     // Position 6 - Line 2 - Si (B4)
];

const NOTE_POSITIONS_Y_MOBILE_STAFF2 = [
  (472 + 242) / 844 * 100,     // Position 0 - Ledger line - Đô (C4) +4px
  (472 + 224) / 844 * 100,     // Position 1 - Space below Line 4 - Rê (D4)
  (472 + 200) / 844 * 100,     // Position 2 - Line 4 (bottom) - Mi (E4)
  (472 + 176) / 844 * 100,     // Position 3 - Space 3 - Pha (F4)
  (472 + 152) / 844 * 100,     // Position 4 - Line 3 - Son (G4)
  (472 + 128) / 844 * 100,     // Position 5 - Space 2 - La (A4)
  (472 + 104) / 844 * 100,     // Position 6 - Line 2 - Si (B4)
];

// getRandomPosition removed - position now determined by shape via SHAPE_TO_POSITION

const PlaceNoteScreenNew = () => {
  const navigate = useNavigate();

  // Get persisted position and setter from store
  const {
    user,
    selectedDiamond,
    setAllNotes,
    setUserNote,
    generatedAvatarUrl,
    generatedForShape,
    setGeneratedAvatar,
    melodyNotes,      // Persisted melody notes (same as your-melody)
    setMelodyNotes,   // Save melody notes to store
  } = useEventStore();

  // Pre-generate avatar: check if we need to generate
  const currentShape = SHAPE_NAME_MAP[selectedDiamond] || 'heart';
  const shouldGenerateAvatar = !generatedAvatarUrl || generatedForShape !== currentShape;

  // Position is always determined by shape (no need for useState or store)
  const positionX = 50; // Always centered
  const positionY = SHAPE_TO_POSITION[selectedDiamond] ?? 0; // Position determined by shape

  const [error] = useState('');
  const [, setLinesComplete] = useState(false); // Lines animation done
  const [diamondVisible, setDiamondVisible] = useState(false); // Diamond can appear
  const [titleVisible, setTitleVisible] = useState(false); // Title appears after diamond animation
  const [isTransitioning, setIsTransitioning] = useState(false); // Zoom-out transition
  const containerRef = useRef(null); // Main container ref for setting CSS vars
  const heartRef = useRef(null);
  const rippleRef = useRef(null);
  const hasNavigatedRef = useRef(false); // Track if user manually navigated
  const audioReadyRef = useRef(false); // Track if audio was initialized by user interaction

  // Phase 2 states (Desktop only - melody view within same page)
  const [isDesktop, setIsDesktop] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 480);
  const [isTablet, setIsTablet] = useState(window.innerWidth > 480 && window.innerWidth <= 1024);
  const [phase, setPhase] = useState(1); // 1 = your-note, 2 = melody
  const [showRandomNotes, setShowRandomNotes] = useState(false); // Control when to show random notes
  const [showPhase2Header, setShowPhase2Header] = useState(false); // Phase 2 header slide-in
  const [isPlaying, setIsPlaying] = useState(false); // Playing melody
  const noteRefs = useRef({}); // Refs for random notes (for playing animation)

  // Use melodyNotes from store (persisted, shared with your-melody page)
  const displayNotes = melodyNotes || [];

  // Helper function to get Y position based on screen size and staff
  // Returns string with unit (vh for mobile/tablet, px for desktop)
  const getNoteYPosition = (positionY, staffIndex = 0) => {
    if (isMobile) {
      // Mobile (≤480px): use mobile arrays
      const vhValue = staffIndex === 0
        ? NOTE_POSITIONS_Y_MOBILE_STAFF1[positionY]
        : NOTE_POSITIONS_Y_MOBILE_STAFF2[positionY];
      return `${vhValue}vh`;
    }
    if (isTablet) {
      // Tablet (481-1024px): use tablet arrays
      const vhValue = staffIndex === 0
        ? NOTE_POSITIONS_Y_TABLET_STAFF1[positionY]
        : NOTE_POSITIONS_Y_TABLET_STAFF2[positionY];
      return `${vhValue}vh`;
    }
    // Desktop (>1024px): use desktop array with px (fixed within centered container)
    return `${NOTE_POSITIONS_Y[positionY]}px`;
  };

  // Helper function to get X position based on screen size and staff
  // All breakpoints: spread notes evenly within their staff
  const getNoteXPosition = (noteIndex, staffIndex = 0) => {
    if (isMobile || isTablet) {
      // Mobile/Tablet: 2 staffs
      // Staff 1: 4 notes (indices 0-3), spread from 15% to 85%
      // Staff 2: 4 notes (indices 4-6 + user at 7), spread from 15% to 85%
      const indexInStaff = staffIndex === 0 ? noteIndex : noteIndex - 4;
      const minX = 15;
      const maxX = 85;
      const step = (maxX - minX) / 3; // 3 gaps for 4 notes per staff
      return minX + indexInStaff * step;
    }
    // Desktop: 8 notes spread evenly from 12.5% to 87.5% (centered)
    const minX = 12.5;
    const maxX = 87.5;
    const step = (maxX - minX) / 7; // 7 gaps for 8 notes
    return minX + noteIndex * step;
  };

  // Shape determined by Y position (same as your-melody)
  const NOTE_TO_SHAPE = {
    0: 'mirror_DMM/HEART-01.webp',  // Đô → Heart
    1: 'mirror_DMM/HEART-02.webp',  // Rê → Oval
    2: 'mirror_DMM/HEART-03.webp',  // Mi → Round
    3: 'mirror_DMM/HEART-04.webp',  // Pha → Pear
    4: 'mirror_DMM/HEART-05.webp',  // Son → Asscher
    5: 'mirror_DMM/HEART-06.webp',  // La → Emerald
    6: 'mirror_DMM/HEART-07.webp',  // Si → Marquise
  };

  // Seeded random function (same as AvatarGenerator) - consistent per user
  const seededRandom = (seed) => {
    const x = Math.sin(seed) * 10000;
    return x - Math.floor(x);
  };

  // Fisher-Yates shuffle with seeded random
  const seededShuffle = (array, seed) => {
    const result = [...array];
    for (let i = result.length - 1; i > 0; i--) {
      const j = Math.floor(seededRandom(seed + i) * (i + 1));
      [result[i], result[j]] = [result[j], result[i]];
    }
    return result;
  };

  // Generate 7 notes - uses lightNumber as seed for consistent melody per user
  // This ensures melodyNotes can be shared between your-note Phase 2 and your-melody
  const generateHappyMelody = useCallback(() => {
    const notes = [];

    // 7 positions (0-6) for 7 notes
    const allPositions = [0, 1, 2, 3, 4, 5, 6];
    // Use lightNumber as seed - same user = same melody
    const lightNumber = user?.lightNumber || 1;
    const shuffledPositions = seededShuffle(allPositions, lightNumber);
    const positions = shuffledPositions;

    // For mobile layout (4 notes on staff 1, 3 on staff 2):
    // Notes 0-3: Staff 1, X positions spread left to right
    // Notes 4-6: Staff 2, X positions spread left to right
    for (let i = 0; i < 7; i++) {
      const staffIndex = i < 4 ? 0 : 1;
      const indexInStaff = staffIndex === 0 ? i : i - 4;
      const notesInStaff = staffIndex === 0 ? 4 : 4;

      const minX = 15;
      const maxX = staffIndex === 0 ? 85 : 65;
      const stepX = (maxX - minX) / (notesInStaff - 1);
      const positionX = minX + indexInStaff * stepX;

      notes.push({
        id: `note-${i}`,
        positionX,
        positionY: positions[i],
        staffIndex,
        shape: NOTE_TO_SHAPE[positions[i]],
        version: MELODY_VERSION,
      });
    }

    return notes;
  }, [user]);

  // Handle next button - Phase 2 on desktop, navigate on mobile/phase2
  const handleNext = useCallback(() => {
    if (isTransitioning || hasNavigatedRef.current) return;

    // Desktop + Phase 1: Trigger Phase 2 animation (no page transition)
    if (isDesktop && phase === 1) {
      // Destroy ripple before transition
      if (rippleRef.current) {
        rippleRef.current.destroy();
        rippleRef.current = null;
      }

      // Check if melodyNotes already exists in store (persisted from previous session)
      // Only generate new melody if not exists or invalid version
      const needsRegenerate = !melodyNotes ||
        melodyNotes.length === 0 ||
        melodyNotes[0].staffIndex === undefined ||
        melodyNotes[0].version !== MELODY_VERSION;

      if (needsRegenerate) {
        const newMelody = generateHappyMelody();
        setMelodyNotes(newMelody);
      }

      // Start phase 2 - diamond moves right, title exits
      setPhase(2);

      // Add phase2 class for CSS scoping (responsive 2-staff layout)
      if (containerRef.current) {
        containerRef.current.classList.add('place-note-new--phase2');
      }

      // Show random notes after a small delay (let diamond start moving)
      setTimeout(() => {
        setShowRandomNotes(true);
      }, 200);

      // Show header after notes slide up (like your-melody)
      setTimeout(() => {
        setShowPhase2Header(true);
      }, 600);

      return;
    }

    // Desktop Phase 2: Navigate to your-wallpaper
    if (isDesktop && phase === 2) {
      hasNavigatedRef.current = true;
      setUserNote({ positionX, positionY });
      navigate(ROUTES.EVENT_CHOOSE_NOTE);
      return;
    }

    // Mobile/Tablet: Navigate to your-melody
    hasNavigatedRef.current = true;
    setUserNote({ positionX, positionY });
    navigate(ROUTES.EVENT_WRITE_MESSAGE);
  }, [isDesktop, phase, isTransitioning, melodyNotes, generateHappyMelody, setMelodyNotes, setUserNote, positionX, positionY, navigate]);

  // Play melody - all notes from left to right (Phase 2)
  // EXACTLY like your-melody: 7 random notes + user note
  // Desktop: sorted by X position (left to right)
  // Mobile/Tablet: sorted by staff first, then X position (staff 1 left-to-right, then staff 2)
  const handlePlayMelody = useCallback(async () => {
    if (isPlaying || phase !== 2) return;

    await initAudio();
    setIsPlaying(true);

    // Destroy idle ripple during melody playback (like your-melody)
    if (rippleRef.current) {
      rippleRef.current.destroy();
      rippleRef.current = null;
    }

    const tempo = 400; // ms between notes

    // Sort all notes based on device
    const allNotes = [
      ...displayNotes.map((note, index) => {
        const staffIndex = note.staffIndex ?? (index < 4 ? 0 : 1);
        return {
          ...note,
          staffIndex,
          calculatedX: getNoteXPosition(index, staffIndex),
          isUserNote: false,
        };
      }),
      {
        id: 'user-note',
        staffIndex: (isMobile || isTablet) ? 1 : 0, // User note on staff 2 for mobile/tablet
        calculatedX: getNoteXPosition(7, (isMobile || isTablet) ? 1 : 0),
        positionY: positionY,
        isUserNote: true,
      },
    ].sort((a, b) => {
      // On desktop, ignore staffIndex - just sort by X position
      if (!isMobile && !isTablet) {
        return a.calculatedX - b.calculatedX;
      }
      // On mobile/tablet, sort by staff first, then by X
      if (a.staffIndex !== b.staffIndex) {
        return a.staffIndex - b.staffIndex; // Play staff 1 first, then staff 2
      }
      return a.calculatedX - b.calculatedX;
    });

    // Play each note in sequence
    for (let i = 0; i < allNotes.length; i++) {
      const note = allNotes[i];
      const noteElement = note.isUserNote ? heartRef.current : noteRefs.current[note.id];

      // Add playing class for visual feedback
      if (noteElement) {
        noteElement.classList.add('place-note-new__note--playing');
      }

      playNoteByPosition(note.positionY);

      await new Promise(resolve => setTimeout(resolve, tempo));

      // Remove playing class
      if (noteElement) {
        noteElement.classList.remove('place-note-new__note--playing');
      }
    }

    // Restart idle ripple after melody ends (like your-melody)
    if (heartRef.current) {
      rippleRef.current = new RippleEffect(heartRef.current, {
        autoRippleCount: 4,
        duration: 5000,
        delay: 1200,
        startSize: 60,
        endSize: 400,
        opacity: 0.6,
        autoPlay: true,
        clickable: false,
        clickRippleCount: 3,
      });
    }

    setIsPlaying(false);
  }, [isPlaying, phase, displayNotes, positionY, isMobile, isTablet]);

  // Play user's note only (Phase 2)
  const handlePlayUserNote = useCallback(async () => {
    if (isPlaying || phase !== 2) return;

    await initAudio();

    // Add playing class
    if (heartRef.current) {
      heartRef.current.classList.add('place-note-new__note--playing');
    }

    playNoteByPosition(positionY);

    // Remove playing class after sound plays (800ms like your-melody)
    setTimeout(() => {
      if (heartRef.current) {
        heartRef.current.classList.remove('place-note-new__note--playing');
      }
    }, 800);
  }, [isPlaying, phase, positionY]);

  // Fetch existing notes on mount
  useEffect(() => {
    const loadNotes = async () => {
      const notes = await fetchAllNotes();
      setAllNotes(notes);
    };
    loadNotes();
  }, [setAllNotes]);

  // Detect viewport size for Phase 2 feature and responsive layout
  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth;
      setIsDesktop(width >= DESKTOP_BREAKPOINT);
      setIsMobile(width <= 480);
      setIsTablet(width > 480 && width <= 1024);
    };
    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Position is now deterministic (based on shape), no need to save to store


  // Initialize audio on first user interaction (required by browser autoplay policy)
  useEffect(() => {
    const initAudioOnInteraction = async () => {
      if (audioReadyRef.current) return;

      try {
        await initAudio();
        audioReadyRef.current = true;
      } catch (e) {
        console.warn('Failed to init audio:', e);
      }

      // Remove listeners after first interaction
      document.removeEventListener('click', initAudioOnInteraction);
      document.removeEventListener('touchstart', initAudioOnInteraction);
    };

    document.addEventListener('click', initAudioOnInteraction);
    document.addEventListener('touchstart', initAudioOnInteraction);

    return () => {
      document.removeEventListener('click', initAudioOnInteraction);
      document.removeEventListener('touchstart', initAudioOnInteraction);
    };
  }, []);

  // Animation sequence: lines (1s) -> diamond appears (0.2s delay) -> title (1s after diamond)
  useEffect(() => {
    // Lines complete after 1s
    const linesTimer = setTimeout(() => {
      setLinesComplete(true);
    }, 1000);

    // Diamond appears 0.2s after lines complete
    const diamondTimer = setTimeout(() => {
      setDiamondVisible(true);
    }, 1200);

    // Title appears 1s after diamond (after diamond scale animation completes)
    const titleTimer = setTimeout(() => {
      setTitleVisible(true);
    }, 2200);

    return () => {
      clearTimeout(linesTimer);
      clearTimeout(diamondTimer);
      clearTimeout(titleTimer);
    };
  }, []);

  // Unified RippleEffect management for both phases
  // - Phase 1: Large ripple (endSize: 950) after diamond appears
  // - Phase 2: Smaller ripple (endSize: 400, like your-melody) after notes slide up
  // - Properly cleanup and restart when switching between phases
  useEffect(() => {
    if (!diamondVisible) return;

    // Determine if we should create ripple based on current phase
    const shouldCreateRipple = phase === 1 || (phase === 2 && showRandomNotes);
    if (!shouldCreateRipple) return;

    // Destroy existing ripple before creating new one
    if (rippleRef.current) {
      rippleRef.current.destroy();
      rippleRef.current = null;
    }

    // Different delay and options for each phase
    const delay = phase === 1 ? 1000 : 500;
    const options = phase === 1
      ? {
          // Phase 1: Large ripple
          autoRippleCount: 6,
          duration: 6000,
          delay: 1000,
          startSize: 80,
          endSize: 950,
          opacity: 0.65,
          autoPlay: true,
          clickable: false,
          clickRippleCount: 5,
        }
      : {
          // Phase 2: Smaller ripple (like your-melody)
          autoRippleCount: 4,
          duration: 5000,
          delay: 1200,
          startSize: 60,
          endSize: 400,
          opacity: 0.6,
          autoPlay: true,
          clickable: false,
          clickRippleCount: 3,
        };

    const rippleTimer = setTimeout(() => {
      if (heartRef.current) {
        rippleRef.current = new RippleEffect(heartRef.current, options);

        // Only play sound on Phase 1 initial load (if audio already initialized)
        if (phase === 1 && (audioReadyRef.current || isAudioInitialized())) {
          playNoteByPosition(positionY);
        }
      }
    }, delay);

    return () => {
      clearTimeout(rippleTimer);
      if (rippleRef.current) {
        rippleRef.current.destroy();
        rippleRef.current = null;
      }
    };
  }, [diamondVisible, phase, showRandomNotes, positionY]);

  // Auto-transition after 2s (when diamond is visible)
  // - Desktop: Phase 1 → Phase 2
  // - Mobile/Tablet: navigate to your-melody
  useEffect(() => {
    if (!diamondVisible || phase !== 1) return; // Only auto-transition in Phase 1

    const transitionTimer = setTimeout(() => {
      if (hasNavigatedRef.current) return; // Skip if user already navigated
      handleNext(); // Uses same logic as Next button
    }, 2000);

    return () => clearTimeout(transitionTimer);
  }, [diamondVisible, phase, handleNext]);

  // Navigate on Enter key press
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Enter') {
        handleNext();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Navigation - go back to Choose Shape
  const handleGoBack = () => {
    navigate(ROUTES.EVENT_CHOOSE_SHAPE);
  };

  return (
    <>
      <NavbarV4 logoOnly />
      <div
        ref={containerRef}
        className="place-note-new"
        data-navbar-theme="black"
      >
        {/* Background with radial rings */}
        <div className="place-note-new__bg">
          <div className="place-note-new__rings" />
        </div>

        {/* Note title - shows in Phase 1, exits with animation in Phase 2 */}
        {titleVisible && (
          <h2 className={`place-note-new__note-title place-note-new__note-title--animate heading-3--no-margin ${phase === 2 ? 'place-note-new__note-title--exit' : ''}`}>
            {`Bạn là nốt sáng ${getNoteName(positionY)}`.split('').map((char, index) => (
              <span
                key={index}
                className="place-note-new__note-char"
                style={{ animationDelay: `${index * 0.08}s` }}
              >
                {char === ' ' ? '\u00A0' : char}
              </span>
            ))}
          </h2>
        )}

        {/* Phase 2: Header (like your-melody) */}
        {phase === 2 && (
          <header className={`place-note-new__phase2-header ${showPhase2Header ? 'place-note-new__phase2-header--animate' : 'place-note-new__phase2-header--hidden'}`}>
            <h2 className="heading-2--no-margin place-note-new__phase2-title">Tương tác cùng Nốt sáng</h2>
            <p className="bodytext-6--no-margin place-note-new__phase2-description">
              Bạn đã tạo nên Nốt sáng của riêng mình. Hãy trải nghiệm và lắng nghe giai điệu được hoàn thiện từ chính sự hiện diện của bạn.
            </p>
          </header>
        )}

        {/* Back and Next buttons */}
        <EventBackButton onClick={handleGoBack} />
        <EventNextButton onClick={handleNext} />

      {/* Solock decoration - left side */}
      <img
        src={getMediaUrl('dmm/solock.webp')}
        alt=""
        className="place-note-new__solock"
      />

      {/* Main content - Music Staff (vertically centered) */}
      <main className="place-note-new__main">
        <div className="place-note-new__staff-container">
          {/* Music Staff with 5 SVG lines */}
          <div className="place-note-new__staff">
            {/* Red highlights on lines - COMMENTED OUT
            <div className="place-note-new__line-highlights">
              {[0, 1, 2, 3, 4].map((lineIndex) => {
                const isOnThisLine = positionY % 2 === 0 && positionY / 2 === lineIndex;
                return (
                  <div
                    key={lineIndex}
                    className={`place-note-new__line-highlight ${isOnThisLine ? 'place-note-new__line-highlight--active' : ''}`}
                  />
                );
              })}
            </div>
            */}

            {/* Green zones - COMMENTED OUT
            <div className="place-note-new__zones">
              {[0, 1, 2, 3].map((zoneIndex) => {
                const isInThisZone = positionY % 2 === 1 && (positionY - 1) / 2 === zoneIndex;
                return (
                  <div
                    key={zoneIndex}
                    className={`place-note-new__zone ${isInThisZone ? 'place-note-new__zone--active' : ''}`}
                  />
                );
              })}
            </div>
            */}

            {/* Staff 1: 5 lines (shown on all devices) */}
            <div className="place-note-new__lines place-note-new__lines--staff1">
              {[0, 1, 2, 3, 4].map((i) => (
                <div
                  key={i}
                  className="place-note-new__line place-note-new__line--animate"
                />
              ))}
            </div>

            {/* Staff 2: 5 lines (mobile/tablet only - Phase 2) */}
            {phase === 2 && (
              <div className="place-note-new__lines place-note-new__lines--staff2">
                {[0, 1, 2, 3, 4].map((i) => (
                  <div
                    key={`staff2-${i}`}
                    className="place-note-new__line"
                  />
                ))}
              </div>
            )}

            {/* Heart note on staff - moves to right in Phase 2 */}
            {/* EXACTLY like your-melody: user note at index 7 on staff 2 (mobile/tablet) */}
            {/* Click/tap to play sound (works on iOS) */}
            {diamondVisible && (
              <div
                ref={heartRef}
                className={`place-note-new__diamond place-note-new__diamond--animate ${positionY === 0 ? 'place-note-new__diamond--position-do' : ''} ${phase === 2 ? 'place-note-new__diamond--phase2' : ''}`}
                style={{
                  left: phase === 2 ? `${getNoteXPosition(7, (isMobile || isTablet) ? 1 : 0)}%` : '50vw',
                  top: phase === 2 ? getNoteYPosition(positionY, (isMobile || isTablet) ? 1 : 0) : `${NOTE_POSITIONS_Y[positionY]}px`,
                  cursor: 'pointer',
                }}
                onClick={async () => {
                  if (isPlaying) return; // Don't play if melody is playing
                  await initAudio();
                  audioReadyRef.current = true;
                  playNoteByPosition(positionY);
                }}
                onTouchEnd={async (e) => {
                  if (isPlaying) return; // Don't play if melody is playing
                  e.preventDefault(); // Prevent double-fire with onClick
                  await initAudio();
                  audioReadyRef.current = true;
                  playNoteByPosition(positionY);
                }}
              >
                <img
                  src={getMediaUrl(DIAMOND_MAP[selectedDiamond] || 'mirror_DMM/HEART-01.webp')}
                  alt="Diamond note"
                  className="place-note-new__heart"
                />
              </div>
            )}

            {/* Phase 2: 7 notes slide up from bottom (EXACTLY like your-melody) */}
            {/* Mobile/Tablet: notes 0-3 on staff 1, notes 4-6 on staff 2 */}
            {showRandomNotes && displayNotes.map((note, index) => {
              const staffIndex = note.staffIndex ?? (index < 4 ? 0 : 1);
              return (
                <div
                  key={note.id}
                  ref={(el) => { noteRefs.current[note.id] = el; }}
                  className={`place-note-new__random-note place-note-new__random-note--animate ${note.positionY === 0 ? 'place-note-new__random-note--position-do' : ''}`}
                  style={{
                    left: `${getNoteXPosition(index, staffIndex)}%`,
                    top: getNoteYPosition(note.positionY, staffIndex),
                    animationDelay: `${index * 0.06}s`, // Staggered animation
                    cursor: 'pointer',
                  }}
                  onClick={async () => {
                    if (isPlaying) return;
                    await initAudio();
                    playNoteByPosition(note.positionY);
                  }}
                  onTouchEnd={async (e) => {
                    if (isPlaying) return;
                    e.preventDefault();
                    await initAudio();
                    playNoteByPosition(note.positionY);
                  }}
                >
                  <img
                    src={getMediaUrl(note.shape)}
                    alt="Note"
                    className="place-note-new__random-heart"
                  />
                </div>
              );
            })}
          </div>
        </div>
      </main>

      {/* Footer - Phase 2: Show melody buttons */}
      <footer className="place-note-new__footer">
        {error && <p className="place-note-new__error">{error}</p>}

        {/* Phase 2: Melody control buttons (like your-melody) */}
        {phase === 2 && (
          <div className="place-note-new__actions">
            {/* Hint text - hidden on desktop, shown on mobile */}
            <p className="place-note-new__hint bodytext-6--no-margin">Chạm vào nốt để nghe</p>
            <GlassThemeButton
              theme="event_spec"
              onClick={handlePlayMelody}
              className={isPlaying ? 'disabled' : ''}
            >
              Nghe 1 đoạn nốt
            </GlassThemeButton>
            <GlassThemeButton
              theme="event_dark"
              onClick={handlePlayUserNote}
              className={isPlaying ? 'disabled' : ''}
            >
              Nghe nốt của tôi
            </GlassThemeButton>
          </div>
        )}
      </footer>

      {/* Hidden Avatar Generator - pre-generate for your-wallpaper page */}
      {shouldGenerateAvatar && (
        <AvatarGenerator
          displayName={user?.displayName || 'Guest'}
          lightNumber={user?.lightNumber || 1}
          diamondShape={currentShape}
          onGenerated={(url) => setGeneratedAvatar(url, currentShape)}
          delay={1500}
        />
      )}
      </div>
    </>
  );
};

export default PlaceNoteScreenNew;
