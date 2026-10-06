import { deliver, textsConfigured } from "./deliver";
import { busyFromIcs } from "./slots";
import { buildSlots } from "./slots";
import { code4, code6, token, uid } from "./ids";
import { normalizePhone } from "./phone";
import { parseReply } from "./reply";
import {
  acceptOption,
  answerShow,
  askToConfirm,
  cancelBooking,
  checkCode,
  declineRequest,
  expireBooking,
  openDirections,
  releaseDirections,
  reminderAction,
  requestChange,
  resubmit,
} from "./rules";
import { readDb, updateDb } from "./store";
import { formatWhen } from "./time";
import { cleanHours, cleanQuestions, cleanServices, slugify } from "./trainer";
import type { Booking, Outbound, Provider, Security, Service, WeekWindow } from "./types";
import { baseUrl, safeHttpUrl } from "./url";

const icsCache = new Map<string, { at: number; ics: string }>();

export async function loadBusy(provider: Provider, now: number) {
  if (!provider.busyIcsUrl) return [];
  const url = safeHttpUrl(provider.busyIcsUrl);
  if (!url) return [];
  try {
    const ics = await fetchIcs(url);
    return busyFromIcs(ics, provider, now);
  } catch {
    return [];
  }
}

export async function fetchIcs(url: string) {
  const hit = icsCache.get(url);
  if (hit && Date.now() - hit.at < 5 * 60000) return hit.ics;
  const safe = safeHttpUrl(url);
  if (!safe) throw new Error("Use a public https calendar link.");
  const res = await fetch(safe, { signal: AbortSignal.timeout(2500), redirect: "follow" });
  if (!res.ok) throw new Error("That calendar didn't respond.");
  const text = (await res.text()).slice(0, 500_000);
  if (!text.includes("BEGIN:VCALENDAR")) throw new Error("That link isn't a calendar file.");
  icsCache.set(url, { at: Date.now(), ics: text });
  return text;
}

function blankProvider(phone: string): Provider {
  return {
    id: uid(),
    phone,
    name: "",
    slug: "",
    photo: "",
    line: "",
    timezone: "America/Los_Angeles",
    address: "",
    placeLabel: "Home studio",
    services: [{ id: uid(), name: "Private session", minutes: 60, price: "", detail: "" }],
    hours: [1, 2, 3, 4, 5].map((dow) => ({ dow, start: "09:00", end: "17:00" })),
    bufferMinutes: 15,
    horizonDays: 14,
    minNoticeMinutes: 180,
    busyIcsUrl: "",
    security: { requireLink: false, linkLabel: "A link that shows it's you", requireId: false, questions: [] },
    confirmLeadMinutes: 30,
    autoCancelIfUnconfirmed: true,
    requestExpiresHours: 12,
    calToken: token(12),
    createdAt: Date.now(),
  };
}

export async function requestOtp(phoneRaw: string) {
  const phone = normalizePhone(phoneRaw);
  if (!phone) return { error: "Enter a mobile number that can get texts." };
  if (!textsConfigured() && process.env.NODE_ENV === "production") {
    return { error: "Texting isn't set up on this server yet." };
  }
  const code = code6();
  await updateDb((db) => {
    db.otps = db.otps.filter((otp) => otp.phone !== phone);
    db.otps.push({ phone, code, expiresAt: Date.now() + 10 * 60000, attempts: 0 });
  });
  if (textsConfigured()) {
    await deliver({
      to: phone,
      body: `Your PrivateLink code is ${code}. It expires in 10 minutes.`,
      providerId: "",
      bookingId: null,
    });
    return { phone };
  }
  return { phone, devCode: code };
}

