import { resubmitBooking, showAnswer, unlockSession } from "@/lib/act";
import { guestUnlocked, setGuestCookie } from "@/lib/auth";
import { readDb } from "@/lib/store";

export const runtime = "nodejs";

export async function POST(req: Request, { params }: { params: Promise<{ publicId: string }> }) {
  const { publicId } = await params;
  const body = await req.json().catch(() => ({}));
  if (body.action === "unlock") {
    const result = await unlockSession(publicId, String(body.code || ""));
    if ("error" in result) return Response.json(result, { status: 400 });
    await setGuestCookie(publicId, String(body.code || "").trim());
    return Response.json({ ok: true });
  }
  const db = await readDb();
  const booking = db.bookings.find((item) => item.publicId === publicId);
  if (!booking || !(await guestUnlocked(publicId, booking.code))) {
    return Response.json({ error: "Enter the session code." }, { status: 401 });
  }
  if (body.action === "resubmit") {
    const result = await resubmitBooking(publicId, {
      options: Array.isArray(body.options) ? body.options : [],
      link: body.link,
      idImage: body.idImage,
      answers: Array.isArray(body.answers) ? body.answers.map(String) : undefined,
    });
    return Response.json(result, { status: "error" in result ? 400 : 200 });
  }
  if (body.action === "show") {
    const result = await showAnswer(publicId, Boolean(body.coming));
    return Response.json(result, { status: "error" in result ? 400 : 200 });
  }
  return Response.json({ error: "That action isn't available." }, { status: 400 });
}
