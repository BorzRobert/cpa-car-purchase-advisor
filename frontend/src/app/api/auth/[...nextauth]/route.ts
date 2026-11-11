import { NextRequest, NextResponse } from "next/server";
import { handlers } from "@/lib/auth";

export const runtime = "nodejs";

/**
 * Furnizorul trimite anularea ca parametru de callback. O tratăm înaintea librăriei pentru ca
 * motivul real să ajungă înapoi în aplicație, nu să fie înlocuit cu o eroare generică în engleză.
 */
function handleProviderError(request: NextRequest): NextResponse | null {
  const error = request.nextUrl.searchParams.get("error");
  if (!error) return null;

  const destination = new URL("/", request.url);
  destination.searchParams.set("authError", error);
  const description = request.nextUrl.searchParams.get("error_description");
  if (description) destination.searchParams.set("authErrorDescription", description);
  console.info(`[auth] furnizorul a refuzat autentificarea: ${error}`);
  return NextResponse.redirect(destination);
}

export async function GET(request: NextRequest) {
  return handleProviderError(request) ?? handlers.GET(request);
}

export async function POST(request: NextRequest) {
  return handleProviderError(request) ?? handlers.POST(request);
}
