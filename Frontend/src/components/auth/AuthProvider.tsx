import React, { useState, useEffect } from "react";
import type { AuthContextType, User } from "../../types/Auth";
import { AuthContext } from "../../contexts/AuthContext.tsx";
import {
  loginRedirect,
  parseCallback,
  getAccessToken,
  clearAuth,
} from "../../auth.config.ts";

interface AuthProviderProps {
  children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      try {
        // Check if this is a callback from W3ID (hash contains access_token)
        const callbackToken = parseCallback();
        if (callbackToken) {
          window.history.replaceState({}, document.title, "/");
          await fetchUser(callbackToken);
          return;
        }

        // Check if we have an existing token
        const token = getAccessToken();
        if (token) {
          await fetchUser(token);
        } else {
          setIsLoading(false);
        }
      } catch (error) {
        console.error("Auth initialization error:", error);
        setIsLoading(false);
      }
    };

    initAuth();
  }, []);

  const fetchUser = async (accessToken: string) => {
    try {
      const response = await fetch("/api/auth/me", {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      if (response.ok) {
        const userData = await response.json();
        setUser(userData);
      } else {
        clearAuth();
      }
    } catch (error) {
      console.error("Failed to fetch user:", error);
      clearAuth();
    } finally {
      setIsLoading(false);
    }
  };

  const login = () => {
    loginRedirect();
  };

  const logout = () => {
    clearAuth();
    setUser(null);
    window.location.href = "/";
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    login,
    logout,
    isLoading,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