export async function verifyOtp(phoneRaw: string, code: string) {
  const phone = normalizePhone(phoneRaw);
  if (!phone) return { error: "Start again with your mobile number." };
  let result: { error?: string; providerId?: string; needsSetup?: boolean } = {};
  await updateDb((db) => {
    const otp = db.otps.find((item) => item.phone === phone);
    if (!otp || otp.expiresAt < Date.now()) {
      result = { error: "That code expired. Request a new one." };
      return;
    }
    if (otp.attempts >= 5) {
      result = { error: "Too many tries. Request a new code." };
      return;
    }
    if (otp.code !== code.trim()) {
      otp.attempts += 1;
      result = { error: "That code doesn't match." };
      return;
    }
    db.otps = db.otps.filter((item) => item.phone !== phone);
    let provider = db.providers.find((item) => item.phone === phone);
    if (!provider) {
      provider = blankProvider(phone);
      db.providers.push(provider);
    }
    result = { providerId: provider.id, needsSetup: !provider.name || !provider.slug || !provider.address };
  });
  return result;
}

export type ProviderPatch = Partial<{
  name: string;
  slug: string;
  photo: string;
  line: string;
  timezone: string;
  address: string;
  placeLabel: string;
  services: Service[];
  hours: WeekWindow[];
  bufferMinutes: number;
  horizonDays: number;
  minNoticeMinutes: number;
  busyIcsUrl: string;
  security: Security;
  confirmLeadMinutes: number;
  autoCancelIfUnconfirmed: boolean;
  requestExpiresHours: number;
}>;

export async function saveProvider(providerId: string, patch: ProviderPatch) {
  if (patch.busyIcsUrl) {
    const safe = safeHttpUrl(patch.busyIcsUrl);
    if (!safe) return { error: "The calendar link has to be a public http address." };
    try {
      await fetchIcs(safe);
      patch = { ...patch, busyIcsUrl: safe };
    } catch (err) {
      return { error: err instanceof Error ? err.message : "Couldn't read that calendar." };
    }
  }
  let error = "";
  await updateDb((db) => {
    const provider = db.providers.find((item) => item.id === providerId);
    if (!provider) {
      error = "Sign in again.";
      return;
    }
    if (patch.name != null) provider.name = patch.name.trim().slice(0, 40);
    if (patch.slug != null) {
      const slug = slugify(patch.slug);
      if (!slug) {
        error = "Use at least 3 letters or numbers for the link.";
        return;
      }
      if (db.providers.some((item) => item.slug === slug && item.id !== provider.id)) {
        error = "That link is already taken.";
        return;
      }
      provider.slug = slug;
    }
    if (patch.photo != null) {
      if (patch.photo && !isImage(patch.photo, 350_000)) {
        error = "Use a smaller photo.";
        return;
      }
      provider.photo = patch.photo;
    }
    if (patch.line != null) provider.line = patch.line.trim().slice(0, 80);
    if (patch.timezone != null) provider.timezone = patch.timezone;
    if (patch.address != null) provider.address = patch.address.trim().slice(0, 160);
    if (patch.placeLabel != null) provider.placeLabel = patch.placeLabel.trim().slice(0, 40);
    if (patch.services) {
      const services = cleanServices(patch.services);
      if (!services.length) {
        error = "Add at least one session.";
        return;
      }
      provider.services = services;
    }
    if (patch.hours) provider.hours = cleanHours(patch.hours);
    if (patch.bufferMinutes != null) provider.bufferMinutes = clamp(patch.bufferMinutes, 0, 120);
    if (patch.horizonDays != null) provider.horizonDays = clamp(patch.horizonDays, 1, 60);
    if (patch.minNoticeMinutes != null) provider.minNoticeMinutes = clamp(patch.minNoticeMinutes, 0, 10080);
    if (patch.busyIcsUrl != null) provider.busyIcsUrl = patch.busyIcsUrl.trim();
    if (patch.security) {
      provider.security = {
        requireLink: Boolean(patch.security.requireLink),
        linkLabel: (patch.security.linkLabel || "A link that shows it's you").trim().slice(0, 80),
        requireId: Boolean(patch.security.requireId),
        questions: cleanQuestions(patch.security.questions || []),
      };
    }
    if (patch.confirmLeadMinutes != null) provider.confirmLeadMinutes = clamp(patch.confirmLeadMinutes, 5, 240);
    if (patch.autoCancelIfUnconfirmed != null) provider.autoCancelIfUnconfirmed = Boolean(patch.autoCancelIfUnconfirmed);
    if (patch.requestExpiresHours != null) provider.requestExpiresHours = clamp(patch.requestExpiresHours, 1, 72);
  });
  return error ? { error } : { ok: true as const };
}

