import { createFileRoute } from "@tanstack/react-router";
import { CalendarDays, History } from "lucide-react";
import { useHydrationMock } from "@/hooks/use-hydration-mock";
import { HistoryWeekChart } from "@/components/hydration/history-week-chart";
import { MascotDrop } from "@/components/hydration/mascot-drop";

const TITLE = "Historia i statystyki tygodniowe — Kropi";
const DESC =
  "Przeglądaj ile wody wypiłeś w poszczególne dni tygodnia. Suma, średnia, dni z celem i najlepszy dzień nawodnienia.";

export const Route = createFileRoute("/history")({
  head: () => ({
    meta: [
      { title: TITLE },
      { name: "description", content: DESC },
      { property: "og:title", content: TITLE },
      { property: "og:description", content: DESC },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: History,
});

function History() {
  const state = useHydrationMock();

  return (
    <main className="mx-auto w-full max-w-5xl px-3 pt-4 pb-24 sm:px-4 sm:pt-6">
      <header className="mb-4 sm:mb-6">
        <p className="text-[11px] tracking-[0.2em] text-muted-foreground uppercase">
          Tydzień 17.08 – 23.08
        </p>
        <h1 className="mt-1 flex items-center gap-2 text-2xl font-bold sm:text-3xl">
          <History className="size-6 text-primary" />
          Historia nawodnienia
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Podsumowanie wypitej wody w bieżącym tygodniu.
        </p>
      </header>

      <HistoryWeekChart history={state.history} />

      <section className="mt-4 glass rounded-3xl p-4 sm:mt-6 sm:p-5">
        <h2 className="mb-3 flex items-center gap-2 text-lg font-semibold">
          <CalendarDays className="size-5 text-primary" />
          Wskazówka Kropi
        </h2>
        <div className="flex items-start gap-4">
          <MascotDrop level={state.level} size={64} />
          <p className="text-sm leading-relaxed text-muted-foreground">
            {state.mascotLine} {state.dayNote}
          </p>
        </div>
      </section>
    </main>
  );
}
