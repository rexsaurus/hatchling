import { cookies } from "next/headers";
import { readDb } from "./store";
import { sign, unsign } from "./token";

const HOST = "pl_host";
const GUEST = "pl_guest";
const PHONE = "pl_phone";

function cookieBase() {
  return {
    httpOnly: true,
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
  };
}

export async function setHostCookie(providerId: string) {
  const exp = Date.now() + 30 * 86400000;
  const jar = await cookies();
  jar.set(HOST, await sign(`${providerId}|${exp}`), { ...cookieBase(), expires: new Date(exp) });
}

export async function clearHostCookie() {
  const jar = await cookies();
  jar.delete(HOST);
}

export async function hostId() {
  const jar = await cookies();
  const data = await unsign(jar.get(HOST)?.value || "");
  if (!data) return null;
  const [id, exp] = data.split("|");
  if (!id || Number(exp) < Date.now()) return null;
  return id;
}

export async function requireHost() {
  const id = await hostId();
  if (!id) return null;
  const db = await readDb();
  return db.providers.find((provider) => provider.id === id) || null;
}

export async function setPhoneCookie(phone: string) {
  const exp = Date.now() + 15 * 60000;
  const jar = await cookies();
  jar.set(PHONE, await sign(`${phone}|${exp}`), { ...cookieBase(), expires: new Date(exp) });
}

export async function phoneFromCookie() {
  const jar = await cookies();
  const data = await unsign(jar.get(PHONE)?.value || "");
  if (!data) return null;
  const [phone, exp] = data.split("|");
  if (!phone || Number(exp) < Date.now()) return null;
  return phone;
}

export async function setGuestCookie(publicId: string, code: string) {
  const exp = Date.now() + 14 * 86400000;
  const jar = await cookies();
  jar.set(GUEST, await sign(`${publicId}|${code}|${exp}`), { ...cookieBase(), expires: new Date(exp) });
}

export async function guestUnlocked(publicId: string, code: string) {
  const jar = await cookies();
  const data = await unsign(jar.get(GUEST)?.value || "");
  if (!data) return false;
  const [id, saved, exp] = data.split("|");
  return id === publicId && saved === code && Number(exp) > Date.now();
}