export async function createInvite(providerId: string, hours: number) {
  const invite = {
    id: uid(),
    providerId,
    token: token(10),
    expiresAt: Date.now() + clamp(hours, 1, 168) * 3600000,
    bookingId: null,
    createdAt: Date.now(),
  };
  await updateDb((db) => {
    db.invites.push(invite);
  });
  return invite;
}

type BookingInput = {
  slug?: string;
  inviteToken?: string;
  serviceId: string;
  options: number[];
  customerName: string;
  customerPhone: string;
  link: string;
  idImage: string;
  answers: string[];
};

export async function createBooking(input: BookingInput) {
  const now = Date.now();
  const phone = normalizePhone(input.customerPhone);
  if (!phone) return { error: "Enter the mobile number where you can get the text." };
  const outbound: Outbound[] = [];
  const outcome: { error: string; booking: Booking | null } = { error: "", booking: null };
  const dbPreview = await readDb();
  const previewProvider = findTarget(dbPreview, input.slug, input.inviteToken);
  const busy = previewProvider && typeof previewProvider !== "string" ? await loadBusy(previewProvider, now) : [];

  await updateDb((db) => {
    const target = findTarget(db, input.slug, input.inviteToken);
    if (typeof target === "string") {
      outcome.error = target;
      return;
    }
    const provider = target;
    if (phone === provider.phone) {
      outcome.error = "Use a different mobile number than the trainer's.";
      return;
    }
    const service = provider.services.find((item) => item.id === input.serviceId);
    if (!service) {
      outcome.error = "Pick a session.";
      return;
    }
    const options = [...new Set(input.options.map(Number))].filter((n) => Number.isFinite(n)).sort((a, b) => a - b);
    if (options.length < 1 || options.length > 3) {
      outcome.error = "Pick one, two, or three times.";
      return;
    }
    const open = new Set(buildSlots({ provider, bookings: db.bookings, minutes: service.minutes, now, busy }).map(String));
    if (options.some((start) => !open.has(String(start)))) {
      outcome.error = "One of those times was just taken. Pick again.";
      return;
    }
    const securityError = checkSecurity(provider, input);
    if (securityError) {
      outcome.error = securityError;
      return;
    }
    const invite = input.inviteToken ? db.invites.find((item) => item.token === input.inviteToken) : null;
    const booking: Booking = {
      id: uid(),
      publicId: token(8),
      code: mintCode(db, provider.id),
      providerId: provider.id,
      inviteId: invite?.id || null,
      customerName: input.customerName.trim().slice(0, 40) || "Client",
      customerPhone: phone,
      serviceId: service.id,
      serviceName: service.name,
      minutes: service.minutes,
      price: service.price,
      options,
      chosenStart: null,
      link: input.link.trim().slice(0, 200),
      idImage: input.idImage || "",
      answers: provider.security.questions
        .map((q) => q.trim())
        .filter(Boolean)
        .map((q, i) => ({ q, a: (input.answers[i] || "").trim().slice(0, 280) })),
      status: "pending",
      changeNote: "",
      showConfirm: "none",
      confirmAskedAt: null,
      directionsToken: null,
      directionsUsedAt: null,
      codeFails: 0,
      codeLockedUntil: 0,
      createdAt: now,
      expiresAt: now + provider.requestExpiresHours * 3600000,
      sessionExpiresAt: now + provider.requestExpiresHours * 3600000,
    };
    db.bookings.push(booking);
    if (invite) invite.bookingId = booking.id;
    outcome.booking = booking;
    outbound.push(...notify(null, booking, provider));
  });
  if (!outcome.booking) return { error: outcome.error || "Could not send the request." };
  await sendAll(outbound);
  return { booking: outcome.booking };
}

