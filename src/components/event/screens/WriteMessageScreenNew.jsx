/**
 * WriteMessageScreenNew (Your Melody) - Step 2: Display 8 notes (7 random + 1 user)
 * Sidebar layout with staff visualization and melody playback
 */
import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';
import useEventStore from '@/store/useEventStore';
import RippleEffect from '@/components/event/effects/ripple-effect';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';
import NavbarV4 from '@/components/navbar/NavbarV4';
import GlassThemeButton from '@/components/common/button/GlassThemeButton';
import EventBackButton from '@/components/event/EventBackButton';
import EventNextButton from '@/components/event/EventNextButton';
import EdgeNextButton from '@/components/event/EdgeNextButton';
import { initAudio, playNoteByPosition, getNoteName } from '@services/event/audio';
import AvatarGenerator from '@/components/event/ui/AvatarGenerator';
// Diamond shape Cloudflare paths array - using optimized webp
// HEART-01 = Heart, HEART-02 = Oval, HEART-03 = Round, HEART-04 = Pear
// HEART-05 = Asscher, HEART-06 = Emerald, HEART-07 = Marquise
const DIAMOND_SHAPES = [
  'mirror_DMM/HEART-01.webp',  // Heart shape
  'mirror_DMM/HEART-02.webp',  // Oval shape
  'mirror_DMM/HEART-03.webp',  // Round shape
  'mirror_DMM/HEART-04.webp',  // Pear shape
  'mirror_DMM/HEART-05.webp',  // Asscher shape
  'mirror_DMM/HEART-06.webp',  // Emerald shape
  'mirror_DMM/HEART-07.webp',  // Marquise shape
];

// Melody version - increment when position mapping changes to force regeneration
const MELODY_VERSION = 4;

