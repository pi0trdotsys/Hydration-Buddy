import { Droplets, Flame } from "lucide-react";
import { cn } from "@/lib/utils";
import { ProgressRing } from "./progress-ring";
import { MascotDrop } from "./mascot-drop";
import { BottlePicker } from "./bottle-picker";
import type { HydrationState } from "@/hooks/use-hydration-mock";

function WaveBed({ progress }: { progress: number }) {
  return (
    <div
      className="pointer-events-none absolute inset-x-0 bottom-0 overflow-hidden rounded-b-3xl transition-[height] duration-700"
      style={{ height: `${Math.max(progress * 100, 4)}%` }}
    >
      <div className="animate-wave absolute bottom-0 h-full w-[200%]">
        <svg viewBox="0 0 1200 120" preserveAspectRatio="none" className="h-full w-full">
          <path
            d="M0 40 Q 150 0 300 40 T 600 40 T 900 40 T 1200 40 V120 H0 Z"
            fill="var(--aqua)"
            opacity="0.14"
          />
        </svg>
      </div>
    </div>
  );
}

function HourlyChart({ intakes, goal }: { intakes: HydrationState["intakes"]; goal: number }) {
  const buckets = Array.from({ length: 16 }, (_, i) => {
    const h = i + 6;
    return {
      h,
      ml: intakes.filter((x) => x.hour === h).reduce((s, x) => s + x.ml, 0),
    };
  });
  const max = Math.max(goal / 4, ...buckets.map((b) => b.ml));

  return (
    <div className="flex h-16 items-end gap-1" aria-hidden>
      {buckets.map((b) => (
        <div key={b.h} className="flex flex-1 flex-col items-center gap-1">
          <div
            className={cn(
              "w-full rounded-t-sm transition-all duration-500",
              b.ml ? "bg-primary/80" : "bg-secondary",
            )}
            style={{ height: `${Math.max((b.ml / max) * 48, 3)}px` }}
          />
          <span className="text-[8px] text-muted-foreground">{b.h % 3 === 0 ? b.h : ""}</span>
        </div>
      ))}
    </div>
  );
}

export function HydrationWidget({
  state,
  size = "lg",
  className,
}: {
  state: HydrationState;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const pct = Math.round(state.progress * 100);

  return (
    <section
      className={cn(
        "glass relative isolate overflow-hidden rounded-3xl p-4",
        size === "sm" && "aspect-square max-w-44",
        size === "md" && "max-w-md",
        size === "lg" && "max-w-2xl p-4 sm:p-6",
        className,
      )}
      aria-label="Widget nawodnienia"
    >
      <WaveBed progress={state.progress} />

      {size === "sm" && (
        <div className="relative flex h-full flex-col items-center justify-center gap-1">
          <ProgressRing progress={state.progress} size={104} stroke={9}>
            <MascotDrop level={state.level} size={44} />
          </ProgressRing>
          <p className="font-display text-lg font-bold">{pct}%</p>
          <p className="text-[10px] text-muted-foreground">
            {state.total} / {state.goal} ml
          </p>
        </div>
      )}

      {size !== "sm" && (
        <div className="relative flex flex-col gap-4">
          <div className="flex items-center gap-4 sm:gap-5">
            <ProgressRing progress={state.progress} size={size === "lg" ? 128 : 108}>
              <span className="font-display text-2xl font-bold">{pct}%</span>
              <span className="text-[10px] tracking-wide text-muted-foreground uppercase">
                dziś
              </span>
            </ProgressRing>

            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Droplets className="size-3.5 text-primary" />
                <span>Nawodnienie</span>
                <span className="ml-auto inline-flex items-center gap-1 text-primary">
                  <Flame className="size-3.5" /> {state.streak} dni
                </span>
              </div>
              <p className="font-display mt-1 text-2xl font-bold sm:text-3xl">
                {state.total.toLocaleString("pl-PL")}
                <span className="text-base font-medium text-muted-foreground">
                  {" "}
                  / {state.goal.toLocaleString("pl-PL")} ml
                </span>
              </p>
              <p className="text-xs text-muted-foreground">
                {state.remaining > 0 ? `Zostało ${state.remaining} ml` : "Cel osiągnięty 🎉"}
              </p>
              <p className="mt-2 line-clamp-2 text-sm text-foreground/90">{state.selfCare}</p>
            </div>

            {size === "lg" && (
              <MascotDrop level={state.level} size={92} className="hidden sm:block" />
            )}
          </div>

          <BottlePicker
            onAdd={state.add}
            onUndo={state.undo}
            canUndo={state.intakes.length > 0}
            compact={size === "md"}
          />

          {size === "lg" && (
            <>
              <HourlyChart intakes={state.intakes} goal={state.goal} />
              <p className="rounded-2xl bg-secondary/60 p-3 text-xs text-muted-foreground">
                <span className="font-display font-semibold text-primary">Czy wiesz, że… </span>
                {state.fact}
              </p>
            </>
          )}
        </div>
      )}
    </section>
  );
}
