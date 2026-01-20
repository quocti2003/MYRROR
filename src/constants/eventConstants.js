/**
 * Event Constants - Configuration for Mirror Diamond Event
 */

// Diamond shapes mapped to musical pitches
export const DIAMOND_CONFIGS = [
  {
    shape: "round",
    pitch: "C4",
    staffLine: 0,
    color: "#E91E63",
    displayName: "Round Brilliant",
    displayNameVi: "Kim cương tròn",
  },
  {
    shape: "oval",
    pitch: "D4",
    staffLine: 1,
    color: "#F48FB1",
    displayName: "Oval",
    displayNameVi: "Kim cương oval",
  },
  {
    shape: "pear",
    pitch: "E4",
    staffLine: 2,
    color: "#EC407A",
    displayName: "Pear",
    displayNameVi: "Kim cương giọt lệ",
  },
  {
    shape: "heart",
    pitch: "F4",
    staffLine: 3,
    color: "#D81B60",
    displayName: "Heart",
    displayNameVi: "Kim cương trái tim",
  },
  {
    shape: "asscher",
    pitch: "G4",
    staffLine: 4,
    color: "#C2185B",
    displayName: "Asscher",
    displayNameVi: "Kim cương asscher",
  },
  {
    shape: "emerald",
    pitch: "A4",
    staffLine: 5,
    color: "#AD1457",
    displayName: "Emerald",
    displayNameVi: "Kim cương emerald",
  },
  {
    shape: "marquise",
    pitch: "B4",
    staffLine: 6,
    color: "#880E4F",
    displayName: "Marquise",
    displayNameVi: "Kim cương marquise",
  },
];

// Mapping from new shape IDs (h1-h7) to shape names in DIAMOND_CONFIGS
// Synced with EventChooseShapePage shapes array
const SHAPE_ID_TO_NAME = {
  h1: 'heart',      // Heart shape
  h2: 'oval',       // Oval shape
  h3: 'round',      // Round shape
  h4: 'pear',       // Pear shape
  h5: 'asscher',    // Asscher shape
  h6: 'emerald',    // Emerald shape
  h7: 'marquise',   // Marquise shape
};

// Get diamond config by shape (supports both legacy names and h1-h7 IDs)
export const getDiamondConfig = (shape) => {
  // First check if it's a new ID (h1-h7), convert to legacy name
  const legacyName = SHAPE_ID_TO_NAME[shape] || shape;
  return DIAMOND_CONFIGS.find((d) => d.shape === legacyName);
};

// Get diamond config by pitch
export const getDiamondByPitch = (pitch) =>
  DIAMOND_CONFIGS.find((d) => d.pitch === pitch);

// Pitch to staff line mapping
export const PITCH_TO_STAFF_LINE = {
  C4: 0,
  D4: 1,
  E4: 2,
  F4: 3,
  G4: 4,
  A4: 5,
  B4: 6,
};

// Staff line to pitch mapping
export const STAFF_LINE_TO_PITCH = {
  0: "C4",
  1: "D4",
  2: "E4",
  3: "F4",
  4: "G4",
  5: "A4",
  6: "B4",
};

// Text constants (Vietnamese)
export const TEXT = {
  // Event info
  eventName: "Mirror Diamond Symphony",
  tagline: "Đặt một viên kim cương, cùng tạo nên giai điệu chung",

  // Screen titles
  ticketTitle: "Nhập mã vé",
  ticketPlaceholder: "VD: MFD-12345",
  ticketButton: "Xác nhận",
  ticketError: "Mã vé không hợp lệ hoặc đã được sử dụng",

  nameTitle: "Tên hiển thị của bạn",
  namePlaceholder: "Nhập tên của bạn",
  nameButton: "Tiếp tục",

  diamondTitle: "Chọn viên kim cương của bạn",
  diamondSubtitle: "Mỗi viên kim cương tương ứng với một nốt nhạc",
  diamondButton: "Đã chọn",

  placeTitle: "Đặt kim cương lên khuông nhạc",
  placeSubtitle: "Chạm vào vị trí bạn muốn đặt",
  placeButton: "Xác nhận vị trí",

  resultTitle: "Chúc mừng!",
  resultSubtitle: "Kim cương của bạn đã được đặt",
  resultLightNumber: "Ánh sáng số",
  resultDownload: "Tải ảnh lưu niệm",
  resultPlayNote: "Nghe nốt của bạn",
  resultPlayAll: "Nghe toàn bộ giai điệu",
  resultRestart: "Bắt đầu lại",

  // Lucky draw
  luckyDrawTitle: "Quay số may mắn",
  luckyDrawWinner: "Người chiến thắng",

  // Misc
  loading: "Đang tải...",
  error: "Đã có lỗi xảy ra",
  demoMode: "CHẾ ĐỘ DEMO",
};

// Admin password (from environment variable)
export const ADMIN_PASSWORD = import.meta.env.VITE_EVENT_ADMIN_PASSWORD || "";

// Demo ticket prefix
export const DEMO_TICKET_PREFIX = "MFD";

// ============================================================
// NOTE POSITION CALCULATION
// ============================================================

/**
 * Calculate note pitch and position based on lightNumber (participant order)
 * Each participant gets a fixed note based on their order
 * @param {number} lightNumber - The participant's order number (1, 2, 3, ...)
 * @returns {Object} { pitch, staffLine, positionX }
 */
export const calculateNoteFromLightNumber = (lightNumber) => {
  // 7 notes in our scale (C4 to B4)
  const noteIndex = (lightNumber - 1) % 7;
  const config = DIAMOND_CONFIGS[noteIndex];

  // Get position from order
  const positionX = calculateNotePositionByOrder(lightNumber);

  return {
    pitch: config.pitch,
    staffLine: config.staffLine,
    positionX,
  };
};

/**
 * Calculate X position based on order (1-based index)
 * Notes are placed from left to right: 1st note at 5%, then spread evenly
 * @param {number} order - The order number (1, 2, 3, ...)
 * @returns {number} positionX as percentage (5-95)
 */
export const calculateNotePositionByOrder = (order) => {
  const notesPerRow = 20; // How many notes fit in one staff width before wrapping
  const noteInRow = (order - 1) % notesPerRow;

  // Start at 5%, end at 95%, evenly distributed
  const positionX = 5 + noteInRow * (90 / (notesPerRow - 1));

  return Math.round(positionX * 100) / 100; // Round to 2 decimal places
};
