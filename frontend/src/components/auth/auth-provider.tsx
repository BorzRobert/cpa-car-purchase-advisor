"use client";

import { createContext, useContext, type ReactNode } from "react";
import { SessionProvider } from "next-auth/react";

const AuthConfiguredContext = createContext(false);

export function AuthProvider({ configured, children }: { configured: boolean; children: ReactNode }) {
  return (
    <AuthConfiguredContext.Provider value={configured}>
      {configured ? <SessionProvider>{children}</SessionProvider> : children}
    </AuthConfiguredContext.Provider>
  );
}

export function AuthConfiguredOnly({ children }: { children: ReactNode }) {
  const configured = useContext(AuthConfiguredContext);
  return configured ? children : null;
}

export function AuthNotConfiguredOnly({ children }: { children: ReactNode }) {
  const configured = useContext(AuthConfiguredContext);
  return configured ? null : children;
}
