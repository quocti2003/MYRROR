import React, { useState, useRef, useEffect, useCallback } from "react";
import { FaceLandmarker, FilesetResolver } from "@mediapipe/tasks-vision";
import "./BirthdayCake.css";

const BirthdayCake = () => {
  const [candlesLit, setCandlesLit] = useState([true, true, true, true, true]);
  const [isBlowing, setIsBlowing] = useState(false);
  const [cameraActive, setCameraActive] = useState(false);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [allBlownOut, setAllBlownOut] = useState(false);
  const [debugInfo, setDebugInfo] = useState({ jawOpen: 0, mouthFunnel: 0, mouthPucker: 0 });

  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const faceLandmarkerRef = useRef(null);
  const streamRef = useRef(null);
  const animationRef = useRef(null);
  const blowingFramesRef = useRef(0);

  // Initialize MediaPipe Face Landmarker
  const initFaceLandmarker = async () => {
    setLoading(true);
    setMessage("Loading face detection...");

    try {
      const vision = await FilesetResolver.forVisionTasks(
        "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm"
      );

      faceLandmarkerRef.current = await FaceLandmarker.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath: "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task",
          delegate: "GPU"
        },
        runningMode: "VIDEO",
        numFaces: 1,
        outputFaceBlendshapes: true,
      });

      setMessage("Face detection ready!");
      setTimeout(() => setMessage(""), 2000);
    } catch (error) {
      console.error("Failed to initialize face landmarker:", error);
      setMessage("Failed to load face detection. Please refresh.");
    } finally {
      setLoading(false);
    }
  };

  // Start camera
  const startCamera = async () => {
    if (!faceLandmarkerRef.current) {
      await initFaceLandmarker();
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { width: 640, height: 480, facingMode: "user" }
      });

      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.onloadeddata = () => {
          setCameraActive(true);
          detectFace();
        };
      }
    } catch (error) {
      console.error("Camera access denied:", error);
      setMessage("Please allow camera access to blow out the candles!");
    }
  };

  // Stop camera
  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    if (animationRef.current) {
      cancelAnimationFrame(animationRef.current);
    }
    setCameraActive(false);
  };

  // Detect face and blowing gesture
  const detectFace = useCallback(() => {
    if (!faceLandmarkerRef.current || !videoRef.current || !cameraActive) return;

    const video = videoRef.current;

    if (video.readyState >= 2) {
      const results = faceLandmarkerRef.current.detectForVideo(video, performance.now());

      if (results.faceBlendshapes && results.faceBlendshapes.length > 0) {
        const blendshapes = results.faceBlendshapes[0].categories;

        // Find mouth-related blendshapes
        const jawOpen = blendshapes.find(b => b.categoryName === "jawOpen")?.score || 0;
        const mouthFunnel = blendshapes.find(b => b.categoryName === "mouthFunnel")?.score || 0;
        const mouthPucker = blendshapes.find(b => b.categoryName === "mouthPucker")?.score || 0;
        const mouthShrugUpper = blendshapes.find(b => b.categoryName === "mouthShrugUpper")?.score || 0;
        const mouthClose = blendshapes.find(b => b.categoryName === "mouthClose")?.score || 0;

        // Update debug info
        setDebugInfo({
          jawOpen: jawOpen.toFixed(2),
          mouthFunnel: mouthFunnel.toFixed(2),
          mouthPucker: mouthPucker.toFixed(2),
          mouthShrugUpper: mouthShrugUpper.toFixed(2)
        });

        // Detect blowing: multiple conditions for different blow styles
        // 1. Mouth puckered (like kissing/blowing) - mouthPucker > 0.2
        // 2. Mouth funnel (O shape) - mouthFunnel > 0.15
        // 3. Mouth shrug with slight opening - preparing to blow
        const isBlowingGesture =
          mouthPucker > 0.15 ||
          mouthFunnel > 0.1 ||
          (mouthShrugUpper > 0.2 && jawOpen > 0.05 && jawOpen < 0.4);

        if (isBlowingGesture) {
          blowingFramesRef.current++;

          // Reduced frames needed for quicker response
          if (blowingFramesRef.current > 5) {
            setIsBlowing(true);
            blowOutCandle();
            blowingFramesRef.current = 0; // Reset after blowing one candle
          }
        } else {
          blowingFramesRef.current = Math.max(0, blowingFramesRef.current - 1);
          if (blowingFramesRef.current === 0) {
            setIsBlowing(false);
          }
        }
      } else {
        // No face detected
        setDebugInfo({ jawOpen: '-', mouthFunnel: '-', mouthPucker: '-', mouthShrugUpper: '-' });
      }
    }

    animationRef.current = requestAnimationFrame(detectFace);
  }, [cameraActive]);

  // Blow out one candle at a time
  const blowOutCandle = () => {
    setCandlesLit(prev => {
      const newState = [...prev];
      const litIndex = newState.findIndex(lit => lit);
      if (litIndex !== -1) {
        newState[litIndex] = false;
      }
      return newState;
    });
  };

  // Check if all candles are blown out
  useEffect(() => {
    if (candlesLit.every(lit => !lit) && !allBlownOut) {
      setAllBlownOut(true);
      setMessage("Happy Birthday! Make a wish! 🎉");
      stopCamera();
    }
  }, [candlesLit, allBlownOut]);

  // Start detection loop when camera becomes active
  useEffect(() => {
    if (cameraActive) {
      detectFace();
    }
    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [cameraActive, detectFace]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopCamera();
      if (faceLandmarkerRef.current) {
        faceLandmarkerRef.current.close();
      }
    };
  }, []);

  // Reset game
  const resetGame = () => {
    setCandlesLit([true, true, true, true, true]);
    setAllBlownOut(false);
    setMessage("");
    blowingFramesRef.current = 0;
  };

  return (
    <div className="birthday-cake-container">
      <div className="birthday-scene">
        {/* Message */}
        {message && (
          <div className={`birthday-message ${allBlownOut ? 'celebration' : ''}`}>
            {message}
          </div>
        )}

        {/* Camera Feed */}
        <div className={`camera-container ${cameraActive ? 'active' : ''}`}>
          <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            className="camera-video"
          />
          <canvas ref={canvasRef} className="camera-canvas" />
          {isBlowing && <div className="blowing-indicator">Blowing detected!</div>}

          {/* Debug Info */}
          <div className="debug-info">
            <div>Jaw: {debugInfo.jawOpen}</div>
            <div>Funnel: {debugInfo.mouthFunnel}</div>
            <div>Pucker: {debugInfo.mouthPucker}</div>
            <div>Shrug: {debugInfo.mouthShrugUpper}</div>
          </div>
        </div>

        {/* Birthday Cake */}
        <div className="cake-wrapper">
          <div className="cake">
            {/* Candles */}
            <div className="candles">
              {candlesLit.map((isLit, index) => (
                <div key={index} className={`candle candle-${index + 1}`}>
                  <div className="candle-body"></div>
                  <div className="candle-wick"></div>
                  {isLit ? (
                    <div className="flame-container">
                      <div className="flame">
                        <div className="flame-inner"></div>
                      </div>
                      <div className="flame-glow"></div>
                    </div>
                  ) : (
                    <div className="smoke">
                      <div className="smoke-particle"></div>
                      <div className="smoke-particle"></div>
                      <div className="smoke-particle"></div>
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* Cake Layers */}
            <div className="cake-top">
              <div className="frosting">
                <div className="drip"></div>
                <div className="drip"></div>
                <div className="drip"></div>
                <div className="drip"></div>
                <div className="drip"></div>
              </div>
            </div>
            <div className="cake-middle"></div>
            <div className="cake-bottom">
              <div className="cake-decoration">
                <span>Happy Birthday!</span>
              </div>
            </div>
            <div className="cake-plate"></div>
          </div>
        </div>

        {/* Confetti when all blown out */}
        {allBlownOut && (
          <div className="confetti-container">
            {[...Array(50)].map((_, i) => (
              <div
                key={i}
                className="confetti"
                style={{
                  left: `${Math.random() * 100}%`,
                  animationDelay: `${Math.random() * 2}s`,
                  backgroundColor: ['#ff6b6b', '#ffd93d', '#6bcb77', '#4d96ff', '#ff6b9d'][Math.floor(Math.random() * 5)]
                }}
              />
            ))}
          </div>
        )}

        {/* Controls */}
        <div className="controls">
          {!cameraActive && !allBlownOut && (
            <button
              className="camera-button"
              onClick={startCamera}
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="loading-spinner"></span>
                  Loading...
                </>
              ) : (
                <>
                  <span className="camera-icon">📷</span>
                  Start Camera to Blow Candles
                </>
              )}
            </button>
          )}

          {cameraActive && !allBlownOut && (
            <div className="instruction">
              <p>Pucker your lips and blow towards the camera!</p>
              <button className="stop-button" onClick={stopCamera}>
                Stop Camera
              </button>
            </div>
          )}

          {allBlownOut && (
            <button className="reset-button" onClick={resetGame}>
              🎂 Light Candles Again
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default BirthdayCake;
