/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'app-primary': '#1E88E5',
        'app-dark-blue': '#1565C0',
        'app-light-blue': '#BBDEFB',
        'app-border': '#D0D7DE',
        'app-bg': '#FAFAFA',
        'unit-bg': '#EBF3FA',
        'unit-text': '#1E40AF',
        'priority-high': '#E53935',
        'priority-med': '#FB8C00',
        'priority-low': '#43A047',
        'footer-gray': '#757575',
      },
      fontFamily: {
        sans: [
          'Roboto',
          'system-ui',
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'sans-serif',
        ],
      },
    },
  },
  plugins: [],
};
