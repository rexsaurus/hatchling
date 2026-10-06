import { toIcs } from "@/lib/ics";
import { readDb } from "@/lib/store";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(req: Request, { params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const key = new URL(req.url).searchParams.get("k");
  const db = await readDb();
  const provider = db.providers.find((item) => item.slug === slug);
  if (!provider || !key || key !== provider.calToken) return new Response("Not found", { status: 404 });
  const ics = toIcs(
    db.bookings
      .filter((booking) => booking.providerId === provider.id && booking.status === "confirmed" && booking.chosenStart)
      .map((booking) => ({
        uid: booking.id,
        start: booking.chosenStart as number,
        end: (booking.chosenStart as number) + booking.minutes * 60000,
        title: `${booking.serviceName} · ${booking.customerName}`,
      })),
  );
  return new Response(ics, { headers: { "Content-Type": "text/calendar; charset=utf-8" } });
}
