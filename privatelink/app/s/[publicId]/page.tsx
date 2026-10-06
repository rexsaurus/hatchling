import type { Metadata } from "next";
import { BookForm } from "@/components/BookForm";
import { CodeGate, ShowButtons } from "@/components/Actions";
import { guestUnlocked } from "@/lib/auth";
import { slotsFor } from "@/lib/open-slots";
import { readDb } from "@/lib/store";
import { formatWhen } from "@/lib/time";
import { publicTrainer } from "@/lib/trainer";

export const dynamic = "force-dynamic";
export const metadata: Metadata = { robots: { index: false, follow: false } };

export default async function SessionPage({ params }: { params: Promise<{ publicId: string }> }) {
  const { publicId } = await params;
  const db = await readDb();
  const booking = db.bookings.find((item) => item.publicId === publicId);
  if (!booking) {
    return (
      <main className="shell">
        <h1>This link isn't valid.</h1>
      </main>
    );
  }
  const unlocked = await guestUnlocked(publicId, booking.code);
  if (!unlocked) {
    return (
      <main className="shell">
        <CodeGate publicId={publicId} />
      </main>
    );
  }
  const provider = db.providers.find((item) => item.id === booking.providerId);
  if (!provider) {
    return (
      <main className="shell">
        <h1>This link isn't valid.</h1>
      </main>
    );
  }
  if (Date.now() > booking.sessionExpiresAt) {
    return (
      <main className="shell">
        <h1>This link expired.</h1>
        <p className="lede">The session isn't on this page anymore.</p>
      </main>
    );
  }
  const when = booking.chosenStart ? formatWhen(booking.chosenStart, provider.timezone) : "";
  return (
    <main className="shell">
      <div className="brand">{provider.name}</div>
      {booking.status === "pending" ? (
        <>
          <h1>Request sent.</h1>
          <p className="lede">{provider.name} has the times. You'll get a text when they answer.</p>
          <div className="code">{booking.code}</div>
          <p className="fine">Keep this code. It's the only way back into this session.</p>
        </>
      ) : null}
      {booking.status === "changes" ? (
        <>
          <h1>Send another time.</h1>
          <BookForm
            mode="resubmit"
            trainerName={provider.name}
            timezone={provider.timezone}
            services={publicTrainer(provider).services.filter((service) => service.id === booking.serviceId)}
            slotsByService={await slotsFor(provider, db.bookings)}
            security={publicTrainer(provider).security}
            publicId={booking.publicId}
            changeNote={booking.changeNote}
          />
        </>
      ) : null}
      {booking.status === "confirmed" ? (
        <>
          <h1>You're set.</h1>
          <div className="card">
            <b>
              {booking.serviceName}
              <br />
              {when}
            </b>
            <p className="fine">Code {booking.code}</p>
          </div>
          {booking.showConfirm === "yes" ? (
            <p className="note mt">You're confirmed. If directions are coming, they'll arrive as a separate one-time link.</p>
          ) : (
            <ShowButtons publicId={booking.publicId} asked={booking.showConfirm === "asked"} />
          )}
        </>
      ) : null}
      {booking.status === "declined" || booking.status === "canceled" || booking.status === "expired" ? (
        <>
          <h1>Closed.</h1>
          <p className="lede">
            {booking.status === "expired" ? "The request expired. Nothing was held." : `This session was ${booking.status}.`}
          </p>
        </>
      ) : null}
    </main>
  );
}
