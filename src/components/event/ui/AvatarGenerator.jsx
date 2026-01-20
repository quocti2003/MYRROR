/**
 * AvatarGenerator - Creates personalized avatar image for event participants
 * Generates a 2500x4462 image by compositing layers from generatedImages folder
 *
 * Layer order (bottom to top):
 * 1. Mountain (background)
 * 2. Grass
 * 3. Flowers
 * 4. Moon
 * 5. Music
 * 6. Diamond (based on user's selected shape)
 * 7. Subheading
 * + Text overlay (user name + light number)
 *
 * Each image already has correct transparency and positioning built-in.
 * Just draw them in order at full canvas size (2500x4462).
 */
import React, { useRef, useEffect, useCallback, useState } from 'react';
import { getMediaUrl } from '@/utils/cloudflareMediaUtil';
import brikeyMouraItalic from '@fonts/BrikeyMoura/1FTVVIPBrikeyMoura-Italic.ttf';

// Image paths on Cloudflare CDN
const MOUNTAINS = [
  'dmm/generatedImages/1_mountains/mountain-1@2500x.webp',
  'dmm/generatedImages/1_mountains/mountain-2@2500x.webp',
  'dmm/generatedImages/1_mountains/mountain-3@2500x.webp',
  'dmm/generatedImages/1_mountains/mountain-4@2500x.webp',
  'dmm/generatedImages/1_mountains/mountain-5@2500x.webp',
  'dmm/generatedImages/1_mountains/mountain-6@2500x.webp',
  'dmm/generatedImages/1_mountains/mountain-7@2500x.webp',
  'dmm/generatedImages/1_mountains/mountain-8@2500x.webp',
  'dmm/generatedImages/1_mountains/mountain-9@2500x.webp',
  'dmm/generatedImages/1_mountains/mountain-10@2500x.webp',
];

const GRASS = [
  'dmm/generatedImages/2_grass/grass-1.webp',
  'dmm/generatedImages/2_grass/grass-2.webp',
  'dmm/generatedImages/2_grass/grass-3.webp',
  'dmm/generatedImages/2_grass/grass-4.webp',
  'dmm/generatedImages/2_grass/grass-5.webp',
  'dmm/generatedImages/2_grass/grass-6.webp',
  'dmm/generatedImages/2_grass/grass-7.webp',
  'dmm/generatedImages/2_grass/grass-8.webp',
  'dmm/generatedImages/2_grass/grass-9.webp',
  'dmm/generatedImages/2_grass/grass-10.webp',
];

const FLOWERS = [
  'dmm/generatedImages/3_flowers/flowers-1.webp',
  'dmm/generatedImages/3_flowers/flowers-2.webp',
  'dmm/generatedImages/3_flowers/flowers-3.webp',
  'dmm/generatedImages/3_flowers/flowers-4.webp',
  'dmm/generatedImages/3_flowers/flowers-5.webp',
  'dmm/generatedImages/3_flowers/flowers-6.webp',
  'dmm/generatedImages/3_flowers/flowers-7.webp',
  'dmm/generatedImages/3_flowers/flowers-8.webp',
  'dmm/generatedImages/3_flowers/flowers-9.webp',
  'dmm/generatedImages/3_flowers/flowers-10.webp',
];

const MOONS = [
  'dmm/generatedImages/4_moons/moon-1.webp',
  'dmm/generatedImages/4_moons/moon-2.webp',
  'dmm/generatedImages/4_moons/moon-3.webp',
  'dmm/generatedImages/4_moons/moon-4.webp',
  'dmm/generatedImages/4_moons/moon-5.webp',
  'dmm/generatedImages/4_moons/moon-6.webp',
  'dmm/generatedImages/4_moons/moon-7.webp',
  'dmm/generatedImages/4_moons/moon-8.webp',
  'dmm/generatedImages/4_moons/moon-9.webp',
  'dmm/generatedImages/4_moons/moon-10.webp',
  'dmm/generatedImages/4_moons/moon-11.webp',
];

const MUSIC = 'dmm/generatedImages/5_music/music.webp';

const SUBHEADING = 'dmm/generatedImages/7_subheading/subheading.webp';

