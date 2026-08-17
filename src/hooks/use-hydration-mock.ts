import { useCallback, useMemo, useState } from "react";
import {
  DAYPART_NOTES,
  FACTS,
  MASCOT_LINES,
  SELF_CARE,
  daypartFor,
  levelFor,
  pick,
} from "@/data/hydration-content";

export type Intake = { id: number; ml: number; hour: number; minute: number };

const SEED_INTAKES: Intake[] = [
  { id: 1, ml: 250, hour: 7, minute: 20 },
  { id: 2, ml: 330, hour: 9, minute: 5 },
  { id: 3, ml: 500, hour: 11, minute: 40 },
  { id: 4, ml: 250, hour: 13, minute: 15 },
  { id: 5, ml: 120, hour: 15, minute: 0 },
];

export const WEEK = [
  { day: "Pn", pct: 0.82 },
  { day: "Wt", pct: 1 },
  { day: "Śr", pct: 0.64 },
  { day: "Cz", pct: 0.95 },
  { day: "Pt", pct: 1 },
  { day: "So", pct: 0.48 },
];

export type HistoryDay = {
  day: string;
  date: string;
  ml: number;
  goal: number;
  pct: number;
  reached: boolean;
};

export const HISTORY: HistoryDay[] = [
  { day: "Pn", date: "11.08", ml: 2050, goal: 2500, pct: 0.82, reached: false },
  { day: "Wt", date: "12.08", ml: 2500, goal: 2500, pct: 1, reached: true },
  { day: "Śr", date: "13.08", ml: 1600, goal: 2500, pct: 0.64, reached: false },
  { day: "Cz", date: "14.08", ml: 2375, goal: 2500, pct: 0.95, reached: false },
  { day: "Pt", date: "15.08", ml: 2500, goal: 2500, pct: 1, reached: true },
  { day: "So", date: "16.08", ml: 1200, goal: 2500, pct: 0.48, reached: false },
];

export const DAY_NAMES: Record<string, string> = {
  Pn: "Poniedziałek",
  Wt: "Wtorek",
  Śr: "Środa",
  Cz: "Czwartek",
  Pt: "Piątek",
  So: "Sobota",
  Nd: "Niedziela",
};

export function useHydrationMock() {
  const [goal, setGoal] = useState(2500);
  const [intakes, setIntakes] = useState<Intake[]>(SEED_INTAKES);
  const [lastAdded, setLastAdded] = useState<number | null>(null);

  const total = useMemo(() => intakes.reduce((s, i) => s + i.ml, 0), [intakes]);
  const progress = Math.min(total / goal, 1);
  const level = levelFor(total / goal);
  const daypart = daypartFor(15);

  const add = useCallback((ml: number) => {
    setIntakes((prev) => [
      ...prev,
      { id: Date.now(), ml, hour: 15 + Math.floor(prev.length / 4), minute: (prev.length * 7) % 60 },
    ]);
    setLastAdded(ml);
  }, []);

  const undo = useCallback(() => {
    setIntakes((prev) => prev.slice(0, -1));
    setLastAdded(null);
  }, []);

  const seed = intakes.length + Math.round(total / 100);

  const history: HistoryDay[] = useMemo(
    () => [
      ...HISTORY,
      {
        day: "Nd",
        date: "17.08",
        ml: total,
        goal,
        pct: progress,
        reached: progress >= 1,
      },
    ],
    [total, goal, progress],
  );

  return {
    goal,
    setGoal,
    intakes,
    total,
    progress,
    level,
    daypart,
    lastAdded,
    add,
    undo,
    remaining: Math.max(goal - total, 0),
    streak: 5,
    week: WEEK,
    history,
    selfCare: pick(SELF_CARE[level], seed),
    selfCareAlt: pick(SELF_CARE[level], seed + 1),
    fact: pick(FACTS, seed * 3 + 1),
    dayNote: pick(DAYPART_NOTES[daypart], seed),
    mascotLine: pick(MASCOT_LINES[level], seed * 5),
  };
}

export type HydrationState = ReturnType<typeof useHydrationMock>;
