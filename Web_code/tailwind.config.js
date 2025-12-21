/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/resources/static/**/*.html",
    "./src/main/resources/static/js/**/*.js"
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        // Light mode colors
        "primary": "#0f766e",
        "primary-dark": "#0f766e",
        "primary-light": "#ccfbf1",
        "primary-subtle": "#f0fdfa",
        "secondary": "#f59e0b",
        "accent": "#10b981",
        "background": "#f1f5f9",
        "surface": "#ffffff",
        "surface-hover": "#f8fafc",
        "text-main": "#1e293b",
        "text-sub": "#64748b",
        "border-color": "#e2e8f0",
        // Dark mode specific colors
        "primary-cyan": "#13b6ec",
        "background-dark": "#101d22",
        "surface-dark": "#1c2427",
        "card-dark": "#1c2427",
        "border-dark": "#3b4d54",
        "text-secondary": "#9db2b9",
        // Status colors
        "success": "#10b981",
        "warning": "#f59e0b",
        "danger": "#ef4444",
      },
      fontFamily: {
        "display": ["Noto Sans KR", "Inter", "sans-serif"],
        "mono": ["JetBrains Mono", "monospace"],
        "paperlogy": ["Paperlogy", "Noto Sans KR", "Inter", "sans-serif"],
        "sans": ["Paperlogy", "Noto Sans KR", "Inter", "sans-serif"],
      },
      borderRadius: {
        "DEFAULT": "0.375rem",
        "lg": "0.5rem",
        "xl": "0.75rem",
        "2xl": "1rem",
        "full": "9999px"
      },
      boxShadow: {
        'soft': '0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03)',
      }
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/container-queries'),
  ],
}

