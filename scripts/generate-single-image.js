import sharp from 'sharp';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const BASE_DIR = path.join(__dirname, '../src/assets/images/dmm/generatedImages');
const OUTPUT_DIR = path.join(__dirname, '../src/assets/images/dmm/output_test');

// Lấy tên người dùng từ command line argument
const userName = process.argv[2] || 'Your Name';
const outputFormat = process.argv[3] || 'png'; // png, jpg, jpeg
const diamondShape = process.argv[4] || 'heart'; // asscher, emerald, heart, marquise, oval, pear, round

console.log(`\n🎨 Generating image for: "${userName}"`);
console.log(`📁 Output format: ${outputFormat.toUpperCase()}`);
console.log(`💎 Diamond shape: ${diamondShape}\n`);

// Tạo text overlay bằng SVG
function createTextOverlay(text, width, height) {
  // Escape special characters for SVG
  const escapedText = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

  const svg = `
    <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <style>
          @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700&amp;display=swap');
        </style>
      </defs>
      <text
        x="50%"
        y="15%"
        text-anchor="middle"
        font-family="Georgia, 'Times New Roman', serif"
        font-size="96"
        font-weight="bold"
        fill="white"
        stroke="#333"
        stroke-width="1"
        filter="drop-shadow(2px 2px 4px rgba(0,0,0,0.5))"
      >${escapedText}</text>
    </svg>
  `;

  return Buffer.from(svg);
}

// Get random image from folder
function getRandomImage(folderPath) {
  if (!fs.existsSync(folderPath)) return null;
  const files = fs.readdirSync(folderPath)
    .filter(f => f.endsWith('.webp') || f.endsWith('.png'));
  if (files.length === 0) return null;
  const randomFile = files[Math.floor(Math.random() * files.length)];
  return path.join(folderPath, randomFile);
}

// Get first image from folder
function getFirstImage(folderPath) {
  if (!fs.existsSync(folderPath)) return null;
  const files = fs.readdirSync(folderPath)
    .filter(f => f.endsWith('.webp') || f.endsWith('.png'))
    .sort();
  if (files.length === 0) return null;
  return path.join(folderPath, files[0]);
}

async function generateImage() {
  const targetWidth = 2500;
  const targetHeight = 4462;

  // Get layers
  const layers = [
    getRandomImage(path.join(BASE_DIR, '1_mountains')),
    getRandomImage(path.join(BASE_DIR, '2_grass')),
    getRandomImage(path.join(BASE_DIR, '3_flowers')),
    getRandomImage(path.join(BASE_DIR, '4_moons')),
    getFirstImage(path.join(BASE_DIR, '5_music')),
    getRandomImage(path.join(BASE_DIR, `6_diamonds/${diamondShape}`)), // Diamond shape from argument
    getFirstImage(path.join(BASE_DIR, '7_subheading')),
  ];

  console.log('Layers:');
  layers.forEach((layer, i) => {
    console.log(`  ${i + 1}. ${layer ? path.basename(layer) : 'MISSING'}`);
  });

  // Filter out missing layers
  const validLayers = layers.filter(l => l !== null);

  if (validLayers.length === 0) {
    console.error('❌ No valid layers found!');
    return;
  }

  // Prepare overlays
  const overlays = [];

  for (let i = 1; i < validLayers.length; i++) {
    const buffer = await sharp(validLayers[i], { limitInputPixels: false })
      .resize(targetWidth, targetHeight, { fit: 'cover', position: 'top' })
      .toBuffer();

    overlays.push({
      input: buffer,
      top: 0,
      left: 0
    });
  }

  // Add text overlay with user name
  const textSvg = createTextOverlay(userName, targetWidth, targetHeight);
  overlays.push({
    input: textSvg,
    top: 0,
    left: 0
  });

  // Create output directory
  if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  }

  // Generate filename
  const safeName = userName.replace(/[^a-zA-Z0-9]/g, '_').toLowerCase();
  const timestamp = Date.now();
  const outputFileName = `${safeName}_${timestamp}.${outputFormat}`;
  const outputPath = path.join(OUTPUT_DIR, outputFileName);

  // Process and save
  let pipeline = sharp(validLayers[0], { limitInputPixels: false })
    .resize(targetWidth, targetHeight, { fit: 'cover', position: 'top' })
    .composite(overlays);

  // Output based on format
  if (outputFormat === 'png') {
    await pipeline
      .png({ quality: 90, compressionLevel: 6 })
      .toFile(outputPath);
  } else if (outputFormat === 'jpg' || outputFormat === 'jpeg') {
    await pipeline
      .jpeg({ quality: 90 })
      .toFile(outputPath);
  } else {
    await pipeline
      .png({ quality: 90 })
      .toFile(outputPath);
  }

  // Get file size
  const stats = fs.statSync(outputPath);
  const sizeKB = (stats.size / 1024).toFixed(1);

  console.log(`\n✅ Image generated successfully!`);
  console.log(`📍 Path: ${outputPath}`);
  console.log(`📐 Size: ${targetWidth} x ${targetHeight}`);
  console.log(`💾 File size: ${sizeKB} KB`);
}

generateImage().catch(err => {
  console.error('❌ Error:', err.message);
  process.exit(1);
});