// Fixed mapping: Note position → Shape (each note has one specific shape)
const NOTE_TO_SHAPE = {
  0: 'mirror_DMM/HEART-01.webp',  // Đô → Heart
  1: 'mirror_DMM/HEART-02.webp',  // Rê → Oval
  2: 'mirror_DMM/HEART-03.webp',  // Mi → Round
  3: 'mirror_DMM/HEART-04.webp',  // Pha → Pear
  4: 'mirror_DMM/HEART-05.webp',  // Son → Asscher
  5: 'mirror_DMM/HEART-06.webp',  // La → Emerald
  6: 'mirror_DMM/HEART-07.webp',  // Si → Marquise
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

// Staff configuration - 7 Y positions for 7 notes (C4-B4)
// Desktop: Staff lines at top=0, notes use fixed pixel positions
// .your-melody__main is centered with top:50% + translateY(-50%)
const NOTE_POSITIONS_Y_DESKTOP = [
  436,  // Position 0 - Đô (C4) - Ledger line below staff
  404,  // Position 1 - Rê (D4) - Space below Line 4
  360,  // Position 2 - Mi (E4) - Line 4 (bottom)
  316,  // Position 3 - Pha (F4) - Space 3
  272,  // Position 4 - Son (G4) - Line 3
  228,  // Position 5 - La (A4) - Space 2
  184,  // Position 6 - Si (B4) - Line 2
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
// Mobile: 4 notes on staff 1, 3 notes on staff 2 (+ user note = 4)
const generateHappyMelody = (lightNumber = 1) => {
  const notes = [];

  // 7 positions (0-6) for 7 notes: 4 lines + 3 spaces
  // Position 0,2,4,6 = lines; Position 1,3,5 = spaces
  const allPositions = [0, 1, 2, 3, 4, 5, 6];
  // Use lightNumber as seed - same user = same melody
  const shuffledPositions = seededShuffle(allPositions, lightNumber);

  // All 7 positions are used - each note gets a unique position
  const positions = shuffledPositions;

  // Shape is determined by note position (fixed mapping via NOTE_TO_SHAPE)
  // No more random shape shuffling

  // For mobile layout (4 notes on staff 1, 3 on staff 2):
  // Notes 0-3: Staff 1, X positions spread left to right
  // Notes 4-6: Staff 2, X positions spread left to right
  // Note 7 (user's note): Staff 2, rightmost position

  // Staff 1: 4 notes, X from 15% to 85%
  // Staff 2: 4 notes (3 random + 1 user), X from 15% to 85%

  for (let i = 0; i < 7; i++) {
    const staffIndex = i < 4 ? 0 : 1; // First 4 notes on staff 1, rest on staff 2
    const indexInStaff = staffIndex === 0 ? i : i - 4;
    const notesInStaff = staffIndex === 0 ? 4 : 4; // 4 notes each staff (including user note in staff 2)

    // Calculate X position: spread evenly within staff, left to right
    // NO random offset - ensure strict left-to-right ordering
    const minX = 15;
    const maxX = staffIndex === 0 ? 85 : 65; // Staff 2 leaves room for user note at 85%
    const stepX = (maxX - minX) / (notesInStaff - 1);
    const positionX = minX + indexInStaff * stepX; // Removed random offset

    notes.push({
      id: `note-${i}`,
      positionX,
      positionY: positions[i],
      staffIndex, // 0 = staff 1 (top), 1 = staff 2 (bottom)
      shape: NOTE_TO_SHAPE[positions[i]], // Shape determined by note position
      version: MELODY_VERSION, // Force regeneration when version changes
    });
  }

  return notes;
};

const WriteMessageScreenNew = () => {
  const navigate = useNavigate();
  const [isEntering, setIsEntering] = useState(true);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 480);
  const [isTablet, setIsTablet] = useState(window.innerWidth > 480 && window.innerWidth <= 1024);

  // Entrance animation states
  const [isAnimatingEntrance, setIsAnimatingEntrance] = useState(false);
  const [headerAnimated, setHeaderAnimated] = useState(false);
  const [userNoteAnimated, setUserNoteAnimated] = useState(false);
  const [animatedNoteIndices, setAnimatedNoteIndices] = useState([]);

  const containerRef = useRef(null);
  const userNoteRef = useRef(null);
  const noteRefs = useRef({});
  const rippleRef = useRef(null);

  const {
    user,
    userNote,
    melodyNotes,
    setMelodyNotes,
    selectedDiamond,
    generatedAvatarUrl,
    generatedForShape,
    setGeneratedAvatar,
  } = useEventStore();

  // Pre-generate avatar: check if we need to generate
  const currentShape = SHAPE_NAME_MAP[selectedDiamond] || 'heart';
  const shouldGenerateAvatar = !generatedAvatarUrl || generatedForShape !== currentShape;

  // Track window resize for mobile/tablet detection
  useEffect(() => {
    const handleResize = () => {
      const width = window.innerWidth;
      setIsMobile(width <= 480);
      setIsTablet(width > 480 && width <= 1024);
    };
    // Call immediately on mount to ensure correct detection
    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // User's note position
  // Desktop: X at 83% (rightmost of single staff)
  // Mobile/Tablet: X at 85% on staff 2 (rightmost of staff 2)
  const userPositionX = (isMobile || isTablet) ? 85 : 83;
  const userPositionY = userNote?.positionY ?? 4;

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
    return `${NOTE_POSITIONS_Y_DESKTOP[positionY]}px`;
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

  // Generate or retrieve melody notes (persisted)
  // Force regenerate if old melody doesn't have staffIndex, wrong distribution, or outdated version
  useEffect(() => {
    const needsRegenerate = !melodyNotes ||
      melodyNotes.length === 0 ||
      melodyNotes[0].staffIndex === undefined ||
      melodyNotes[0].version !== MELODY_VERSION || // Force regen when position mapping changes
      // Check if notes have correct 4-3 distribution (notes 0-3 on staff 0, notes 4-6 on staff 1)
      melodyNotes.some((note, i) => {
        const expectedStaff = i < 4 ? 0 : 1;
        return note.staffIndex !== expectedStaff;
      });

    if (needsRegenerate) {
      const lightNumber = user?.lightNumber || 1;
      const newMelody = generateHappyMelody(lightNumber);
      setMelodyNotes(newMelody);
    }
  }, [melodyNotes, setMelodyNotes, user]);

  // Use persisted melody or empty array while loading
  const displayNotes = melodyNotes || [];

  // Preload webp images
  useEffect(() => {
    DIAMOND_SHAPES.forEach(shape => {
      const img = new Image();
      img.src = getMediaUrl(shape);
    });
  }, []);


  // Remove entering class after animation
  useEffect(() => {
    const timer = setTimeout(() => {
      setIsEntering(false);
    }, 1200);
    return () => clearTimeout(timer);
  }, []);

  // Entrance animation sequence - runs when page loads (after entering animation)
  useEffect(() => {
    if (isEntering || displayNotes.length === 0) return;

    const runEntranceAnimation = async () => {
      setIsAnimatingEntrance(true);

      // Header slides in
      setHeaderAnimated(true);

      // User's note appears
      await new Promise(resolve => setTimeout(resolve, 200));
      setUserNoteAnimated(true);

      // All 7 notes slide up at the same time (no sound)
      await new Promise(resolve => setTimeout(resolve, 300));
      setAnimatedNoteIndices([0, 1, 2, 3, 4, 5, 6]);

      // Wait for animation to complete
      await new Promise(resolve => setTimeout(resolve, 500));
      setIsAnimatingEntrance(false);
    };

    runEntranceAnimation();
  }, [isEntering, displayNotes.length]);

  // Initialize RippleEffect on user's note (after entrance, not during play or animation)
  useEffect(() => {
    if (isEntering || isPlaying || isAnimatingEntrance) return;

    if (userNoteRef.current && !rippleRef.current) {
      rippleRef.current = new RippleEffect(userNoteRef.current, {
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

    return () => {
      if (rippleRef.current) {
        rippleRef.current.destroy();
        rippleRef.current = null;
      }
    };
  }, [isEntering, isPlaying, isAnimatingEntrance]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (rippleRef.current) {
        rippleRef.current.destroy();
      }
    };
  }, []);

  // Play melody - all notes from left to right
  const handlePlayMelody = useCallback(async () => {
    if (isPlaying) return;

    // Initialize audio and wait for Safari to be ready
    await initAudio();
    setIsPlaying(true);

    // Pause user's continuous ripple during melody playback
    if (rippleRef.current) {
      rippleRef.current.destroy();
      rippleRef.current = null;
    }

    const tempo = 400; // ms between notes

    // Sort notes: by staffIndex first (mobile only), then by calculated X position
    const allNotes = [
      ...displayNotes.map((note, index) => ({
        ...note,
        isUserNote: false,
        noteIndex: index,
        calculatedX: getNoteXPosition(index, note.staffIndex || 0)
      })),
      {
        id: 'user-note',
        positionX: userPositionX,
        positionY: userPositionY,
        isUserNote: true,
        noteIndex: 7,
        calculatedX: getNoteXPosition(7, 1), // User note on staff 2
        staffIndex: 1 // User note always on staff 2 for mobile
      },
    ].sort((a, b) => {
      // On desktop, ignore staffIndex - just sort by X position
      if (!isMobile && !isTablet) {
        return a.calculatedX - b.calculatedX;
      }
      // On mobile/tablet, sort by staff first, then by X
      const staffA = a.staffIndex ?? 0;
      const staffB = b.staffIndex ?? 0;
      if (staffA !== staffB) {
        return staffA - staffB;
      }
      return a.calculatedX - b.calculatedX;
    });

    for (let i = 0; i < allNotes.length; i++) {
      const note = allNotes[i];
      const noteElement = note.isUserNote ? userNoteRef.current : noteRefs.current[note.id];

      // Add playing class directly to DOM for instant visual feedback
      if (noteElement) {
        noteElement.classList.add('your-melody__note--playing');
      }

      // Play the sound based on Y position
      playNoteByPosition(note.positionY);

      // Wait before next note
      await new Promise(resolve => setTimeout(resolve, tempo));

      // Remove playing class after note is done (clear ripple immediately)
      if (noteElement) {
        noteElement.classList.remove('your-melody__note--playing');
      }
    }

    // Restart user's continuous ripple after melody ends
    if (userNoteRef.current) {
      rippleRef.current = new RippleEffect(userNoteRef.current, {
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
  }, [isPlaying, displayNotes, userPositionX, userPositionY, isMobile, isTablet]);

  // Play only user's note
  const handlePlayUserNote = useCallback(async () => {
    // Initialize audio and wait for Safari to be ready
    await initAudio();

    // Add playing class directly to DOM
    if (userNoteRef.current) {
      userNoteRef.current.classList.add('your-melody__note--playing');
    }

    playNoteByPosition(userPositionY);

    // Remove playing class after sound plays
    setTimeout(() => {
      if (userNoteRef.current) {
        userNoteRef.current.classList.remove('your-melody__note--playing');
      }
    }, 800);
  }, [userPositionY]);

  // Navigation
  const handleGoBack = () => {
    navigate(ROUTES.EVENT_PLACE_NOTE);
  };

  const handleNext = () => {
    navigate(ROUTES.EVENT_CHOOSE_NOTE);
  };

  // Get user's diamond shape
  // HEART-01 = Heart, HEART-02 = Oval, HEART-03 = Round, HEART-04 = Pear
  // HEART-05 = Asscher, HEART-06 = Emerald, HEART-07 = Marquise
  const getUserDiamondShape = () => {
    if (selectedDiamond) {
      const shapeMap = {
        // Shape IDs (from EventChooseShapePage)
        h1: 'mirror_DMM/HEART-01.webp',  // Heart
        h2: 'mirror_DMM/HEART-02.webp',  // Oval
        h3: 'mirror_DMM/HEART-03.webp',  // Round
        h4: 'mirror_DMM/HEART-04.webp',  // Pear
        h5: 'mirror_DMM/HEART-05.webp',  // Asscher
        h6: 'mirror_DMM/HEART-06.webp',  // Emerald
        h7: 'mirror_DMM/HEART-07.webp',  // Marquise
        // Legacy shape names (from old /event flow)
        heart: 'mirror_DMM/HEART-01.webp',
        oval: 'mirror_DMM/HEART-02.webp',
        round: 'mirror_DMM/HEART-03.webp',
        pear: 'mirror_DMM/HEART-04.webp',
        asscher: 'mirror_DMM/HEART-05.webp',
        emerald: 'mirror_DMM/HEART-06.webp',
        marquise: 'mirror_DMM/HEART-07.webp',
      };
      return shapeMap[selectedDiamond] || 'mirror_DMM/HEART-01.webp';
    }
    return 'mirror_DMM/HEART-01.webp';
  };

  return (
    <>
      <NavbarV4 logoOnly />
      <div
        ref={containerRef}
        className={`your-melody ${isEntering ? 'your-melody--entering' : ''}`}
        data-navbar-theme="black"
      >
        {/* Background */}
        <div className="your-melody__bg" />

        {/* Header - Title and description at top left */}
        <header className={`your-melody__header ${!headerAnimated ? 'your-melody__header--hidden' : ''} ${headerAnimated && isAnimatingEntrance ? 'your-melody__header--animate' : ''}`}>
          <h2 className="heading-2--no-margin your-melody__title">Tương tác cùng Nốt sáng</h2>
          <p className="bodytext-6--no-margin your-melody__description">
            Bạn đã tạo nên Nốt sáng của riêng mình. Hãy trải nghiệm và lắng nghe giai điệu được hoàn thiện từ chính sự hiện diện của bạn.
          </p>
        </header>

        {/* Main content - Staff with notes */}
        <main className="your-melody__main">
          {/* Solock decoration - left side */}
          <img
            src={getMediaUrl('dmm/solock.webp')}
            alt=""
            className="your-melody__solock"
          />

          {/* Staff container */}
          <div className="your-melody__staff-container">
            <div className="your-melody__staff">
              {/* Staff 1: 5 lines (top) */}
              <div className="your-melody__lines your-melody__lines--staff1">
                {[0, 1, 2, 3, 4].map((i) => (
                  <div key={i} className="your-melody__line" />
                ))}
              </div>

              {/* Staff 2: 5 lines (bottom) - mobile only */}
              <div className="your-melody__lines your-melody__lines--staff2">
                {[0, 1, 2, 3, 4].map((i) => (
                  <div key={`staff2-${i}`} className="your-melody__line" />
                ))}
              </div>

              {/* Random notes (7) - show all notes on both staffs */}
              {/* Random notes - click/tap to play sound (works on iOS) */}
              {/* Hide first random note on mobile */}
              {displayNotes.map((note, index) => {
                // Skip first note on mobile
                if (isMobile && index === 0) return null;

                const xPos = getNoteXPosition(index, note.staffIndex || 0);
                const yPos = getNoteYPosition(note.positionY, note.staffIndex || 0);
                const isNoteAnimated = animatedNoteIndices.includes(index);
                const shouldHide = !isNoteAnimated; // Hidden until animated
                return (
                  <div
                    key={note.id}
                    ref={(el) => { noteRefs.current[note.id] = el; }}
                    className={`your-melody__note ${shouldHide ? 'your-melody__note--hidden' : ''} ${isNoteAnimated && isAnimatingEntrance ? 'your-melody__note--animate-slide' : ''} ${note.positionY === 0 ? 'your-melody__note--position-do' : ''}`}
                    style={{
                      left: `${xPos}%`,
                      top: yPos,
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
                      alt="Diamond note"
                      className="your-melody__note-img"
                    />
                    {/* <span className="your-melody__note-name">{getNoteName(note.positionY)}</span> */}
                  </div>
                );
              })}

              {/* User's note (on staff 2 for mobile/tablet, staff 1 for desktop) */}
              {/* Click/tap to play sound (works on iOS) */}
              <div
                ref={userNoteRef}
                className={`your-melody__note your-melody__note--user ${!userNoteAnimated ? 'your-melody__note--hidden' : ''} ${userNoteAnimated && isAnimatingEntrance ? 'your-melody__note--animate-fly' : ''} ${userPositionY === 0 ? 'your-melody__note--position-do' : ''}`}
                style={{
                  left: `${getNoteXPosition(7, 1)}%`,
                  top: getNoteYPosition(userPositionY, (isMobile || isTablet) ? 1 : 0),
                  cursor: 'pointer',
                }}
                onClick={async () => {
                  if (isPlaying) return;
                  await initAudio();
                  playNoteByPosition(userPositionY);
                }}
                onTouchEnd={async (e) => {
                  if (isPlaying) return;
                  e.preventDefault();
                  await initAudio();
                  playNoteByPosition(userPositionY);
                }}
              >
                <img
                  src={getMediaUrl(getUserDiamondShape())}
                  alt="Your note"
                  className="your-melody__note-img your-melody__note-img--user"
                />
                {/* <span className="your-melody__note-name">{getNoteName(userPositionY)}</span> */}
              </div>
            </div>
          </div>
        </main>

        {/* Back and Next buttons */}
        <EventBackButton onClick={handleGoBack} />
        <EventNextButton onClick={handleNext} />

        <EdgeNextButton
          onClick={handleNext}
          title="Tương tác cùng Nốt sáng"
          description="Bạn đã tạo nên Nốt sáng của riêng mình. Hãy trải nghiệm và lắng nghe giai điệu được hoàn thiện từ chính sự hiện diện của bạn."
        />

        {/* Footer - action buttons */}
        <footer className="your-melody__footer">
          {/* Action buttons */}
          <div className="your-melody__actions">
            {/* Hint text - mobile only */}
            <p className="your-melody__hint bodytext-6--no-margin">Chạm vào nốt để nghe</p>
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
        </footer>

        {/* Hidden Avatar Generator - continue pre-generate for your-wallpaper page */}
        {shouldGenerateAvatar && (
          <AvatarGenerator
            displayName={user?.displayName || 'Guest'}
            lightNumber={user?.lightNumber || 1}
            diamondShape={currentShape}
            onGenerated={(url) => setGeneratedAvatar(url, currentShape)}
            delay={1000}
          />
        )}
      </div>
    </>
  );
};

export default WriteMessageScreenNew;
