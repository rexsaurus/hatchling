import { zonedToUtc } from "./time";

export type Busy = { start: number; end: number };

function unfold(ics: string) {
  return ics.replace(/\r\n/g, "\n").replace(/\n[ \t]/g, "");
}

function parseStamp(value: string, params: string, fallbackZone: string) {
  const dateOnly = params.includes("VALUE=DATE") || /^\d{8}$/.test(value);
  if (dateOnly) {
    const y = Number(value.slice(0, 4));
    const mo = Number(value.slice(4, 6));
    const d = Number(value.slice(6, 8));
    const zone = /TZID=([^;:]+)/.exec(params)?.[1] || fallbackZone;
    return { start: zonedToUtc(y, mo, d, 0, 0, zone), allDay: true, zone };
  }
  const match = /^(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})?(Z?)$/.exec(value);
  if (!match) return null;
  const y = Number(match[1]);
  const mo = Number(match[2]);
  const d = Number(match[3]);
  const h = Number(match[4]);
  const mi = Number(match[5]);
  if (match[7] === "Z") return { start: Date.UTC(y, mo - 1, d, h, mi), allDay: false, zone: "UTC" };
  const zone = /TZID=([^;:]+)/.exec(params)?.[1] || fallbackZone;
  return { start: zonedToUtc(y, mo, d, h, mi, zone), allDay: false, zone };
}

export function parseBusy(ics: string, fallbackZone: string, rangeStart: number, rangeEnd: number): Busy[] {
  const text = unfold(ics);
  const blocks = text.split("BEGIN:VEVENT").slice(1);
  const busy: Busy[] = [];
  for (const block of blocks) {
    const body = block.split("END:VEVENT")[0];
    const startLine = body.split("\n").find((line) => line.startsWith("DTSTART"));
    const endLine = body.split("\n").find((line) => line.startsWith("DTEND"));
    if (!startLine) continue;
    const [startHead, startVal] = splitProp(startLine);
    const start = parseStamp(startVal, startHead, fallbackZone);
    if (!start) continue;
    let endMs: number;
    if (endLine) {
      const [endHead, endVal] = splitProp(endLine);
      const end = parseStamp(endVal, endHead, start.zone);
      endMs = end ? end.start : start.start + (start.allDay ? 86400000 : 3600000);
    } else {
      endMs = start.start + (start.allDay ? 86400000 : 3600000);
    }
    if (endMs <= rangeStart || start.start >= rangeEnd) continue;
    if (/TRANSP:TRANSPARENT/.test(body)) continue;
    if (/STATUS:CANCELLED/.test(body)) continue;
    busy.push({ start: start.start, end: endMs });
  }
  return busy;
}

function splitProp(line: string): [string, string] {
  const idx = line.indexOf(":");
  if (idx < 0) return [line, ""];
  return [line.slice(0, idx), line.slice(idx + 1).trim()];
}

export function toIcs(events: { start: number; end: number; title: string; uid: string }[]) {
  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//PrivateLink//EN",
    "CALSCALE:GREGORIAN",
  ];
  for (const event of events) {
    lines.push(
      "BEGIN:VEVENT",
      `UID:${event.uid}@privatelink`,
      `DTSTAMP:${stamp(Date.now())}`,
      `DTSTART:${stamp(event.start)}`,
      `DTEND:${stamp(event.end)}`,
      `SUMMARY:${event.title.replace(/[\n,;]/g, " ")}`,
      "END:VEVENT",
    );
  }
  lines.push("END:VCALENDAR");
  return lines.join("\r\n");
}

function stamp(ms: number) {
  const d = new Date(ms);
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getUTCFullYear()}${p(d.getUTCMonth() + 1)}${p(d.getUTCDate())}T${p(d.getUTCHours())}${p(d.getUTCMinutes())}${p(d.getUTCSeconds())}Z`;
}
