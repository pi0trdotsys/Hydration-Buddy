import { createFileRoute } from "@tanstack/react-router";
import { useHydrationMock } from "@/hooks/use-hydration-mock";
import { HydrationWidget } from "@/components/hydration/hydration-widget";

const TITLE = "Widget nawodnienia — 3 rozmiary | Kropi";
const DESC =
  "Podgląd skalowalnego widgetu nawodnienia w rozmiarach 1x1, 2x1 i 2x2 z klikalnymi butelkami i postępem dnia.";

export const Route = createFileRoute("/widget")({
  head: () => ({
    meta: [
      { title: TITLE },
      { name: "description", content: DESC },
      { property: "og:title", content: TITLE },
      { property: "og:description", content: DESC },
    ],
  }),
  component: WidgetPage,
});

function WidgetPage() {
  const state = useHydrationMock();

  return (
    <main className="mx-auto w-full max-w-5xl px-4 pt-6 pb-20">
      <h1 className="text-3xl font-bold">Widget — skalowalny</h1>
      <p className="mt-2 max-w-xl text-sm text-muted-foreground">
        Jeden komponent, trzy rozmiary. Stan jest współdzielony na tej stronie — kliknij butelkę w
        dowolnym widgecie, a wszystkie zaktualizują się razem.
      </p>

      <div className="mt-8 grid gap-8">
        {(
          [
            ["sm", "Mały (1×1)", "Pierścień postępu, procent i mini-maskotka."],
            ["md", "Średni (2×1)", "Postęp, licznik ml, butelki i jednolinijkowy komentarz."],
            ["lg", "Duży (2×2)", "Wszystko powyżej + wykres godzinowy i ciekawostka."],
          ] as const
        ).map(([size, label, desc]) => (
          <section key={size}>
            <h2 className="text-lg font-semibold">{label}</h2>
            <p className="mb-3 text-xs text-muted-foreground">{desc}</p>
            <HydrationWidget state={state} size={size} />
          </section>
        ))}
      </div>
    </main>
  );
}
