import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./lib/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      screens: {
        // Mobile-first breakpoints
        'sm': '640px',   // Small devices (landscape phones)
        'md': '768px',   // Medium devices (tablets)
        'lg': '1024px',  // Large devices (desktops)
        'xl': '1280px',  // Extra large devices (large desktops)
        '2xl': '1536px', // 2X large devices
      },
      colors: {
        background: "var(--background)",
        foreground: "var(--foreground)",
        kiro: {
          purple: {
            400: "#9b7cff",
            500: "#6f42c1", // Adjusted for 4.5:1 contrast with white text (was #7c5cff, 4.35:1)
            600: "#5b3fe6",
            700: "#4329b3",
          },
          orange: {
            400: "#fb923c", // Lighter shade for text accents on dark backgrounds (8.94:1 on dark)
            500: "#c2410c", // Adjusted for 4.5:1 contrast with white text (5.18:1) - for buttons
            600: "#9a3412", // Darker hover state for buttons
          },
          ink: {
            900: "#0b0b12",
            950: "#06060a",
          },
          slate: {
            100: "#f1f1f8",
            200: "#d9d9e6",
            400: "#a1a1aa", // Added for better contrast (7.89:1 on dark)
            500: "#8b8b9e", // Lightened for 4.5:1 contrast on dark (was #6b6b82, 3.90:1)
          },
        },
      },
      keyframes: {
        fadeIn: {
          "0%": { opacity: "0", transform: "translateY(10px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        fadeOut: {
          "0%": { opacity: "1" },
          "100%": { opacity: "0" },
        },
        slideDown: {
          "0%": { opacity: "0", transform: "translateY(-12px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        progress: {
          "0%": { backgroundPosition: "0% 50%" },
          "100%": { backgroundPosition: "100% 50%" },
        },
      },
      animation: {
        "fade-in": "fadeIn 0.45s ease-out",
        "fade-out": "fadeOut 0.35s ease-in forwards",
        "slide-down": "slideDown 0.3s ease-out",
        progress: "progress 2s linear infinite",
      },
    },
  },
  plugins: [],
};

export default config;
