import NextAuth from "next-auth";
import GitHub from "next-auth/providers/github";

/**
 * Configurația autentificării este opțională la pornire, ca proiectul să poată fi rulat și la curs
 * înainte ca fiecare student să-și creeze aplicația OAuth. În producție, lipsa ei închide ruta de chat.
 */
export const authConfigured = Boolean(
  process.env.AUTH_GITHUB_ID &&
  process.env.AUTH_GITHUB_SECRET &&
  (process.env.AUTH_SECRET || process.env.NEXTAUTH_SECRET)
);

const auth = NextAuth({
  providers: authConfigured ? [GitHub] : [],
  secret: process.env.AUTH_SECRET || process.env.NEXTAUTH_SECRET,
  pages: {
    error: "/"
  }
});

export const { handlers, signIn, signOut, auth: getServerSession } = auth;
