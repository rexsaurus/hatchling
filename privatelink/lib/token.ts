function secret() {
  if (process.env.SESSION_SECRET) return process.env.SESSION_SECRET;
  if (process.env.NODE_ENV === "production") throw new Error("SESSION_SECRET is required");
  return "privatelink-dev-secret";
}

function safeEqual(a: string, b: string) {
  if (a.length !== b.length) return false;
  let out = 0;
  for (let i = 0; i < a.length; i++) out |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return out === 0;
}

async function hmac(data: string) {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret()),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(data));
  return [...new Uint8Array(sig)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

export async function sign(data: string) {
  return `${data}.${await hmac(data)}`;
}

export async function unsign(token: string) {
  const idx = token.lastIndexOf(".");
  if (idx < 0) return null;
  const data = token.slice(0, idx);
  const sig = token.slice(idx + 1);
  if (!safeEqual(sig, await hmac(data))) return null;
  return data;
}
