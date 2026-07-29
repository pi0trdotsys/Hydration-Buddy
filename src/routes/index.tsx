import { createFileRoute } from "@tanstack/react-router";
import { Droplets, Settings2 } from "lucide-react";
import { useHydrationMock } from "@/hooks/use-hydration-mock";
import { HydrationWidget } from "@/components/hydration/hydration-widget";
import { IntakeTimeline, WeekBar } from "@/components/hydration/intake-timeline";
import { InsightCard } from "@/components/hydration/insight-card";
import { MascotDrop } from "@/components/hydration/mascot-drop";

const TITLE = "Kropi — makieta aplikacji do mierzenia nawodnienia";
const DESC =
  "Ekran główny aplikacji nawodnienia: skalowalny widget, klikalne butelki, postęp dnia, komentarz self-care i ciekawostki o wodzie.";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: TITLE },
      { name: "description", content: DESC },
      { property: "og:title", content: TITLE },
      { property: "og:description", content: DESC },
    ],
  }),
  component: Index,
});

function Index() {
  const state = useHydrationMock();

  return (
    <main className="mx-auto w-full max-w-5xl px-4 pt-6 pb-20">
      <header className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-xs tracking-[0.2em] text-muted-foreground uppercase">Niedziela, dziś</p>
          <h1 className="mt-1 text-3xl font-bold">Twoje nawodnienie</h1>
        </div>
        <label className="glass flex items-center gap-3 rounded-2xl px-4 py-2.5 text-sm">
          <Settings2 className="size-4 text-primary" />
          <span className="text-muted-foreground">Cel</span>
          <input
            type="range"
            min={1000}
            max={4000}
            step={100}
            value={state.goal}
            onChange={(e) => state.setGoal(Number(e.target.value))}
            className="w-28 accent-primary"
          />
          <span className="font-display font-semibold tabular-nums">{state.goal} ml</span>
        </label>
      </header>

      <HydrationWidget state={state} size="lg" className="max-w-none" />

      <div className="mt-6 grid gap-4 md:grid-cols-2">
        <InsightCard eyebrow="Self-care na dziś" body={state.selfCareAlt} />
        <InsightCard eyebrow="Ciekawostka" body={state.fact} />
      </div>

      <section className="mt-6 grid gap-4 lg:grid-cols-[1.2fr_1fr]">
        <div>
          <h2 className="mb-3 flex items-center gap-2 text-lg font-semibold">
            <Droplets className="size-4 text-primary" /> Dzisiejsze łyki
          </h2>
          <IntakeTimeline intakes={state.intakes} />
        </div>
        <div>
          <h2 className="mb-3 text-lg font-semibold">Tydzień</h2>
          <WeekBar week={state.week} todayPct={state.progress} />
          <div className="glass mt-4 flex items-center gap-4 rounded-3xl p-5">
            <MascotDrop level={state.level} size={84} />
            <div>
              <p className="font-display text-sm font-semibold text-primary">Kropi mówi</p>
              <p className="mt-1 text-sm text-muted-foreground">{state.mascotLine}</p>
              <p className="mt-2 text-xs text-muted-foreground/80">{state.dayNote}</p>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
