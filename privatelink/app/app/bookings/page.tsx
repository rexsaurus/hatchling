import Link from "next/link";
import { redirect } from "next/navigation";
import { Nav } from "@/components/Nav";
import { requireHost } from "@/lib/auth";
import { readDb } from "@/lib/store";
import { formatWhen } from "@/lib/time";

export const dynamic = "force-dynamic";

export default async function BookingsPage() {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  const db = await readDb();
  const mine = db.bookings
    .filter((booking) => booking.providerId === provider.id)
    .sort((a, b) => b.createdAt - a.createdAt);
  return (
    <main className="shell">
      <div className="brand">Requests</div>
      <h1>Everyone who wrote in.</h1>
      {mine.length === 0 ? <p className="muted">When someone uses your link, the request lands here and in a text.</p> : null}
      {mine.map((booking) => (
        <Link key={booking.id} href={`/app/bookings/${booking.id}`} className="item">
          <b>
            {booking.customerName} · {booking.status}
          </b>
          <small>
            {booking.serviceName}
            {booking.chosenStart ? ` · ${formatWhen(booking.chosenStart, provider.timezone)}` : ""}
          </small>
        </Link>
      ))}
      <Nav active="bookings" />
    </main>
  );
}
