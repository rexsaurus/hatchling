import Link from "next/link";

const ITEMS = [
  ["home", "/app", "Home"],
  ["bookings", "/app/bookings", "Requests"],
  ["link", "/app/link", "Link"],
  ["setup", "/app/setup", "Setup"],
] as const;

export function Nav({ active }: { active: (typeof ITEMS)[number][0] }) {
  return (
    <nav className="nav">
      {ITEMS.map(([id, href, label]) => (
        <Link key={id} href={href} className={id === active ? "on" : ""}>
          {label}
        </Link>
      ))}
    </nav>
  );
}
