import assert from "node:assert/strict";
import test from "node:test";
import { parseBusy } from "./ics.ts";
import { buildSlots } from "./slots.ts";
import { zonedToUtc } from "./time.ts";
import type { Booking, Provider } from "./types.ts";

const ZONE = "America/Los_Angeles";
const NOW = Date.parse("2026-10-05T15:00:00.000Z");

function provider(): Provider {
  return {
    id: "p1",
    phone: "+15551212",
    name: "Jordan",
    slug: "jordan",
    photo: "",
    line: "",
    timezone: ZONE,
    address: "",
    placeLabel: "",
    services: [],
    hours: [{ dow: 1, start: "09:00", end: "11:00" }],
    bufferMinutes: 0,
    horizonDays: 2,
    minNoticeMinutes: 0,
    busyIcsUrl: "",
    security: { requireLink: false, linkLabel: "", requireId: false, questions: [] },
    confirmLeadMinutes: 30,
    autoCancelIfUnconfirmed: true,
    requestExpiresHours: 12,
    calToken: "cal",
    createdAt: NOW,
  };
}

test("turns a trainer's hours into open starts", () => {
  assert.equal(zonedToUtc(2026, 10, 5, 9, 0, ZONE), Date.parse("2026-10-05T16:00:00.000Z"));
  assert.equal(zonedToUtc(2026, 1, 5, 9, 0, ZONE), Date.parse("2026-01-05T17:00:00.000Z"));
  const slots = buildSlots({ provider: provider(), bookings: [], minutes: 60, now: NOW });
  assert.deepEqual(slots, [
    Date.parse("2026-10-05T16:00:00.000Z"),
    Date.parse("2026-10-05T16:30:00.000Z"),
    Date.parse("2026-10-05T17:00:00.000Z"),
  ]);
});

test("a held request and a busy calendar block those times", () => {
  const held: Booking = {
    id: "b",
    publicId: "abcd2345",
    code: "1111",
    providerId: "p1",
    inviteId: null,
    customerName: "Sam",
    customerPhone: "+15550000",
    serviceId: "s",
    serviceName: "Strength",
    minutes: 60,
    price: "",
    options: [Date.parse("2026-10-05T16:00:00.000Z")],
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
    expiresAt: NOW + 3600000,
    sessionExpiresAt: NOW + 3600000,
  };
  const slots = buildSlots({ provider: provider(), bookings: [held], minutes: 60, now: NOW });
  assert.deepEqual(slots, [Date.parse("2026-10-05T17:00:00.000Z")]);

  const ics = [
    "BEGIN:VCALENDAR",
    "BEGIN:VEVENT",
    "DTSTART:20261005T170000Z",
    "DTEND:20261005T180000Z",
    "END:VEVENT",
    "END:VCALENDAR",
  ].join("\n");
  const busy = parseBusy(ics, ZONE, NOW, NOW + 3 * 86400000);
  const withBusy = buildSlots({ provider: provider(), bookings: [], minutes: 60, now: NOW, busy });
  assert.equal(withBusy.includes(Date.parse("2026-10-05T17:00:00.000Z")), false);
  assert.equal(withBusy.includes(Date.parse("2026-10-05T16:00:00.000Z")), true);
});
