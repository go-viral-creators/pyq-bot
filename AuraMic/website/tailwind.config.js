/** Tailwind config: scans the generated pages. Brand colours mirror the Android app theme. */
module.exports = {
  content: ["./*.html", "./assets/*.js"],
  theme: {
    extend: {
      colors: { aura: { violet: "#7C5CFF", violet2: "#9B83FF", indigo: "#5B3DF5", cyan: "#22D3EE", pink: "#FF5C93", mint: "#34D399", ink: "#0A0A12", s1: "#12121C", s2: "#171724", s3: "#1E1E2D", line: "#26263A" } },
      fontFamily: { sans: ["Inter", "ui-sans-serif", "system-ui", "Segoe UI", "Roboto", "Arial", "sans-serif"] },
    },
  },
  plugins: [],
};
