/**
 * Google Auth Service - Direct Google Sign-In
 *
 * Two auth flows:
 * 1. Popup flow (default): Uses Google Identity Services SDK for better UX
 * 2. Redirect flow: For Android in-app browsers (Messenger/Facebook) where popup doesn't work
 *
 * Note: iOS in-app browsers are blocked by Google (403 disallowed_useragent)
 */

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

let googleAuthInitialized = false;

/**
 * Load Google Identity Services script
 */
export function loadGoogleScript() {
  return new Promise((resolve, reject) => {
    if (window.google?.accounts) {
      resolve();
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = resolve;
    script.onerror = reject;
    document.head.appendChild(script);
  });
}

/**
 * Initialize Google Sign-In
 * @param {Function} onSuccess - Callback with user data
 * @param {Function} onError - Callback with error
 */
export async function initGoogleSignIn(onSuccess, onError) {
  try {
    await loadGoogleScript();

    if (!GOOGLE_CLIENT_ID) {
      throw new Error('VITE_GOOGLE_CLIENT_ID chưa được cấu hình');
    }

    // Store callbacks for OAuth popup flow
    oauthSuccessCallback = onSuccess;
    oauthErrorCallback = onError;

    window.google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: (response) => {
        if (response.credential) {
          // Decode JWT to get user info
          const userData = decodeJwtToken(response.credential);
          onSuccess(userData);
        } else {
          onError('Đăng nhập thất bại');
        }
      },
      auto_select: false,
      cancel_on_tap_outside: true,
      // Disable FedCM to avoid CORS issues, use traditional popup instead
      use_fedcm_for_prompt: false,
    });

    googleAuthInitialized = true;
  } catch (error) {
    console.error('Google Sign-In init error:', error);
    onError(error.message);
  }
}

// Store callbacks for OAuth popup flow
let oauthSuccessCallback = null;
let oauthErrorCallback = null;

/**
 * Sign in with Google using popup flow (Google Identity Services SDK)
 */
export async function signInWithGoogle() {
  if (!GOOGLE_CLIENT_ID) {
    console.error('Google Client ID chưa được cấu hình');
    if (oauthErrorCallback) {
      oauthErrorCallback('Google Client ID chưa được cấu hình');
    }
    return;
  }

  try {
    // Đảm bảo Google script đã load
    await loadGoogleScript();

    // Dùng google.accounts.id.prompt() - Google SDK tự hiển thị UI đăng nhập
    // Callback đã được set trong initGoogleSignIn()
    window.google.accounts.id.prompt((notification) => {
      if (notification.isNotDisplayed()) {
        // Fallback to OAuth2 token client when One Tap doesn't display
        fallbackToTokenClient();
      }
      // credential_returned case is handled by initialize() callback
    });
  } catch (error) {
    console.error('Google Sign-In error:', error);
    if (oauthErrorCallback) {
      oauthErrorCallback(error.message || 'Đăng nhập thất bại');
    }
  }
}

/**
 * Sign in with Google using REDIRECT flow (implicit grant)
 * For in-app browsers like Messenger where popup doesn't work
 * Returns access token directly in URL hash
 */
export function signInWithGoogleRedirect() {
  if (!GOOGLE_CLIENT_ID) {
    console.error('Google Client ID chưa được cấu hình');
    if (oauthErrorCallback) {
      oauthErrorCallback('Google Client ID chưa được cấu hình');
    }
    return;
  }

  const redirectUri = `${window.location.origin}/the-muse-of-love-grown/login`;

  // Build OAuth URL for implicit flow (response_type=token)
  const authUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth');
  authUrl.searchParams.set('client_id', GOOGLE_CLIENT_ID);
  authUrl.searchParams.set('redirect_uri', redirectUri);
  authUrl.searchParams.set('response_type', 'token');
  authUrl.searchParams.set('scope', 'openid email profile');
  authUrl.searchParams.set('prompt', 'select_account');

  // Redirect to Google OAuth
  window.location.href = authUrl.toString();
}

/**
 * Handle OAuth callback - check URL hash for access token
 * Call this on page load to handle redirect back from Google
 * @returns {Promise<Object|null>} User data or null if no callback
 */
export async function handleOAuthRedirectCallback() {
  // Check URL hash for access token (implicit flow)
  const hash = window.location.hash.substring(1);
  if (!hash) return null;

  const params = new URLSearchParams(hash);
  const accessToken = params.get('access_token');

  if (!accessToken) return null;

  try {
    // Fetch user info from Google
    const response = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch user info');
    }

    const userData = await response.json();

    // Clean up URL hash
    window.history.replaceState({}, document.title, window.location.pathname);

    return {
      id: userData.sub,
      email: userData.email,
      displayName: userData.name,
      firstName: userData.given_name,
      lastName: userData.family_name,
      avatarUrl: userData.picture,
      emailVerified: userData.email_verified,
      provider: 'google',
    };
  } catch (error) {
    console.error('OAuth callback error:', error);
    // Clean up URL hash
    window.history.replaceState({}, document.title, window.location.pathname);
    return null;
  }
}

/**
 * Fallback to OAuth2 token client when One Tap doesn't work
 */
async function fallbackToTokenClient() {
  try {
    const client = window.google.accounts.oauth2.initTokenClient({
      client_id: GOOGLE_CLIENT_ID,
      scope: 'openid email profile',
      callback: async (tokenResponse) => {
        if (tokenResponse.error) {
          oauthErrorCallback?.(tokenResponse.error);
          return;
        }

        try {
          const response = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
            headers: { Authorization: `Bearer ${tokenResponse.access_token}` },
          });
          const userData = await response.json();

          if (userData) {
            oauthSuccessCallback?.({
              id: userData.sub,
              email: userData.email,
              displayName: userData.name,
              firstName: userData.given_name,
              lastName: userData.family_name,
              avatarUrl: userData.picture,
              emailVerified: userData.email_verified,
              provider: 'google',
            });
          } else {
            oauthErrorCallback?.('Không thể lấy thông tin user');
          }
        } catch {
          oauthErrorCallback?.('Không thể lấy thông tin user');
        }
      },
      error_callback: (error) => {
        oauthErrorCallback?.(error.message || 'Đăng nhập thất bại');
      },
    });

    client.requestAccessToken({ prompt: 'select_account' });
  } catch (error) {
    oauthErrorCallback?.(error.message || 'Đăng nhập thất bại');
  }
}

/**
 * Render Google Sign-In button
 * @param {HTMLElement} element - Container element
 * @param {Object} options - Button options
 */
export function renderGoogleButton(element, options = {}) {
  if (!googleAuthInitialized || !element) return;

  window.google.accounts.id.renderButton(element, {
    type: 'standard',
    theme: 'outline',
    size: 'large',
    text: 'continue_with',
    shape: 'rectangular',
    logo_alignment: 'left',
    width: element.offsetWidth || 300,
    ...options,
  });
}

/**
 * Decode Google JWT token to get user info
 */
function decodeJwtToken(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );

    const payload = JSON.parse(jsonPayload);

    return {
      id: payload.sub, // Google user ID
      email: payload.email,
      displayName: payload.name,
      firstName: payload.given_name,
      lastName: payload.family_name,
      avatarUrl: payload.picture,
      emailVerified: payload.email_verified,
      provider: 'google',
    };
  } catch (error) {
    console.error('JWT decode error:', error);
    return null;
  }
}

/**
 * Sign out (clear Google session)
 */
export function signOutGoogle() {
  if (window.google?.accounts) {
    window.google.accounts.id.disableAutoSelect();
  }
}
