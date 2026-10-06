import { parseBusy, type Busy } from "./ics";
import { addCalendarDays, calendarDow, parseHm, zoneParts, zonedToUtc } from "./time";
import type { Booking, Provider } from "./types";

const STEP = 30;

export function heldRanges(bookings: Booking[], bufferMinutes: number): Busy[] {
  const ranges: Busy[] = [];
  for (const booking of bookings) {
    const pad = (booking.minutes + bufferMinutes) * 60000;
    if (booking.status === "pending") {
      for (const start of booking.options) ranges.push({ start, end: start + pad });
    } else if (booking.status === "confirmed" && booking.chosenStart != null) {
      ranges.push({ start: booking.chosenStart, end: booking.chosenStart + pad });
    }
  }
  return ranges;
}

export function buildSlots(input: {
  provider: Provider;
  bookings: Booking[];
  minutes: number;
  now: number;
  busy?: Busy[];
  limit?: number;
}) {
  const { provider, minutes, now } = input;
  const today = zoneParts(now, provider.timezone);
  const horizon = Math.min(Math.max(provider.horizonDays, 1), 60);
  const notice = provider.minNoticeMinutes * 60000;
  const held = heldRanges(
    input.bookings.filter((booking) => booking.providerId === provider.id),
    provider.bufferMinutes,
  );
  const busy = input.busy || [];
  const slots: number[] = [];

  for (let day = 0; day < horizon; day++) {
    const date = addCalendarDays(today.y, today.mo, today.d, day);
    const dow = calendarDow(date.y, date.mo, date.d);
    for (const window of provider.hours) {
      if (window.dow !== dow) continue;
      const startMin = parseHm(window.start);
      const endMin = parseHm(window.end);
      if (startMin == null || endMin == null || endMin <= startMin) continue;
      for (let cursor = startMin; cursor + minutes <= endMin; cursor += STEP) {
        const start = zonedToUtc(date.y, date.mo, date.d, Math.floor(cursor / 60), cursor % 60, provider.timezone);
        const end = start + minutes * 60000;
        if (start < now + notice) continue;
        const blocked = [...held, ...busy].some((range) => start < range.end && range.start < end);
        if (blocked) continue;
        slots.push(start);
        if (slots.length >= (input.limit ?? 48)) return slots;
      }
    }
  }
  return slots;
}

export function busyFromIcs(ics: string, provider: Provider, now: number) {
  const end = now + provider.horizonDays * 86400000;
  return parseBusy(ics, provider.timezone, now, end);
}
