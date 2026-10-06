import { redirect } from "next/navigation";
import { requireHost } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  return children;
}
