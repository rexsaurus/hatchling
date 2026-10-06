import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "PrivateLink",
  description: "A private booking page for home-studio trainers. Clients request a time. You answer by text.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link href="https://fonts.googleapis.com/css2?family=Newsreader:opsz,wght@6..72,500;6..72,600&family=Outfit:wght@400;500;600&display=swap" rel="stylesheet" />
      </head>
      <body>{children}</body>
    </html>
  );
}
