/**
 * Auth Service for Event System - Social OAuth via Supabase
 * Supports: Google, Facebook
 */
import { getSupabaseClient, isSupabaseConfigured } from './supabase';

/**
 * Sign in with Google OAuth
 * @returns {Promise<Object>} { success: boolean, error?: string }
 */
export async function signInWithGoogle() {
  if (!isSupabaseConfigured()) {
    return { success: false, error: 'Supabase chưa được cấu hình' };
  }

  try {
    const supabase = getSupabaseClient();

    // Use VITE_REDIRECT_URL from environment or fallback to current origin
    const redirectUrl = import.meta.env.VITE_REDIRECT_URL ||
                       `${window.location.origin}/the-muse-of-love-grown/login`;

    const { data, error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo: redirectUrl,
        queryParams: {
          access_type: 'offline',
          prompt: 'consent',
        },
      },
    });

    if (error) {
      console.error('Google sign in error:', error);
      return { success: false, error: error.message };
    }

    return { success: true, data };
  } catch (error) {
    console.error('Google sign in error:', error);
    return { success: false, error: 'Đăng nhập thất bại. Vui lòng thử lại.' };
  }
}

/**
 * Sign in with Facebook OAuth
 * @returns {Promise<Object>} { success: boolean, error?: string }
 */
export async function signInWithFacebook() {
  if (!isSupabaseConfigured()) {
    return { success: false, error: 'Supabase chưa được cấu hình' };
  }

  try {
    const supabase = getSupabaseClient();

    const redirectUrl = import.meta.env.VITE_REDIRECT_URL ||
                       `${window.location.origin}/the-muse-of-love-grown/login`;

    const { data, error } = await supabase.auth.signInWithOAuth({
      provider: 'facebook',
      options: {
        redirectTo: redirectUrl,
        scopes: 'public_profile', // 'email' requires Facebook App Review approval
      },
    });

    if (error) {
      console.error('Facebook sign in error:', error);
      return { success: false, error: error.message };
    }

    return { success: true, data };
  } catch (error) {
    console.error('Facebook sign in error:', error);
    return { success: false, error: 'Đăng nhập thất bại. Vui lòng thử lại.' };
  }
}

/**
 * Get current authenticated user
 * @returns {Promise<Object|null>} User object or null
 */
export async function getCurrentUser() {
  if (!isSupabaseConfigured()) {
    return null;
  }

  try {
    const supabase = getSupabaseClient();
    const { data: { user }, error } = await supabase.auth.getUser();

    if (error || !user) {
      return null;
    }

    return {
      id: user.id,
      email: user.email,
      displayName: user.user_metadata?.full_name || user.user_metadata?.name || user.email?.split('@')[0],
      avatarUrl: user.user_metadata?.avatar_url,
      provider: user.app_metadata?.provider,
    };
  } catch (error) {
    console.error('Get current user error:', error);
    return null;
  }
}

/**
 * Sign out current user
 * @returns {Promise<Object>} { success: boolean, error?: string }
 */
export async function signOut() {
  if (!isSupabaseConfigured()) {
    return { success: true };
  }

  try {
    const supabase = getSupabaseClient();
    const { error } = await supabase.auth.signOut();

    if (error) {
      console.error('Sign out error:', error);
      return { success: false, error: error.message };
    }

    return { success: true };
  } catch (error) {
    console.error('Sign out error:', error);
    return { success: false, error: 'Đăng xuất thất bại' };
  }
}

/**
 * Subscribe to auth state changes
 * @param {Function} callback - Called with user object when auth state changes
 * @returns {Function} Unsubscribe function
 */
export function onAuthStateChange(callback) {
  if (!isSupabaseConfigured()) {
    return () => {};
  }

  try {
    const supabase = getSupabaseClient();

    const { data: { subscription } } = supabase.auth.onAuthStateChange(
      async (event, session) => {
        if (event === 'SIGNED_IN' && session?.user) {
          const user = {
            id: session.user.id,
            email: session.user.email,
            displayName: session.user.user_metadata?.full_name ||
                         session.user.user_metadata?.name ||
                         session.user.email?.split('@')[0],
            avatarUrl: session.user.user_metadata?.avatar_url,
            provider: session.user.app_metadata?.provider,
          };
          callback(user);
        } else if (event === 'SIGNED_OUT') {
          callback(null);
        }
      }
    );

    return () => {
      subscription?.unsubscribe();
    };
  } catch (error) {
    console.error('Auth state change subscription error:', error);
    return () => {};
  }
}

/**
 * Get session (for checking if user is logged in on page load)
 * @returns {Promise<Object|null>} Session object or null
 */
export async function getSession() {
  if (!isSupabaseConfigured()) {
    return null;
  }

  try {
    const supabase = getSupabaseClient();
    const { data: { session }, error } = await supabase.auth.getSession();

    if (error || !session) {
      return null;
    }

    return session;
  } catch (error) {
    console.error('Get session error:', error);
    return null;
  }
}
