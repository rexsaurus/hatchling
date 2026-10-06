"use client";

import { useState } from "react";
import { fileToDataUrl } from "./photo";

const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const ZONES = [
  "America/Los_Angeles",
  "America/Denver",
  "America/Phoenix",
  "America/Chicago",
  "America/New_York",
  "America/Anchorage",
  "Pacific/Honolulu",
];

type Service = { id: string; name: string; minutes: number; price: string; detail: string };
type Hour = { dow: number; start: string; end: string };

async function save(body: unknown) {
  const res = await fetch("/api/provider", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || "Couldn't save.");
}

function SaveButton({ onClick, label }: { onClick: () => Promise<void>; label: string }) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [ok, setOk] = useState(false);
  return (
    <>
      {error ? <p className="error">{error}</p> : null}
      {ok ? <p className="fine">Saved.</p> : null}
      <button
        className="btn mt"
        type="button"
        disabled={pending}
        onClick={async () => {
          setPending(true);
          setError("");
          setOk(false);
          try {
            await onClick();
            setOk(true);
          } catch (err) {
            setError(err instanceof Error ? err.message : "Couldn't save.");
          } finally {
            setPending(false);
          }
        }}
      >
        {pending ? "Saving" : label}
      </button>
    </>
  );
}

export function ProfileEditor(props: {
  name: string;
  slug: string;
  photo: string;
  line: string;
  placeLabel: string;
  address: string;
  timezone: string;
  finish?: boolean;
}) {
  const [name, setName] = useState(props.name);
  const [slug, setSlug] = useState(props.slug);
  const [photo, setPhoto] = useState(props.photo);
  const [line, setLine] = useState(props.line);
  const [placeLabel, setPlaceLabel] = useState(props.placeLabel);
  const [address, setAddress] = useState(props.address);
  const [timezone, setTimezone] = useState(props.timezone);
  return (
    <div>
      <div className="hero mb">
        {photo ? <img className="photo" src={photo} alt="" /> : <div className="photo">{name.slice(0, 1) || "P"}</div>}
        <div>
          <div className="label">Photo</div>
          <input
            type="file"
            accept="image/*"
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (file) setPhoto(await fileToDataUrl(file));
            }}
          />
        </div>
      </div>
      <label htmlFor="name">Name</label>
      <input id="name" value={name} onChange={(e) => setName(e.target.value)} />
      <label htmlFor="slug">Your link</label>
      <input id="slug" value={slug} onChange={(e) => setSlug(e.target.value)} placeholder="jordan" />
      <label htmlFor="line">One line</label>
      <input id="line" value={line} onChange={(e) => setLine(e.target.value)} placeholder="Strength at my home studio" />
      <label htmlFor="place">Public place name</label>
      <input id="place" value={placeLabel} placeholder="Home studio" onChange={(e) => setPlaceLabel(e.target.value)} />
      <label htmlFor="address">Address clients get only after they confirm</label>
      <textarea id="address" value={address} onChange={(e) => setAddress(e.target.value)} />
      <label htmlFor="zone">Timezone</label>
      <select id="zone" value={timezone} onChange={(e) => setTimezone(e.target.value)}>
        {ZONES.map((zone) => (
          <option key={zone}>{zone}</option>
        ))}
      </select>
      <SaveButton
        label={props.finish ? "Open my link" : "Save profile"}
        onClick={async () => {
          await save({ name, slug, photo, line, placeLabel, address, timezone });
          if (props.finish) location.href = "/app";
        }}
      />
    </div>
  );
}

export function ServicesEditor({ initial }: { initial: Service[] }) {
  const [services, setServices] = useState<Service[]>(initial.length ? initial : [{ id: "", name: "Private session", minutes: 60, price: "", detail: "" }]);
  return (
    <div className="stack">
      {services.map((service, index) => (
        <div className="card" key={index}>
          <label htmlFor={`n${index}`}>Name</label>
          <input id={`n${index}`} value={service.name} onChange={(e) => update(index, { name: e.target.value })} />
          <label htmlFor={`m${index}`}>Length</label>
          <select id={`m${index}`} value={service.minutes} onChange={(e) => update(index, { minutes: Number(e.target.value) })}>
            {[30, 45, 60, 90, 120].map((minutes) => (
              <option key={minutes} value={minutes}>
                {minutes} min
              </option>
            ))}
          </select>
          <label htmlFor={`p${index}`}>Price, optional</label>
          <input id={`p${index}`} value={service.price} onChange={(e) => update(index, { price: e.target.value })} />
          <label htmlFor={`d${index}`}>Detail</label>
          <input id={`d${index}`} value={service.detail} onChange={(e) => update(index, { detail: e.target.value })} />
          <button className="btn ghost mt" type="button" onClick={() => setServices(services.filter((_, i) => i !== index))}>
            Remove
          </button>
        </div>
      ))}
      <button
        className="btn ghost"
        type="button"
        onClick={() => setServices([...services, { id: "", name: "", minutes: 60, price: "", detail: "" }])}
      >
        Add a session
      </button>
      <SaveButton label="Save sessions" onClick={() => save({ services })} />
    </div>
  );

  function update(index: number, patch: Partial<Service>) {
    setServices((current) => current.map((service, i) => (i === index ? { ...service, ...patch } : service)));
  }
}

