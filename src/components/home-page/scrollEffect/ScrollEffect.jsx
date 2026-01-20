import { useState, useEffect, useRef, useCallback } from "react";
import { useLocation } from "react-router-dom";
import Logo from "@assets/images/Logo.svg";
import { ROUTES } from "@/constants/routes";
import GlassThemeButton from "@/components/common/button/GlassThemeButton";
import { useBottomTheme } from "@/hooks/useBottomTheme";
import "./ScrollEffect.css";

// Throttle utility for scroll performance
const useThrottle = (callback, delay) => {
  const lastCall = useRef(0);
  const lastArgs = useRef(null);
  const timeoutRef = useRef(null);

  return useCallback((...args) => {
    const now = Date.now();
    lastArgs.current = args;

    if (now - lastCall.current >= delay) {
      lastCall.current = now;
      callback(...args);
    } else if (!timeoutRef.current) {
      // Schedule trailing call
      timeoutRef.current = setTimeout(() => {
        lastCall.current = Date.now();
        timeoutRef.current = null;
        callback(...lastArgs.current);
      }, delay - (now - lastCall.current));
    }
  }, [callback, delay]);
};

export default function ScrollEffect({ isAnyOverlayOpen = false }) {
  const location = useLocation();
  const { theme: arrowTheme } = useBottomTheme();
  const isImmersiveShowroomPage =
    location.pathname === ROUTES.IMMERSIVE_SHOWROOM;

  // Original ScrollEffect refs
  const finalGradientRef = useRef(null);
  const finalGradientTopRef = useRef(null);
  const finalGradientBottomRef = useRef(null);
  const gradientInitialRef = useRef(null);
  const mainLogoRef = useRef(null);
  const containerRef = useRef(null);

  // Split animation refs
  const splitTopRef = useRef(null);
  const splitBottomRef = useRef(null);

  // MirrorIntroduce state and refs
  const canvasRef = useRef(null);
  const phase2CanvasLayerRef = useRef(null);
  const mirrorIntroduceSectionRef = useRef(null);
  const [images, setImages] = useState([]);
  const [frameIndex, setFrameIndex] = useState(0);
  const [text1AutoProgress, setText1AutoProgress] = useState(0);
  const [hasStartedScrolling, setHasStartedScrolling] = useState(false);
  const lastRenderedFrameRef = useRef(-1);
  const [isArrowVisible, setIsArrowVisible] = useState(true);
  const [isImmersiveCollapsed, setIsImmersiveCollapsed] = useState(false);
  const isAutoScrollingRef = useRef(false);
  const lastScrollYRef = useRef(0);

  // Smooth frame interpolation refs
  const targetFrameRef = useRef(0);
  const displayedFrameRef = useRef(0);
  const animationFrameRef = useRef(null);
  const framesPerTick = 8; // Max frames to advance per animation tick (controls smoothness vs speed)

  const numFrames = 480; // Actual frames from 251222_60FPS_1080x1920.mp4 (10s * 60fps)
  const scrollEffectHeight = 250; // vh for scroll effect - reduced to make mirror introduce start earlier
  const mirrorIntroduceHeight = 600; // vh for mirror introduce

  // Throttled handler for immersive collapse (16ms = ~60fps)
  const handleImmersiveCollapse = useCallback(() => {
    const scrollY = window.scrollY;
    setIsImmersiveCollapsed(scrollY > 100);
  }, []);

  const throttledImmersiveCollapse = useThrottle(handleImmersiveCollapse, 16);

  // Detect scroll to collapse immersive button
  useEffect(() => {
    window.addEventListener("scroll", throttledImmersiveCollapse, { passive: true });
    // Initial check
    handleImmersiveCollapse();

    return () => window.removeEventListener("scroll", throttledImmersiveCollapse);
  }, [throttledImmersiveCollapse, handleImmersiveCollapse]);

  // Smooth frame interpolation animation loop
  useEffect(() => {
    const animateFrames = () => {
      const target = targetFrameRef.current;
      const current = displayedFrameRef.current;

      if (current !== target) {
        // Calculate direction and step
        const diff = target - current;
        const step = Math.sign(diff) * Math.min(Math.abs(diff), framesPerTick);
        const newFrame = current + step;

        displayedFrameRef.current = newFrame;
        setFrameIndex(newFrame);
      }

      animationFrameRef.current = requestAnimationFrame(animateFrames);
    };

    animationFrameRef.current = requestAnimationFrame(animateFrames);

    return () => {
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
    };
  }, []);

  // Handle arrow click - scroll to next section
  const handleArrowClick = () => {
    const scrollY = window.scrollY;
    const windowHeight = window.innerHeight;
    const scrollEffectEnd = (scrollEffectHeight / 100) * windowHeight;
    const totalHeight =
      ((scrollEffectHeight + mirrorIntroduceHeight) / 100) * windowHeight;

    // Determine which section we're in and scroll to next
    if (scrollY < scrollEffectEnd) {
      // Currently in ScrollEffect section -> scroll to where first text appears
      // Text 1 starts auto-appearing right after split (250vh), so scroll a bit further to see text
      const textStartPosition = scrollEffectEnd + windowHeight * 0.5; // 250vh + 50vh buffer
      window.scrollTo({
        top: textStartPosition,
        behavior: "smooth",
      });
    } else if (scrollY < totalHeight - 100) {
      // Currently in MirrorIntroduce section -> scroll to next page section (after this component)
      // Add 100px buffer so that once we're near the end, we switch to section-based navigation
      window.scrollTo({
        top: totalHeight,
        behavior: "smooth",
      });
    } else {
      // Already past ScrollEffect component -> find all sections and scroll to next one
      const sections = Array.from(document.querySelectorAll("[data-section]"));
      let nextSection = null;

      // Find the first section that starts below current scroll position
      for (let i = 0; i < sections.length; i++) {
        const section = sections[i];
        const rect = section.getBoundingClientRect();

        // If section top is more than 50px below viewport top, it's the next section
        if (rect.top > 50) {
          nextSection = section;
          break;
        }
      }

      // If found next section, scroll to it; otherwise scroll one viewport
      if (nextSection) {
        const rect = nextSection.getBoundingClientRect();
        const sectionName = nextSection.getAttribute("data-section");
        const sectionTop = scrollY + rect.top;
        const sectionBottom = sectionTop + rect.height;

        let targetPosition;

        // For contact-us section: scroll to end (bottom of section touches bottom of viewport)
        if (sectionName === "contact-us") {
          targetPosition = sectionBottom - windowHeight;
        } else {
          // Default: scroll to top of section
          targetPosition = sectionTop;
        }

        window.scrollTo({
          top: targetPosition,
          behavior: "smooth",
        });
      } else {
        // No next section found, scroll one viewport
        window.scrollTo({
          top: scrollY + windowHeight,
          behavior: "smooth",
        });
      }
    }
  };

  // Text slides data
  const textSlides = [
    {
      titleParts: [
        "Welcome to the Mirrorverse,",
        "where is built upon a single,",
        "powerful element: ",
      ],
      highlight: "reflection.",
      subtitle: "",
    },
    {
      title: "Reflection of Beauty",
      subtitle:
        "From the sparkle on the surface to the brilliance within, Mirror celebrates the harmony between outer elegance and inner light.",
    },
    {
      title: "Reflection of Self",
      subtitle:
        "A quiet moment of encounter - where you face yourself in all dimensions: past, present, and becoming.",
    },
    {
      title: "Reflection of Artistry and Innovation",
      subtitle:
        "Each creation is shaped with technological precision and human soul - where machine intelligence and intuition craft in unison.",
    },
  ];

  // Function to get frame path
  function getFramePath(index) {
    return `/home-page/frames-new/frame_${index
      .toString()
      .padStart(4, "0")}.webp`;
  }

  // Progressive preload images - load in batches
  function preloadImages() {
    const batchSize = 50; // Increased from 20 to 50 for better performance
    const loadedImagesArray = new Array(numFrames);
    let loadedCount = 0;

    const loadBatch = (startIndex, endIndex) => {
      const promises = [];
      for (let i = startIndex; i <= Math.min(endIndex, numFrames); i++) {
        const promise = new Promise((resolve, reject) => {
          const img = new Image();
          img.onload = () => {
            loadedImagesArray[i - 1] = img;
            loadedCount++;
            resolve(img);
          };
          img.onerror = reject;
          img.src = getFramePath(i);
        });
        promises.push(promise);
      }
      return Promise.all(promises);
    };

    // Load first batch immediately
    loadBatch(1, batchSize)
      .then(() => {
        setImages([...loadedImagesArray]);

        // Progressively load remaining batches in background
        let nextBatch = batchSize + 1;
        const intervalId = setInterval(() => {
          if (nextBatch <= numFrames) {
            loadBatch(nextBatch, nextBatch + batchSize - 1)
              .then(() => {
                setImages([...loadedImagesArray]);
              })
              .catch(() => {
                // Image batch load failed silently
              });
            nextBatch += batchSize;
          } else {
            clearInterval(intervalId);
          }
        }, 200); // Reduced interval from 300ms to 200ms for faster loading
      })
      .catch(() => {
        // Initial image load failed silently
      });
  }

  useEffect(() => {
    // Check if we need to scroll to top
    if (sessionStorage.getItem("scrollToTop") === "true") {
      window.scrollTo(0, 0);
      sessionStorage.removeItem("scrollToTop");
    }

    // Preload images
    preloadImages();
  }, []);

  // Auto-scroll in Phase 1: scroll down -> go to Phase 2, scroll up -> go to top
  useEffect(() => {
    let autoScrollTimeout = null;

    const handleAutoScroll = () => {
      const scrollY = window.scrollY;
      const windowHeight = window.innerHeight;
      const scrollEffectEnd = (scrollEffectHeight / 100) * windowHeight;

      // Check if scrolling down or up
      const isScrollingDown = scrollY > lastScrollYRef.current;
      lastScrollYRef.current = scrollY;

      // If auto-scroll is running, check if user is manually scrolling against auto-scroll direction
      if (isAutoScrollingRef.current) {
        // Cancel auto-scroll if user scrolls in opposite direction
        const autoScrollingDown = isAutoScrollingRef.current === "down";
        const autoScrollingUp = isAutoScrollingRef.current === "up";

        if (
          (autoScrollingDown && !isScrollingDown) ||
          (autoScrollingUp && isScrollingDown)
        ) {
          isAutoScrollingRef.current = false;
          if (autoScrollTimeout) {
            clearTimeout(autoScrollTimeout);
            autoScrollTimeout = null;
          }
        }
        return;
      }

      // Check if user is in Phase 1
      const isInPhase1 = scrollY > 0 && scrollY < scrollEffectEnd;

      if (!isInPhase1) return;

      // Determine which half of Phase 1 user is in
      const phase1MidPoint = scrollEffectEnd * 0.5;
      const isInLowerHalf = scrollY >= phase1MidPoint;
      const isInUpperHalf = scrollY < phase1MidPoint;

      // SCROLLING DOWN: Auto-scroll to Phase 2 (text start position)
      if (isScrollingDown && isInUpperHalf) {
        // Only trigger if user was near the top (< 50% of Phase 1)
        isAutoScrollingRef.current = "down";

        // Auto-scroll to where text 1 appears (250vh + 50vh buffer)
        const textStartPosition = scrollEffectEnd + windowHeight * 0.5;

        window.scrollTo({
          top: textStartPosition,
          behavior: "smooth",
        });

        // Reset flag after scroll completes (smooth scroll takes ~1.5s)
        autoScrollTimeout = setTimeout(() => {
          isAutoScrollingRef.current = false;
        }, 1500);
      }
      // SCROLLING UP: Auto-scroll to top
      else if (!isScrollingDown && isInLowerHalf) {
        // Only trigger if user is in lower half scrolling up
        isAutoScrollingRef.current = "up";

        // Auto-scroll to top
        window.scrollTo({
          top: 0,
          behavior: "smooth",
        });

        // Reset flag after scroll completes
        autoScrollTimeout = setTimeout(() => {
          isAutoScrollingRef.current = false;
        }, 1500);
      }
    };

    window.addEventListener("scroll", handleAutoScroll, { passive: true });
    return () => {
      window.removeEventListener("scroll", handleAutoScroll);
      if (autoScrollTimeout) {
        clearTimeout(autoScrollTimeout);
      }
    };
  }, [scrollEffectHeight]);

  useEffect(() => {
    let ticking = false;

    const updateScroll = () => {
      if (!containerRef.current) {
        ticking = false;
        return;
      }

      const scrollY = window.scrollY;
      const windowHeight = window.innerHeight;

      // Calculate which section we're in
      const scrollEffectEnd = (scrollEffectHeight / 100) * windowHeight; // Use scrollEffectHeight variable

      // Phase 1: ScrollEffect (0 to scrollEffectHeight vh)
      if (scrollY <= scrollEffectEnd) {
        const progress = scrollY / scrollEffectEnd;

        // Make Phase 2 canvas visible early so transparent center shows it
        if (phase2CanvasLayerRef.current) {
          // Fade in canvas as gradient wipe starts (0-30% progress)
          const canvasOpacity = Math.min(progress / 0.3, 1);
          phase2CanvasLayerRef.current.style.opacity = canvasOpacity;
        }

        // Keep mirror introduce text section invisible during Phase 1
        if (mirrorIntroduceSectionRef.current) {
          mirrorIntroduceSectionRef.current.style.opacity = 0;
        }

        // Original scroll effect animations
        if (finalGradientRef.current) {
          // Complete gradient wipe by 60% progress instead of 100%
          const wipeProgress = Math.min(progress / 0.6, 1);
          const wipePosition = 400 - wipeProgress * 350;
          finalGradientRef.current.style.setProperty(
            "--wipe-progress",
            `${wipePosition}%`
          );
        }
        if (finalGradientTopRef.current) {
          const wipeProgress = Math.min(progress / 0.6, 1);
          const wipePosition = 400 - wipeProgress * 350;
          finalGradientTopRef.current.style.setProperty(
            "--wipe-progress",
            `${wipePosition}%`
          );
        }
        if (finalGradientBottomRef.current) {
          const wipeProgress = Math.min(progress / 0.6, 1);
          const wipePosition = 400 - wipeProgress * 350;
          finalGradientBottomRef.current.style.setProperty(
            "--wipe-progress",
            `${wipePosition}%`
          );
        }

        if (mainLogoRef.current) {
          let logoOpacity = 1;

          if (progress >= 0.5 && progress <= 0.6) {
            // Logo fade out from 50% to 60% (175vh to 210vh) - earlier
            const logoFadeProgress = (progress - 0.5) / 0.1;
            logoOpacity = 1 - logoFadeProgress;
          } else if (progress > 0.6) {
            logoOpacity = 0;
          } else if (progress < 0.5) {
            logoOpacity = 1;
          }

          mainLogoRef.current.style.opacity = logoOpacity;
        }

        // Fade animation at end of scroll effect (93-100% progress)
        // Extended range to make animation smoother when scrolling back
        if (splitTopRef.current && splitBottomRef.current) {
          if (progress >= 0.93) {
            // Calculate fade progress with extended range (93-100% instead of 95-100%)
            const fadeProgress = Math.min((progress - 0.93) / 0.07, 1); // 0 to 1

            // Fade from center: top half fades upward, bottom half fades downward
            const maskPosition = fadeProgress * 100; // 0% to 100%

            // Top half: fade from 50vh (center) to 0vh (top)
            splitTopRef.current.style.maskImage = `linear-gradient(to top, transparent ${maskPosition}%, black 100%)`;
            splitTopRef.current.style.webkitMaskImage = `linear-gradient(to top, transparent ${maskPosition}%, black 100%)`;

            // Bottom half: fade from 50vh (center) to 100vh (bottom)
            splitBottomRef.current.style.maskImage = `linear-gradient(to bottom, transparent ${maskPosition}%, black 100%)`;
            splitBottomRef.current.style.webkitMaskImage = `linear-gradient(to bottom, transparent ${maskPosition}%, black 100%)`;

            // Fade out combined sections smoothly instead of instant display toggle
            const combinedOpacity = Math.max(0, 1 - fadeProgress * 2);
            if (finalGradientRef.current) {
              finalGradientRef.current.style.opacity = combinedOpacity;
            }
            if (gradientInitialRef.current) {
              gradientInitialRef.current.style.opacity = combinedOpacity;
            }

            // Fade in split sections smoothly
            const splitOpacity = Math.min(1, fadeProgress * 2);
            splitTopRef.current.style.opacity = splitOpacity;
            splitBottomRef.current.style.opacity = splitOpacity;

            // Fade in canvas layer during split animation
            if (phase2CanvasLayerRef.current) {
              // Ensure canvas is fully visible by end of split
              phase2CanvasLayerRef.current.style.opacity = 1;
            }

            // Much slower mirror introduce fade in - very gradual
            if (mirrorIntroduceSectionRef.current) {
              // Use stronger easing function to make opacity fade much slower
              const easedOpacity = Math.pow(fadeProgress, 4); // Higher power = much slower start
              mirrorIntroduceSectionRef.current.style.opacity = easedOpacity;
            }

            // Always show both to allow opacity transitions
            splitTopRef.current.style.display = "block";
            splitBottomRef.current.style.display = "block";
            if (finalGradientRef.current) {
              finalGradientRef.current.style.display = "block";
            }
            if (gradientInitialRef.current) {
              gradientInitialRef.current.style.display = "block";
            }
          } else {
            // Reset mask
            splitTopRef.current.style.maskImage = "none";
            splitTopRef.current.style.webkitMaskImage = "none";
            splitBottomRef.current.style.maskImage = "none";
            splitBottomRef.current.style.webkitMaskImage = "none";

            // Hide mirror introduce when fade is not active
            if (mirrorIntroduceSectionRef.current) {
              mirrorIntroduceSectionRef.current.style.opacity = 0;
            }

            // Reset canvas layer opacity based on early phase progress
            if (phase2CanvasLayerRef.current) {
              // Canvas fades in 0-30% progress
              const canvasOpacity = Math.min(progress / 0.3, 1);
              phase2CanvasLayerRef.current.style.opacity = canvasOpacity;
            }

            // Reset opacities
            splitTopRef.current.style.opacity = 0;
            splitBottomRef.current.style.opacity = 0;
            if (finalGradientRef.current) {
              finalGradientRef.current.style.opacity = 1;
            }
            if (gradientInitialRef.current) {
              gradientInitialRef.current.style.opacity = 1;
            }

            // Hide split sections, show combined sections
            splitTopRef.current.style.display = "none";
            splitBottomRef.current.style.display = "none";
            // Reset gradient layers display to block when scrolling back to Phase 1
            if (finalGradientRef.current) {
              finalGradientRef.current.style.display = "block";
            }
            if (gradientInitialRef.current) {
              gradientInitialRef.current.style.display = "block";
            }
          }
        }

        // Reset mirror introduce elements - set target to 0, animation loop will smoothly transition
        targetFrameRef.current = 0;
      }
      // Phase 2: MirrorIntroduce (after scrollEffectHeight vh)
      else {
        const mirrorScrolled = scrollY - scrollEffectEnd;
        const mirrorScrollRange =
          (mirrorIntroduceHeight / 100) * windowHeight - windowHeight;
        const mirrorProgress = Math.min(
          Math.max(mirrorScrolled / mirrorScrollRange, 0),
          1
        );

        // Calculate target frame index for video - animation loop will smoothly interpolate
        const index = Math.min(
          numFrames - 1,
          Math.floor(mirrorProgress * numFrames)
        );
        targetFrameRef.current = index;

        // Ensure Phase 2 canvas is fully visible
        if (phase2CanvasLayerRef.current) {
          phase2CanvasLayerRef.current.style.opacity = 1;
        }

        // Ensure all phase 1 elements are properly hidden
        if (mainLogoRef.current) {
          mainLogoRef.current.style.opacity = 0;
        }

        // Hide all Phase 1 gradient layers in Phase 2
        if (splitTopRef.current && splitBottomRef.current) {
          // Hide split sections completely in Phase 2
          splitTopRef.current.style.display = "none";
          splitBottomRef.current.style.display = "none";

          // Hide gradient layers
          if (finalGradientRef.current) {
            finalGradientRef.current.style.display = "none";
          }
          if (gradientInitialRef.current) {
            gradientInitialRef.current.style.display = "none";
          }
        }

        // Ensure mirror introduce is fully visible
        if (mirrorIntroduceSectionRef.current) {
          mirrorIntroduceSectionRef.current.style.opacity = 1;
        }
      }

      ticking = false;
    };

    const handleScroll = () => {
      if (!ticking) {
        window.requestAnimationFrame(updateScroll);
        ticking = true;
      }
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    handleScroll(); // Call once to set initial state
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Auto-animation for Text 1 when user starts scrolling
  useEffect(() => {
    const handleScrollStart = () => {
      const scrollY = window.scrollY;
      const scrollEffectEnd = (scrollEffectHeight / 100) * window.innerHeight;

      // Check if user has scrolled into mirror introduce section
      if (scrollY > scrollEffectEnd && !hasStartedScrolling) {
        setHasStartedScrolling(true);

        // Start auto animation - only fade in and pause (not move up)
        let startTime = null;
        const duration = 400; // 0.4 second to reach pause state

        const animate = (timestamp) => {
          if (!startTime) startTime = timestamp;
          const elapsed = timestamp - startTime;
          const progress = Math.min(elapsed / duration, 1);

          // Only animate to pause state (0.5 = shorter pause phase)
          setText1AutoProgress(Math.min(progress * 0.5, 0.5));

          if (progress < 1) {
            requestAnimationFrame(animate);
          }
        };

        requestAnimationFrame(animate);
      }
    };

    window.addEventListener("scroll", handleScrollStart, { passive: true });
    return () => window.removeEventListener("scroll", handleScrollStart);
  }, [hasStartedScrolling, scrollEffectHeight]);

  // Throttled handler for arrow visibility (32ms = ~30fps, less critical)
  const handleArrowVisibilityCheck = useCallback(() => {
    const footerSection = document.querySelector(
      '[data-section="contact-us"]'
    );

    if (footerSection) {
      const rect = footerSection.getBoundingClientRect();
      const scrollY = window.scrollY;
      const footerTop = scrollY + rect.top;

      // Hide arrow when we're within 100px of footer top
      if (scrollY >= footerTop - 100) {
        setIsArrowVisible(false);
      } else {
        setIsArrowVisible(true);
      }
    }
  }, []);

  const throttledArrowVisibility = useThrottle(handleArrowVisibilityCheck, 32);

  // Hide arrow button when scrolled to footer (ContactUs section)
  useEffect(() => {
    window.addEventListener("scroll", throttledArrowVisibility, { passive: true });
    handleArrowVisibilityCheck(); // Initial check

    return () => window.removeEventListener("scroll", throttledArrowVisibility);
  }, [throttledArrowVisibility, handleArrowVisibilityCheck]);

  // Update canvas when frame changes - only render if frame is different
  useEffect(() => {
    if (!canvasRef.current || !images.length) {
      return;
    }

    // Skip if same frame already rendered
    if (frameIndex === lastRenderedFrameRef.current) return;

    const image = images[frameIndex];
    if (!image) {
      return; // Image not loaded yet
    }

    lastRenderedFrameRef.current = frameIndex;

    requestAnimationFrame(() => {
      const canvas = canvasRef.current;
      if (!canvas) return;

      const context = canvas.getContext("2d", { alpha: false });

      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;

      const scaleX = canvas.width / image.width;
      const scaleY = canvas.height / image.height;
      const scale = Math.max(scaleX, scaleY);

      const scaledWidth = image.width * scale;
      const scaledHeight = image.height * scale;

      const offsetX = (canvas.width - scaledWidth) / 2;
      const offsetY = (canvas.height - scaledHeight) / 2;

      context.drawImage(image, offsetX, offsetY, scaledWidth, scaledHeight);
    });
  }, [frameIndex, images]);

  const renderTextSlides = () => {
    const scrollY = window.scrollY;
    const scrollEffectEnd = (scrollEffectHeight / 100) * window.innerHeight;

    if (scrollY <= scrollEffectEnd) return null;

    const mirrorScrolled = scrollY - scrollEffectEnd;
    const mirrorScrollRange =
      (mirrorIntroduceHeight / 100) * window.innerHeight - window.innerHeight;
    const scrollProgress = Math.min(
      Math.max(mirrorScrolled / mirrorScrollRange, 0),
      1
    );

    return textSlides.map((slide, index) => {
      let transform = "translateY(30%)";
      let opacity = 0;

      // Text timing logic with new animation schedule
      if (index === 0) {
        // Text 1: Hybrid animation - auto fade in + scroll-controlled move up
        if (hasStartedScrolling) {
          if (text1AutoProgress <= 0.375) {
            // Auto fade in phase (0-37.5% of auto animation) - back to longer fade in
            const fadeProgress = text1AutoProgress / 0.375;
            transform = `translateY(${(1 - fadeProgress) * 30}%)`;
            opacity = fadeProgress;
          } else if (text1AutoProgress < 0.5) {
            // Auto pause phase (25-50% of auto animation) - shorter pause
            transform = `translateY(0%)`;
            opacity = 1;
          } else {
            // Auto animation complete, now use scroll progress for move up + fade out
            if (scrollProgress <= 0.15) {
              // Stay in pause state until user scrolls more - earlier trigger
              transform = `translateY(0%)`;
              opacity = 1;
            } else {
              // Move up AND fade out phase controlled by scroll (15%+)
              const moveProgress = Math.min((scrollProgress - 0.15) / 0.1, 1);
              transform = `translateY(${-moveProgress * 20}%)`;
              opacity = 1 - moveProgress; // Fade out as it moves up
            }
          }
        } else {
          // Not started scrolling yet, hide text
          transform = `translateY(30%)`;
          opacity = 0;
        }
      } else if (index === 1) {
        // Text 2: fade in (25-35%), pause (35-49%), fade out (49-52%)
        if (scrollProgress < 0.25) {
          transform = "translateY(30%)";
          opacity = 0;
        } else if (scrollProgress <= 0.35) {
          // Fade in phase
          const fadeProgress = (scrollProgress - 0.25) / 0.1;
          transform = `translateY(${(1 - fadeProgress) * 30}%)`;
          opacity = fadeProgress;
        } else if (scrollProgress <= 0.49) {
          // Pause phase (extended: 35-49% = 14%)
          transform = `translateY(0%)`;
          opacity = 1;
        } else if (scrollProgress <= 0.52) {
          // Move up + fade out phase (short: 49-52% = 3%)
          const fadeProgress = (scrollProgress - 0.49) / 0.03;
          transform = `translateY(${-fadeProgress * 20}%)`;
          opacity = 1 - fadeProgress;
        } else {
          transform = "translateY(-20%)";
          opacity = 0;
        }
      } else if (index === 2) {
        // Text 3: fade in (52-62%), pause (62-76%), fade out (76-79%)
        if (scrollProgress < 0.52) {
          transform = "translateY(30%)";
          opacity = 0;
        } else if (scrollProgress <= 0.62) {
          // Fade in phase
          const fadeProgress = (scrollProgress - 0.52) / 0.1;
          transform = `translateY(${(1 - fadeProgress) * 30}%)`;
          opacity = fadeProgress;
        } else if (scrollProgress <= 0.76) {
          // Pause phase (extended: 62-76% = 14%)
          transform = `translateY(0%)`;
          opacity = 1;
        } else if (scrollProgress <= 0.79) {
          // Move up + fade out phase (short: 76-79% = 3%)
          const fadeProgress = (scrollProgress - 0.76) / 0.03;
          transform = `translateY(${-fadeProgress * 20}%)`;
          opacity = 1 - fadeProgress;
        } else {
          transform = "translateY(-20%)";
          opacity = 0;
        }
      } else if (index === 3) {
        // Text 4: slide up + fade in (79-89%), then stay visible
        if (scrollProgress < 0.79) {
          transform = "translateY(30%)";
          opacity = 0;
        } else if (scrollProgress <= 0.89) {
          // Slide up + fade in together
          const fadeProgress = (scrollProgress - 0.79) / 0.1;
          transform = `translateY(${(1 - fadeProgress) * 30}%)`;
          opacity = fadeProgress;
        } else {
          // Stay visible - no more animation
          transform = "translateY(0%)";
          opacity = 1;
        }
      }

      // Staggered slide for title and subtitle (Text 2, 3, 4)
      // Description slides slower than title - continues moving after title stops
      let titleOpacity = 1;
      let titleTransform = "translateY(0%)";
      let subtitleOpacity = 1;
      let subtitleTransform = "translateY(0%)";

      if (index === 1) {
        // Text 2: fade in (25-35%)
        if (scrollProgress >= 0.25 && scrollProgress <= 0.4) {
          // Title: slides up fast (finishes at 31%)
          const titleSlideProgress = Math.min(
            (scrollProgress - 0.25) / 0.06,
            1
          );
          titleTransform = `translateY(${(1 - titleSlideProgress) * 30}%)`;
          titleOpacity = Math.min((scrollProgress - 0.25) / 0.08, 1);

          // Subtitle: slides up much slower (finishes at 40%)
          const subtitleSlideProgress = Math.min(
            (scrollProgress - 0.25) / 0.15,
            1
          );
          subtitleTransform = `translateY(${
            (1 - subtitleSlideProgress) * 30
          }%)`;
          subtitleOpacity = Math.min((scrollProgress - 0.28) / 0.07, 1);
        }
      } else if (index === 2) {
        // Text 3: fade in (52-62%)
        if (scrollProgress >= 0.52 && scrollProgress <= 0.67) {
          // Title: slides up fast (finishes at 58%)
          const titleSlideProgress = Math.min(
            (scrollProgress - 0.52) / 0.06,
            1
          );
          titleTransform = `translateY(${(1 - titleSlideProgress) * 30}%)`;
          titleOpacity = Math.min((scrollProgress - 0.52) / 0.08, 1);

          // Subtitle: slides up much slower (finishes at 67%)
          const subtitleSlideProgress = Math.min(
            (scrollProgress - 0.52) / 0.15,
            1
          );
          subtitleTransform = `translateY(${
            (1 - subtitleSlideProgress) * 30
          }%)`;
          subtitleOpacity = Math.min((scrollProgress - 0.55) / 0.07, 1);
        }
      } else if (index === 3) {
        // Text 4: fade in (79-89%)
        if (scrollProgress >= 0.79 && scrollProgress <= 0.94) {
          // Title: slides up fast (finishes at 85%)
          const titleSlideProgress = Math.min(
            (scrollProgress - 0.79) / 0.06,
            1
          );
          titleTransform = `translateY(${(1 - titleSlideProgress) * 30}%)`;
          titleOpacity = Math.min((scrollProgress - 0.79) / 0.08, 1);

          // Subtitle: slides up much slower (finishes at 94%)
          const subtitleSlideProgress = Math.min(
            (scrollProgress - 0.79) / 0.15,
            1
          );
          subtitleTransform = `translateY(${
            (1 - subtitleSlideProgress) * 30
          }%)`;
          subtitleOpacity = Math.min((scrollProgress - 0.82) / 0.07, 1);
        }
      }

      return (
        <div
          key={index}
          className="text-slide"
          style={{
            transform,
            opacity,
            transition:
              index === 3
                ? "none"
                : "transform 0.3s ease-out, opacity 0.3s ease-out",
          }}
        >
          <div
            className={`slide-content ${
              index === 0 ? "slide-content-wide" : "slide-content-narrow"
            }`}
          >
            <h1
              className={`${
                index === 0 ? "heading-1--no-margin" : "heading-2--no-margin"
              } slide-title`}
              style={{
                opacity: titleOpacity,
                transform: titleTransform,
                transition:
                  index >= 1
                    ? "transform 0.3s ease-out, opacity 0.3s ease-out"
                    : "none",
              }}
            >
              {slide.titleParts ? (
                <>
                  {slide.titleParts.map((part, i) => (
                    <span key={i}>
                      {part}
                      {i < slide.titleParts.length - 1 && <br />}
                    </span>
                  ))}
                  {slide.highlight && (
                    <span className="slide-highlight">{slide.highlight}</span>
                  )}
                </>
              ) : (
                <>
                  {slide.title}
                  {slide.highlight && (
                    <span className="slide-highlight">{slide.highlight}</span>
                  )}
                </>
              )}
            </h1>
            <p
              className={
                index === 0
                  ? "heading-1--no-margin slide-subtitle"
                  : "bodytext-4--no-margin slide-subtitle"
              }
              style={{
                opacity: subtitleOpacity,
                transform: subtitleTransform,
                transition:
                  index >= 1
                    ? "transform 0.3s ease-out, opacity 0.3s ease-out"
                    : "none",
                marginTop: index === 0 ? 0 : "12px",
              }}
            >
              {slide.subtitle}
            </p>
          </div>
        </div>
      );
    });
  };

  return (
    <div
      className="unified-scroll-container"
      ref={containerRef}
      style={{ height: `${scrollEffectHeight + mirrorIntroduceHeight}vh` }}
    >
      {/* Phase 2 Canvas Layer - Sticky and independent */}
      <div className="phase2-canvas-sticky-wrapper">
        <div className="phase2-canvas-layer" ref={phase2CanvasLayerRef}>
          <canvas ref={canvasRef} className="sequence-canvas" />
        </div>
      </div>

      {/* Original ScrollEffect Section */}
      <div className="scroll-effect-section">
        <div className="scroll-container">
          <div className="homepage">
            {/* Split sections for animation */}
            <div className="split-section split-top" ref={splitTopRef}>
              <div className="gradient-initial">
                <div className="gradient-top"></div>
              </div>
              <div className="gradient-final" ref={finalGradientTopRef}></div>
            </div>

            <div className="split-section split-bottom" ref={splitBottomRef}>
              <div className="gradient-initial">
                <div className="gradient-bottom"></div>
              </div>
              <div
                className="gradient-final"
                ref={finalGradientBottomRef}
              ></div>
            </div>

            {/* Original combined sections for normal scroll effect */}
            <div className="gradient-initial" ref={gradientInitialRef}>
              <div className="gradient-top"></div>
              <div className="gradient-bottom"></div>
            </div>

            <div className="gradient-final" ref={finalGradientRef}></div>

            <div className="logo-center" ref={mainLogoRef}>
              <img src={Logo} alt="Mirror Logo" className="main-logo" />
            </div>
          </div>
        </div>
      </div>

      {/* Fixed Immersive Button - visible except in Immersive Showroom and when overlay is open */}
      {!isImmersiveShowroomPage && !isAnyOverlayOpen && (
        <div className="fixed-immersive-container">
          <GlassThemeButton
            theme={arrowTheme === "white" ? "dark" : "light"}
            icon="globe"
            isCollapsed={isImmersiveCollapsed}
          >
            Immersive Showroom
          </GlassThemeButton>
        </div>
      )}

      {/* Fixed Arrow Button - visible except in footer and when overlay is open */}
      {!isImmersiveShowroomPage && !isAnyOverlayOpen && isArrowVisible && (
        <div className="fixed-arrow-container">
          <GlassThemeButton
            theme={arrowTheme === "white" ? "dark" : "light"}
            icon="arrow"
            onClick={handleArrowClick}
          />
        </div>
      )}

      {/* MirrorIntroduce Section - Text overlays only */}
      <div className="mirror-introduce-section" ref={mirrorIntroduceSectionRef}>
        <div className="mirror-introduce-sticky">
          <div className="canvas-container">
            <div className="video-overlay">
              <div className="text-slides-container">{renderTextSlides()}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
