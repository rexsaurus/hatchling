import { redirect } from "next/navigation";
import { HoursEditor } from "@/components/Editors";
import { Nav } from "@/components/Nav";
import { requireHost } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function HoursPage() {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  return (
    <main className="shell">
      <div className="brand">Hours</div>
      <h1>When you train.</h1>
      <HoursEditor
        hours={provider.hours}
        bufferMinutes={provider.bufferMinutes}
        horizonDays={provider.horizonDays}
        minNoticeMinutes={provider.minNoticeMinutes}
        busyIcsUrl={provider.busyIcsUrl}
      />
      <Nav active="setup" />
    </main>
  );
}
