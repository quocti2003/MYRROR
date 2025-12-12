import QRCode from 'qrcode';

const url = 'https://www.mirrorfuturediamond.com/';

const sizes = [256, 512, 1024, 2000];

async function generateQRCodes() {
  for (const size of sizes) {
    const fileName = `qr-code-${size}.svg`;

    await QRCode.toFile(fileName, url, {
      type: 'svg',
      errorCorrectionLevel: 'H',
      width: size,
      margin: 4,
      color: {
        dark: '#000000',
        light: '#FFFFFF'
      }
    });

    console.log(`✓ Generated ${fileName} (${size}x${size}px)`);
  }

  console.log('\nDone! All QR codes generated.');
  console.log('URL encoded:', url);
}

generateQRCodes().catch(console.error);
