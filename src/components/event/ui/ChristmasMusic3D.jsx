/**
 * ChristmasMusic3D - 3D Music Sheet Visualization using Three.js
 * Features:
 * - Three.js 3D rendering with React Three Fiber
 * - Bloom post-processing effect for glowing notes
 * - Web Audio API for synchronized audio playback
 * - Camera animation following the music
 */
import React, { useRef, useState, useEffect, useCallback } from 'react';
import { Canvas, useFrame, useThree } from '@react-three/fiber';
import { Line } from '@react-three/drei';
import { EffectComposer, Bloom } from '@react-three/postprocessing';
import * as THREE from 'three';
import {
  MELODY_NOTES,
  BASS_NOTES,
  ALL_NOTES,
  SONG_INFO,
  SONG_TEMPO,
  NOTE_FREQUENCIES,
  calculateNotePosition,
} from '@/constants/christmasSong';

// ============================================
// Constants
// ============================================
const BEAT_WIDTH = 0.5; // Width per beat in 3D units
const STAFF_LINE_GAP = 0.08; // Smaller staff for better proportion
const NOTE_RADIUS = 0.05; // Proportional to staff

// Luxury color palette
const TREBLE_COLOR = '#E8B4B8'; // Rose gold
const BASS_COLOR = '#B8D4E8';   // Ice platinum
const STAFF_COLOR = '#8B7355';  // Warm bronze
const PAPER_COLOR = '#F5F0E8';  // Cream ivory

// ============================================
// Helper Functions
// ============================================
const getNoteY = (pitch, clef) => {
  const position = calculateNotePosition(pitch, clef);
  return position * (STAFF_LINE_GAP / 2);
};

const getNoteX = (startBeat) => {
  return startBeat * BEAT_WIDTH;
};

// ============================================
// 3D Components
// ============================================

// Luxury note - diamond/gem style
const Note3D = ({ note, index, isActive, glowIntensity }) => {
  const meshRef = useRef();
  const ringRef = useRef();

  const x = getNoteX(note.startBeat);
  const y = getNoteY(note.pitch, note.clef);
  const z = note.clef === 'treble' ? 0.08 : -0.08;

  // Luxury colors - rose gold for treble, platinum for bass
  const baseColor = note.clef === 'treble' ? '#5C4033' : '#4A4A4A'; // Bronze / Charcoal
  const glowColor = note.clef === 'treble' ? '#D4AF37' : '#C0C0C0'; // Gold / Silver
  const accentColor = note.clef === 'treble' ? '#E8B4B8' : '#B8D4E8'; // Rose / Ice

  useFrame((state) => {
    if (meshRef.current && isActive) {
      // Subtle pulse
      const scale = 1 + Math.sin(state.clock.elapsedTime * 8) * 0.1;
      meshRef.current.scale.setScalar(scale);
    } else if (meshRef.current) {
      meshRef.current.scale.setScalar(1);
    }

    // Rotating ring when active
    if (ringRef.current && isActive) {
      ringRef.current.rotation.z += 0.02;
    }
  });

  const isGlowing = glowIntensity > 0 || isActive;
  const finalGlow = isActive ? 1 : glowIntensity;

  return (
    <group position={[x, y, z]}>
      {/* Subtle outer aura */}
      {isGlowing && (
        <mesh>
          <ringGeometry args={[NOTE_RADIUS * 1.5, NOTE_RADIUS * 2.5 * finalGlow, 32]} />
          <meshBasicMaterial
            color={glowColor}
            transparent
            opacity={finalGlow * 0.15}
            side={THREE.DoubleSide}
          />
        </mesh>
      )}

      {/* Elegant ring accent */}
      {isGlowing && (
        <mesh ref={ringRef} rotation={[Math.PI / 2, 0, 0]}>
          <torusGeometry args={[NOTE_RADIUS * 1.2, 0.008, 8, 32]} />
          <meshStandardMaterial
            color={accentColor}
            emissive={accentColor}
            emissiveIntensity={finalGlow * 2}
            metalness={0.9}
            roughness={0.1}
            toneMapped={false}
          />
        </mesh>
      )}

      {/* Main note - small elegant sphere */}
      <mesh ref={meshRef}>
        <sphereGeometry args={[NOTE_RADIUS * 0.7, 32, 32]} />
        <meshStandardMaterial
          color={isGlowing ? glowColor : baseColor}
          emissive={glowColor}
          emissiveIntensity={isGlowing ? finalGlow * 2 : 0}
          metalness={0.3}
          roughness={0.4}
          toneMapped={false}
        />
      </mesh>

      {/* Inner sparkle */}
      {isGlowing && (
        <mesh>
          <sphereGeometry args={[NOTE_RADIUS * 0.3, 16, 16]} />
          <meshBasicMaterial
            color="#ffffff"
            transparent
            opacity={finalGlow * 0.8}
          />
        </mesh>
      )}
    </group>
  );
};

