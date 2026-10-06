import type { Metadata } from "next";
import { CodeGate, Reveal } from "@/components/Actions";
import { guestUnlocked } from "@/lib/auth";
import { readDb } from "@/lib/store";

export const dynamic = "force-dynamic";
export const metadata: Metadata = { robots: { index: false, follow: false } };

export default async function WayPage({ params }: { params: Promise<{ publicId: string; token: string }> }) {
  const { publicId, token } = await params;
  const db = await readDb();
  const booking = db.bookings.find((item) => item.publicId === publicId);
  if (!booking || booking.directionsToken !== token) {
    return (
      <main className="shell">
        <h1>This link isn't valid.</h1>
      </main>
    );
  }
  if (!(await guestUnlocked(publicId, booking.code))) {
    return (
      <main className="shell">
        <CodeGate publicId={publicId} />
      </main>
    );
  }
  if (booking.directionsUsedAt) {
    return (
      <main className="shell">
        <h1>Already opened.</h1>
        <p className="lede">This directions link works once.</p>
      </main>
    );
  }
  return (
    <main className="shell">
      <Reveal publicId={publicId} token={token} />
    </main>
  );
}
