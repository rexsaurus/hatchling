export type YMD = { y: number; mo: number; d: number };

const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

export function zoneParts(ms: number, timeZone: string) {
  const fmt = new Intl.DateTimeFormat("en-US", {
    timeZone,
    hourCycle: "h23",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    weekday: "short",
  });
  const map: Record<string, string> = {};
  for (const part of fmt.formatToParts(new Date(ms))) {
    if (part.type !== "literal") map[part.type] = part.value;
  }
  const hour = map.hour === "24" ? 0 : Number(map.hour);
  return {
    y: Number(map.year),
    mo: Number(map.month),
    d: Number(map.day),
    h: hour,
    mi: Number(map.minute),
    dow: WEEKDAYS.indexOf(map.weekday),
  };
}

function tzOffset(utcMs: number, timeZone: string) {
  const parts = zoneParts(utcMs, timeZone);
  const asUtc = Date.UTC(parts.y, parts.mo - 1, parts.d, parts.h, parts.mi);
  return asUtc - utcMs;
}

export function zonedToUtc(y: number, mo: number, d: number, h: number, mi: number, timeZone: string) {
  let utc = Date.UTC(y, mo - 1, d, h, mi);
  for (let i = 0; i < 2; i++) utc = Date.UTC(y, mo - 1, d, h, mi) - tzOffset(utc, timeZone);
  return utc;
}

export function addCalendarDays(y: number, mo: number, d: number, days: number): YMD {
  const dt = new Date(Date.UTC(y, mo - 1, d + days));
  return { y: dt.getUTCFullYear(), mo: dt.getUTCMonth() + 1, d: dt.getUTCDate() };
}

export function calendarDow(y: number, mo: number, d: number) {
  return new Date(Date.UTC(y, mo - 1, d)).getUTCDay();
}

export function formatWhen(ms: number, timeZone: string) {
  return new Intl.DateTimeFormat("en-US", {
    timeZone,
    weekday: "short",
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(ms));
}

export function formatDay(y: number, mo: number, d: number, timeZone: string) {
  const noon = zonedToUtc(y, mo, d, 12, 0, timeZone);
  return new Intl.DateTimeFormat("en-US", {
    timeZone,
    weekday: "short",
    month: "short",
    day: "numeric",
  }).format(new Date(noon));
}

export function formatTime(ms: number, timeZone: string) {
  return new Intl.DateTimeFormat("en-US", {
    timeZone,
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(ms));
}

export function parseHm(value: string) {
  const match = /^(\d{2}):(\d{2})$/.exec(value);
  if (!match) return null;
  const h = Number(match[1]);
  const mi = Number(match[2]);
  if (h > 23 || mi > 59) return null;
  return h * 60 + mi;
}

export const TIMEZONES = [
  "America/Los_Angeles",
  "America/Denver",
  "America/Phoenix",
  "America/Chicago",
  "America/New_York",
  "America/Anchorage",
  "Pacific/Honolulu",
];
