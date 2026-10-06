import { saveProvider } from "@/lib/act";
import { hostId } from "@/lib/auth";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const id = await hostId();
  if (!id) return Response.json({ error: "Sign in again." }, { status: 401 });
  const body = await req.json().catch(() => null);
  if (!body || typeof body !== "object") return Response.json({ error: "Nothing to save." }, { status: 400 });
  const result = await saveProvider(id, body);
  return Response.json(result, { status: "error" in result ? 400 : 200 });
}
