import type { Metadata } from "next";
import { Noto_Sans, Noto_Sans_Display } from "next/font/google";
import "./globals.css";
import AuthBootstrap from "./AuthBootstrap";
import { ThemeProvider } from "@/components/theme/ThemeProvider";

const notoSans = Noto_Sans({
  variable: "--font-noto-sans",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

const notoSansDisplay = Noto_Sans_Display({
  variable: "--font-noto-sans-display",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

export const metadata: Metadata = {
  title: "Real-Time Chat System",
  description: "A real-time chat application built with Next.js and Spring Boot",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${notoSans.variable} ${notoSansDisplay.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <body className="min-h-full flex flex-col" suppressHydrationWarning>
        {/* Set dark mode before body content paints to avoid a flash of the
            wrong theme. Placed in <body> (not <html>) because a <script> is
            not a valid child of <html> — placing it there triggered a Next.js
            dev-overlay hydration error whose portal intercepted e2e pointer
            events (see mobile-nav.spec.ts "clicking Channels tab"). */}
        <script
          dangerouslySetInnerHTML={{
            __html: `document.documentElement.classList.add('dark')`,
          }}
        />
        <ThemeProvider>
          <AuthBootstrap />
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
