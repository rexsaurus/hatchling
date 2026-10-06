import Link from "next/link";
import { Nav } from "@/components/Nav";
import { requireHost } from "@/lib/auth";
import { readDb } from "@/lib/store";
import { formatWhen } from "@/lib/time";
import { isBookable } from "@/lib/trainer";
import { redirect } from "next/navigation";

export const dynamic = "force-dynamic";

export default async function DashboardPage() {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  const db = await readDb();
  const mine = db.bookings.filter((booking) => booking.providerId === provider.id);
  const pending = mine.filter((booking) => booking.status === "pending" || booking.status === "changes");
  const next = mine
    .filter((booking) => booking.status === "confirmed" && booking.chosenStart && booking.chosenStart > Date.now())
    .sort((a, b) => (a.chosenStart || 0) - (b.chosenStart || 0))[0];
  return (
    <main className="shell">
      <div className="brand">PrivateLink</div>
      <h1>{provider.name || "Your studio"}</h1>
      {!isBookable(provider) ? (
        <div className="note mb">
          Finish your page before clients can book. <Link href="/onboarding">Add your name and address</Link>
        </div>
      ) : (
        <p className="lede">/{provider.slug}</p>
      )}
      {next ? (
        <div className="card mb">
          <div className="label">Next session</div>
          <b>
            {next.customerName} · {formatWhen(next.chosenStart || 0, provider.timezone)}
          </b>
          <div className="fine">{next.showConfirm === "yes" ? "They're coming." : "Waiting on a show-up text."}</div>
        </div>
      ) : null}
      <h2>Open requests</h2>
      {pending.length === 0 ? <p className="muted">Nothing waiting.</p> : null}
      {pending.map((booking) => (
        <Link key={booking.id} href={`/app/bookings/${booking.id}`} className="item">
          <b>
            {booking.customerName} · {booking.serviceName}
          </b>
          <small>{booking.status === "changes" ? "Waiting on new times" : `${booking.options.length} time${booking.options.length === 1 ? "" : "s"} offered`}</small>
        </Link>
      ))}
      <Nav active="home" />
    </main>
  );
}
