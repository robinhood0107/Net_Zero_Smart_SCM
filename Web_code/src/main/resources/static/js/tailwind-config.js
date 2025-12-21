tailwind.config = {
    darkMode: "class",
    theme: {
        extend: {
            colors: {
                primary: "#006d77",
                "primary-light": "#e0f2f1",
                "primary-hover": "#004d40",
                background: "#f1f5f9",
                surface: "#ffffff",
                "surface-alt": "#f9fafb",
                "text-main": "#111827",
                "text-sub": "#64748b",
                border: "#cbd5e1",
                "border-color": "#e2e8f0",
                "primary-dark": "#13b6ec",
                "primary-cyan": "#13b6ec",
                "background-light": "#f6f8f8",
                "background-dark": "#101d22",
                "card-dark": "#1c2427",
                "surface-dark": "#1c2427",
                "surface-dark-lighter": "#243238",
                "border-dark": "#3b4d54",
                "text-secondary": "#9db2b9",
                success: "#10b981",
                warning: "#f59e0b",
                accent: "#f97316",
            },
            fontFamily: {
                sans: ["Noto Sans KR", "sans-serif"],
                display: ["Inter", "sans-serif"]
            },
            borderRadius: {
                DEFAULT: "0.25rem",
                lg: "0.5rem",
                xl: "0.75rem",
                "2xl": "1rem",
                full: "9999px",
            }
        },
    },
}

