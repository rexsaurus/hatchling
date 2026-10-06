import { BookForm } from "@/components/BookForm";
import { slotsFor } from "@/lib/open-slots";
import { readDb } from "@/lib/store";
import { publicTrainer } from "@/lib/trainer";

export const dynamic = "force-dynamic";

export default async function InvitePage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = await params;
  const db = await readDb();
  const invite = db.invites.find((item) => item.token === token);
  const provider = invite && db.providers.find((item) => item.id === invite.providerId);
  if (!invite || !provider) {
    return (
      <main className="shell">
        <h1>This link isn't valid.</h1>
      </main>
    );
  }
  if (invite.bookingId || invite.expiresAt <= Date.now()) {
    return (
      <main className="shell">
        <div className="brand">PrivateLink</div>
        <h1>This link is finished.</h1>
        <p className="lede">It was one time, and that time is over.</p>
      </main>
    );
  }
  const pub = publicTrainer(provider);
  const slotsByService = await slotsFor(provider, db.bookings);
  return (
    <main className="shell">
      <div className="brand">One-time link</div>
      <div className="hero mt">
        {pub.photo ? <img className="photo" src={pub.photo} alt="" /> : <div className="photo">{pub.name.slice(0, 1)}</div>}
        <div>
          <h1>{pub.name}</h1>
          <p className="muted">This link works once.</p>
        </div>
      </div>
      <div className="mt">
        <BookForm
          mode="new"
          trainerName={pub.name}
          timezone={pub.timezone}
          services={pub.services}
          slotsByService={slotsByService}
          security={pub.security}
          inviteToken={invite.token}
        />
      </div>
    </main>
  );
}