export async function decide(
  bookingId: string,
  providerId: string,
  action: "yes" | "change" | "no" | "ask" | "directions" | "cancel",
  extra: { option?: number; note?: string } = {},
) {
  const outbound: Outbound[] = [];
  let error = "";
  await updateDb((db) => {
    const booking = db.bookings.find((item) => item.id === bookingId && item.providerId === providerId);
    const provider = db.providers.find((item) => item.id === providerId);
    if (!booking || !provider) {
      error = "That request isn't here.";
      return;
    }
    const before = { ...booking };
    const resendDirections = action === "directions" && Boolean(booking.directionsToken) && booking.directionsUsedAt == null;
    const result = applyDecision(booking, provider, action, extra, Date.now());
    if (!result.ok) {
      error = result.error;
      return;
    }
    Object.assign(booking, result.booking);
    if (resendDirections && booking.directionsToken) {
      const session = `${baseUrl()}/s/${booking.publicId}`;
      outbound.push(
        {
          to: booking.customerPhone,
          body: `${provider.name || "Your trainer"} sent directions. This link opens once:\n${session}/way/${booking.directionsToken}`,
          providerId: provider.id,
          bookingId: booking.id,
        },
        {
          to: provider.phone,
          body: `Directions link sent to ${booking.customerName}. It works once.`,
          providerId: provider.id,
          bookingId: booking.id,
        },
      );
    } else {
      outbound.push(...notify(before, booking, provider));
    }
  });
  if (error) return { error };
  await sendAll(outbound);
  return { ok: true as const };
}

export async function resubmitBooking(
  publicId: string,
  input: { options: number[]; link?: string; idImage?: string; answers?: string[] },
) {
  const now = Date.now();
  const preview = await readDb();
  const existing = preview.bookings.find((item) => item.publicId === publicId);
  const previewProvider = existing && preview.providers.find((item) => item.id === existing.providerId);
  const busy = previewProvider ? await loadBusy(previewProvider, now) : [];
  const outbound: Outbound[] = [];
  let error = "";
  await updateDb((db) => {
    const booking = db.bookings.find((item) => item.publicId === publicId);
    const provider = booking && db.providers.find((item) => item.id === booking.providerId);
    if (!booking || !provider) {
      error = "This link isn't valid.";
      return;
    }
    const service = provider.services.find((item) => item.id === booking.serviceId);
    if (!service) {
      error = "That session is no longer offered.";
      return;
    }
    const open = new Set(buildSlots({ provider, bookings: db.bookings.filter((item) => item.id !== booking.id), minutes: service.minutes, now, busy }).map(String));
    const options = [...new Set(input.options.map(Number))].filter((n) => Number.isFinite(n));
    if (options.some((start) => !open.has(String(start)))) {
      error = "One of those times was just taken. Pick again.";
      return;
    }
    if (provider.security.requireLink && input.link != null && !safeClientLink(input.link)) {
      error = "Add a full link, starting with https://";
      return;
    }
    if (input.idImage && !isImage(input.idImage, 500_000)) {
      error = "Use a smaller ID photo.";
      return;
    }
    const before = { ...booking };
    const next = resubmit(booking, options, provider, now);
    if (!next.ok) {
      error = next.error;
      return;
    }
    Object.assign(booking, next.booking);
    if (input.link) booking.link = input.link.trim().slice(0, 200);
    if (input.idImage) booking.idImage = input.idImage;
    if (input.answers) {
      booking.answers = provider.security.questions
        .map((q) => q.trim())
        .filter(Boolean)
        .map((q, i) => ({ q, a: (input.answers?.[i] || booking.answers[i]?.a || "").trim().slice(0, 280) }));
    }
    outbound.push(...notify(before, booking, provider));
  });
  if (error) return { error };
  await sendAll(outbound);
  return { ok: true as const };
}

export async function showAnswer(publicId: string, coming: boolean) {
  const outbound: Outbound[] = [];
  let error = "";
  await updateDb((db) => {
    const booking = db.bookings.find((item) => item.publicId === publicId);
    const provider = booking && db.providers.find((item) => item.id === booking.providerId);
    if (!booking || !provider) {
      error = "This link isn't valid.";
      return;
    }
    const before = { ...booking };
    const next = answerShow(booking, coming, Date.now());
    if (!next.ok) {
      error = next.error;
      return;
    }
    Object.assign(booking, next.booking);
    outbound.push(...notify(before, booking, provider));
  });
  if (error) return { error };
  await sendAll(outbound);
  return { ok: true as const };
}

