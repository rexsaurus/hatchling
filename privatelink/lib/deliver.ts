import { uid } from "./ids";
import { updateDb } from "./store";
import type { Outbound } from "./types";

type TwilioClient = {
  messages: {
    create: (args: { to: string; from: string; body: string; mediaUrl?: string[] }) => Promise<unknown>;
  };
};

let cached: TwilioClient | null | undefined;

async function client(): Promise<TwilioClient | null> {
  if (cached !== undefined) return cached;
  const sid = process.env.TWILIO_ACCOUNT_SID;
  const token = process.env.TWILIO_AUTH_TOKEN;
  if (!sid || !token || !process.env.TWILIO_FROM_NUMBER) {
    cached = null;
    return null;
  }
  const twilio = (await import("twilio")).default;
  cached = twilio(sid, token) as TwilioClient;
  return cached;
}

export function textsConfigured() {
  return Boolean(process.env.TWILIO_ACCOUNT_SID && process.env.TWILIO_AUTH_TOKEN && process.env.TWILIO_FROM_NUMBER);
}

export async function deliver(text: Outbound) {
  const from = process.env.TWILIO_FROM_NUMBER || "PrivateLink";
  let delivered = false;
  const twilio = await client();
  if (twilio && process.env.TWILIO_FROM_NUMBER) {
    try {
      await twilio.messages.create({
        to: text.to,
        from: process.env.TWILIO_FROM_NUMBER,
        body: text.body,
        ...(text.mediaUrl ? { mediaUrl: [text.mediaUrl] } : {}),
      });
      delivered = true;
    } catch (err) {
      console.error("Twilio send failed", err);
    }
  }
  await updateDb((db) => {
    db.messages.push({
      id: uid(),
      providerId: text.providerId,
      bookingId: text.bookingId,
      to: text.to,
      from,
      body: text.body,
      mediaUrl: text.mediaUrl || "",
      direction: "out",
      delivered,
      createdAt: Date.now(),
    });
  });
  return delivered;
}
