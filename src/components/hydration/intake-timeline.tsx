import { cn } from "@/lib/utils";
import type { HydrationState } from "@/hooks/use-hydration-mock";

export function IntakeTimeline({ intakes }: { intakes: HydrationState["intakes"] }) {
  return (
    <ol className="flex flex-col gap-2">
      {[...intakes].reverse().map((i) => (
        <li key={i.id} className="glass flex items-center gap-3 rounded-2xl px-4 py-2.5">
          <span className="font-display w-12 text-xs text-muted-foreground tabular-nums">
            {String(i.hour).padStart(2, "0")}:{String(i.minute).padStart(2, "0")}
          </span>
          <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-secondary">
            <div
              className="h-full rounded-full bg-primary/80"
              style={{ width: `${Math.min((i.ml / 750) * 100, 100)}%` }}
            />
          </div>
          <span className="font-display text-sm font-semibold">{i.ml} ml</span>
        </li>
      ))}
      {intakes.length === 0 && (
        <li className="glass rounded-2xl px-4 py-6 text-center text-sm text-muted-foreground">
          Brak wpisów. Kliknij butelkę w widgecie.
        </li>
      )}
    </ol>
  );
}

export function WeekBar({
  week,
  todayPct,
}: {
  week: HydrationState["week"];
  todayPct: number;
}) {
  const days = [...week, { day: "Nd", pct: todayPct }];
  return (
    <div className="glass flex items-end justify-between gap-2 rounded-3xl p-4">
      {days.map((d, i) => (
        <div key={d.day} className="flex flex-1 flex-col items-center gap-2">
          <div className="flex h-24 w-full items-end justify-center rounded-xl bg-secondary/60 p-1">
            <div
              className={cn(
                "w-full rounded-lg transition-all duration-700",
                d.pct >= 1 ? "bg-primary" : "bg-primary/50",
                i === days.length - 1 && "ring-2 ring-primary/60",
              )}
              style={{ height: `${Math.max(d.pct * 100, 6)}%` }}
            />
          </div>
          <span className="text-[11px] text-muted-foreground">{d.day}</span>
        </div>
      ))}
    </div>
  );
}
