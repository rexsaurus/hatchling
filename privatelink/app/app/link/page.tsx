import { redirect } from "next/navigation";
import { CopyLink, InviteButton } from "@/components/Actions";
import { Nav } from "@/components/Nav";
import { requireHost } from "@/lib/auth";
import { readDb } from "@/lib/store";
import { formatWhen } from "@/lib/time";
import { baseUrl } from "@/lib/url";

export const dynamic = "force-dynamic";

export default async function LinkPage() {
  const provider = await requireHost();
  if (!provider) redirect("/start");
  const db = await readDb();
  const origin = baseUrl();
  const page = provider.slug ? `${origin}/t/${provider.slug}` : "";
  const feed = provider.slug ? `${origin}/api/cal/${provider.slug}?k=${provider.calToken}` : "";
  const invites = db.invites.filter((invite) => invite.providerId === provider.id).sort((a, b) => b.createdAt - a.createdAt);
  return (
    <main className="shell">
      <div className="brand">Your link</div>
      <h1>The page clients open.</h1>
      {page ? (
        <>
          <div className="card">
            <div className="fine">{page}</div>
          </div>
          <div className="mt">
            <CopyLink value={page} />
          </div>
          <p className="fine">This page stays up. Each booking then gets its own expiring session link and code.</p>
          <h2 className="mt">Calendar</h2>
          <p className="fine">Subscribe in Apple or Google Calendar. Confirmed sessions show up there. To block busy time, paste your calendar's secret ICS link under Hours.</p>
          <div className="card mt">
            <div className="fine">{feed}</div>
          </div>
        </>
      ) : (
        <p className="note">Set your link name in Profile first.</p>
      )}
      <h2 className="mt">One-time links</h2>
      <p className="lede">Send one person a link that dies after they request a time, or when the clock runs out.</p>
      <InviteButton />
      {invites.map((invite) => {
        const dead = invite.bookingId || invite.expiresAt <= Date.now();
        return (
          <div key={invite.id} className="item">
            <b>{dead ? "Finished" : "Open"}</b>
            <small>
              {origin}/i/{invite.token}
              <br />
              {dead ? "Used or expired" : `Expires ${formatWhen(invite.expiresAt, provider.timezone)}`}
            </small>
          </div>
        );
      })}
      <Nav active="link" />
    </main>
  );
}
