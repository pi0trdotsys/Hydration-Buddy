import { cn } from "@/lib/utils";
import type { HydrationState } from "@/hooks/use-hydration-mock";
import { Trophy, Droplets, TrendingUp, Target } from "lucide-react";

export function HistoryWeekChart({ history }: { history: HydrationState["history"] }) {
  const max = Math.max(...history.map((d) => d.ml), 1);
  const total = history.reduce((s, d) => s + d.ml, 0);
  const avg = Math.round(total / history.length);
  const reachedDays = history.filter((d) => d.reached).length;
  const best = history.reduce((best, d) => (d.ml > best.ml ? d : best), history[0]);

  return (
    <div className="grid gap-3">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatTile icon={Droplets} label="Suma" value={`${total.toLocaleString("pl-PL")} ml`} />
        <StatTile icon={TrendingUp} label="Średnio" value={`${avg.toLocaleString("pl-PL")} ml`} />
        <StatTile icon={Target} label="Cel" value={`${history[0]?.goal.toLocaleString("pl-PL")} ml`} />
        <StatTile
          icon={Trophy}
          label="Dni z celem"
          value={`${reachedDays} / ${history.length}`}
          highlight={reachedDays > 0}
        />
      </div>

      <div className="glass rounded-3xl p-4 sm:p-5">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-display text-sm font-semibold">Nawodnienie w tym tygodniu</h3>
          <span className="text-xs text-muted-foreground">ml / dzień</span>
        </div>

        <div className="flex items-end justify-between gap-2 sm:gap-3">
          {history.map((d, i) => {
            const isToday = i === history.length - 1;
            const height = Math.max((d.ml / max) * 100, 6);
            return (
              <div key={d.day} className="flex flex-1 flex-col items-center gap-2">
                <div className="relative flex h-40 w-full items-end justify-center rounded-2xl bg-secondary/40 p-1.5">
                  <div
                    className={cn(
                      "w-full rounded-xl transition-all duration-700",
                      d.reached ? "bg-primary" : "bg-primary/50",
                      isToday && "ring-2 ring-primary/60",
                    )}
                    style={{ height: `${height}%` }}
                  />
                  <span className="absolute bottom-2 text-[10px] font-semibold text-primary-foreground drop-shadow">
                    {d.ml >= 1000 ? `${(d.ml / 1000).toFixed(1).replace(".", ",")}l` : `${d.ml}`}
                  </span>
                </div>
                <div className="flex flex-col items-center gap-0.5">
                  <span className="text-xs font-semibold text-foreground">{d.day}</span>
                  <span className="text-[10px] text-muted-foreground">{d.date}</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="glass rounded-3xl p-4 sm:p-5">
        <h3 className="font-display mb-3 text-sm font-semibold">Szczegóły dni</h3>
        <ul className="flex flex-col gap-2">
          {history.map((d) => (
            <li
              key={d.day}
              className="flex items-center gap-3 rounded-2xl bg-secondary/40 px-4 py-3"
            >
              <div
                className={cn(
                  "flex size-8 shrink-0 items-center justify-center rounded-full text-xs font-bold",
                  d.reached ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground",
                )}
              >
                {d.day}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium">{d.date}</p>
                <p className="text-xs text-muted-foreground">
                  {d.ml.toLocaleString("pl-PL")} / {d.goal.toLocaleString("pl-PL")} ml
                </p>
              </div>
              <div className="text-right">
                <p className="font-display text-sm font-semibold tabular-nums">
                  {Math.round(d.pct * 100)}%
                </p>
                {d.reached && <p className="text-[10px] text-primary">Cel osiągnięty</p>}
              </div>
            </li>
          ))}
        </ul>
      </div>

      <div className="glass rounded-3xl p-4 sm:p-5">
        <h3 className="font-display mb-2 text-sm font-semibold">Najlepszy dzień</h3>
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-full bg-primary/20 text-primary">
            <Trophy className="size-5" />
          </div>
          <div>
            <p className="text-sm font-medium">
              {best.day}, {best.date}
            </p>
            <p className="text-xs text-muted-foreground">
              {best.ml.toLocaleString("pl-PL")} ml —{" "}
              {best.reached ? "cel osiągnięty" : "blisko celu"}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

function StatTile({
  icon: Icon,
  label,
  value,
  highlight,
}: {
  icon: React.ElementType;
  label: string;
  value: string;
  highlight?: boolean;
}) {
  return (
    <div className={cn("glass rounded-2xl p-3 sm:p-4", highlight && "border-primary/30")}>
      <div className="flex items-center gap-2">
        <Icon className={cn("size-4", highlight ? "text-primary" : "text-muted-foreground")} />
        <span className="text-xs text-muted-foreground">{label}</span>
      </div>
      <p className={cn("font-display mt-1 text-lg font-semibold tabular-nums", highlight && "text-primary")}>
        {value}
      </p>
    </div>
  );
}
