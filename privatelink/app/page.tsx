import Link from "next/link";

export default function HomePage() {
  return (
    <main className="shell">
      <div className="brand">PrivateLink</div>
      <h1>Your home studio, booked by text.</h1>
      <p className="lede">A simple page, like a link in your bio. Clients request a time. You get a picture of the request and reply yes, change, or no.</p>
      <Link href="/start" className="btn" style={{ display: "grid", placeItems: "center", textDecoration: "none" }}>
        Create your link
      </Link>
      <div className="steps">
        <div className="step">
          <div className="num">1</div>
          <div>Set your photo, sessions, and the hours you actually train.</div>
        </div>
        <div className="step">
          <div className="num">2</div>
          <div>A client picks one time, or a few if they're not sure. No account.</div>
        </div>
        <div className="step">
          <div className="num">3</div>
          <div>You get a text with the request. Accept, ask for a change, or decline.</div>
        </div>
        <div className="step">
          <div className="num">4</div>
          <div>Thirty minutes out, they confirm. Then you can send directions, once.</div>
        </div>
      </div>
      <div className="card">
        <b>The session is a one-time link.</b>
        <p className="fine">It has a short code. Someone who only has the link can't open it. Your address stays off the page until they say they're coming.</p>
      </div>
      <p className="fine mt">For personal trainers who work from home. No chat thread.</p>
    </main>
  );
}
