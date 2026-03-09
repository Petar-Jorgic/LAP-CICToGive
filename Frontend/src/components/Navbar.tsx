import React from "react";
import { Link } from "react-router";
import { useAuth } from "../hooks/useAuth.tsx";
import { ThemeToggle } from "./ui/ThemeToggle.tsx";

// TEST: Hot reload check - this should appear in the browser

export const Navbar: React.FC = () => {
  const { logout, isAuthenticated, isLoading } = useAuth();

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="navbar bg-base-100 shadow-sm px-2 pr-8 ">
      {/* TEST: If you can see this comment, hot reload works! */}
      <div className="navbar-start">
        <Link
          to={isAuthenticated ? "/home" : "/"}
          className="text-xl px-2 py-1 h-auto w-auto flex items-center justify-start no-hover"
        >
          <img
            src="/images/CIC_Austria_Logo_black.jpg"
            alt="IBMCICLogo"
            className="h-20 mr-2  "
          />
        </Link>
      </div>
      {isAuthenticated && (
        <div className="navbar-center">
          <ul className="menu menu-horizontal px-1 text-2xl gap-6">
            <li>
              <Link className="" to="/home">
                Durchsuchen
              </Link>
            </li>
            <li>
              <Link className="" to="/my-items">
                Meine Artikel
              </Link>
            </li>
            <li>
              <Link className="" to="/profile">
                Profil
              </Link>
            </li>
          </ul>
        </div>
      )}

      <div className="navbar-end">
        <div className="flex items-center gap-4">
          <ThemeToggle />
          {isAuthenticated && (
            <button
              className="btn btn-ghost text-xl  "
              onClick={handleLogout}
              disabled={isLoading}
            >
              {isLoading ? "Abmelden..." : "Abmelden"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
