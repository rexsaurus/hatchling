import { redirect } from "next/navigation";
import { ProfileEditor } from "@/components/Editors";
import { requireHost } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function OnboardingPage() {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  return (
    <main className="shell">
      <div className="brand">PrivateLink</div>
      <h1>Set up the page clients see.</h1>
      <p className="lede">The address is private. It goes out only after someone confirms they're coming.</p>
      <ProfileEditor
        finish
        name={provider.name}
        slug={provider.slug}
        photo={provider.photo}
        line={provider.line}
        placeLabel={provider.placeLabel}
        address={provider.address}
        timezone={provider.timezone}
      />
    </main>
  );
}
