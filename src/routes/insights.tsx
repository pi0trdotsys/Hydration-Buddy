import { createFileRoute } from "@tanstack/react-router";
import { DAYPART_NOTES, FACTS, MASCOT_LINES, SELF_CARE } from "@/data/hydration-content";
import { InsightCard } from "@/components/hydration/insight-card";

const TITLE = "Ciekawostki o wodzie i komentarze self-care | Kropi";
const DESC =
  "Pełny zestaw treści aplikacji: ciekawostki o nawodnieniu, komentarze self-care na każdy poziom postępu i kwestie maskotki.";

export const Route = createFileRoute("/insights")({
  head: () => ({
    meta: [
      { title: TITLE },
      { name: "description", content: DESC },
      { property: "og:title", content: TITLE },
      { property: "og:description", content: DESC },
    ],
  }),
  component: InsightsPage,
});

const LEVEL_LABEL = {
  low: "Niski postęp (0–25%)",
  mid: "Średni postęp (26–60%)",
  high: "Wysoki postęp (61–99%)",
  done: "Cel osiągnięty (100%)",
} as const;

function InsightsPage() {
  return (
    <main className="mx-auto w-full max-w-5xl px-4 pt-6 pb-20">
      <h1 className="text-3xl font-bold">Baza treści</h1>
      <p className="mt-2 max-w-xl text-sm text-muted-foreground">
        Wszystkie komunikaty, które aplikacja losuje deterministycznie na podstawie postępu i pory
        dnia.
      </p>

      <h2 className="mt-10 mb-4 text-lg font-semibold">Self-care wg poziomu</h2>
      <div className="grid gap-4 md:grid-cols-2">
        {(Object.keys(SELF_CARE) as Array<keyof typeof SELF_CARE>).map((lvl) => (
          <div key={lvl} className="glass rounded-3xl p-5">
            <p className="font-display text-[11px] tracking-[0.18em] text-primary uppercase">
              {LEVEL_LABEL[lvl]}
            </p>
            <ul className="mt-3 flex list-disc flex-col gap-2 pl-4 text-sm text-muted-foreground">
              {SELF_CARE[lvl].map((t) => (
                <li key={t}>{t}</li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      <h2 className="mt-10 mb-4 text-lg font-semibold">Wg pory dnia</h2>
      <div className="grid gap-4 md:grid-cols-3">
        {(Object.keys(DAYPART_NOTES) as Array<keyof typeof DAYPART_NOTES>).map((part) => (
          <div key={part} className="glass rounded-3xl p-5">
            <p className="font-display text-[11px] tracking-[0.18em] text-primary uppercase">
              {part === "morning" ? "Rano" : part === "day" ? "Dzień" : "Wieczór"}
            </p>
            <ul className="mt-3 flex list-disc flex-col gap-2 pl-4 text-sm text-muted-foreground">
              {DAYPART_NOTES[part].map((t) => (
                <li key={t}>{t}</li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      <h2 className="mt-10 mb-4 text-lg font-semibold">Ciekawostki ({FACTS.length})</h2>
      <div className="grid gap-3 md:grid-cols-2">
        {FACTS.map((f, i) => (
          <InsightCard key={f} eyebrow={`#${String(i + 1).padStart(2, "0")}`} body={f} />
        ))}
      </div>

      <h2 className="mt-10 mb-4 text-lg font-semibold">Kwestie Kropi</h2>
      <div className="glass rounded-3xl p-5">
        <ul className="grid gap-2 text-sm text-muted-foreground md:grid-cols-2">
          {(Object.keys(MASCOT_LINES) as Array<keyof typeof MASCOT_LINES>).flatMap((lvl) =>
            MASCOT_LINES[lvl].map((l) => (
              <li key={l}>
                <span className="text-primary">„</span>
                {l}
                <span className="text-primary">”</span>
              </li>
            )),
          )}
        </ul>
      </div>
    </main>
  );
}
