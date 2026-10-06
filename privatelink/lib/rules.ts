import type { Booking, Provider } from "./types";

const HOUR = 3600000;

export function freshExpiry(provider: Provider, now: number) {
  return now + provider.requestExpiresHours * HOUR;
}

export function acceptOption(booking: Booking, optionIndex: number, now: number) {
  if (booking.status !== "pending") return fail("This request isn't waiting on an answer.");
  if (now >= booking.expiresAt) return fail("This request expired.");
  const start = booking.options[optionIndex];
  if (start == null) return fail("That time isn't one of the options.");
  const next: Booking = {
    ...booking,
    status: "confirmed",
    chosenStart: start,
    options: [start],
    changeNote: "",
    showConfirm: "none",
    confirmAskedAt: null,
    sessionExpiresAt: start + booking.minutes * 60000 + 6 * HOUR,
  };
  return { ok: true as const, booking: next };
}

export function requestChange(booking: Booking, note: string, provider: Provider, now: number) {
  if (booking.status !== "pending" && booking.status !== "confirmed") {
    return fail("This request can't be changed now.");
  }
  const next: Booking = {
    ...booking,
    status: "changes",
    chosenStart: null,
    changeNote: note.trim().slice(0, 280) || "Please send another time.",
    showConfirm: "none",
    confirmAskedAt: null,
    directionsToken: null,
    directionsUsedAt: null,
    expiresAt: freshExpiry(provider, now),
    sessionExpiresAt: freshExpiry(provider, now),
  };
  return { ok: true as const, booking: next };
}

export function declineRequest(booking: Booking, now: number) {
  if (booking.status !== "pending" && booking.status !== "changes") {
    return fail("This request is already settled.");
  }
  return terminal(booking, "declined", now);
}

export function cancelBooking(booking: Booking, now: number) {
  if (booking.status === "declined" || booking.status === "canceled" || booking.status === "expired") {
    return fail("This session is already closed.");
  }
  return terminal(booking, "canceled", now);
}

export function resubmit(booking: Booking, options: number[], provider: Provider, now: number) {
  if (booking.status !== "changes") return fail("This session isn't waiting on a new time.");
  if (now >= booking.expiresAt) return fail("This request expired.");
  if (options.length < 1 || options.length > 3) return fail("Pick one, two, or three times.");
  const next: Booking = {
    ...booking,
    status: "pending",
    options: [...options].sort((a, b) => a - b),
    chosenStart: null,
    changeNote: "",
    expiresAt: freshExpiry(provider, now),
    sessionExpiresAt: freshExpiry(provider, now),
  };
  return { ok: true as const, booking: next };
}

export function askToConfirm(booking: Booking, now: number) {
  if (booking.status !== "confirmed" || booking.chosenStart == null) {
    return fail("Confirm the session before asking them to show up.");
  }
  if (booking.showConfirm === "yes") return fail("They already confirmed.");
  const next: Booking = { ...booking, showConfirm: "asked" as const, confirmAskedAt: now };
  return { ok: true as const, booking: next };
}

export function answerShow(booking: Booking, coming: boolean, now: number) {
  if (booking.status !== "confirmed") return fail("This session isn't confirmed.");
  if (booking.showConfirm === "yes" && coming) return { ok: true as const, booking };
  const next: Booking = { ...booking, showConfirm: coming ? "yes" as const : "no" as const };
  if (!coming) return terminal(next, "canceled", now);
  return { ok: true as const, booking: next };
}

export function releaseDirections(booking: Booking, now: number, nextToken: string) {
  if (booking.status !== "confirmed" || booking.showConfirm !== "yes") {
    return fail("They need to confirm they're coming before directions go out.");
  }
  if (booking.directionsToken && booking.directionsUsedAt == null) {
    return { ok: true as const, booking };
  }
  const next: Booking = {
    ...booking,
    directionsToken: nextToken,
    directionsUsedAt: null,
  };
  void now;
  return { ok: true as const, booking: next };
}

export function openDirections(booking: Booking, token: string, now: number) {
  if (!booking.directionsToken || booking.directionsToken !== token) return fail("This directions link isn't valid.");
  if (booking.directionsUsedAt != null) return fail("This directions link was already opened.");
  if (booking.status !== "confirmed") return fail("This session is closed.");
  const next: Booking = { ...booking, directionsUsedAt: now };
  return { ok: true as const, booking: next };
}

export function checkCode(booking: Booking, code: string, now: number) {
  if (booking.codeLockedUntil > now) {
    return { ok: false as const, booking, error: "Too many tries. Wait a few minutes and use the code from the text." };
  }
  if (code.trim() !== booking.code) {
    const fails = booking.codeFails + 1;
    const next: Booking = {
      ...booking,
      codeFails: fails,
      codeLockedUntil: fails >= 5 ? now + 15 * 60000 : booking.codeLockedUntil,
    };
    return { ok: false as const, booking: next, error: "That code doesn't match." };
  }
  return { ok: true as const, booking: { ...booking, codeFails: 0, codeLockedUntil: 0 } };
}

export function reminderAction(booking: Booking, provider: Provider, now: number) {
  if ((booking.status === "pending" || booking.status === "changes") && now >= booking.expiresAt) return "expire" as const;
  if (booking.status !== "confirmed" || booking.chosenStart == null) return null;
  const start = booking.chosenStart;
  const lead = provider.confirmLeadMinutes * 60000;
  if (booking.showConfirm === "none" && now >= start - lead && now < start) return "ask" as const;
  if (provider.autoCancelIfUnconfirmed && (booking.showConfirm === "asked" || booking.showConfirm === "no") && now >= start) {
    return "autocancel" as const;
  }
  return null;
}

export function expireBooking(booking: Booking, now: number) {
  if (booking.status !== "pending" && booking.status !== "changes") return fail("This request isn't open.");
  return terminal(booking, "expired", now);
}

function terminal(booking: Booking, status: "declined" | "canceled" | "expired", now: number) {
  const next: Booking = {
    ...booking,
    status,
    chosenStart: status === "canceled" ? booking.chosenStart : null,
    directionsToken: null,
    sessionExpiresAt: now + 12 * HOUR,
  };
  return { ok: true as const, booking: next };
}

function fail(error: string) {
  return { ok: false as const, error };
}
