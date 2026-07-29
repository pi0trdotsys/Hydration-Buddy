import { cn } from "@/lib/utils";

export function InsightCard({
  eyebrow,
  title,
  body,
  className,
}: {
  eyebrow: string;
  title?: string;
  body: string;
  className?: string;
}) {
  return (
    <article className={cn("glass rounded-3xl p-5", className)}>
      <p className="font-display text-[11px] tracking-[0.18em] text-primary uppercase">{eyebrow}</p>
      {title && <h3 className="mt-2 text-lg font-semibold">{title}</h3>}
      <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{body}</p>
    </article>
  );
}
