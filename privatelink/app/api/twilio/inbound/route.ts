import { applyInbound } from "@/lib/act";
import { textsConfigured } from "@/lib/deliver";
import { baseUrl } from "@/lib/url";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const form = await req.formData();
  const params: Record<string, string> = {};
  for (const [key, value] of form.entries()) params[key] = String(value);
  if (textsConfigured()) {
    const twilio = (await import("twilio")).default;
    const signature = req.headers.get("x-twilio-signature") || "";
    const url = `${baseUrl()}/api/twilio/inbound`;
    const valid = twilio.validateRequest(process.env.TWILIO_AUTH_TOKEN || "", signature, url, params);
    if (!valid) return new Response("Invalid signature", { status: 403 });
  } else if (process.env.NODE_ENV === "production") {
    return new Response("Twilio is not configured", { status: 503 });
  }
  await applyInbound(params.From || "", params.Body || "");
  return new Response("<Response></Response>", { headers: { "Content-Type": "text/xml" } });
}
