/**
 * TestNotesPage - Test page to verify 7 notes (1 octave)
 * Shows notes from Đô (C4) to Si (B4) with Harp sound
 */
import { useState } from 'react';
import { initAudio, playNoteByName, NOTE_NAMES_VI, ALL_NOTES } from '@services/event/audio';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';
import './TestNotesPage.css';

// Diamond shapes for visual variety (7 shapes for 7 notes)
const DIAMOND_SHAPES = [
  'mirror_DMM/HEART-01.webp',
  'mirror_DMM/HEART-02.webp',
  'mirror_DMM/HEART-03.webp',
  'mirror_DMM/HEART-04.webp',
  'mirror_DMM/HEART-05.webp',
  'mirror_DMM/HEART-06.webp',
  'mirror_DMM/HEART-07.webp',
];

const TestNotesPage = () => {
  const [playingNote, setPlayingNote] = useState(null);
  const [isPlayingScale, setIsPlayingScale] = useState(false);

  // Play a single note
  const handlePlayNote = async (noteName) => {
    await initAudio();
    setPlayingNote(noteName);
    playNoteByName(noteName);

    setTimeout(() => {
      setPlayingNote(null);
    }, 800);
  };

  // Play full scale Đô → Sí (14 notes)
  const handlePlayScale = async () => {
    if (isPlayingScale) return;

    await initAudio();
    setIsPlayingScale(true);

    for (let i = 0; i < ALL_NOTES.length; i++) {
      const note = ALL_NOTES[i];
      setPlayingNote(note);
      playNoteByName(note);
      await new Promise(resolve => setTimeout(resolve, 600));
      setPlayingNote(null);
      await new Promise(resolve => setTimeout(resolve, 100));
    }

    setIsPlayingScale(false);
  };

  // Play reverse scale Sí → Đô
  const handlePlayReverseScale = async () => {
    if (isPlayingScale) return;

    await initAudio();
    setIsPlayingScale(true);

    const reversed = [...ALL_NOTES].reverse();
    for (let i = 0; i < reversed.length; i++) {
      const note = reversed[i];
      setPlayingNote(note);
      playNoteByName(note);
      await new Promise(resolve => setTimeout(resolve, 600));
      setPlayingNote(null);
      await new Promise(resolve => setTimeout(resolve, 100));
    }

    setIsPlayingScale(false);
  };

  // All 7 notes (single octave)
  const allNotes = ALL_NOTES; // C4 - B4 (Đô - Si)

  return (
    <div className="test-notes">
      <h1 className="test-notes__title">Test 7 Nốt Nhạc</h1>
      <p className="test-notes__subtitle">
        Quãng tám 1: Đô → Si (C4 - B4) | Âm thanh: Harp 🎵
      </p>

      {/* Control buttons */}
      <div className="test-notes__controls">
        <button
          className="test-notes__btn"
          onClick={handlePlayScale}
          disabled={isPlayingScale}
        >
          ▶ Chơi thang âm (Đô → Si)
        </button>
        <button
          className="test-notes__btn test-notes__btn--secondary"
          onClick={handlePlayReverseScale}
          disabled={isPlayingScale}
        >
          ◀ Chơi ngược (Si → Đô)
        </button>
      </div>

      {/* 7 notes: C4 - B4 */}
      <div className="test-notes__octave">
        <h2 className="test-notes__octave-title">Đô - Rê - Mi - Pha - Son - La - Si</h2>
        <div className="test-notes__notes-row">
          {allNotes.map((noteName, index) => {
            const vnName = NOTE_NAMES_VI[noteName];
            const isPlaying = playingNote === noteName;
            const shapeIndex = index % DIAMOND_SHAPES.length;

            return (
              <div
                key={noteName}
                className={`test-notes__note-card ${isPlaying ? 'test-notes__note-card--playing' : ''}`}
                onClick={() => handlePlayNote(noteName)}
              >
                <img
                  src={getMediaUrl(DIAMOND_SHAPES[shapeIndex])}
                  alt={vnName}
                  className="test-notes__note-img"
                />
                <span className="test-notes__note-vn">{vnName}</span>
                <span className="test-notes__note-en">{noteName}</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Reference table */}
      <div className="test-notes__reference">
        <h2>Bảng tham chiếu 7 nốt</h2>
        <div className="test-notes__table-container">
          <table className="test-notes__table">
            <thead>
              <tr>
                <th>STT</th>
                <th>Nốt</th>
                <th>Tên VN</th>
                <th>Tần số (Hz)</th>
              </tr>
            </thead>
            <tbody>
              {allNotes.map((note, idx) => (
                <tr
                  key={note}
                  className={playingNote === note ? 'test-notes__row--playing' : ''}
                  onClick={() => handlePlayNote(note)}
                >
                  <td>{idx + 1}</td>
                  <td><strong>{note}</strong></td>
                  <td className="test-notes__cell-vn">{NOTE_NAMES_VI[note]}</td>
                  <td>
                    {note === 'C4' && '261.63'}
                    {note === 'D4' && '293.66'}
                    {note === 'E4' && '329.63'}
                    {note === 'F4' && '349.23'}
                    {note === 'G4' && '392.00'}
                    {note === 'A4' && '440.00'}
                    {note === 'B4' && '493.88'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default TestNotesPage;