// Diamond images mapped to shape names from user selection
const DIAMONDS = {
  heart: [
    'dmm/generatedImages/6_diamonds/heart/heart-1_1@2500x.webp',
    'dmm/generatedImages/6_diamonds/heart/heart-2@2500x.webp',
    'dmm/generatedImages/6_diamonds/heart/heart-3@2500x.webp',
  ],
  round: [
    'dmm/generatedImages/6_diamonds/round/round-1.webp',
    'dmm/generatedImages/6_diamonds/round/round-2.webp',
    'dmm/generatedImages/6_diamonds/round/round-3.webp',
  ],
  emerald: [
    'dmm/generatedImages/6_diamonds/emerald/emerald-1.webp',
    'dmm/generatedImages/6_diamonds/emerald/emerald-2.webp',
    'dmm/generatedImages/6_diamonds/emerald/emerald-3.webp',
  ],
  marquise: [
    'dmm/generatedImages/6_diamonds/marquise/marquise-1.webp',
    'dmm/generatedImages/6_diamonds/marquise/marquise-2.webp',
    'dmm/generatedImages/6_diamonds/marquise/marquise-3.webp',
  ],
  oval: [
    'dmm/generatedImages/6_diamonds/oval/oval-1.webp',
    'dmm/generatedImages/6_diamonds/oval/oval-2.webp',
    'dmm/generatedImages/6_diamonds/oval/oval-3.webp',
  ],
  pear: [
    'dmm/generatedImages/6_diamonds/pear/pear-1.webp',
    'dmm/generatedImages/6_diamonds/pear/pear-2.webp',
    'dmm/generatedImages/6_diamonds/pear/pear-3.webp',
  ],
  asscher: [
    'dmm/generatedImages/6_diamonds/asscher/asscher-1.webp',
    'dmm/generatedImages/6_diamonds/asscher/asscher-2.webp',
    'dmm/generatedImages/6_diamonds/asscher/asscher-3.webp',
  ],
};

// Helper to load image with retry
const loadImage = (src, retries = 2) => {
  return new Promise((resolve, reject) => {
    const attempt = (remainingRetries) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => resolve(img);
      img.onerror = (err) => {
        if (remainingRetries > 0) {
          // Retry with cache busting
          setTimeout(() => attempt(remainingRetries - 1), 500);
        } else {
          reject(err);
        }
      };
      // Add cache busting for retries
      img.src = remainingRetries < retries ? `${src}?t=${Date.now()}` : src;
    };
    attempt(retries);
  });
};

// Seeded random for consistent results per user
const seededRandom = (seed) => {
  const x = Math.sin(seed) * 10000;
  return x - Math.floor(x);
};

