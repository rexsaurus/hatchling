import Link from "next/link";
import { LogoutButton } from "@/components/Actions";
import { Nav } from "@/components/Nav";

export const dynamic = "force-dynamic";

const LINKS = [
  ["/app/profile", "Profile", "Photo, name, private address"],
  ["/app/services", "Sessions", "What you offer and how long"],
  ["/app/hours", "Hours", "When you're free, and your calendar"],
  ["/app/security", "Before you accept", "ID, questions, confirmations"],
  ["/app/texts", "Texts", "What PrivateLink sent"],
];

export default function SetupPage() {
  return (
    <main className="shell">
      <div className="brand">Setup</div>
      <h1>Your studio.</h1>
      {LINKS.map(([href, title, detail]) => (
        <Link key={href} href={href} className="item">
          <b>{title}</b>
          <small>{detail}</small>
        </Link>
      ))}
      <div className="mt">
        <LogoutButton />
      </div>
      <Nav active="setup" />
    </main>
  );
}
