import { createBooking } from "@/lib/act";
import { setGuestCookie } from "@/lib/auth";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const result = await createBooking({
    slug: body.slug,
    inviteToken: body.inviteToken,
    serviceId: String(body.serviceId || ""),
    options: Array.isArray(body.options) ? body.options : [],
    customerName: String(body.customerName || ""),
    customerPhone: String(body.customerPhone || ""),
    link: String(body.link || ""),
    idImage: String(body.idImage || ""),
    answers: Array.isArray(body.answers) ? body.answers.map(String) : [],
  });
  if ("error" in result) return Response.json(result, { status: 400 });
  await setGuestCookie(result.booking.publicId, result.booking.code);
  return Response.json({ publicId: result.booking.publicId });
}
