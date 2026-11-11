/**
 * Conținutul ghidului „Cum folosesc aplicația”, ca date: ghidul, ecranul de start și dialogul de bun venit îl citesc din același loc.
 *
 * DE CE un registru (nu JSX scris de mână): un text se corectează într-un singur loc, o secțiune nouă e o intrare în listă,
 * iar exemplele de întrebări stau lângă etichetă, deci nu poți schimba eticheta și uita textul. Acest fișier este și sursa
 * sugestiilor de pe ecranul de start, ca ecranul gol și ghidul să nu ofere liste diferite de exemple.
 * Textele sunt în română (copie de interfață); cifrele concrete (prețuri, costuri) NU apar aici, ele vin mereu din unelte.
 */
import {
  BookOpen,
  Calculator,
  ClipboardCheck,
  Gauge,
  LayoutPanelLeft,
  Lightbulb,
  Scale,
  ShieldAlert,
  Sparkles,
  UserRound,
  Wallet,
  Zap,
  type LucideIcon
} from "lucide-react";
import type { JourneyStageId } from "@/lib/journey";

export type ExamplePrompt = {
  id: string;
  label: string;
  icon: LucideIcon;
  /** Etapa din călătoria de cumpărare căreia îi aparține întrebarea (vezi `lib/journey.ts`). */
  stage: JourneyStageId;
  prompt: string;
};

/** Primele patru sunt scenariile de acceptare ale produsului și apar și pe ecranul de start. */
export const EXAMPLE_PROMPTS: ExamplePrompt[] = [
  {
    id: "powertrain",
    label: "Benzină, hibrid sau electric?",
    icon: Zap,
    stage: "needs",
    prompt:
      "Am 20.000 € și fac aproximativ 15.000 km pe an, mai ales în oraș. Nu pot încărca acasă. Ce motorizare mi se potrivește: benzină, hibrid sau electric?"
  },
  {
    id: "compare",
    label: "Compară trei modele",
    icon: Scale,
    stage: "compare",
    prompt: "Compară un Golf, un Corolla și un Octavia pentru o familie de patru persoane, pe 5 ani."
  },
  {
    id: "listing",
    label: "Verifică un anunț",
    icon: ClipboardCheck,
    stage: "search",
    prompt: "Vreau să verifici un anunț. Îl lipesc mai jos și îmi spui dacă e o ofertă bună:\n\n"
  },
  {
    id: "tco",
    label: "Cât costă pe 5 ani?",
    icon: Calculator,
    stage: "compare",
    prompt: "Calculează costul total de deținere pe 5 ani pentru cele mai potrivite mașini pentru profilul meu."
  },
  {
    id: "financing",
    label: "Rată sau cash?",
    icon: Wallet,
    stage: "shortlist",
    prompt: "Cât ar fi rata lunară pentru prima variantă din lista mea scurtă, cu un avans de 20%, pe 5 ani?"
  }
];

/** Câte exemple apar pe ecranul de start; restul se văd doar în ghid. */
export const START_SCREEN_PROMPT_COUNT = 4;

export type GuideBlock =
  | { id: string; kind: "text"; title: string; body: string }
  | { id: string; kind: "prompts"; title: string; body: string }
  | { id: string; kind: "glossary"; title: string; body: string }
  | { id: string; kind: "delete-data"; title: string; body: string };

export type GuideSection = {
  id: string;
  label: string;
  icon: LucideIcon;
  blocks: GuideBlock[];
};

