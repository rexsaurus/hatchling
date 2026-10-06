import { notFound } from "next/navigation";
import { BookForm } from "@/components/BookForm";
import { slotsFor } from "@/lib/open-slots";
import { readDb } from "@/lib/store";
import { isBookable, publicTrainer } from "@/lib/trainer";

export const dynamic = "force-dynamic";

export default async function TrainerPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const db = await readDb();
  const provider = db.providers.find((item) => item.slug === slug);
  if (!provider || !provider.name) notFound();
  const pub = publicTrainer(provider);
  const bookable = isBookable(provider);
  const slotsByService = bookable ? await slotsFor(provider, db.bookings) : {};
  return (
    <main className="shell">
      <div className="brand">PrivateLink</div>
      <div className="hero mt">
        {pub.photo ? <img className="photo" src={pub.photo} alt="" /> : <div className="photo">{pub.name.slice(0, 1)}</div>}
        <div>
          <h1>{pub.name}</h1>
          <p className="muted">{pub.line || pub.placeLabel}</p>
        </div>
      </div>
      {bookable ? (
        <div className="mt">
          <BookForm
            mode="new"
            trainerName={pub.name}
            timezone={pub.timezone}
            services={pub.services}
            slotsByService={slotsByService}
            security={pub.security}
            slug={pub.slug}
          />
        </div>
      ) : (
        <p className="note mt">This page isn't taking sessions yet.</p>
      )}
    </main>
  );
}