export async function unlockSession(publicId: string, code: string) {
  let error = "";
  let ok = false;
  await updateDb((db) => {
    const booking = db.bookings.find((item) => item.publicId === publicId);
    if (!booking) {
      error = "This link isn't valid.";
      return;
    }
    const checked = checkCode(booking, code, Date.now());
    Object.assign(booking, checked.booking);
    if (!checked.ok) error = checked.error;
    else ok = true;
  });
  return ok ? { ok: true as const } : { error };
}

export async function revealDirections(publicId: string, wayToken: string) {
  let error = "";
  let address = "";
  let place = "";
  await updateDb((db) => {
    const booking = db.bookings.find((item) => item.publicId === publicId);
    const provider = booking && db.providers.find((item) => item.id === booking.providerId);
    if (!booking || !provider) {
      error = "This link isn't valid.";
      return;
    }
    const opened = openDirections(booking, wayToken, Date.now());
    if (!opened.ok) {
      error = opened.error;
      return;
    }
    Object.assign(booking, opened.booking);
    address = provider.address;
    place = provider.placeLabel;
  });
  return error ? { error } : { address, place };
}

export async function applyInbound(fromRaw: string, body: string) {
  const from = normalizePhone(fromRaw) || fromRaw.trim();
  const reply = parseReply(body);
  const outbound: Outbound[] = [];
  let help = "";
  await updateDb((db) => {
    const provider = db.providers.find((item) => item.phone === from);
    const now = Date.now();
    if (provider) {
      db.messages.push({
        id: uid(),
        providerId: provider.id,
        bookingId: null,
        to: "PrivateLink",
        from,
        body: body.slice(0, 500),
        mediaUrl: "",
        direction: "in",
        delivered: true,
        createdAt: now,
      });
      const booking = findForProvider(db.bookings, provider.id, reply.action === "unknown" ? null : codeOf(reply));
      if (!booking && reply.action !== "unknown") {
        help = "No open request matched that code.";
        return;
      }
      if (!booking || reply.action === "unknown") {
        help = "Reply 1, 2, or 3 to accept a time, CHANGE and the code, or NO and the code.";
        return;
      }
      const before = { ...booking };
      const result =
        reply.action === "yes"
          ? acceptOption(booking, (reply.option ?? 1) - 1, now)
          : reply.action === "change"
            ? requestChange(booking, reply.note, provider, now)
            : reply.action === "no"
              ? booking.status === "confirmed"
                ? cancelBooking(booking, now)
                : declineRequest(booking, now)
              : reply.action === "here"
                ? { ok: false as const, error: "That's a reply for the client." }
                : { ok: false as const, error: "Reply YES, CHANGE, or NO, plus the 4-digit code." };
      if (!result.ok) {
        help = result.error;
        return;
      }
      Object.assign(booking, result.booking);
      outbound.push(...notify(before, booking, provider));
      return;
    }

    const code = codeOf(reply);
    const booking = db.bookings.find((item) => item.customerPhone === from && (!code || item.code === code) && item.sessionExpiresAt > now);
    const owner = booking && db.providers.find((item) => item.id === booking.providerId);
    if (booking && owner) {
      db.messages.push({
        id: uid(),
        providerId: owner.id,
        bookingId: booking.id,
        to: "PrivateLink",
        from,
        body: body.slice(0, 500),
        mediaUrl: "",
        direction: "in",
        delivered: true,
        createdAt: now,
      });
    }
    if (!booking || !owner) {
      help = "Open the session link in your text and use the 4-digit code.";
      return;
    }
    if (reply.action !== "here" && reply.action !== "no") {
      help = `Reply HERE ${booking.code} if you're coming, or NO ${booking.code} if you're not.`;
      return;
    }
    const before = { ...booking };
    const result = answerShow(booking, reply.action === "here", now);
    if (!result.ok) {
      help = result.error;
      return;
    }
    Object.assign(booking, result.booking);
    outbound.push(...notify(before, booking, owner));
  });
  await sendAll(outbound);
  if (help) {
    const db = await readDb();
    const provider = db.providers.find((item) => item.phone === from);
    await deliver({
      to: from,
      body: help,
      providerId: provider?.id || "",
      bookingId: null,
    });
  }
  return { ok: true as const };
}