export function HoursEditor(props: {
  hours: Hour[];
  bufferMinutes: number;
  horizonDays: number;
  minNoticeMinutes: number;
  busyIcsUrl: string;
}) {
  const [hours, setHours] = useState<Hour[]>(
    DAYS.map((_, dow) => props.hours.find((hour) => hour.dow === dow) || { dow, start: "09:00", end: "17:00" }),
  );
  const [on, setOn] = useState(DAYS.map((_, dow) => props.hours.some((hour) => hour.dow === dow)));
  const [bufferMinutes, setBuffer] = useState(props.bufferMinutes);
  const [horizonDays, setHorizon] = useState(props.horizonDays);
  const [minNoticeMinutes, setNotice] = useState(props.minNoticeMinutes);
  const [busyIcsUrl, setBusy] = useState(props.busyIcsUrl);
  return (
    <div>
      {DAYS.map((day, dow) => (
        <div key={day} className="card mt">
          <label className="check">
            <input type="checkbox" checked={on[dow]} onChange={(e) => setOn(on.map((value, i) => (i === dow ? e.target.checked : value)))} />
            {day}
          </label>
          {on[dow] ? (
            <div className="row">
              <input type="time" value={hours[dow].start} onChange={(e) => setHour(dow, { start: e.target.value })} />
              <input type="time" value={hours[dow].end} onChange={(e) => setHour(dow, { end: e.target.value })} />
            </div>
          ) : null}
        </div>
      ))}
      <label htmlFor="buffer">Buffer between sessions (minutes)</label>
      <input id="buffer" inputMode="numeric" value={bufferMinutes} onChange={(e) => setBuffer(Number(e.target.value))} />
      <label htmlFor="horizon">How many days ahead</label>
      <input id="horizon" inputMode="numeric" value={horizonDays} onChange={(e) => setHorizon(Number(e.target.value))} />
      <label htmlFor="notice">Minimum notice (minutes)</label>
      <input id="notice" inputMode="numeric" value={minNoticeMinutes} onChange={(e) => setNotice(Number(e.target.value))} />
      <label htmlFor="ics">Busy calendar link</label>
      <input id="ics" value={busyIcsUrl} placeholder="https://...ics" onChange={(e) => setBusy(e.target.value)} />
      <p className="fine">Paste a secret ICS address from Apple or Google Calendar. Those events block time on your page.</p>
      <SaveButton
        label="Save hours"
        onClick={() =>
          save({
            hours: hours.filter((_, dow) => on[dow]),
            bufferMinutes,
            horizonDays,
            minNoticeMinutes,
            busyIcsUrl,
          })
        }
      />
    </div>
  );

  function setHour(dow: number, patch: Partial<Hour>) {
    setHours((current) => current.map((hour) => (hour.dow === dow ? { ...hour, ...patch } : hour)));
  }
}

export function SecurityEditor(props: {
  requireLink: boolean;
  linkLabel: string;
  requireId: boolean;
  questions: string[];
  confirmLeadMinutes: number;
  autoCancelIfUnconfirmed: boolean;
  requestExpiresHours: number;
}) {
  const [requireLink, setLink] = useState(props.requireLink);
  const [linkLabel, setLabel] = useState(props.linkLabel);
  const [requireId, setId] = useState(props.requireId);
  const [questions, setQuestions] = useState(props.questions.length ? props.questions : [""]);
  const [confirmLeadMinutes, setLead] = useState(props.confirmLeadMinutes);
  const [autoCancelIfUnconfirmed, setAuto] = useState(props.autoCancelIfUnconfirmed);
  const [requestExpiresHours, setExpiry] = useState(props.requestExpiresHours);
  return (
    <div>
      <label className="check">
        <input type="checkbox" checked={requireLink} onChange={(e) => setLink(e.target.checked)} />
        Ask for a link
      </label>
      {requireLink ? <input value={linkLabel} onChange={(e) => setLabel(e.target.value)} /> : null}
      <label className="check">
        <input type="checkbox" checked={requireId} onChange={(e) => setId(e.target.checked)} />
        Ask for a photo of their ID
      </label>
      <div className="label">Questions</div>
      {questions.map((question, index) => (
        <input key={index} className="mt" value={question} placeholder="Any injuries?" onChange={(e) => setQuestions(questions.map((item, i) => (i === index ? e.target.value : item)))} />
      ))}
      <button className="btn ghost mt" type="button" onClick={() => setQuestions([...questions, ""])}>
        Add a question
      </button>
      <label htmlFor="lead">Ask them to confirm this many minutes before</label>
      <select id="lead" value={confirmLeadMinutes} onChange={(e) => setLead(Number(e.target.value))}>
        {[15, 30, 60, 120].map((minutes) => (
          <option key={minutes} value={minutes}>
            {minutes} minutes
          </option>
        ))}
      </select>
      <label className="check">
        <input type="checkbox" checked={autoCancelIfUnconfirmed} onChange={(e) => setAuto(e.target.checked)} />
        Auto-cancel if they never confirm
      </label>
      <label htmlFor="expiry">Unanswered requests expire after</label>
      <select id="expiry" value={requestExpiresHours} onChange={(e) => setExpiry(Number(e.target.value))}>
        {[6, 12, 24, 48].map((hours) => (
          <option key={hours} value={hours}>
            {hours} hours
          </option>
        ))}
      </select>
      <SaveButton
        label="Save"
        onClick={() =>
          save({
            security: { requireLink, linkLabel, requireId, questions },
            confirmLeadMinutes,
            autoCancelIfUnconfirmed,
            requestExpiresHours,
          })
        }
      />
    </div>
  );
}
