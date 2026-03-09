import React, { useState, useEffect } from "react";
import type { AuthContextType, User } from "../../types/Auth";
import { AuthContext } from "../../contexts/AuthContext.tsx";
import { userManager } from "../../auth.config.ts";

interface AuthProviderProps {
  children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      try {
        // Check if this is a callback from W3ID (hash contains tokens)
        if (window.location.hash.includes("access_token")) {
          const oidcUser = await userManager.signinRedirectCallback(
            window.location.href,
          );
          // Clean up URL
          window.history.replaceState({}, document.title, "/");

          if (oidcUser?.access_token) {
            await fetchUser(oidcUser.access_token);
          }
          return;
        }

        // Check if we have an existing session
        const oidcUser = await userManager.getUser();
        if (oidcUser && !oidcUser.expired && oidcUser.access_token) {
          await fetchUser(oidcUser.access_token);
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
      }
    } catch (error) {
      console.error("Failed to fetch user:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const login = () => {
    userManager.signinRedirect().catch((err) => {
      console.error("W3ID redirect failed:", err);
      alert("Login-Redirect fehlgeschlagen: " + err.message);
    });
  };

  const logout = async () => {
    await userManager.removeUser();
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
