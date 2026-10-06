import { ImageResponse } from "next/og";
import { prettyPhone } from "@/lib/phone";
import { readDb } from "@/lib/store";
import { formatWhen } from "@/lib/time";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const db = await readDb();
  const booking = db.bookings.find((item) => item.id === id);
  const provider = booking && db.providers.find((item) => item.id === booking.providerId);
  if (!booking || !provider) return new Response("Not found", { status: 404 });
  const times = booking.options.map((start, index) => `${index + 1}  ${formatWhen(start, provider.timezone)}`);
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          background: "#f3eee6",
          color: "#1c1915",
          padding: 48,
          fontFamily: "sans-serif",
        }}
      >
        <div style={{ display: "flex", fontSize: 22, letterSpacing: 4, color: "#1d4334" }}>PRIVATELINK</div>
        <div style={{ display: "flex", fontSize: 54, marginTop: 18 }}>{booking.customerName}</div>
        <div style={{ display: "flex", fontSize: 28, marginTop: 8 }}>
          {booking.serviceName} · {booking.minutes} min{booking.price ? ` · $${booking.price}` : ""}
        </div>
        <div style={{ display: "flex", fontSize: 24, marginTop: 8 }}>{prettyPhone(booking.customerPhone)}</div>
        <div style={{ display: "flex", flexDirection: "column", marginTop: 28, fontSize: 28, lineHeight: 1.4 }}>
          {times.map((line, index) => (
            <div key={index} style={{ display: "flex" }}>
              {line}
            </div>
          ))}
        </div>
        <div style={{ display: "flex", flexDirection: "column", marginTop: 28, fontSize: 22, color: "#3d3428" }}>
          {booking.link ? <div style={{ display: "flex" }}>Link: {booking.link.slice(0, 80)}</div> : null}
          {booking.idImage ? <div style={{ display: "flex" }}>ID photo is in PrivateLink, not in this text.</div> : null}
          {booking.answers.slice(0, 3).map((answer) => (
            <div key={answer.q} style={{ display: "flex" }}>
              {answer.q} {answer.a.slice(0, 80)}
            </div>
          ))}
        </div>
        <div style={{ display: "flex", marginTop: "auto", fontSize: 22 }}>Code {booking.code}</div>
      </div>
    ),
    { width: 800, height: 1000 },
  );
}
