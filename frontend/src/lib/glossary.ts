/**
 * Glosarul termenilor financiari din interfață, ca date: același text apare în tooltip-uri și în tab-ul „Glosar” din ghid.
 *
 * DE CE un registru: o definiție se scrie o singură dată; tooltip-ul de pe cardul de comparație și ghidul nu pot
 * ajunge să spună lucruri diferite despre același termen.
 */
export type GlossaryId = "tco" | "apr" | "depreciation" | "residual";

export type GlossaryEntry = { id: GlossaryId; term: string; short: string; long: string };

export const GLOSSARY: GlossaryEntry[] = [
  {
    id: "tco",
    term: "TCO (cost total de deținere)",
    short: "Tot ce te costă mașina pe perioada cât o ții: depreciere, energie, asigurare, taxe și mentenanță.",
    long: "TCO înseamnă „total cost of ownership”. Prețul de achiziție arată doar începutul: o mașină ieftină care consumă mult sau se depreciază repede poate ajunge mai scumpă decât una cu preț mai mare. Consilierul îl calculează pe un orizont în ani și un kilometraj anual, ambele afișate lângă rezultat."
  },
  {
    id: "apr",
    term: "DAE (APR)",
    short: "Dobânda anuală efectivă: costul real al creditului pe an, cu comisioane incluse.",
    long: "DAE (în engleză APR) este procentul care include dobânda și comisioanele creditului. Rata lunară și costul total al finanțării se calculează din ea; două oferte cu aceeași dobândă nominală pot avea DAE diferit."
  },
  {
    id: "depreciation",
    term: "Depreciere",
    short: "Cât valorează mașina mai puțin la revânzare decât ai plătit pe ea.",
    long: "Deprecierea este, de regulă, cel mai mare cost al unei mașini, deși nu o plătești lunar. Se calculează ca diferența dintre prețul de achiziție și valoarea reziduală estimată după perioada aleasă."
  },
  {
    id: "residual",
    term: "Valoare reziduală",
    short: "Procentul din preț pe care îl mai poți obține la revânzare după câțiva ani.",
    long: "Valoarea reziduală estimată (de exemplu, după 5 ani) este o cifră orientativă: depinde de kilometraj, stare și piață. Cu cât e mai mare, cu atât deprecierea este mai mică."
  }
];

export function glossaryEntry(id: GlossaryId): GlossaryEntry {
  return GLOSSARY.find(entry => entry.id === id) ?? GLOSSARY[0];
}