export const GUIDE_SECTIONS: GuideSection[] = [
  {
    id: "capabilities",
    label: "Ce poate face",
    icon: Sparkles,
    blocks: [
      {
        id: "interview",
        kind: "text",
        title: "Te întreabă doar ce îi lipsește",
        body: "Consilierul pornește de la bugetul tău, de cât conduci și de ce ai nevoie, și nu repetă întrebări la care a primit deja răspuns."
      },
      {
        id: "search",
        kind: "text",
        title: "Caută și compară variante",
        body: "Caută în catalogul aplicației, le ordonează pe un scor din șase criterii și le compară una lângă alta."
      },
      {
        id: "cost",
        kind: "text",
        title: "Calculează costul real",
        body: "Costul total de deținere, rata lunară, diferența dintre electric și benzină: toate sunt calcule deterministe, făcute de aplicație, nu estimate din memorie de model."
      },
      {
        id: "listing",
        kind: "text",
        title: "Se uită peste un anunț",
        body: "Lipește textul unui anunț și îți spune ce a găsit, ce semnale de alarmă are și ce să întrebi vânzătorul."
      }
    ]
  },
  {
    id: "good-answers",
    label: "Răspunsuri bune",
    icon: Lightbulb,
    blocks: [
      {
        id: "tips",
        kind: "text",
        title: "Spune-i trei lucruri",
        body: "Bugetul (total sau pe lună), câți kilometri faci pe an și unde (oraș sau autostradă), plus câți pasageri sunt de obicei. Cu astea trei, restul vine din întrebări scurte."
      },
      {
        id: "constraints",
        kind: "text",
        title: "Spune și ce nu vrei",
        body: "„Fără diesel”, „obligatoriu cutie automată”: consilierul le păstrează în profil și le respectă la fiecare căutare."
      },
      {
        id: "prompts",
        kind: "prompts",
        title: "Exemple de întrebări",
        body: "Apasă pe o întrebare ca s-o pui în câmpul de scris. Nu se trimite singură: o poți ajusta înainte."
      }
    ]
  },
  {
    id: "reading",
    label: "Cum citești rezultatele",
    icon: Gauge,
    blocks: [
      {
        id: "card",
        kind: "text",
        title: "Cardul unei variante",
        body: "Arată prețul orientativ cu data cifrelor („date la”), consumul sau autonomia, locurile, portbagajul și siguranța. Insignele spun doar ce rezultă din cifre, de exemplu „Cel mai mic cost lunar”."
      },
      {
        id: "score",
        kind: "text",
        title: "Scorul (inelul de pe card)",
        body: "Scorul de la 0 la 100 combină șase criterii cu ponderi afișate în tabelul de comparație. Un criteriu marcat „nu se aplică” nu intră în medie, ca să nu pară un scor prost."
      },
      {
        id: "tco",
        kind: "text",
        title: "Costul total de deținere",
        body: "Bara stivuită arată din ce se compune: depreciere, energie, asigurare, taxe și mentenanță. De obicei deprecierea e cea mai mare parte, chiar dacă nu o plătești lunar."
      }
    ]
  },
  {
    id: "panel",
    label: "Panou și listă scurtă",
    icon: LayoutPanelLeft,
    blocks: [
      {
        id: "side-panel",
        kind: "text",
        title: "Panoul din dreapta",
        body: "Arată etapa în care ești, nevoile pe care le știe consilierul, lista scurtă și costul total al variantei de pe primul loc. Se actualizează în timp ce consilierul lucrează. Pe telefon îl deschizi din butonul de sus, din dreapta."
      },
      {
        id: "budget",
        kind: "text",
        title: "Bugetul",
        body: "Mută cursorul și alege dacă suma e totală sau pe lună; consilierul folosește valoarea la următoarea căutare."
      },
      {
        id: "shortlist",
        kind: "text",
        title: "Lista scurtă",
        body: "Apasă „Adaugă în lista scurtă” pe un card sau cere-i consilierului s-o facă. Variantele rămân salvate între vizite, cât timp nu ștergi datele."
      }
    ]
  },
  {
    id: "limits",
    label: "Limite",
    icon: ShieldAlert,
    blocks: [
      {
        id: "indicative",
        kind: "text",
        title: "Prețurile sunt orientative",
        body: "Vin dintr-un catalog cu dată („date la”), nu din piața de azi. Verifică oferta reală înainte să decizi."
      },
      {
        id: "scope",
        kind: "text",
        title: "Nu e consultanță financiară",
        body: "Estimările depind de ipotezele afișate (ani, kilometri pe an). Consilierul vorbește doar despre cumpărarea unei mașini și ignoră cererile din afara subiectului."
      },
      {
        id: "unsaved",
        kind: "text",
        title: "Cardurile nu se păstrează la reîncărcare",
        body: "Textul conversației se salvează, dar cardurile și comparația apar doar cât pagina rămâne deschisă. Cere din nou comparația dacă ai nevoie de ea."
      }
    ]
  },
  {
    id: "privacy",
    label: "Confidențialitate",
    icon: UserRound,
    blocks: [
      {
        id: "anonymous",
        kind: "text",
        title: "Fără cont, fără date personale",
        body: "Folosești aplicația printr-o sesiune anonimă. Nu cerem nume, e-mail sau telefon; nu scrie date personale în conversație."
      },
      {
        id: "delete",
        kind: "delete-data",
        title: "Șterge-mi datele",
        body: "Șterge sesiunea, profilul, conversațiile și lista scurtă de pe server. Nu se poate anula; după ștergere pornești cu o sesiune nouă."
      }
    ]
  },
  {
    id: "glossary",
    label: "Glosar",
    icon: BookOpen,
    blocks: [
      {
        id: "terms",
        kind: "glossary",
        title: "Termeni folosiți în aplicație",
        body: "Termenii subliniați cu puncte din interfață se explică și aici."
      }
    ]
  }
];

export type OnboardingStep = { id: string; icon: LucideIcon; title: string; body: string };

/** Dialogul de bun venit, scurt: trei ecrane, apoi trimitere către ghidul complet. */
export const ONBOARDING_STEPS: OnboardingStep[] = [
  {
    id: "welcome",
    icon: Sparkles,
    title: "Bun venit la CPA",
    body: "Un consilier care te ajută să alegi o mașină: te întreabă ce are nevoie, caută, compară și calculează costul real, nu doar prețul de achiziție."
  },
  {
    id: "talk",
    icon: Lightbulb,
    title: "Începe cu bugetul și cum conduci",
    body: "Spune ce buget ai, câți kilometri faci pe an și câți pasageri sunt de obicei. Dacă nu știi de unde să începi, alege una dintre întrebările-exemplu: se pun în câmpul de scris, iar tu o poți schimba."
  },
  {
    id: "read",
    icon: Scale,
    title: "Cifrele vin din calcule, nu din memorie",
    body: "Prețurile sunt orientative și au mereu data lângă ele. Panoul din dreapta ține nevoile tale, lista scurtă și costul total la îndemână. Ghidul din meniu (sau tasta ?) explică tot."
  }
];
