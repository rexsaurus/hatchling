# PrivateLink

A mobile booking page for personal trainers who work from home. Clients open a simple link, request one or more times, and the trainer answers by text. The session itself is a separate one-time link with a 4-digit code. The studio address is sent only after the client confirms they are coming, and that directions link opens once.

## Run locally

```bash
cd privatelink
npm install
npm test
npm run dev
```

Open http://localhost:3000. Without Twilio, signup shows the text code on screen, and every booking text is stored under Setup → Texts and at `/dev/inbox`.

## Host on Vercel

Set the project **Root Directory** to `privatelink`.

Add a Vercel KV (or Upstash Redis) store and these environment variables:

- `SESSION_SECRET` — long random string
- `PUBLIC_BASE_URL` — the public https origin, no trailing slash
- `KV_REST_API_URL` and `KV_REST_API_TOKEN`
- `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`
- `CRON_SECRET` — Vercel Cron calls `/api/cron/reminders` every 5 minutes

Point a Twilio messaging webhook at `POST {PUBLIC_BASE_URL}/api/twilio/inbound`.

Trainers reply `1`, `2`, or `3` to accept that offered time, `CHANGE 4821` to ask for new times, or `NO 4821` to decline. Clients reply `HERE 4821` or `NO 4821` when asked if they are coming.

Confirmed sessions can be subscribed to from Apple or Google Calendar via the secret feed on the Link screen. Paste a secret ICS URL under Hours to block busy times from an existing calendar.