// Staff lines
const StaffLines = ({ startX, endX, y, z }) => {
  const lines = [];

  for (let i = 0; i < 5; i++) {
    const lineY = y + i * STAFF_LINE_GAP;
    lines.push(
      <Line
        key={`staff-line-${z}-${i}`}
        points={[[startX, lineY, z], [endX, lineY, z]]}
        color={STAFF_COLOR}
        lineWidth={1}
      />
    );
  }

  return <>{lines}</>;
};

// Bar lines
const BarLines = ({ beats, trebleY, bassY }) => {
  const barBeats = [0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42];

  return (
    <>
      {barBeats.map((beat, index) => {
        const x = getNoteX(beat);
        return (
          <Line
            key={`bar-${index}`}
            points={[
              [x, trebleY - STAFF_LINE_GAP, 0],
              [x, trebleY + 4 * STAFF_LINE_GAP + STAFF_LINE_GAP, 0],
            ]}
            color={STAFF_COLOR}
            lineWidth={index === 0 || index === barBeats.length - 1 ? 2 : 1}
          />
        );
      })}
    </>
  );
};

// Elegant sheet background - cream with subtle sheen
const SheetPaper = ({ width, height }) => {
  return (
    <mesh position={[width / 2 - 1, 0, -0.05]} rotation={[0, 0, 0]}>
      <planeGeometry args={[width + 2, height]} />
      <meshStandardMaterial
        color={PAPER_COLOR}
        metalness={0.05}
        roughness={0.9}
      />
    </mesh>
  );
};

// Group notes by beat to find simultaneous notes
const groupNotesByBeat = (notes) => {
  const groups = [];
  let currentGroup = [];
  let currentBeat = -1;

  notes.forEach((note, idx) => {
    if (Math.abs(note.startBeat - currentBeat) < 0.1) {
      // Same beat - add to current group
      currentGroup.push({ note, idx });
    } else {
      // New beat - save previous group and start new one
      if (currentGroup.length > 0) {
        groups.push(currentGroup);
      }
      currentGroup = [{ note, idx }];
      currentBeat = note.startBeat;
    }
  });

  if (currentGroup.length > 0) {
    groups.push(currentGroup);
  }

  return groups;
};

// Luxury ball component - sleek with light trail
const SingleBall = ({ position, scale, color = "#D4AF37", isTreble = true }) => {
  const trailRef = useRef();

  // Luxury color palette
  const coreColor = isTreble ? '#D4AF37' : '#E5E4E2'; // Gold / Platinum
  const glowColor = isTreble ? '#E8B4B8' : '#B8D4E8'; // Rose / Ice blue
  const innerColor = '#FFFFFF';

  useFrame((state) => {
    if (trailRef.current) {
      trailRef.current.rotation.z = state.clock.elapsedTime * 2;
    }
  });

  return (
    <group position={position}>
      {/* Elegant outer ring - rotating */}
      <mesh ref={trailRef} scale={[scale, scale, scale]} rotation={[Math.PI / 2, 0, 0]}>
        <torusGeometry args={[0.06, 0.004, 8, 48]} />
        <meshStandardMaterial
          color={coreColor}
          emissive={coreColor}
          emissiveIntensity={3}
          metalness={1}
          roughness={0}
          toneMapped={false}
        />
      </mesh>

      {/* Subtle aura */}
      <mesh scale={[scale, scale, scale]}>
        <ringGeometry args={[0.04, 0.08, 32]} />
        <meshBasicMaterial
          color={glowColor}
          transparent
          opacity={0.2}
          side={THREE.DoubleSide}
        />
      </mesh>

      {/* Main ball - small and refined */}
      <mesh scale={[scale, scale, scale]}>
        <sphereGeometry args={[0.035, 32, 32]} />
        <meshStandardMaterial
          color={coreColor}
          emissive={coreColor}
          emissiveIntensity={2}
          metalness={0.8}
          roughness={0.1}
          toneMapped={false}
        />
      </mesh>

      {/* Inner bright core */}
      <mesh scale={[scale * 0.6, scale * 0.6, scale * 0.6]}>
        <sphereGeometry args={[0.02, 16, 16]} />
        <meshBasicMaterial
          color={innerColor}
          transparent
          opacity={0.9}
        />
      </mesh>

      {/* Light streak effect */}
      <mesh scale={[scale, scale, scale]} rotation={[0, 0, Math.PI / 4]}>
        <planeGeometry args={[0.15, 0.008]} />
        <meshBasicMaterial
          color={coreColor}
          transparent
          opacity={0.4}
          side={THREE.DoubleSide}
        />
      </mesh>
      <mesh scale={[scale, scale, scale]} rotation={[0, 0, -Math.PI / 4]}>
        <planeGeometry args={[0.12, 0.006]} />
        <meshBasicMaterial
          color={innerColor}
          transparent
          opacity={0.3}
          side={THREE.DoubleSide}
        />
      </mesh>
    </group>
  );
};

