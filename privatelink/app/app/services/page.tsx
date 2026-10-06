import { redirect } from "next/navigation";
import { ServicesEditor } from "@/components/Editors";
import { Nav } from "@/components/Nav";
import { requireHost } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function ServicesPage() {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  return (
    <main className="shell">
      <div className="brand">Sessions</div>
      <h1>What you offer.</h1>
      <ServicesEditor initial={provider.services} />
      <Nav active="setup" />
    </main>
  );
}