export async function runReminders(now = Date.now()) {
  const outbound: Outbound[] = [];
  let count = 0;
  await updateDb((db) => {
    for (const booking of db.bookings) {
      const provider = db.providers.find((item) => item.id === booking.providerId);
      if (!provider) continue;
      const action = reminderAction(booking, provider, now);
      if (!action) continue;
      const before = { ...booking };
      const result =
        action === "expire" ? expireBooking(booking, now) : action === "ask" ? askToConfirm(booking, now) : cancelBooking(booking, now);
      if (!result.ok) continue;
      Object.assign(booking, result.booking);
      outbound.push(...notify(before, booking, provider));
      count += 1;
    }
  });
  await sendAll(outbound);
  return count;
}

function applyDecision(
  booking: Booking,
  provider: Provider,
  action: "yes" | "change" | "no" | "ask" | "directions" | "cancel",
  extra: { option?: number; note?: string },
  now: number,
) {
  if (action === "yes") return acceptOption(booking, extra.option ?? 0, now);
  if (action === "change") return requestChange(booking, extra.note || "", provider, now);
  if (action === "no") return booking.status === "confirmed" ? cancelBooking(booking, now) : declineRequest(booking, now);
  if (action === "cancel") return cancelBooking(booking, now);
  if (action === "ask") return askToConfirm(booking, now);
  return releaseDirections(booking, now, token(16));
}

function findTarget(db: Awaited<ReturnType<typeof readDb>>, slug?: string, inviteToken?: string) {
  if (inviteToken) {
    const invite = db.invites.find((item) => item.token === inviteToken);
    if (!invite) return "This link isn't valid.";
    if (invite.bookingId || invite.expiresAt <= Date.now()) return "This one-time link is finished.";
    const provider = db.providers.find((item) => item.id === invite.providerId);
    if (!provider || !provider.address || !provider.services.length) return "This trainer isn't taking sessions yet.";
    return provider;
  }
  const provider = db.providers.find((item) => item.slug === slug);
  if (!provider || !provider.name) return "This page isn't taking bookings.";
  if (!provider.address || !provider.services.length || !provider.hours.length) return "This trainer isn't taking sessions yet.";
  return provider;
}

function checkSecurity(provider: Provider, input: BookingInput) {
  if (provider.security.requireLink && !safeClientLink(input.link)) return "Add a full link, starting with https://";
  if (provider.security.requireId && !isImage(input.idImage, 500_000)) return "Add a photo of your ID.";
  const questions = provider.security.questions.map((q) => q.trim()).filter(Boolean);
  for (let i = 0; i < questions.length; i++) {
    if (!(input.answers[i] || "").trim()) return "Answer the questions before sending.";
  }
  return "";
}

function safeClientLink(value: string) {
  try {
    const url = new URL(value.trim());
    return url.protocol === "https:" || url.protocol === "http:";
  } catch {
    return false;
  }
}

function isImage(value: string, max: number) {
  return /^data:image\/(jpeg|png);base64,/.test(value) && value.length <= max;
}

function mintCode(db: Awaited<ReturnType<typeof readDb>>, providerId: string) {
  const used = new Set(
    db.bookings
      .filter((booking) => booking.providerId === providerId && ["pending", "changes", "confirmed"].includes(booking.status))
      .map((booking) => booking.code),
  );
  for (let i = 0; i < 20; i++) {
    const code = code4();
    if (!used.has(code)) return code;
  }
  return code4();
}

function findForProvider(bookings: Booking[], providerId: string, code: string | null) {
  const open = bookings.filter(
    (booking) => booking.providerId === providerId && ["pending", "changes", "confirmed"].includes(booking.status),
  );
  if (code) return open.find((booking) => booking.code === code) || null;
  return open.length === 1 ? open[0] : null;
}