const AvatarGenerator = ({
  displayName,
  lightNumber,
  diamondShape,
  onGenerated,
  delay = 0, // Delay in ms before starting generation
}) => {
  const canvasRef = useRef(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const hasGenerated = useRef(false);
  const [shouldGenerate, setShouldGenerate] = useState(delay === 0);

  const generateAvatar = useCallback(async () => {
    const canvas = canvasRef.current;
    if (!canvas || isGenerating || hasGenerated.current) return;

    setIsGenerating(true);
    hasGenerated.current = true;

    // Get 2D context
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      console.error('Failed to get canvas context');
      setIsGenerating(false);
      hasGenerated.current = false;
      onGenerated?.(null); // Notify parent of failure
      return;
    }

    // Canvas size matches the layer images (full quality)
    const width = 2500;
    const height = 4462;
    canvas.width = width;
    canvas.height = height;

    try {
      // Load BrikeyMoura Italic font for text rendering
      try {
        const font = new FontFace(
          'BrikeyMoura',
          `url(${brikeyMouraItalic})`,
          { style: 'italic', weight: '400' }
        );
        await font.load();
        document.fonts.add(font);
        await document.fonts.ready; // Wait for font to be ready
      } catch (fontError) {
        console.warn('Could not load BrikeyMoura font, using fallback:', fontError);
      }

      // Use lightNumber as seed for unique combination per user
      const seed = lightNumber || 1;

      // Select random image from each category based on seed
      const mountainIndex = Math.floor(seededRandom(seed) * MOUNTAINS.length);
      const grassIndex = Math.floor(seededRandom(seed + 1) * GRASS.length);
      const flowersIndex = Math.floor(seededRandom(seed + 2) * FLOWERS.length);
      const moonIndex = Math.floor(seededRandom(seed + 3) * MOONS.length);
      const diamondVariantIndex = Math.floor(seededRandom(seed + 4) * 3);

      // Get diamond images based on user's selected shape
      const diamondImages = DIAMONDS[diamondShape] || DIAMONDS.heart;

      // Load all layer images in parallel (using Cloudflare CDN URLs)
      const [mountainImg, grassImg, flowersImg, moonImg, musicImg, diamondImg, subheadingImg] = await Promise.all([
        loadImage(getMediaUrl(MOUNTAINS[mountainIndex])),
        loadImage(getMediaUrl(GRASS[grassIndex])),
        loadImage(getMediaUrl(FLOWERS[flowersIndex])),
        loadImage(getMediaUrl(MOONS[moonIndex])),
        loadImage(getMediaUrl(MUSIC)),
        loadImage(getMediaUrl(diamondImages[diamondVariantIndex])),
        loadImage(getMediaUrl(SUBHEADING)),
      ]);

      // Clear canvas
      ctx.clearRect(0, 0, width, height);

      // Draw layers in order at original size (0, 0)
      // Each image already has correct position/transparency built-in
      // Layer 1: Mountain (background)
      ctx.drawImage(mountainImg, 0, 0);

      // Layer 2: Grass
      ctx.drawImage(grassImg, 0, 0);

      // Layer 3: Flowers
      ctx.drawImage(flowersImg, 0, 0);

      // Layer 4: Moon
      ctx.drawImage(moonImg, 0, 0);

      // Layer 5: Music
      ctx.drawImage(musicImg, 0, 0);

      // Layer 6: Diamond (user's selected shape)
      ctx.drawImage(diamondImg, 0, 0);

      // Layer 7: Subheading
      ctx.drawImage(subheadingImg, 0, 0);

      // Layer 8: Text overlay - User name with metallic chrome gradient effect
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.font = 'italic 233px BrikeyMoura, Georgia, serif'; // 70pt ≈ 233px

      const textX = width / 2;
      const textY = height * 0.14;
      const displayText = displayName || 'Guest';
      const textMetrics = ctx.measureText(displayText);
      const textWidth = textMetrics.width;

      // Horizontal gradient (left to right: #fa5a86 → #bc224c)
      const gradient = ctx.createLinearGradient(
        textX - textWidth / 2, textY,  // left
        textX + textWidth / 2, textY   // right
      );
      gradient.addColorStop(0, '#fa5a86');    // Light pink (left)
      gradient.addColorStop(1, '#bc224c');    // Dark pink (right)

      // Shadow for depth
      ctx.shadowColor = 'rgba(139, 21, 56, 0.5)';
      ctx.shadowBlur = 10;
      ctx.shadowOffsetX = 0;
      ctx.shadowOffsetY = 4;

      // Draw gradient text
      ctx.fillStyle = gradient;
      ctx.fillText(displayText, textX, textY);

      // Reset shadow
      ctx.shadowBlur = 0;
      ctx.shadowOffsetY = 0;

      // Export as JPEG (smaller file size than PNG)
      const dataUrl = canvas.toDataURL('image/jpeg', 0.9);
      onGenerated?.(dataUrl);
    } catch (error) {
      console.error('Error generating avatar:', error);
      hasGenerated.current = false;
      // Notify parent of failure
      onGenerated?.(null);
    } finally {
      setIsGenerating(false);
    }
  }, [displayName, lightNumber, diamondShape, onGenerated, isGenerating]);

  // Delay generation to avoid blocking entrance animation
  useEffect(() => {
    if (delay > 0) {
      const timer = setTimeout(() => {
        setShouldGenerate(true);
      }, delay);
      return () => clearTimeout(timer);
    }
  }, [delay]);

  // Only generate when shouldGenerate is true (after delay)
  useEffect(() => {
    if (shouldGenerate) {
      generateAvatar();
    }
  }, [generateAvatar, shouldGenerate]);

  // Reset when props change to allow regeneration
  useEffect(() => {
    hasGenerated.current = false;
  }, [displayName, lightNumber, diamondShape]);

  return (
    <canvas
      ref={canvasRef}
      style={{ display: 'none' }}
      aria-hidden="true"
    />
  );
};

// Download helper - handles iOS Safari which doesn't support download attribute
export const downloadAvatar = async (dataUrl, filename) => {
  // Detect iOS
  const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

  if (isIOS) {
    // iOS: Convert data URL to blob and open in new tab for user to long-press save
    try {
      const response = await fetch(dataUrl);
      const blob = await response.blob();
      const blobUrl = URL.createObjectURL(blob);

      // Open in new tab - user can long-press to save
      const newTab = window.open(blobUrl, '_blank');

      if (!newTab) {
        // Popup blocked - fallback to showing alert
        alert('Nhấn giữ hình ảnh để lưu vào thiết bị của bạn');
        window.location.href = blobUrl;
      }

      // Clean up blob URL after a delay
      setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);
    } catch (error) {
      console.error('iOS download error:', error);
      // Fallback: open data URL directly
      window.open(dataUrl, '_blank');
    }
  } else {
    // Other browsers: use standard download
    const link = document.createElement('a');
    link.download = filename;
    link.href = dataUrl;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
};

export default AvatarGenerator;
