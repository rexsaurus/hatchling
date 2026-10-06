import { redirect } from "next/navigation";
import { ProfileEditor } from "@/components/Editors";
import { Nav } from "@/components/Nav";
import { requireHost } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function ProfilePage() {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  return (
    <main className="shell">
      <div className="brand">Profile</div>
      <h1>The page, and the address.</h1>
      <ProfileEditor
        name={provider.name}
        slug={provider.slug}
        photo={provider.photo}
        line={provider.line}
        placeLabel={provider.placeLabel}
        address={provider.address}
        timezone={provider.timezone}
      />
      <Nav active="setup" />
    </main>
  );
}
