import Link from "next/link";
import { redirect } from "next/navigation";
import { Nav } from "@/components/Nav";
import { requireHost } from "@/lib/auth";
import { textsConfigured } from "@/lib/deliver";
import { readDb } from "@/lib/store";

export const dynamic = "force-dynamic";

export default async function TextsPage() {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  const db = await readDb();
  const messages = db.messages.filter((message) => message.providerId === provider.id).slice(-40).reverse();
  return (
    <main className="shell">
      <div className="brand">Texts</div>
      <h1>What went out.</h1>
      {!textsConfigured() ? (
        <p className="note">
          Twilio isn't connected, so these stay here. {process.env.NODE_ENV !== "production" ? <Link href="/dev/inbox">Open the test inbox</Link> : null}
        </p>
      ) : null}
      <div className="stack mt">
        {messages.length === 0 ? <p className="muted">No texts yet.</p> : null}
        {messages.map((message) => (
          <div key={message.id} className={message.direction === "in" ? "bubble in" : "bubble"}>
            <div className="fine">{message.direction === "in" ? `From ${message.from}` : `To ${message.to}`}</div>
            {message.body}
            {message.mediaUrl ? <img src={message.mediaUrl} alt="Request snapshot" /> : null}
          </div>
        ))}
      </div>
      <Nav active="setup" />
    </main>
  );
}
