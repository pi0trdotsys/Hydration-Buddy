import { cn } from "@/lib/utils";
import type { Level } from "@/data/hydration-content";

const FACE: Record<Level, { eye: string; mouth: string; alpha: number }> = {
  low: { eye: "M -6 0 h 5 M 6 0 h -5", mouth: "M -7 9 q 7 -5 14 0", alpha: 0.65 },
  mid: { eye: "", mouth: "M -7 7 q 7 6 14 0", alpha: 0.85 },
  high: { eye: "", mouth: "M -8 6 q 8 9 16 0", alpha: 1 },
  done: { eye: "", mouth: "M -9 5 q 9 12 18 0", alpha: 1 },
};

export function MascotDrop({
  level,
  size = 96,
  className,
  onPoke,
}: {
  level: Level;
  size?: number;
  className?: string;
  onPoke?: () => void;
}) {
  const face = FACE[level];
  const celebrating = level === "done";

  return (
    <button
      type="button"
      onClick={onPoke}
      aria-label="Kropi, maskotka nawodnienia"
      className={cn("relative shrink-0 transition-transform active:scale-90", className)}
      style={{ width: size, height: size }}
    >
      {celebrating &&
        [0, 1, 2, 3].map((i) => (
          <span
            key={i}
            className="animate-bubble absolute bottom-2 size-2 rounded-full bg-primary/70"
            style={{ left: `${18 + i * 20}%`, animationDelay: `${i * 0.35}s` }}
          />
        ))}
      <svg viewBox="-50 -55 100 110" width={size} height={size} className="animate-float">
        <defs>
          <linearGradient id="dropGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--aqua)" />
            <stop offset="100%" stopColor="var(--aqua-deep)" />
          </linearGradient>
        </defs>
        <path
          d="M0 -48 C 22 -18, 40 -2, 40 16 A 40 40 0 1 1 -40 16 C -40 -2, -22 -18, 0 -48 Z"
          fill="url(#dropGrad)"
          opacity={face.alpha}
        />
        <ellipse cx="-14" cy="-6" rx="8" ry="10" fill="var(--color-background)" opacity="0.9" />
        <ellipse cx="14" cy="-6" rx="8" ry="10" fill="var(--color-background)" opacity="0.9" />
        <circle cx="-12" cy="-4" r="4" fill="var(--color-foreground)" />
        <circle cx="16" cy="-4" r="4" fill="var(--color-foreground)" />
        <path
          d={face.mouth}
          transform="translate(0 14)"
          stroke="var(--color-background)"
          strokeWidth="4"
          strokeLinecap="round"
          fill="none"
        />
        <ellipse cx="-24" cy="14" rx="7" ry="4" fill="var(--color-background)" opacity="0.25" />
        <ellipse cx="26" cy="14" rx="7" ry="4" fill="var(--color-background)" opacity="0.25" />
      </svg>
    </button>
  );
}
