import { verifyOtp } from "@/lib/act";
import { phoneFromCookie, setHostCookie } from "@/lib/auth";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const phone = await phoneFromCookie();
  if (!phone) return Response.json({ error: "Start again with your mobile number." }, { status: 400 });
  const body = await req.json().catch(() => ({}));
  const result = await verifyOtp(phone, String(body.code || ""));
  if (result.error || !result.providerId) return Response.json({ error: result.error || "Try again." }, { status: 400 });
  await setHostCookie(result.providerId);
  return Response.json({ needsSetup: result.needsSetup });
}
