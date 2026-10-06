import { revealDirections } from "@/lib/act";
import { guestUnlocked } from "@/lib/auth";
import { readDb } from "@/lib/store";

export const runtime = "nodejs";

export async function POST(req: Request, { params }: { params: Promise<{ publicId: string }> }) {
  const { publicId } = await params;
  const body = await req.json().catch(() => ({}));
  const db = await readDb();
  const booking = db.bookings.find((item) => item.publicId === publicId);
  if (!booking || !(await guestUnlocked(publicId, booking.code))) {
    return Response.json({ error: "Enter the session code first." }, { status: 401 });
  }
  const result = await revealDirections(publicId, String(body.token || ""));
  return Response.json(result, { status: "error" in result ? 400 : 200 });
}
