/**
 * Ruta `/` — aplicația CPA (Car Purchase Advisor).
 *
 * DE CE pagina asta doar compune și nu conține logică:
 * ea răspunde la o singură întrebare — „din ce zone e făcut ecranul?". Cele trei zone
 * (sidebar, header, centru) își gestionează singure starea prin store. Așa, când adăugăm
 * agentul, nu se atinge nimic aici.
 *
 * DE CE `SidebarProvider` e aici și nu în layout:
 * nu toate rutele viitoare vor avea sidebar. Dacă providerul ar fi în layout, ar împinge
 * structura de sidebar peste toate rutele — inclusiv pagini de tip landing, care n-o vor.
 *
 * DE CE dialogul de preferințe e randat aici, la același nivel:
 * ca să fie deasupra întregii aplicații, nu în interiorul sidebar-ului. Un dialog randat
 * într-un panou care se închide (pe mobil, sidebar-ul e un `Sheet`) ar dispărea împreună cu el.
 */
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/layout/app-sidebar";
import { Chat } from "@/components/chat/chat";
import { SettingsDialog } from "@/components/settings/settings-dialog";

export default function Home() {
  return (
    // `h-svh` + `overflow-hidden`: aplicația ocupă exact ecranul, iar derularea se întâmplă
    // DOAR în listele interne (conversații, mesaje). Altfel pagina ar derula în întregime și
    // composer-ul ar fugi de sub degete pe telefon.
    <SidebarProvider className="h-svh overflow-hidden">
      <AppSidebar />
      <SidebarInset className="min-h-0 min-w-0 overflow-hidden">
        <Chat />
      </SidebarInset>
      <SettingsDialog />
    </SidebarProvider>
  );
}
