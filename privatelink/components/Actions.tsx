"use client";

import { useState } from "react";

async function post(url: string, body: unknown) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || "That didn't work.");
  return data;
}

export function StartForm() {
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [devCode, setDevCode] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  async function requestCode(event: React.FormEvent) {
    event.preventDefault();
    setPending(true);
    setError("");
    try {
      const data = await post("/api/auth/request", { phone });
      setDevCode(data.devCode || "");
      setSent(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Try again.");
    } finally {
      setPending(false);
    }
  }

  async function verify(event: React.FormEvent) {
    event.preventDefault();
    setPending(true);
    setError("");
    try {
      const data = await post("/api/auth/verify", { code });
      location.href = data.needsSetup ? "/onboarding" : "/app";
    } catch (err) {
      setError(err instanceof Error ? err.message : "Try again.");
      setPending(false);
    }
  }

  if (!sent) {
    return (
      <form onSubmit={requestCode} className="stack">
        <label htmlFor="phone">Mobile number</label>
        <input id="phone" inputMode="tel" autoComplete="tel" value={phone} onChange={(e) => setPhone(e.target.value)} required />
        {error ? <p className="error">{error}</p> : null}
        <button className="btn" disabled={pending} type="submit">
          {pending ? "Sending" : "Text me a code"}
        </button>
      </form>
    );
  }

  return (
    <form onSubmit={verify} className="stack">
      <h2>Enter the code</h2>
      {devCode ? <div className="note">Texts aren't connected on this server. Your code is {devCode}.</div> : <p className="muted">Sent to {phone}.</p>}
      <input inputMode="numeric" autoComplete="one-time-code" value={code} onChange={(e) => setCode(e.target.value)} required />
      {error ? <p className="error">{error}</p> : null}
      <button className="btn" disabled={pending} type="submit">
        {pending ? "Checking" : "Continue"}
      </button>
    </form>
  );
}

export function CodeGate({ publicId }: { publicId: string }) {
  const [code, setCode] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  return (
    <form
      onSubmit={async (event) => {
        event.preventDefault();
        setPending(true);
        setError("");
        try {
          await post(`/api/session/${publicId}`, { action: "unlock", code });
          location.reload();
        } catch (err) {
          setError(err instanceof Error ? err.message : "Try again.");
          setPending(false);
        }
      }}
    >
      <h1>Enter the code</h1>
      <p className="lede">It's the 4-digit code from the text. The link alone doesn't open the session.</p>
      <input inputMode="numeric" autoComplete="one-time-code" value={code} onChange={(e) => setCode(e.target.value)} aria-label="Session code" />
      {error ? <p className="error">{error}</p> : null}
      <button className="btn mt" disabled={pending} type="submit">
        {pending ? "Checking" : "Open session"}
      </button>
    </form>
  );
}

export function ShowButtons({ publicId, asked }: { publicId: string; asked: boolean }) {
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  async function answer(coming: boolean) {
    setPending(true);
    setError("");
    try {
      await post(`/api/session/${publicId}`, { action: "show", coming });
      location.reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Try again.");
      setPending(false);
    }
  }
  return (
    <div className="stack mt">
      {asked ? <div className="note">Confirm you're coming. If you don't, the session can be canceled.</div> : null}
      {error ? <p className="error">{error}</p> : null}
      <button className="btn" type="button" disabled={pending} onClick={() => void answer(true)}>
        I'm coming
      </button>
      <button className="btn ghost" type="button" disabled={pending} onClick={() => void answer(false)}>
        I can't make it
      </button>
    </div>
  );
}

export function Reveal({ publicId, token }: { publicId: string; token: string }) {
  const [address, setAddress] = useState("");
  const [place, setPlace] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  if (address) {
    return (
      <div className="card">
        <div className="label">{place || "Where to go"}</div>
        <h2>{address}</h2>
        <p className="fine">This was a one-time link. It won't open again.</p>
      </div>
    );
  }
  return (
    <div>
      <h1>Directions</h1>
      <p className="lede">This opens once, on this phone.</p>
      {error ? <p className="error">{error}</p> : null}
      <button
        className="btn"
        type="button"
        disabled={pending}
        onClick={async () => {
          setPending(true);
          setError("");
          try {
            const data = await post(`/api/session/${publicId}/way`, { token });
            setAddress(data.address);
            setPlace(data.place || "");
          } catch (err) {
            setError(err instanceof Error ? err.message : "This link is finished.");
            setPending(false);
          }
        }}
      >
        {pending ? "Opening" : "Show the address"}
      </button>
    </div>
  );
}

export function DecisionPanel(props: {
  id: string;
  status: string;
  showConfirm: string;
  options: { index: number; label: string }[];
}) {
  const [note, setNote] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  async function go(action: string, extra: Record<string, unknown> = {}) {
    setPending(true);
    setError("");
    try {
      await post(`/api/bookings/${props.id}`, { action, ...extra });
      location.reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Try again.");
      setPending(false);
    }
  }
  return (
    <div className="stack mt">
      {props.status === "pending"
        ? props.options.map((option) => (
            <button key={option.index} className="btn" type="button" disabled={pending} onClick={() => void go("yes", { option: option.index })}>
              Accept {option.label}
            </button>
          ))
        : null}
      {props.status === "pending" || props.status === "confirmed" ? (
        <>
          <label htmlFor="note">Ask for a change</label>
          <textarea id="note" value={note} placeholder="Mornings work better." onChange={(e) => setNote(e.target.value)} />
          <button className="btn ghost" type="button" disabled={pending} onClick={() => void go("change", { note })}>
            Request a change
          </button>
        </>
      ) : null}
      {props.status === "pending" || props.status === "changes" ? (
        <button className="btn clay" type="button" disabled={pending} onClick={() => void go("no")}>
          Decline
        </button>
      ) : null}
      {props.status === "confirmed" ? (
        <>
          <button className="btn ghost" type="button" disabled={pending} onClick={() => void go("ask")}>
            Ask them to confirm
          </button>
          {props.showConfirm === "yes" ? (
            <button className="btn" type="button" disabled={pending} onClick={() => void go("directions")}>
              Send directions
            </button>
          ) : null}
          <button className="btn clay" type="button" disabled={pending} onClick={() => void go("cancel")}>
            Cancel session
          </button>
        </>
      ) : null}
      {error ? <p className="error">{error}</p> : null}
    </div>
  );
}

export function CopyLink({ value }: { value: string }) {
  const [done, setDone] = useState(false);
  return (
    <button
      className="btn"
      type="button"
      onClick={async () => {
        try {
          await navigator.clipboard.writeText(value);
        } catch {
          /* The field below stays selectable. */
        }
        setDone(true);
        setTimeout(() => setDone(false), 1500);
      }}
    >
      {done ? "Copied" : "Copy link"}
    </button>
  );
}

export function InviteButton() {
  const [hours, setHours] = useState(24);
  const [error, setError] = useState("");
  return (
    <div className="stack">
      <label htmlFor="hours">One-time link expires in</label>
      <select id="hours" value={hours} onChange={(e) => setHours(Number(e.target.value))}>
        <option value={6}>6 hours</option>
        <option value={24}>24 hours</option>
        <option value={72}>3 days</option>
      </select>
      {error ? <p className="error">{error}</p> : null}
      <button
        className="btn"
        type="button"
        onClick={async () => {
          setError("");
          try {
            await post("/api/invites", { hours });
            location.reload();
          } catch (err) {
            setError(err instanceof Error ? err.message : "Try again.");
          }
        }}
      >
        Create a one-time link
      </button>
    </div>
  );
}

export function LogoutButton() {
  return (
    <button
      className="btn ghost"
      type="button"
      onClick={async () => {
        await fetch("/api/auth/logout", { method: "POST" });
        location.href = "/";
      }}
    >
      Log out
    </button>
  );
}

export function DevReply() {
  const [from, setFrom] = useState("");
  const [body, setBody] = useState("YES 0000");
  const [error, setError] = useState("");
  return (
    <form
      className="stack mt"
      onSubmit={async (event) => {
        event.preventDefault();
        setError("");
        try {
          await post("/api/dev/reply", { from, body });
          location.reload();
        } catch (err) {
          setError(err instanceof Error ? err.message : "Try again.");
        }
      }}
    >
      <h2>Simulate a reply</h2>
      <label htmlFor="from">From number</label>
      <input id="from" value={from} onChange={(e) => setFrom(e.target.value)} />
      <label htmlFor="body">Text</label>
      <input id="body" value={body} onChange={(e) => setBody(e.target.value)} />
      {error ? <p className="error">{error}</p> : null}
      <button className="btn" type="submit">
        Send reply
      </button>
      <button
        className="btn ghost"
        type="button"
        onClick={async () => {
          await fetch("/api/cron/reminders");
          location.reload();
        }}
      >
        Run reminders
      </button>
    </form>
  );
}
