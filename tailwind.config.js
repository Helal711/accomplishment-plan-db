/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'app-primary': '#1976D2',
        'app-primary-dark': '#115293',
        'app-secondary': '#0D47A1',
        'priority-high': '#E53935',
        'priority-med': '#FB8C00',
        'priority-low': '#43A047',
        'unit-bg': '#E3F2FD',
        'unit-text': '#1976D2',
        'app-border': '#E5E7EB',
      },
    },
  },
  plugins: [],
}
