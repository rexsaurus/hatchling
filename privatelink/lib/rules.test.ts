import assert from "node:assert/strict";
import test from "node:test";
import { parseReply } from "./reply.ts";
import {
  acceptOption,
  answerShow,
  askToConfirm,
  checkCode,
  declineRequest,
  openDirections,
  releaseDirections,
  reminderAction,
  requestChange,
  resubmit,
} from "./rules.ts";
import type { Booking, Provider } from "./types.ts";

const NOW = Date.parse("2026-10-06T18:00:00.000Z");

function provider(): Provider {
  return {
    id: "p1",
    phone: "+15551212",
    name: "Jordan",
    slug: "jordan",
    photo: "",
    line: "",
    timezone: "America/Los_Angeles",
    address: "12 Oak",
    placeLabel: "Home studio",
    services: [],
    hours: [],
    bufferMinutes: 15,
    horizonDays: 14,
    minNoticeMinutes: 120,
    busyIcsUrl: "",
    security: { requireLink: false, linkLabel: "", requireId: false, questions: [] },
    confirmLeadMinutes: 30,
    autoCancelIfUnconfirmed: true,
    requestExpiresHours: 12,
    calToken: "cal",
    createdAt: NOW,
  };
}

function booking(overrides: Partial<Booking> = {}): Booking {
  return {
    id: "b1",
    publicId: "abcd2345",
    code: "4821",
    providerId: "p1",
    inviteId: null,
    customerName: "Sam",
    customerPhone: "+15557654",
    serviceId: "s1",
    serviceName: "Strength",
    minutes: 60,
    price: "80",
    options: [NOW + 86400000, NOW + 2 * 86400000],
    chosenStart: null,
    link: "",
    idImage: "",
    answers: [],
    status: "pending",
    changeNote: "",
    showConfirm: "none",
    confirmAskedAt: null,
    directionsToken: null,
    directionsUsedAt: null,
    codeFails: 0,
    codeLockedUntil: 0,
    createdAt: NOW,
    expiresAt: NOW + 12 * 3600000,
    sessionExpiresAt: NOW + 12 * 3600000,
    ...overrides,
  };
}

test("parses trainer and client texts", () => {
  assert.deepEqual(parseReply("2 4821"), { action: "yes", option: 2, code: "4821" });
  assert.deepEqual(parseReply("YES 4821"), { action: "yes", option: null, code: "4821" });
  assert.deepEqual(parseReply("CHANGE 4821 mornings only"), { action: "change", code: "4821", note: "mornings only" });
  assert.deepEqual(parseReply("NO 4821"), { action: "no", code: "4821" });
  assert.deepEqual(parseReply("HERE 4821"), { action: "here", code: "4821" });
  assert.equal(parseReply("hello").action, "unknown");
});

test("accepts one offered time and releases the rest", () => {
  const accepted = acceptOption(booking(), 1, NOW);
  assert.equal(accepted.ok, true);
  if (!accepted.ok) return;
  assert.equal(accepted.booking.status, "confirmed");
  assert.equal(accepted.booking.chosenStart, NOW + 2 * 86400000);
  assert.deepEqual(accepted.booking.options, [NOW + 2 * 86400000]);
  const again = acceptOption(accepted.booking, 0, NOW);
  assert.equal(again.ok, false);
});

test("a change lets the client send new times, and a wrong code locks", () => {
  const changed = requestChange(booking(), "  Mornings. ", provider(), NOW);
  assert.equal(changed.ok, true);
  if (!changed.ok) return;
  assert.equal(changed.booking.status, "changes");
  assert.equal(changed.booking.changeNote, "Mornings.");
  const sent = resubmit(changed.booking, [NOW + 3 * 86400000], provider(), NOW);
  assert.equal(sent.ok, true);
  if (!sent.ok) return;
  assert.equal(sent.booking.status, "pending");

  let current = booking();
  for (let i = 0; i < 5; i++) {
    const tried = checkCode(current, "0000", NOW);
    assert.equal(tried.ok, false);
    current = tried.booking;
  }
  assert.ok(current.codeLockedUntil > NOW);
  const locked = checkCode(current, "4821", NOW);
  assert.equal(locked.ok, false);
});

test("directions open once, and only after they confirm", () => {
  const accepted = acceptOption(booking(), 0, NOW);
  assert.equal(accepted.ok, true);
  if (!accepted.ok) return;
  const early = releaseDirections(accepted.booking, NOW, "toktoktoktoktok1");
  assert.equal(early.ok, false);
  const asked = askToConfirm(accepted.booking, NOW);
  assert.equal(asked.ok, true);
  if (!asked.ok) return;
  const coming = answerShow(asked.booking, true, NOW);
  assert.equal(coming.ok, true);
  if (!coming.ok) return;
  const released = releaseDirections(coming.booking, NOW, "toktoktoktoktok1");
  assert.equal(released.ok, true);
  if (!released.ok) return;
  const opened = openDirections(released.booking, "toktoktoktoktok1", NOW);
  assert.equal(opened.ok, true);
  if (!opened.ok) return;
  const second = openDirections(opened.booking, "toktoktoktoktok1", NOW + 1);
  assert.equal(second.ok, false);
});

test("reminders ask 30 minutes out and auto-cancel at the start", () => {
  const start = NOW + 20 * 60000;
  const confirmed = booking({
    status: "confirmed",
    chosenStart: start,
    options: [start],
    sessionExpiresAt: start + 3600000,
  });
  assert.equal(reminderAction(confirmed, provider(), start - 30 * 60000), "ask");
  const asked = { ...confirmed, showConfirm: "asked" as const };
  assert.equal(reminderAction(asked, provider(), start), "autocancel");
  const pending = booking({ expiresAt: NOW - 1 });
  assert.equal(reminderAction(pending, provider(), NOW), "expire");
  const declined = declineRequest(booking(), NOW);
  assert.equal(declined.ok, true);
});
