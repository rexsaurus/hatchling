import { redirect } from "next/navigation";
import { SecurityEditor } from "@/components/Editors";
import { Nav } from "@/components/Nav";
import { requireHost } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function SecurityPage() {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  return (
    <main className="shell">
      <div className="brand">Before you accept</div>
      <h1>What you need to know.</h1>
      <p className="lede">A link, an ID photo, or a few questions. Then, before the session, a text asking if they're coming.</p>
      <SecurityEditor
        requireLink={provider.security.requireLink}
        linkLabel={provider.security.linkLabel}
        requireId={provider.security.requireId}
        questions={provider.security.questions}
        confirmLeadMinutes={provider.confirmLeadMinutes}
        autoCancelIfUnconfirmed={provider.autoCancelIfUnconfirmed}
        requestExpiresHours={provider.requestExpiresHours}
      />
      <Nav active="setup" />
    </main>
  );
}