// Bouncing ball(s) that LEADS the music - splits when multiple notes
const BouncingBall = ({ allNotes, visible, onBallHitNote }) => {
  const groupRef = useRef();
  const lastHitGroupRef = useRef(-1);
  const lastBeatRef = useRef(-1); // Track last beat to detect new notes
  const animationRef = useRef({ startTime: null });
  const [ballPositions, setBallPositions] = useState([{ x: 0, y: 0.5, z: 0.1, scale: 1, color: "#FFD700" }]);

  // Group notes by beat
  const noteGroups = React.useMemo(() => groupNotesByBeat(allNotes), [allNotes]);

  // Seconds per beat based on tempo
  const SECONDS_PER_BEAT = 60 / SONG_TEMPO;

  useFrame((state) => {
    if (!visible) return;

    const now = state.clock.elapsedTime;

    if (animationRef.current.startTime === null) {
      animationRef.current.startTime = now;
    }

    const elapsed = now - animationRef.current.startTime;

    // Convert elapsed time to current beat position
    const currentBeat = elapsed / SECONDS_PER_BEAT;

    // Find current group based on beat position
    let currentGroupIdx = 0;
    let groupProgress = 0;

    for (let i = 0; i < noteGroups.length; i++) {
      const group = noteGroups[i];
      const groupBeat = group[0].note.startBeat;
      const nextGroupBeat = noteGroups[i + 1]?.[0]?.note.startBeat ?? (groupBeat + Math.max(...group.map(g => g.note.duration)));

      if (currentBeat >= groupBeat && currentBeat < nextGroupBeat) {
        currentGroupIdx = i;
        const groupDuration = nextGroupBeat - groupBeat;
        groupProgress = (currentBeat - groupBeat) / groupDuration;
        break;
      }

      if (currentBeat < groupBeat) {
        // Before first note
        currentGroupIdx = Math.max(0, i - 1);
        break;
      }

      currentGroupIdx = i;
    }

    if (currentGroupIdx >= noteGroups.length) {
      currentGroupIdx = noteGroups.length - 1;
      groupProgress = 1;
    }

    const currentGroup = noteGroups[currentGroupIdx];
    const nextGroup = noteGroups[currentGroupIdx + 1];

    if (!currentGroup) return;

    // Trigger sound when entering a new group (more robust detection)
    const currentGroupBeat = currentGroup[0].note.startBeat;
    const justEnteredGroup = lastHitGroupRef.current !== currentGroupIdx &&
                             currentBeat >= currentGroupBeat;

    if (justEnteredGroup) {
      lastHitGroupRef.current = currentGroupIdx;
      lastBeatRef.current = currentGroupBeat;
      currentGroup.forEach(({ idx, note }) => {
        onBallHitNote?.(idx, note);
      });
    }

    const numBalls = currentGroup.length;
    const nextNumBalls = nextGroup?.length || 1;

    // Calculate positions for each ball
    const newPositions = [];

    if (nextGroup && groupProgress > 0.6) {
      // Jumping to next group
      const jumpT = (groupProgress - 0.6) / 0.4;
      const easeT = jumpT < 0.5
        ? 2 * jumpT * jumpT
        : 1 - Math.pow(-2 * jumpT + 2, 2) / 2;

      // Determine how balls split/merge
      if (numBalls === 1 && nextNumBalls === 2) {
        // Split: 1 ball becomes 2
        const fromNote = currentGroup[0].note;
        const fromX = getNoteX(fromNote.startBeat);
        const fromY = getNoteY(fromNote.pitch, fromNote.clef);
        const fromZ = fromNote.clef === 'treble' ? 0.1 : -0.06;

        nextGroup.forEach(({ note }, i) => {
          const toX = getNoteX(note.startBeat);
          const toY = getNoteY(note.pitch, note.clef);
          const toZ = note.clef === 'treble' ? 0.1 : -0.06;

          const arcHeight = 0.15 + Math.abs(toY - fromY) * 0.3;
          const arc = Math.sin(jumpT * Math.PI) * arcHeight;

          const splitOffset = (i === 0 ? -1 : 1) * easeT * 0.03;

          newPositions.push({
            x: fromX + (toX - fromX) * easeT,
            y: fromY + (toY - fromY) * easeT + arc + 0.06,
            z: fromZ + (toZ - fromZ) * easeT + splitOffset,
            scale: 0.6 + easeT * 0.4,
            isTreble: note.clef === 'treble'
          });
        });
      } else if (numBalls === 2 && nextNumBalls === 1) {
        // Merge: 2 balls become 1
        const toNote = nextGroup[0].note;
        const toX = getNoteX(toNote.startBeat);
        const toY = getNoteY(toNote.pitch, toNote.clef);
        const toZ = toNote.clef === 'treble' ? 0.1 : -0.06;

        currentGroup.forEach(({ note }, i) => {
          const fromX = getNoteX(note.startBeat);
          const fromY = getNoteY(note.pitch, note.clef);
          const fromZ = note.clef === 'treble' ? 0.1 : -0.06;

          const arcHeight = 0.15 + Math.abs(toY - fromY) * 0.3;
          const arc = Math.sin(jumpT * Math.PI) * arcHeight;

          newPositions.push({
            x: fromX + (toX - fromX) * easeT,
            y: fromY + (toY - fromY) * easeT + arc + 0.06,
            z: fromZ + (toZ - fromZ) * easeT,
            scale: 1 - easeT * 0.25,
            isTreble: note.clef === 'treble'
          });
        });

        // Add merged ball appearing
        if (easeT > 0.7) {
          const mergeScale = (easeT - 0.7) / 0.3;
          newPositions.push({
            x: toX,
            y: toY + 0.06 + Math.sin(jumpT * Math.PI) * 0.1,
            z: toZ,
            scale: mergeScale,
            isTreble: true
          });
        }
      } else {
        // Same number of balls - normal jump
        currentGroup.forEach(({ note }, i) => {
          const fromX = getNoteX(note.startBeat);
          const fromY = getNoteY(note.pitch, note.clef);
          const fromZ = note.clef === 'treble' ? 0.1 : -0.06;

          const targetNote = nextGroup[Math.min(i, nextGroup.length - 1)].note;
          const toX = getNoteX(targetNote.startBeat);
          const toY = getNoteY(targetNote.pitch, targetNote.clef);
          const toZ = targetNote.clef === 'treble' ? 0.1 : -0.06;

          const arcHeight = 0.15 + Math.abs(toY - fromY) * 0.3;
          const arc = Math.sin(jumpT * Math.PI) * arcHeight;

          newPositions.push({
            x: fromX + (toX - fromX) * easeT,
            y: fromY + (toY - fromY) * easeT + arc + 0.06,
            z: fromZ + (toZ - fromZ) * easeT,
            scale: 1 + Math.sin(jumpT * Math.PI) * 0.2,
            isTreble: note.clef === 'treble'
          });
        });
      }
    } else {
      // Sitting on current notes
      currentGroup.forEach(({ note }, i) => {
        const noteX = getNoteX(note.startBeat);
        const noteY = getNoteY(note.pitch, note.clef);
        const noteZ = note.clef === 'treble' ? 0.1 : -0.06;

        const bounce = Math.sin(groupProgress * Math.PI * 2) * 0.01;
        let scale = 1;
        if (groupProgress < 0.15) {
          scale = 1.2 - groupProgress * 1.3;
        }

        newPositions.push({
          x: noteX,
          y: noteY + 0.06 + bounce,
          z: noteZ,
          scale,
          isTreble: note.clef === 'treble'
        });
      });
    }

    setBallPositions(newPositions);
  });

  if (!visible) return null;

  return (
    <group ref={groupRef}>
      {ballPositions.map((pos, i) => (
        <SingleBall
          key={i}
          position={[pos.x, pos.y, pos.z]}
          scale={pos.scale}
          isTreble={pos.isTreble !== false}
        />
      ))}
    </group>
  );
};

