"use client";

import { useEffect } from "react";
import { AlertCircle } from "lucide-react";
import { toast } from "sonner";
import { getAuthErrorMessage } from "@/lib/auth-errors";

export function AuthErrorNotice() {
  useEffect(() => {
    const url = new URL(window.location.href);
    const code = url.searchParams.get("authError");
    if (!code) return;
    toast.error(getAuthErrorMessage(code), { icon: <AlertCircle /> });
    url.searchParams.delete("authError");
    url.searchParams.delete("authErrorDescription");
    window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
  }, []);

  return null;
}