function codeOf(reply: ReturnType<typeof parseReply>) {
  if (reply.action === "unknown") return null;
  return reply.code;
}

function notify(before: Booking | null, after: Booking, provider: Provider): Outbound[] {
  const session = `${baseUrl()}/s/${after.publicId}`;
  const who = provider.name || "Your trainer";
  const name = after.customerName || "A client";
  const when = after.chosenStart ? formatWhen(after.chosenStart, provider.timezone) : "";
  const pack = (to: string, body: string, mediaUrl?: string): Outbound => ({
    to,
    body,
    mediaUrl,
    providerId: provider.id,
    bookingId: after.id,
  });

  if (!before) {
    return [
      pack(provider.phone, trainerRequest(after, provider), `${baseUrl()}/api/snapshot/${after.id}`),
      pack(
        after.customerPhone,
        `Request sent to ${who}.\nCode ${after.code}\n${session}\nIf there's no answer by ${formatWhen(after.expiresAt, provider.timezone)}, it closes.`,
      ),
    ];
  }
  if (after.status === "confirmed" && before.status !== "confirmed") {
    return [
      pack(after.customerPhone, `${who} confirmed ${after.serviceName}\n${when}\n${session}\nCode ${after.code}`),
      pack(provider.phone, `You accepted ${name} for ${when}. They have the session link.`),
    ];
  }
  if (after.status === "changes" && before.status !== "changes") {
    return [
      pack(after.customerPhone, `${who} asked for a change: ${after.changeNote}\nSend new times here:\n${session}\nCode ${after.code}`),
      pack(provider.phone, `Change request sent to ${name}.`),
    ];
  }
  if (after.status === "pending" && before.status === "changes") {
    return [
      pack(provider.phone, trainerRequest(after, provider), `${baseUrl()}/api/snapshot/${after.id}`),
      pack(after.customerPhone, `New times sent to ${who}.\nCode ${after.code}\n${session}`),
    ];
  }
  if (after.status === "declined" && before.status !== "declined") {
    return [
      pack(after.customerPhone, `${who} can't take this request. No time was held.`),
      pack(provider.phone, `You declined ${name}.`),
    ];
  }
  if (after.status === "canceled" && before.status !== "canceled") {
    return [
      pack(after.customerPhone, `The session with ${who} was canceled.`),
      pack(provider.phone, `The session with ${name} was canceled.`),
    ];
  }
  if (after.status === "expired" && before.status !== "expired") {
    return [
      pack(after.customerPhone, `The request to ${who} expired. Nothing was held.`),
      pack(provider.phone, `${name}'s request expired without an answer.`),
    ];
  }
  if (after.showConfirm === "asked" && before.showConfirm !== "asked") {
    return [pack(after.customerPhone, `Are you coming to ${who} at ${when}?\nReply HERE ${after.code} or NO ${after.code}\n${session}`)];
  }
  if (after.showConfirm === "yes" && before.showConfirm !== "yes") {
    return [
      pack(provider.phone, `${name} is coming at ${when}. You can send directions.`),
      pack(after.customerPhone, `You're confirmed for ${who} at ${when}.`),
    ];
  }
  if (after.directionsToken && after.directionsToken !== before.directionsToken) {
    return [
      pack(after.customerPhone, `${who} sent directions. This link opens once:\n${session}/way/${after.directionsToken}`),
      pack(provider.phone, `Directions link sent to ${name}. It works once.`),
    ];
  }
  return [];
}

function trainerRequest(booking: Booking, provider: Provider) {
  const lines = booking.options.map((start, i) => `${i + 1}) ${formatWhen(start, provider.timezone)}`);
  return [
    `PrivateLink ${booking.code}`,
    `${booking.customerName} · ${booking.serviceName} · ${booking.minutes} min`,
    ...lines,
    `Reply 1, 2, or 3 to accept, CHANGE ${booking.code}, or NO ${booking.code}`,
  ].join("\n");
}

async function sendAll(outbound: Outbound[]) {
  for (const text of outbound) await deliver(text);
}

function clamp(n: number, min: number, max: number) {
  return Math.min(max, Math.max(min, Math.round(Number(n) || min)));
}
