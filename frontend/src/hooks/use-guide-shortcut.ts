"use client";

/**
 * Scurtătura `?` care deschide ghidul.
 *
 * DE CE se ignoră în câmpuri de text și când e deschis alt dialog: într-un câmp, „?” e un caracter pe care utilizatorul
 * îl scrie; peste alt dialog, ghidul s-ar suprapune peste el și ar fura focusul.
 */
import { useEffect } from "react";
import { useAppStore } from "@/store/useAppStore";

const EDITABLE = new Set(["INPUT", "TEXTAREA", "SELECT"]);

export function useGuideShortcut() {
  const setGuideOpen = useAppStore(state => state.setGuideOpen);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key !== "?" || event.ctrlKey || event.metaKey || event.altKey) return;
      const target = event.target as HTMLElement | null;
      if (target && (target.isContentEditable || EDITABLE.has(target.tagName))) return;
      if (document.querySelector("[role='dialog']")) return;
      event.preventDefault();
      setGuideOpen(true);
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [setGuideOpen]);
}
