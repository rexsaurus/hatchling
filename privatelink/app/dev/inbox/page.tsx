import { notFound } from "next/navigation";
import { DevReply } from "@/components/Actions";
import { readDb } from "@/lib/store";

export const dynamic = "force-dynamic";

export default async function InboxPage() {
  if (process.env.NODE_ENV === "production") notFound();
  const db = await readDb();
  const messages = [...db.messages].reverse();
  return (
    <main className="shell">
      <div className="brand">Test inbox</div>
      <h1>Texts on this server.</h1>
      <p className="lede">Twilio isn't required here. Reply as the trainer or the client, using the number on the text.</p>
      <DevReply />
      <div className="stack mt">
        {messages.map((message) => (
          <div key={message.id} className={message.direction === "in" ? "bubble in" : "bubble"}>
            <div className="fine">
              {message.direction} · {message.from} → {message.to}
            </div>
            {message.body}
            {message.mediaUrl ? <img src={message.mediaUrl} alt="Request snapshot" /> : null}
          </div>
        ))}
      </div>
    </main>
  );
}
