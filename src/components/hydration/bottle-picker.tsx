import { useState } from "react";
import { Plus, Undo2 } from "lucide-react";
import { BOTTLES } from "@/data/hydration-content";
import { cn } from "@/lib/utils";

function BottleGlyph({ ml, active }: { ml: number; active: boolean }) {
  const fill = Math.min(ml / 750, 1);
  return (
    <svg viewBox="0 0 24 40" className="h-9 w-6">
      <rect x="9" y="1" width="6" height="5" rx="1.5" fill="currentColor" opacity="0.5" />
      <rect
        x="4"
        y="6"
        width="16"
        height="32"
        rx="6"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        opacity="0.7"
      />
      <rect
        x="5.5"
        y={36.5 - 29 * fill}
        width="13"
        height={29 * fill}
        rx="4.5"
        fill="currentColor"
        opacity={active ? 0.95 : 0.45}
      />
    </svg>
  );
}

export function BottlePicker({
  onAdd,
  onUndo,
  canUndo,
  compact,
}: {
  onAdd: (ml: number) => void;
  onUndo: () => void;
  canUndo: boolean;
  compact?: boolean;
}) {
  const [pulsed, setPulsed] = useState<number | null>(null);
  const [custom, setCustom] = useState(false);
  const [customMl, setCustomMl] = useState(400);

  const tap = (ml: number) => {
    setPulsed(ml);
    onAdd(ml);
    window.setTimeout(() => setPulsed((p) => (p === ml ? null : p)), 700);
  };

  return (
    <div className="grid grid-cols-4 items-stretch gap-2 sm:flex sm:flex-wrap">
      {BOTTLES.map((b) => (
        <button
          key={b.ml}
          type="button"
          onClick={() => tap(b.ml)}
          className={cn(
            "group glass flex flex-col items-center gap-1 rounded-2xl px-3 py-2 text-primary transition-all hover:-translate-y-0.5 hover:border-primary/50 active:scale-95",
            compact ? "min-w-0 sm:min-w-14" : "min-w-0 sm:min-w-18",
            pulsed === b.ml && "animate-pulse-ring border-primary/70",
          )}
          aria-label={`Dodaj ${b.ml} ml (${b.label})`}
        >
          <BottleGlyph ml={b.ml} active={pulsed === b.ml} />
          <span className="font-display text-xs font-semibold text-foreground">{b.ml}</span>
          {!compact && <span className="text-[10px] text-muted-foreground">{b.label}</span>}
        </button>
      ))}

      {custom ? (
        <div className="glass flex flex-col items-center justify-center gap-1 rounded-2xl px-3 py-2">
          <input
            type="range"
            min={50}
            max={1500}
            step={50}
            value={customMl}
            onChange={(e) => setCustomMl(Number(e.target.value))}
            className="w-full max-w-24 accent-primary"
            aria-label="Własna pojemność w ml"
          />
          <button
            type="button"
            onClick={() => tap(customMl)}
            className="font-display rounded-full bg-primary px-3 py-0.5 text-xs font-semibold text-primary-foreground"
          >
            + {customMl} ml
          </button>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setCustom(true)}
          className={cn(
            "glass flex flex-col items-center justify-center gap-1 rounded-2xl border-dashed px-3 py-2 text-muted-foreground transition-colors hover:text-primary",
            compact ? "min-w-0 sm:min-w-14" : "min-w-0 sm:min-w-18",
          )}
          aria-label="Własna pojemność"
        >
          <Plus className="size-5" />
          <span className="text-[10px]">custom</span>
        </button>
      )}

      <button
        type="button"
        onClick={onUndo}
        disabled={!canUndo}
        className="glass flex items-center justify-center rounded-2xl px-3 text-muted-foreground transition-colors hover:text-foreground disabled:opacity-30"
        aria-label="Cofnij ostatni łyk"
      >
        <Undo2 className="size-4" />
      </button>
    </div>
  );
}
