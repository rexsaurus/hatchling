import type { Provider, Service, WeekWindow } from "./types";

const RESERVED = new Set(["app", "start", "api", "t", "i", "s", "dev", "onboarding", "login", "signup", "privatelink"]);

export function slugify(name: string) {
  const slug = name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 24);
  if (slug.length < 3 || RESERVED.has(slug)) return "";
  return slug;
}

export function isBookable(provider: Provider) {
  return Boolean(
    provider.name.trim() &&
      provider.slug &&
      provider.address.trim() &&
      provider.services.length > 0 &&
      provider.hours.length > 0,
  );
}

export function publicTrainer(provider: Provider) {
  return {
    name: provider.name,
    slug: provider.slug,
    photo: provider.photo,
    line: provider.line,
    placeLabel: provider.placeLabel,
    timezone: provider.timezone,
    services: provider.services.map((service) => ({
      id: service.id,
      name: service.name,
      minutes: service.minutes,
      price: service.price,
      detail: service.detail,
    })),
    security: {
      requireLink: provider.security.requireLink,
      linkLabel: provider.security.linkLabel || "A link that shows it's you",
      requireId: provider.security.requireId,
      questions: provider.security.questions.map((q) => q.trim()).filter(Boolean),
    },
  };
}

export function cleanServices(input: Service[]) {
  const services: Service[] = [];
  for (const service of input.slice(0, 8)) {
    const name = service.name.trim().slice(0, 40);
    const minutes = Number(service.minutes);
    if (!name || ![30, 45, 60, 90, 120].includes(minutes)) continue;
    services.push({
      id: service.id || crypto.randomUUID(),
      name,
      minutes,
      price: String(service.price || "").replace(/[^\d.]/g, "").slice(0, 8),
      detail: service.detail.trim().slice(0, 80),
    });
  }
  return services;
}

export function cleanHours(input: WeekWindow[]) {
  const hours: WeekWindow[] = [];
  for (const window of input.slice(0, 14)) {
    if (window.dow < 0 || window.dow > 6) continue;
    if (!/^\d{2}:\d{2}$/.test(window.start) || !/^\d{2}:\d{2}$/.test(window.end)) continue;
    if (window.end <= window.start) continue;
    hours.push({ dow: window.dow, start: window.start, end: window.end });
  }
  return hours;
}

export function cleanQuestions(input: string[]) {
  return input.map((q) => q.trim().slice(0, 120)).filter(Boolean).slice(0, 5);
}
