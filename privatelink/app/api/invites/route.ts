import { createInvite } from "@/lib/act";
import { hostId } from "@/lib/auth";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const providerId = await hostId();
  if (!providerId) return Response.json({ error: "Sign in again." }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const invite = await createInvite(providerId, Number(body.hours) || 24);
  return Response.json({ token: invite.token });
}
