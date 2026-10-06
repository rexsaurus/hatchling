import { loadBusy } from "./act";
import { buildSlots } from "./slots";
import type { Booking, Provider } from "./types";

export async function slotsFor(provider: Provider, bookings: Booking[]) {
  const now = Date.now();
  const busy = await loadBusy(provider, now);
  const slotsByService: Record<string, number[]> = {};
  for (const service of provider.services) {
    slotsByService[service.id] = buildSlots({
      provider,
      bookings,
      minutes: service.minutes,
      now,
      busy,
    });
  }
  return slotsByService;
}
