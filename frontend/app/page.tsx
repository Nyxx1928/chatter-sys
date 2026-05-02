import Link from "next/link";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";

export default function Home() {
  return (
    <div className="flex flex-col flex-1 items-center justify-center px-4 py-8 sm:px-6 lg:px-8">
      <main className="w-full max-w-md space-y-8">
        {/* Header */}
        <div className="text-center">
          <h1 className="text-4xl font-bold tracking-tight text-zinc-900 dark:text-zinc-50 sm:text-5xl">
            Real-Time Chat
          </h1>
          <p className="mt-4 text-lg text-zinc-600 dark:text-zinc-400">
            Connect with others instantly through our real-time messaging platform
          </p>
        </div>

        {/* Action Cards */}
        <Card className="p-6 space-y-4">
          <div className="space-y-2">
            <h2 className="text-xl font-semibold text-zinc-900 dark:text-zinc-50">
              Get Started
            </h2>
            <p className="text-sm text-zinc-600 dark:text-zinc-400">
              Sign in to your account or create a new one to start chatting
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Link href="/auth-demo" className="flex-1">
              <Button variant="primary" className="w-full">
                Sign In
              </Button>
            </Link>
            <Link href="/auth-demo" className="flex-1">
              <Button variant="secondary" className="w-full">
                Register
              </Button>
            </Link>
          </div>
        </Card>

        {/* Features */}
        <div className="grid gap-4 sm:grid-cols-2">
          <Card className="p-4">
            <div className="space-y-2">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-100 dark:bg-blue-900">
                <svg
                  className="h-6 w-6 text-blue-600 dark:text-blue-300"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M13 10V3L4 14h7v7l9-11h-7z"
                  />
                </svg>
              </div>
              <h3 className="font-semibold text-zinc-900 dark:text-zinc-50">
                Real-Time Messaging
              </h3>
              <p className="text-sm text-zinc-600 dark:text-zinc-400">
                Send and receive messages instantly with WebSocket technology
              </p>
            </div>
          </Card>

          <Card className="p-4">
            <div className="space-y-2">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-100 dark:bg-green-900">
                <svg
                  className="h-6 w-6 text-green-600 dark:text-green-300"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"
                  />
                </svg>
              </div>
              <h3 className="font-semibold text-zinc-900 dark:text-zinc-50">
                Multiple Rooms
              </h3>
              <p className="text-sm text-zinc-600 dark:text-zinc-400">
                Create and join different chat rooms for various topics
              </p>
            </div>
          </Card>

          <Card className="p-4">
            <div className="space-y-2">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-100 dark:bg-purple-900">
                <svg
                  className="h-6 w-6 text-purple-600 dark:text-purple-300"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                  />
                </svg>
              </div>
              <h3 className="font-semibold text-zinc-900 dark:text-zinc-50">
                Secure Authentication
              </h3>
              <p className="text-sm text-zinc-600 dark:text-zinc-400">
                Your conversations are protected with JWT authentication
              </p>
            </div>
          </Card>

          <Card className="p-4">
            <div className="space-y-2">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-orange-100 dark:bg-orange-900">
                <svg
                  className="h-6 w-6 text-orange-600 dark:text-orange-300"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
              </div>
              <h3 className="font-semibold text-zinc-900 dark:text-zinc-50">
                Message History
              </h3>
              <p className="text-sm text-zinc-600 dark:text-zinc-400">
                Access your conversation history anytime you need it
              </p>
            </div>
          </Card>
        </div>

        {/* Footer */}
        <div className="text-center text-sm text-zinc-500 dark:text-zinc-500">
          <p>Built with Next.js, TypeScript, and Spring Boot</p>
        </div>
      </main>
    </div>
  );
}
