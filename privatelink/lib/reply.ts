export type Reply =
  | { action: "yes"; option: number | null; code: string | null }
  | { action: "change"; code: string | null; note: string }
  | { action: "no"; code: string | null }
  | { action: "here"; code: string | null }
  | { action: "unknown" };

export function parseReply(body: string): Reply {
  const text = body.trim().replace(/\s+/g, " ");
  if (!text) return { action: "unknown" };
  const code = text.match(/\b(\d{4})\b/)?.[1] || null;
  const head = text.split(" ")[0]?.toUpperCase() || "";

  if (["HERE", "COMING", "CONFIRM", "ONMYWAY"].includes(head)) {
    return { action: "here", code };
  }
  if (["CHANGE", "MODIFY", "RESCHEDULE"].includes(head)) {
    const note = text.replace(/^(\S+)\s+(\d{4})?\s*/, "").trim();
    return { action: "change", code, note };
  }
  if (["NO", "DECLINE", "CANCEL", "STOP", "CANT", "CAN'T"].includes(head)) {
    return { action: "no", code };
  }
  if (["YES", "Y", "ACCEPT", "OK", "OKAY"].includes(head)) {
    const option = optionNumber(text);
    return { action: "yes", option, code };
  }
  if (/^[123]$/.test(head)) {
    return { action: "yes", option: Number(head), code };
  }
  return { action: "unknown" };
}

function optionNumber(text: string) {
  const match = text.match(/\b([123])\b/);
  return match ? Number(match[1]) : null;
}
