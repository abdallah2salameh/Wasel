import { PropsWithChildren } from "react";

export function StatusPill({
  tone = "neutral",
  children
}: PropsWithChildren<{ tone?: "neutral" | "alert" | "warning" }>) {
  return <span className={`pill ${tone}`}>{children}</span>;
}

export function EmptyState({
  title,
  description
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      <p style={{ marginBottom: 0 }}>{description}</p>
    </div>
  );
}
