import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import path from "node:path";
import type { DB } from "./types";

const KEY = "pl:db";
const LOCK = "pl:lock";

export function emptyDb(): DB {
  return { v: 1, providers: [], invites: [], bookings: [], messages: [], otps: [] };
}

function usingKv() {
  return Boolean(process.env.KV_REST_API_URL && process.env.KV_REST_API_TOKEN);
}

function dataFile() {
  return process.env.DATA_FILE || path.join(process.cwd(), ".data", "db.json");
}

function prune(db: DB) {
  const now = Date.now();
  db.otps = db.otps.filter((otp) => otp.expiresAt > now - 3600000);
  if (db.messages.length > 400) db.messages.splice(0, db.messages.length - 400);
  return db;
}

async function kv(command: unknown[]) {
  const res = await fetch(process.env.KV_REST_API_URL!, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${process.env.KV_REST_API_TOKEN}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(command),
  });
  if (!res.ok) throw new Error(`Could not reach the database (${res.status}).`);
  return (await res.json()) as { result: unknown };
}

async function readKv(): Promise<DB> {
  const got = await kv(["GET", KEY]);
  if (typeof got.result !== "string" || !got.result) return emptyDb();
  try {
    const parsed = JSON.parse(got.result) as DB;
    if (parsed.v !== 1) return emptyDb();
    return parsed;
  } catch {
    return emptyDb();
  }
}

async function writeKv(db: DB) {
  await kv(["SET", KEY, JSON.stringify(prune(db))]);
}

async function withLock(fn: () => Promise<void>) {
  for (let i = 0; i < 25; i++) {
    const got = await kv(["SET", LOCK, "1", "NX", "EX", "8"]);
    if (got.result === "OK") {
      try {
        await fn();
      } finally {
        await kv(["DEL", LOCK]);
      }
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 40 * (i + 1)));
  }
  throw new Error("PrivateLink is busy. Try again.");
}

async function readFileDb(): Promise<DB> {
  try {
    const parsed = JSON.parse(await readFile(dataFile(), "utf8")) as DB;
    if (parsed.v !== 1) return emptyDb();
    return parsed;
  } catch {
    return emptyDb();
  }
}

async function writeFileDb(db: DB) {
  const file = dataFile();
  await mkdir(path.dirname(file), { recursive: true });
  const tmp = `${file}.${process.pid}.tmp`;
  await writeFile(tmp, JSON.stringify(prune(db)));
  await rename(tmp, file);
}

let chain: Promise<unknown> = Promise.resolve();

export function updateDb<T>(fn: (db: DB) => T | Promise<T>): Promise<T> {
  const run = chain.then(() => mutate(fn));
  chain = run.then(
    () => undefined,
    () => undefined,
  );
  return run;
}

async function mutate<T>(fn: (db: DB) => T | Promise<T>): Promise<T> {
  if (!usingKv() && process.env.VERCEL) {
    throw new Error("Add a Vercel KV database before taking bookings.");
  }
  if (!usingKv()) {
    const db = await readFileDb();
    const result = await fn(db);
    await writeFileDb(db);
    return result;
  }
  let result!: T;
  await withLock(async () => {
    const db = await readKv();
    result = await fn(db);
    await writeKv(db);
  });
  return result;
}

export async function readDb() {
  if (!usingKv() && process.env.VERCEL) {
    throw new Error("Add a Vercel KV database before taking bookings.");
  }
  return usingKv() ? readKv() : readFileDb();
}