// Camera controller that follows the bouncing ball
const CameraController = ({ currentBeat, isPlaying, ballX }) => {
  const { camera } = useThree();
  const lookAtRef = useRef(new THREE.Vector3(0, 0, 0));
  const camPosRef = useRef(new THREE.Vector3(0, 1, 0.7));

  useFrame(() => {
    if (isPlaying && currentBeat >= 0) {
      // Camera follows ball - closer for larger view
      const targetX = ballX - 0.3;  // Slightly behind ball
      const targetY = 1;            // Lower, closer
      const targetZ = 0.7;          // Much closer

      // Very smooth camera position
      camPosRef.current.x = THREE.MathUtils.lerp(camPosRef.current.x, targetX, 0.03);
      camPosRef.current.y = THREE.MathUtils.lerp(camPosRef.current.y, targetY, 0.03);
      camPosRef.current.z = THREE.MathUtils.lerp(camPosRef.current.z, targetZ, 0.03);

      camera.position.copy(camPosRef.current);

      // Look ahead of ball for diagonal perspective
      const targetLookAtX = ballX + 0.5;  // Look ahead
      lookAtRef.current.x = THREE.MathUtils.lerp(lookAtRef.current.x, targetLookAtX, 0.03);
      lookAtRef.current.y = 0;
      lookAtRef.current.z = 0;

      camera.lookAt(lookAtRef.current);
    }
  });

  return null;
};

