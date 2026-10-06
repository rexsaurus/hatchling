import { redirect } from "next/navigation";
import { StartForm } from "@/components/Actions";
import { requireHost } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function StartPage() {
  const provider = await requireHost();
  if (provider?.name && provider.slug && provider.address) redirect("/app");
  if (provider) redirect("/onboarding");
  return (
    <main className="shell">
      <div className="brand">PrivateLink</div>
      <h1>Your number is the login.</h1>
      <p className="lede">We'll text you a code. This is the number clients' requests will reach.</p>
      <StartForm />
    </main>
  );
}
