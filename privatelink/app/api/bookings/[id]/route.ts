import { decide } from "@/lib/act";
import { hostId } from "@/lib/auth";

export const runtime = "nodejs";

export async function POST(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const providerId = await hostId();
  if (!providerId) return Response.json({ error: "Sign in again." }, { status: 401 });
  const { id } = await params;
  const body = await req.json().catch(() => ({}));
  const action = body.action;
  if (!["yes", "change", "no", "ask", "directions", "cancel"].includes(action)) {
    return Response.json({ error: "That action isn't available." }, { status: 400 });
  }
  const result = await decide(id, providerId, action, { option: body.option, note: body.note });
  return Response.json(result, { status: "error" in result ? 400 : 200 });
}
