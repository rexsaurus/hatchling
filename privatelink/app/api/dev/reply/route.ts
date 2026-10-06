import { applyInbound } from "@/lib/act";

export const runtime = "nodejs";

export async function POST(req: Request) {
  if (process.env.NODE_ENV === "production") return Response.json({ error: "Not available." }, { status: 404 });
  const body = await req.json().catch(() => ({}));
  await applyInbound(String(body.from || ""), String(body.body || ""));
  return Response.json({ ok: true });
}
