import { clearHostCookie } from "@/lib/auth";

export async function POST() {
  await clearHostCookie();
  return Response.json({ ok: true });
}