// Title text
const TitleText = () => {
  return (
    <Text
      position={[-1, 2, 0]}
      fontSize={0.3}
      color="#333333"
      anchorX="left"
      anchorY="middle"
      font="/fonts/Inter-Bold.woff"
    >
      We Wish You A Merry Christmas
    </Text>
  );
};

// ============================================
// Main Scene
// ============================================
const MusicScene = ({ isPlaying, activeNoteIdx, glowingNotes, onBallHitNote, sortedNotes, playKey }) => {
  const totalBeats = SONG_INFO.totalBeats;
  const sheetWidth = totalBeats * BEAT_WIDTH + 4;
  const [ballX, setBallX] = useState(0);

  const trebleY = 0.25;  // Closer together
  const bassY = -0.25;   // Smaller gap between staves

  // Update ballX when active note changes
  useEffect(() => {
    if (activeNoteIdx >= 0 && sortedNotes[activeNoteIdx]) {
      setBallX(getNoteX(sortedNotes[activeNoteIdx].startBeat));
    } else if (activeNoteIdx === -1) {
      setBallX(0); // Reset when stopped/replaying
    }
  }, [activeNoteIdx, sortedNotes]);

  return (
    <>
      {/* Elegant lighting setup */}
      <ambientLight intensity={0.3} color="#ffffff" />
      <directionalLight position={[5, 8, 5]} intensity={0.6} color="#fff5e6" />
      <pointLight position={[0, 5, 2]} intensity={0.4} color="#D4AF37" />
      <pointLight position={[-5, 3, -3]} intensity={0.2} color="#E8B4B8" />

      {/* Sheet music group - rotated to lay flat horizontally (on XZ plane) */}
      <group rotation={[-Math.PI / 2, 0, 0]}>
        {/* Sheet paper background */}
        <SheetPaper width={sheetWidth} height={1.2} />

        {/* Treble staff */}
        <StaffLines startX={-0.5} endX={sheetWidth - 1} y={trebleY} z={0} />

        {/* Bass staff */}
        <StaffLines startX={-0.5} endX={sheetWidth - 1} y={bassY} z={0} />

        {/* Bar lines */}
        <BarLines beats={totalBeats} trebleY={trebleY} bassY={bassY} />

        {/* All notes - using sortedNotes for consistent indexing */}
        {sortedNotes.map((note, index) => {
          const isActive = index === activeNoteIdx;
          const glowData = glowingNotes.find((g) => g.index === index);
          const glowIntensity = glowData?.intensity || 0;

          return (
            <Note3D
              key={`note-${index}`}
              note={note}
              index={index}
              isActive={isActive}
              glowIntensity={glowIntensity}
            />
          );
        })}

        {/* Bouncing ball - the LEADER */}
        <BouncingBall
          key={playKey}
          allNotes={sortedNotes}
          visible={isPlaying}
          onBallHitNote={onBallHitNote}
        />
      </group>

      {/* Camera controller */}
      <CameraController key={playKey} currentBeat={0} isPlaying={isPlaying} ballX={ballX} />
    </>
  );
};

