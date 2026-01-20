import "./Lumex91.css";
import ShineGlassButton from "@components/common/button/ShineGlassButton";
import ShinyText from "@components/common/shiny-text/ShinyText";
import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { getNewsDetailRoute } from "@/constants/routes";
import { optimizedTransitionUtils } from "@utils/transitionUtil/optimizedTransitionUtils";

const TOTAL_FRAMES = 361;
const FRAME_PATH = "/home-page/lumex91-frames/frame_";
const BATCH_SIZE = 50; // Load 50 frames per batch for better performance

const Lumex91 = ({ externalProgress = null }) => {
  const [framesLoaded, setFramesLoaded] = useState(false);
  const [loadedCount, setLoadedCount] = useState(0); // Track loading progress
  const [shouldLoadFrames, setShouldLoadFrames] = useState(false);
  const videoBoxRef = useRef(null);
  const canvasRef = useRef(null);
  const imagesRef = useRef(new Array(TOTAL_FRAMES).fill(null));
  const lastRenderedFrameRef = useRef(-1);
  const navigate = useNavigate();

  // Preload all frames when approaching viewport (earlier trigger)
  useEffect(() => {
    if (!videoBoxRef.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setShouldLoadFrames(true);
            observer.disconnect();
          }
        });
      },
      {
        rootMargin: "1500px", // Load much earlier - when ~1500px away from viewport
        threshold: 0,
      }
    );

    observer.observe(videoBoxRef.current);

    return () => observer.disconnect();
  }, []);

  // Batch load frame images for better performance
  useEffect(() => {
    if (!shouldLoadFrames) return;

    const loadImage = (index) => {
      return new Promise((resolve) => {
        const img = new Image();
        const frameNum = String(index).padStart(3, "0");
        img.src = `${FRAME_PATH}${frameNum}.webp`;
        img.onload = () => {
          imagesRef.current[index - 1] = img; // Store directly in ref (0-indexed)
          resolve(img);
        };
        img.onerror = () => resolve(null);
      });
    };

    // Load frames in batches
    const loadBatch = async (startIndex, endIndex) => {
      const promises = [];
      for (let i = startIndex; i <= Math.min(endIndex, TOTAL_FRAMES); i++) {
        promises.push(loadImage(i));
      }
      await Promise.all(promises);
      setLoadedCount(Math.min(endIndex, TOTAL_FRAMES));
    };

    // Load first batch immediately, then continue in background
    const loadAllBatches = async () => {
      // First batch - load immediately for quick display
      await loadBatch(1, BATCH_SIZE);
      setFramesLoaded(true); // Can start displaying after first batch

      // Load remaining batches in background
      let nextStart = BATCH_SIZE + 1;
      while (nextStart <= TOTAL_FRAMES) {
        await loadBatch(nextStart, nextStart + BATCH_SIZE - 1);
        nextStart += BATCH_SIZE;
      }
    };

    loadAllBatches();
  }, [shouldLoadFrames]);

  // Draw frame to canvas - optimized to only redraw when frame changes
  const drawFrame = (frameIndex) => {
    if (!canvasRef.current) return;

    // Skip if same frame already rendered
    if (frameIndex === lastRenderedFrameRef.current) return;

    // Get image, or find nearest loaded frame if not yet loaded
    let image = imagesRef.current[frameIndex - 1]; // frames are 1-indexed

    // If requested frame not loaded, find nearest loaded frame
    if (!image) {
      for (let i = frameIndex - 1; i >= 0; i--) {
        if (imagesRef.current[i]) {
          image = imagesRef.current[i];
          break;
        }
      }
    }

    if (!image) return;

    lastRenderedFrameRef.current = frameIndex;

    const canvas = canvasRef.current;
    const context = canvas.getContext("2d", { alpha: true });

    // Set canvas size to match container
    const container = videoBoxRef.current;
    if (container) {
      const rect = container.getBoundingClientRect();
      if (canvas.width !== rect.width || canvas.height !== rect.height) {
        canvas.width = rect.width;
        canvas.height = rect.height;
      }
    }

    // Clear canvas before drawing new frame
    context.clearRect(0, 0, canvas.width, canvas.height);

    // Calculate cover sizing (like object-fit: cover)
    const scaleX = canvas.width / image.width;
    const scaleY = canvas.height / image.height;
    const scale = Math.max(scaleX, scaleY);

    const scaledWidth = image.width * scale;
    const scaledHeight = image.height * scale;

    const offsetX = (canvas.width - scaledWidth) / 2;
    const offsetY = (canvas.height - scaledHeight) / 2;

    context.drawImage(image, offsetX, offsetY, scaledWidth, scaledHeight);
  };

  // Update frame based on external progress - only on scroll, not continuous RAF
  useEffect(() => {
    if (!framesLoaded || externalProgress === null) return;

    const frameIndex = Math.min(
      TOTAL_FRAMES,
      Math.max(1, Math.ceil(externalProgress * TOTAL_FRAMES))
    );

    requestAnimationFrame(() => {
      drawFrame(frameIndex);
    });
  }, [framesLoaded, externalProgress]);

  // Handle resize - redraw current frame
  useEffect(() => {
    if (!framesLoaded) return;

    const handleResize = () => {
      // Force redraw by resetting last rendered frame
      const currentFrame = lastRenderedFrameRef.current;
      lastRenderedFrameRef.current = -1;
      if (currentFrame > 0) {
        requestAnimationFrame(() => {
          drawFrame(currentFrame);
        });
      }
    };

    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, [framesLoaded]);

  // Draw first frame when loaded
  useEffect(() => {
    if (framesLoaded && imagesRef.current.length > 0) {
      requestAnimationFrame(() => {
        drawFrame(1);
      });
    }
  }, [framesLoaded]);

  const handleExploreClick = async () => {
    await optimizedTransitionUtils.transitionToRoute(
      navigate,
      getNewsDetailRoute("milan")
    );
  };

  return (
    <section className="lumex91">
      <div className="lumex91-container">
        <div className="lumex91-video-box" ref={videoBoxRef}>
          {/* Placeholder image - shown while frames loading */}
          <img
            className={`lumex91-video lumex91-placeholder ${
              framesLoaded ? "lumex91-placeholder-hide" : ""
            }`}
            src={`${FRAME_PATH}001.webp`}
            alt="Mirror-Lumex 91"
          />

          {/* Canvas for smooth frame rendering */}
          <canvas
            ref={canvasRef}
            className={`lumex91-video lumex91-canvas ${
              framesLoaded ? "lumex91-canvas-visible" : ""
            }`}
          />
        </div>

        <div className="lumex91-content">
          <h1 className="heading-1--no-margin">
            <ShinyText text="Mirror Lumex - 91™" speed={2} />
          </h1>
          <ShineGlassButton theme="footer" onClick={handleExploreClick}>
            Explore more
          </ShineGlassButton>
        </div>
      </div>
    </section>
  );
};

export default Lumex91;
