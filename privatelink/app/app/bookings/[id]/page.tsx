import { notFound, redirect } from "next/navigation";
import { DecisionPanel } from "@/components/Actions";
import { Nav } from "@/components/Nav";
import { requireHost } from "@/lib/auth";
import { prettyPhone } from "@/lib/phone";
import { readDb } from "@/lib/store";
import { formatWhen } from "@/lib/time";

export const dynamic = "force-dynamic";

export default async function BookingPage({ params }: { params: Promise<{ id: string }> }) {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  const { id } = await params;
  const db = await readDb();
  const booking = db.bookings.find((item) => item.id === id && item.providerId === provider.id);
  if (!booking) notFound();
  return (
    <main className="shell">
      <div className="brand">Request {booking.code}</div>
      <h1>{booking.customerName}</h1>
      <p className="lede">
        {booking.serviceName} · {booking.minutes} min{booking.price ? ` · $${booking.price}` : ""} · {booking.status}
      </p>
      <div className="card">
        <div>{prettyPhone(booking.customerPhone)}</div>
        {booking.chosenStart ? <div>Booked {formatWhen(booking.chosenStart, provider.timezone)}</div> : null}
        {booking.status === "pending"
          ? booking.options.map((start) => <div key={start}>{formatWhen(start, provider.timezone)}</div>)
          : null}
        {booking.link ? <div className="fine mt">{booking.link}</div> : null}
        {booking.answers.map((answer) => (
          <p key={answer.q}>
            <b>{answer.q}</b>
            <br />
            {answer.a}
          </p>
        ))}
        {booking.idImage ? <img src={booking.idImage} alt="ID submitted with the request" style={{ width: "100%", borderRadius: 12 }} /> : null}
        {booking.changeNote ? <div className="note mt">{booking.changeNote}</div> : null}
        <div className="fine mt">Show-up: {booking.showConfirm}</div>
      </div>
      <DecisionPanel
        id={booking.id}
        status={booking.status}
        showConfirm={booking.showConfirm}
        options={booking.options.map((start, index) => ({ index, label: formatWhen(start, provider.timezone) }))}
      />
      <Nav active="bookings" />
    </main>
  );
}
