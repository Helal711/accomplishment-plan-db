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
      },
    },
  },
  plugins: [],
}
