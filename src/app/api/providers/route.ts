/**
 * Expune către UI doar starea de configurare a providerelor.
 *
 * DE CE există un endpoint separat: browserul nu trebuie să citească `process.env`, nici măcar
 * ca boolean. Serverul traduce lipsa cheii într-un motiv de afișat fără să trimită secretul.
 */
import { PROVIDER_LIST } from "@/lib/providers";
import { isProviderConfigured } from "@/lib/providers.server";

export const runtime = "nodejs";

export function GET() {
  return Response.json(
    Object.fromEntries(
      PROVIDER_LIST.map(provider => [
        provider.id,
        isProviderConfigured(provider.id)
          ? { configured: true }
          : { configured: false, reason: "Cheia acestui provider nu este configurată pe server." }
      ])
    )
  );
}
