import { requestOtp } from "@/lib/act";
import { setPhoneCookie } from "@/lib/auth";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const result = await requestOtp(String(body.phone || ""));
  if ("error" in result && result.error) return Response.json(result, { status: 400 });
  if (result.phone) await setPhoneCookie(result.phone);
  return Response.json({ devCode: "devCode" in result ? result.devCode : undefined });
}
