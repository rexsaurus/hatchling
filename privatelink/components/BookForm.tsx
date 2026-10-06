"use client";

import { useMemo, useState } from "react";
import { fileToDataUrl } from "./photo";

type Service = { id: string; name: string; minutes: number; price: string; detail: string };
type Security = { requireLink: boolean; linkLabel: string; requireId: boolean; questions: string[] };

export function BookForm(props: {
  mode: "new" | "resubmit";
  trainerName: string;
  timezone: string;
  services: Service[];
  slotsByService: Record<string, number[]>;
  security: Security;
  slug?: string;
  inviteToken?: string;
  publicId?: string;
  changeNote?: string;
}) {
  const [serviceId, setServiceId] = useState(props.services[0]?.id || "");
  const [picked, setPicked] = useState<number[]>([]);
  const [step, setStep] = useState(props.mode === "resubmit" || props.services.length === 1 ? 1 : 0);
  const [link, setLink] = useState("");
  const [idImage, setIdImage] = useState("");
  const [answers, setAnswers] = useState<string[]>(props.security.questions.map(() => ""));
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const slots = props.slotsByService[serviceId] || [];
  const groups = useMemo(() => groupSlots(slots, props.timezone), [slots, props.timezone]);
  const needsSecurity = props.security.requireLink || props.security.requireId || props.security.questions.length > 0;
  const service = props.services.find((item) => item.id === serviceId);

  function toggle(start: number) {
    setPicked((current) => {
      if (current.includes(start)) return current.filter((item) => item !== start);
      if (current.length >= 3) return current;
      return [...current, start];
    });
  }

  async function submit() {
    setPending(true);
    setError("");
    const payload =
      props.mode === "resubmit"
        ? { action: "resubmit", options: picked, link, idImage, answers }
        : {
            slug: props.slug,
            inviteToken: props.inviteToken,
            serviceId,
            options: picked,
            customerName: name,
            customerPhone: phone,
            link,
            idImage,
            answers,
          };
    const url = props.mode === "resubmit" ? `/api/session/${props.publicId}` : "/api/bookings";
    const res = await fetch(url, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(payload),
    });
    const data = await res.json().catch(() => ({}));
    setPending(false);
    if (!res.ok) {
      setError(data.error || "That didn't send.");
      return;
    }
    if (props.mode === "resubmit") location.reload();
    else location.href = `/s/${data.publicId}`;
  }

  return (
    <div className="stack">
      {props.changeNote ? <div className="note">{props.changeNote}</div> : null}
      {step === 0 ? (
        <div className="stack">
          <h2>What do you want?</h2>
          {props.services.map((item) => (
            <button key={item.id} className={item.id === serviceId ? "service on" : "service"} type="button" onClick={() => setServiceId(item.id)}>
              <b>{item.name}</b>
              <small>
                {item.minutes} min{item.price ? ` · $${item.price}` : ""}
                {item.detail ? ` · ${item.detail}` : ""}
              </small>
            </button>
          ))}
          <button className="btn" type="button" onClick={() => setStep(1)}>
            Choose times
          </button>
        </div>
      ) : null}
      {step === 1 ? (
        <div>
          <h2>Pick a time</h2>
          <p className="muted">One, two, or three. {props.trainerName} will text you which one works.</p>
          {groups.length === 0 ? <p>No open times in this window.</p> : null}
          {groups.map((group) => (
            <div key={group.label} className="day">
              <div className="label">{group.label}</div>
              <div className="times">
                {group.slots.map((slot) => (
                  <button key={slot.start} type="button" className={picked.includes(slot.start) ? "chip on" : "chip"} onClick={() => toggle(slot.start)}>
                    {slot.label}
                  </button>
                ))}
              </div>
            </div>
          ))}
          <p className="fine mt">{picked.length} selected</p>
          {error ? <p className="error">{error}</p> : null}
          <button
            className="btn mt"
            type="button"
            disabled={picked.length === 0}
            onClick={() => {
              if (props.mode === "new" && needsSecurity) setStep(2);
              else if (props.mode === "new") setStep(3);
              else if (needsSecurity) setStep(2);
              else void submit();
            }}
          >
            Continue
          </button>
        </div>
      ) : null}
      {step === 2 ? (
        <div>
          <h2>Before they accept</h2>
          {props.security.requireLink ? (
            <>
              <label htmlFor="link">{props.security.linkLabel}</label>
              <input id="link" value={link} placeholder="https://" onChange={(e) => setLink(e.target.value)} />
            </>
          ) : null}
          {props.security.requireId ? (
            <>
              <div className="label">Photo of your ID</div>
              <input
                type="file"
                accept="image/*"
                onChange={async (e) => {
                  const file = e.target.files?.[0];
                  if (file) setIdImage(await fileToDataUrl(file, 900));
                }}
              />
            </>
          ) : null}
          {props.security.questions.map((question, index) => (
            <div key={question}>
              <label htmlFor={`q${index}`}>{question}</label>
              <input
                id={`q${index}`}
                value={answers[index] || ""}
                onChange={(e) => setAnswers((current) => current.map((item, i) => (i === index ? e.target.value : item)))}
              />
            </div>
          ))}
          <button className="btn mt" type="button" onClick={() => (props.mode === "resubmit" ? void submit() : setStep(3))}>
            {props.mode === "resubmit" ? "Send new times" : "Continue"}
          </button>
        </div>
      ) : null}
      {step === 3 ? (
        <div>
          <h2>Where should the text go?</h2>
          <label htmlFor="name">What should {props.trainerName} call you?</label>
          <input id="name" value={name} onChange={(e) => setName(e.target.value)} />
          <label htmlFor="phone">Mobile number</label>
          <input id="phone" inputMode="tel" autoComplete="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <div className="note mt">
            {service?.name} · {picked.length} time{picked.length === 1 ? "" : "s"}. No account. You'll get a code by text.
          </div>
          {error ? <p className="error">{error}</p> : null}
          <button className="btn mt" type="button" disabled={pending} onClick={() => void submit()}>
            {pending ? "Sending" : "Request this"}
          </button>
        </div>
      ) : null}
    </div>
  );
}

function groupSlots(slots: number[], timeZone: string) {
  const groups: { label: string; slots: { start: number; label: string }[] }[] = [];
  for (const start of slots) {
    const label = new Intl.DateTimeFormat("en-US", { timeZone, weekday: "short", month: "short", day: "numeric" }).format(start);
    const time = new Intl.DateTimeFormat("en-US", { timeZone, hour: "numeric", minute: "2-digit" }).format(start);
    const last = groups[groups.length - 1];
    if (!last || last.label !== label) groups.push({ label, slots: [{ start, label: time }] });
    else last.slots.push({ start, label: time });
  }
  return groups;
}
