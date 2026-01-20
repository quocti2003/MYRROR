/**
 * Cloudflare Media Utility
 * Manages media URLs from Cloudflare CDN with fallback to public folder
 */

/**
 * Get Cloudflare media base URL from environment variable
 * @returns {string} Base URL for Cloudflare media
 */
const getCloudflareBaseUrl = () => {
  return import.meta.env.VITE_CLOUDFLARE_MEDIA_URL || "";
};

/**
 * Generate media URL from Cloudflare or fallback to public folder
 * @param {string} path - Media file path (e.g., "home-page/MIRROR-LUMEX 91.mp4")
 * @param {Object} options - Additional options
 * @param {boolean} options.forceCloudflare - Force using Cloudflare even without env var (will return empty if not configured)
 * @returns {string} Full media URL
 */
export const getMediaUrl = (path, options = {}) => {
  // If path is empty or undefined, return empty string
  if (!path) return "";

  // If path is already an absolute URL (from API), return as-is
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }

  const { forceCloudflare = false } = options;
  const cloudflareBaseUrl = getCloudflareBaseUrl();

  // Remove leading slash if exists
  const cleanPath = path.startsWith("/") ? path.slice(1) : path;

  // If Cloudflare URL is configured, use it
  if (cloudflareBaseUrl) {
    // Ensure base URL doesn't end with slash
    const baseUrl = cloudflareBaseUrl.endsWith("/")
      ? cloudflareBaseUrl.slice(0, -1)
      : cloudflareBaseUrl;
    return `${baseUrl}/${cleanPath}`;
  }

  // Fallback to public folder if not forcing Cloudflare
  if (!forceCloudflare) {
    return `/${cleanPath}`;
  }

  // If forcing Cloudflare but not configured, return empty
  console.warn(
    "Cloudflare media URL not configured. Set VITE_CLOUDFLARE_MEDIA_URL in .env"
  );
  return "";
};

/**
 * Generate video URL with specific options
 * @param {string} path - Video file path
 * @param {Object} options - Video-specific options
 * @returns {string} Video URL
 */
export const getVideoUrl = (path, options = {}) => {
  return getMediaUrl(path, options);
};

/**
 * Generate image URL with specific options
 * @param {string} path - Image file path
 * @param {Object} options - Image-specific options
 * @returns {string} Image URL
 */
export const getImageUrl = (path, options = {}) => {
  return getMediaUrl(path, options);
};

/**
 * Check if Cloudflare is configured
 * @returns {boolean} True if Cloudflare URL is set
 */
export const isCloudflareConfigured = () => {
  return Boolean(getCloudflareBaseUrl());
};

export default {
  getMediaUrl,
  getVideoUrl,
  getImageUrl,
  isCloudflareConfigured,
};
