/**
 * ChristmasMusicPage - Standalone page for Christmas music visualization
 * Supports both 2D (SVG) and 3D (Three.js) versions
 */
import React, { useState, Suspense } from 'react';
import ChristmasMusicSheet from '@components/event/ui/ChristmasMusicSheet';
import '@styles/event.css';

// Lazy load 3D component (heavy)
const ChristmasMusic3D = React.lazy(() =>
  import('@components/event/ui/ChristmasMusic3D')
);

const ChristmasMusicPage = () => {
  const [loopCount, setLoopCount] = useState(0);
  const [use3D, setUse3D] = useState(true); // Default to 3D version

  const handleComplete = () => {
    setLoopCount((prev) => prev + 1);
  };

  const toggleVersion = () => {
    setUse3D((prev) => !prev);
  };

  return (
    <div className="christmas-music-page">
      {/* Version toggle button */}
      <button className="christmas-version-toggle" onClick={toggleVersion}>
        {use3D ? 'Chuyển sang 2D' : 'Chuyển sang 3D'}
      </button>

      {use3D ? (
        <Suspense
          fallback={
            <div className="christmas-loading">
              <span>Đang tải 3D...</span>
            </div>
          }
        >
          <ChristmasMusic3D onComplete={handleComplete} />
        </Suspense>
      ) : (
        <ChristmasMusicSheet
          autoPlay={false}
          showTitle={true}
          onComplete={handleComplete}
        />
      )}

      {/* Loop counter */}
      {loopCount > 0 && (
        <div className="christmas-loop-counter">
          Played: {loopCount} times
        </div>
      )}
    </div>
  );
};

export default ChristmasMusicPage;
