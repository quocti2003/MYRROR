// src/context/AuthContext.js
import {
  createContext,
  useState,
  useContext,
  useEffect,
  useCallback,
} from "react";
import api from "@/services/api"; // Use centralized API configuration
import requestThrottle from "@utils/transitionUtil/requestThrottle";
import { ROUTES } from "@/constants/routes";

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [userCache, setUserCache] = useState({ data: null, timestamp: null });
  const CACHE_DURATION = 3000000; // 50 minutes cache (almost full token lifetime)

  // Persistent storage helpers
  const STORAGE_KEYS = {
    USER_DATA: "userData",
    USER_CACHE_TIMESTAMP: "userCacheTimestamp",
  };

  const saveUserToStorage = useCallback((userData) => {
    try {
      localStorage.setItem(STORAGE_KEYS.USER_DATA, JSON.stringify(userData));
      localStorage.setItem(
        STORAGE_KEYS.USER_CACHE_TIMESTAMP,
        Date.now().toString()
      );
    } catch (error) {
      // Failed to save user data to localStorage
    }
  }, []);

  const getUserFromStorage = useCallback(() => {
    try {
      const userData = localStorage.getItem(STORAGE_KEYS.USER_DATA);
      const timestamp = localStorage.getItem(STORAGE_KEYS.USER_CACHE_TIMESTAMP);

      if (userData && timestamp) {
        const parsedData = JSON.parse(userData);
        const cacheAge = Date.now() - parseInt(timestamp);

        if (cacheAge < CACHE_DURATION) {
          return parsedData;
        }
      }
    } catch (error) {
      // Failed to get user data from localStorage
    }
    return null;
  }, [CACHE_DURATION]);

  const clearUserFromStorage = useCallback(() => {
    try {
      localStorage.removeItem(STORAGE_KEYS.USER_DATA);
      localStorage.removeItem(STORAGE_KEYS.USER_CACHE_TIMESTAMP);
    } catch (error) {
      // Failed to clear user data from localStorage
    }
  }, []);

  // Helper functions for caching
  const isCacheValid = useCallback(() => {
    if (!userCache.data || !userCache.timestamp) return false;
    return Date.now() - userCache.timestamp < CACHE_DURATION;
  }, [userCache, CACHE_DURATION]);

  const setCachedUser = useCallback(
    (userData) => {
      setUser(userData);
      setUserCache({
        data: userData,
        timestamp: Date.now(),
      });
      saveUserToStorage(userData); // Save to persistent storage
    },
    [saveUserToStorage]
  );

  const fetchUserProfile = useCallback(async () => {
    // Check memory cache first
    if (isCacheValid() && userCache.data) {
      // Using memory cached user data
      setUser(userCache.data);
      return userCache.data;
    }

    // Check persistent storage cache
    const storedUser = getUserFromStorage();
    if (storedUser) {
      setUser(storedUser);
      setUserCache({ data: storedUser, timestamp: Date.now() });
      return storedUser;
    }

    // Only fetch from API if no cache available and not throttled
    // Note: User profile endpoint uses /api/v1/users/* path (merged from user-service)
    const endpoint = "/api/v1/users/me";

    if (!requestThrottle.canMakeRequest(endpoint)) {
      const waitTime = requestThrottle.getTimeUntilNext(endpoint);
      // Request throttled, waiting before next attempt
      // Return cached data if available while throttled
      if (storedUser) {
        setUser(storedUser);
        return storedUser;
      }
      throw new Error("Request throttled and no cached data available");
    }

    try {
      const response = await api.get(endpoint);
      setCachedUser(response.data);
      return response.data;
    } catch (error) {
      // If API fails but we have tokens, assume user is authenticated
      const token = localStorage.getItem("accessToken");
      if (token && error.response?.status === 429) {
        // API rate limited, but user has valid token - treating as authenticated
        // Create minimal user object from token (if possible)
        throw error; // Let caller handle gracefully
      }
      throw error;
    }
  }, [isCacheValid, userCache.data, setCachedUser, getUserFromStorage]);

  const login = async (username, password) => {
    try {
      const response = await api.post("/api/v1/auth/authenticate", {
        username,
        password,
      });

      const { accessToken, refreshToken } = response.data;

      localStorage.setItem("accessToken", accessToken);
      localStorage.setItem("refreshToken", refreshToken);

      // Decode JWT to see userId
      if (accessToken) {
        try {
          const base64Payload = accessToken.split(".")[1];
          const payload = JSON.parse(atob(base64Payload));
        } catch (decodeError) {
          console.error("❌ Error decoding JWT:", decodeError);
        }
      }

      const userData = await fetchUserProfile();
      return userData;
    } catch (error) {
      console.error("❌ Login error:", error);
      throw error;
    }
  };

  const logout = useCallback(() => {
    setUser(null);
    setUserCache({ data: null, timestamp: null }); // Clear memory cache
    clearUserFromStorage(); // Clear persistent cache
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    window.location.href = ROUTES.AUTH_LOGIN;
  }, [clearUserFromStorage]);

  useEffect(() => {
    let isMounted = true;

    const verifyUser = async () => {
      const token = localStorage.getItem("accessToken");

      if (token) {
        // Try to get user from storage first (immediate response)
        const storedUser = getUserFromStorage();
        if (storedUser && isMounted) {
          setUser(storedUser);
          setUserCache({ data: storedUser, timestamp: Date.now() });
        }

        try {
          // Then try to fetch fresh data (background refresh)
          if (isMounted) {
            await fetchUserProfile();
          }
        } catch (error) {
          if (error.response?.status === 429) {
            // Rate limited - using fallback authentication
            // Don't clear tokens on rate limit
            // If we have stored user data, keep it
            if (!storedUser && isMounted) {
              // Create minimal authenticated state
              // No cached user but have token - treating as authenticated
            }
          } else if (error.response?.status === 401) {
            // Only clear on actual auth failure, but be more conservative
            // Check if we have recent cached user data before clearing everything
            const storedUser = getUserFromStorage();
            if (!storedUser || Date.now() - parseInt(localStorage.getItem(STORAGE_KEYS.USER_CACHE_TIMESTAMP) || 0) > CACHE_DURATION) {
              // No cached data or cache is really old - clear everything
              localStorage.removeItem("accessToken");
              localStorage.removeItem("refreshToken");
              if (isMounted) {
                setUser(null);
                setUserCache({ data: null, timestamp: null });
                clearUserFromStorage();
              }
            } else {
              // We have recent cached data - might be temporary network issue, keep session
              console.warn("401 error but have recent cache - keeping session for now");
            }
          } else {
            // API error during verification
            // For other errors, keep existing auth state if available
          }
        }
      } else {
        // No token found - user not authenticated
      }

      if (isMounted) {
        setIsLoading(false);
      }
    };

    verifyUser();

    return () => {
      isMounted = false;
    };
  }, []); // Remove dependencies to run only once on mount

  const refreshUser = useCallback(
    async (forceRefresh = false) => {
      const token = localStorage.getItem("accessToken");
      if (token) {
        try {
          if (forceRefresh) {
            // Force fresh data by clearing cache
            setUserCache({ data: null, timestamp: null });
            clearUserFromStorage();
          }
          return await fetchUserProfile();
        } catch (error) {
          if (error.response?.status === 429) {
            // On rate limit, try to use cached data
            const storedUser = getUserFromStorage();
            if (storedUser) {
              // Rate limited during refresh - using cached data
              setUser(storedUser);
              return storedUser;
            }
            // Rate limited and no cached data available
          } else if (error.response?.status === 401) {
            // Be more conservative about clearing auth on refresh
            const storedUser = getUserFromStorage();
            if (!storedUser || Date.now() - parseInt(localStorage.getItem(STORAGE_KEYS.USER_CACHE_TIMESTAMP) || 0) > CACHE_DURATION) {
              // No cached data or cache is really old - clear everything
              setUser(null);
              setUserCache({ data: null, timestamp: null });
              clearUserFromStorage();
              localStorage.removeItem("accessToken");
              localStorage.removeItem("refreshToken");
            } else {
              console.warn("401 during refresh but have recent cache - keeping session");
            }
          }
          throw error;
        }
      }
    },
    [fetchUserProfile, clearUserFromStorage, getUserFromStorage]
  );

  const value = {
    user,
    setUser,
    login,
    logout,
    refreshUser,
    isAuthenticated: !!user,
    isLoading,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  return useContext(AuthContext);
};
