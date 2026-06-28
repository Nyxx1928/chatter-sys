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
      fontFamily: {
        sans: ['var(--font-noto-sans)', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
        display: ['var(--font-noto-sans-display)', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
      },
      screens: {
        'sm': '640px',
        'md': '768px',
        'lg': '1024px',
        'xl': '1280px',
        '2xl': '1536px',
      },
      colors: {
        background: "var(--background)",
        foreground: "var(--foreground)",
        slack: {
          primary: 'var(--slack-primary)',
          'primary-light': 'var(--slack-primary-light)',
          'accent-green': 'var(--slack-accent-green)',
          'accent-blue': 'var(--slack-accent-blue)',
          'accent-yellow': 'var(--slack-accent-yellow)',
          'accent-red': 'var(--slack-accent-red)',
          'surface-primary': 'var(--slack-surface-primary)',
          'surface-secondary': 'var(--slack-surface-secondary)',
          'surface-tertiary': 'var(--slack-surface-tertiary)',
          'text-primary': 'var(--slack-text-primary)',
          'text-secondary': 'var(--slack-text-secondary)',
          'text-inverse': 'var(--slack-text-inverse)',
          border: 'var(--slack-border)',
        },
        kiro: {
          purple: {
            400: "#9b7cff",
            500: "#6f42c1",
            600: "#5b3fe6",
            700: "#4329b3",
          },
          orange: {
            400: "#fb923c",
            500: "#c2410c",
            600: "#9a3412",
          },
          ink: {
            900: "#0b0b12",
            950: "#06060a",
          },
          slate: {
            100: "#f1f1f8",
            200: "#d9d9e6",
            400: "#a1a1aa",
            500: "#8b8b9e",
          },
        },
      },
      spacing: {
        xs: '4px',
        sm: '8px',
        md: '12px',
        lg: '16px',
        xl: '20px',
        '2xl': '24px',
        '3xl': '32px',
      },
      borderRadius: {
        sm: '4px',
        md: '6px',
        lg: '8px',
        pill: '9999px',
      },
      boxShadow: {
        'slack-sm': '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        'slack-md': '0 2px 4px 0 rgba(0, 0, 0, 0.1)',
        'slack-lg': '0 4px 8px 0 rgba(0, 0, 0, 0.15)',
      },
      fontSize: {
        'display-xl': ['32px', { lineHeight: '38px', fontWeight: '700' }],
        'display-lg': ['24px', { lineHeight: '30px', fontWeight: '700' }],
        'display-md': ['20px', { lineHeight: '26px', fontWeight: '600' }],
        'body-lg': ['16px', { lineHeight: '24px', fontWeight: '400' }],
        'body-md': ['15px', { lineHeight: '22px', fontWeight: '400' }],
        'body-sm': ['13px', { lineHeight: '18px', fontWeight: '400' }],
        caption: ['11px', { lineHeight: '14px', fontWeight: '400' }],
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