// ============================================
// Main Component
// ============================================
const ChristmasMusic3D = ({ onComplete }) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [activeNoteIdx, setActiveNoteIdx] = useState(-1);
  const [glowingNotes, setGlowingNotes] = useState([]);
  const [hasPlayed, setHasPlayed] = useState(false);
  const [playKey, setPlayKey] = useState(0); // Key to force remount BouncingBall

  const audioCtxRef = useRef(null);
  const audioIframeRef = useRef(null);
  const glowTimeoutsRef = useRef([]);

  // Sort notes by startBeat for ball to follow
  const sortedNotes = React.useMemo(() =>
    [...ALL_NOTES].sort((a, b) => a.startBeat - b.startBeat),
    []
  );

  // Initialize Web Audio API using iframe (to avoid iJewel3d SDK conflicts)
  const initAudio = async () => {
    if (audioCtxRef.current && audioCtxRef.current.state !== 'closed') {
      if (audioCtxRef.current.state === 'suspended') {
        await audioCtxRef.current.resume();
      }
      return audioCtxRef.current;
    }

    try {
      const iframe = document.createElement('iframe');
      iframe.style.cssText = 'position:absolute;width:0;height:0;border:0;pointer-events:none;';
      iframe.setAttribute('aria-hidden', 'true');
      document.body.appendChild(iframe);
      audioIframeRef.current = iframe;

      await new Promise((resolve) => {
        if (iframe.contentWindow) resolve();
        else iframe.onload = resolve;
      });

      const iframeWindow = iframe.contentWindow;
      const AC = iframeWindow.AudioContext || iframeWindow.webkitAudioContext;
      if (!AC) return null;

      audioCtxRef.current = new AC();
      if (audioCtxRef.current.state === 'suspended') {
        await audioCtxRef.current.resume();
      }
      return audioCtxRef.current;
    } catch (error) {
      console.error('Failed to init audio:', error);
      return null;
    }
  };

  // Play a single note
  const playNote = useCallback((pitch, duration = 0.3, isBass = false) => {
    const ctx = audioCtxRef.current;
    if (!ctx) return;

    if (ctx.state === 'suspended') ctx.resume();

    try {
      const frequency = NOTE_FREQUENCIES[pitch];
      if (!frequency) return;

      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = isBass ? 'triangle' : 'sine';
      osc.frequency.setValueAtTime(frequency, now);

      const volume = isBass ? 0.25 : 0.45;
      gain.gain.setValueAtTime(0, now);
      gain.gain.linearRampToValueAtTime(volume, now + 0.02);
      gain.gain.linearRampToValueAtTime(volume * 0.5, now + 0.1);
      gain.gain.linearRampToValueAtTime(0, now + duration);

      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + duration + 0.05);
    } catch (error) {
      console.error('Failed to play note:', error);
    }
  }, []);

  // Find the last beat in the song
  const lastNoteBeat = React.useMemo(() => {
    let maxBeat = 0;
    sortedNotes.forEach(note => {
      const noteEnd = note.startBeat + note.duration;
      if (noteEnd > maxBeat) maxBeat = noteEnd;
    });
    return maxBeat;
  }, [sortedNotes]);

  // Ball hits a note - THIS triggers sound and glow
  const handleBallHitNote = useCallback((noteIdx, note) => {
    // Play sound
    const duration = note.duration * (60 / SONG_TEMPO);
    playNote(note.pitch, duration, note.clef === 'bass');

    // Set active note
    setActiveNoteIdx(noteIdx);

    // Add to glowing notes with fade out
    setGlowingNotes(prev => {
      const filtered = prev.filter(g => g.index !== noteIdx);
      return [...filtered, { index: noteIdx, intensity: 1 }];
    });

    // Fade out glow after note duration
    const fadeTimeout = setTimeout(() => {
      const fadeSteps = 10;
      const fadeInterval = 50;
      let step = 0;

      const fadeTimer = setInterval(() => {
        step++;
        const intensity = 1 - (step / fadeSteps);

        if (step >= fadeSteps) {
          clearInterval(fadeTimer);
          setGlowingNotes(prev => prev.filter(g => g.index !== noteIdx));
        } else {
          setGlowingNotes(prev =>
            prev.map(g => g.index === noteIdx ? { ...g, intensity } : g)
          );
        }
      }, fadeInterval);

      glowTimeoutsRef.current.push(fadeTimer);
    }, duration * 1000);

    glowTimeoutsRef.current.push(fadeTimeout);

    // Check if this is the last note (based on beat position, not index)
    const noteEndBeat = note.startBeat + note.duration;
    if (noteEndBeat >= lastNoteBeat - 0.1) {
      setTimeout(() => {
        setIsPlaying(false);
        setActiveNoteIdx(-1);
        setGlowingNotes([]);
        onComplete?.();
      }, duration * 1000 + 500);
    }
  }, [playNote, lastNoteBeat, onComplete]);

  // Cleanup
  useEffect(() => {
    return () => {
      glowTimeoutsRef.current.forEach(t => clearTimeout(t));
      if (audioCtxRef.current?.state !== 'closed') {
        audioCtxRef.current?.close().catch(() => {});
      }
      if (audioIframeRef.current?.parentNode) {
        audioIframeRef.current.parentNode.removeChild(audioIframeRef.current);
      }
    };
  }, []);

  // Stop playback
  const stopPlayback = useCallback(() => {
    glowTimeoutsRef.current.forEach(t => clearTimeout(t));
    glowTimeoutsRef.current = [];
    setIsPlaying(false);
    setActiveNoteIdx(-1);
    setGlowingNotes([]);
  }, []);

  // Start playback
  const startPlayback = useCallback(async () => {
    const ctx = await initAudio();
    if (!ctx) {
      console.error('Failed to initialize audio');
      return;
    }

    glowTimeoutsRef.current.forEach(t => clearTimeout(t));
    glowTimeoutsRef.current = [];
    setActiveNoteIdx(-1);
    setGlowingNotes([]);
    setPlayKey(prev => prev + 1); // Force BouncingBall to remount and reset
    setIsPlaying(true);
    setHasPlayed(true);
  }, []);

  return (
    <div className="christmas-music-3d">
      {/* Title */}
      <div className="christmas-3d-title">
        <h2>We Wish You A Merry Christmas</h2>
      </div>

      {/* 3D Canvas - Luxury dark theme */}
      <Canvas
        camera={{ position: [-0.3, 1, 0.7], fov: 55, near: 0.1, far: 100 }}
        gl={{ antialias: true, alpha: true }}
        style={{ background: 'linear-gradient(180deg, #0d0d0d 0%, #1a1a1a 50%, #0d0d0d 100%)' }}
      >
        <MusicScene
          isPlaying={isPlaying}
          activeNoteIdx={activeNoteIdx}
          glowingNotes={glowingNotes}
          onBallHitNote={handleBallHitNote}
          sortedNotes={sortedNotes}
          playKey={playKey}
        />

        {/* Subtle, elegant bloom effect */}
        <EffectComposer>
          <Bloom
            intensity={1.5}
            luminanceThreshold={0.2}
            luminanceSmoothing={0.9}
            mipmapBlur
            radius={0.4}
          />
        </EffectComposer>
      </Canvas>

      {/* Play/Stop button overlay */}
      {!isPlaying && (
        <div className="christmas-3d-overlay">
          <button
            className="christmas-3d-play-btn"
            onClick={startPlayback}
          >
            <svg viewBox="0 0 24 24" width="48" height="48">
              {hasPlayed ? (
                // Replay icon
                <path d="M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z" fill="currentColor" />
              ) : (
                // Play icon
                <polygon points="5,3 19,12 5,21" fill="currentColor" />
              )}
            </svg>
            <span>{hasPlayed ? 'Phát lại' : 'Phát nhạc'}</span>
          </button>
        </div>
      )}

      {isPlaying && (
        <div className="christmas-3d-controls">
          <button className="christmas-3d-stop-btn" onClick={stopPlayback}>
            Dừng
          </button>
        </div>
      )}
    </div>
  );
};

export default ChristmasMusic3D;
