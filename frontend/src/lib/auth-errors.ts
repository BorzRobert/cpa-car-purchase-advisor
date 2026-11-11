const AUTH_ERROR_MESSAGES: Record<string, string> = {
  access_denied: "Autentificarea a fost anulată. Ai rămas în aplicație și poți încerca din nou.",
  OAuthSignin: "Nu am putut începe autentificarea cu furnizorul.",
  OAuthCallback: "Furnizorul nu a putut finaliza autentificarea.",
  OAuthAccountNotLinked: "Acest cont nu poate fi asociat cu autentificarea aleasă.",
  Configuration: "Autentificarea nu este configurată corect pe server."
};

export function getAuthErrorMessage(code: string): string {
  if (code === "access_denied") return AUTH_ERROR_MESSAGES.access_denied;
  return AUTH_ERROR_MESSAGES[code] ?? "Autentificarea nu a reușit. Încearcă din nou.";
}
