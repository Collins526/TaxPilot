/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  corePlugins: {
    // The legacy screens are styled with hand-written SCSS; disabling preflight
    // keeps their appearance intact while new screens adopt Tailwind utilities.
    preflight: false
  },
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eef7f4',
          100: '#d6ebe3',
          500: '#1f8a6d',
          600: '#177057',
          700: '#125844',
          900: '#0b3327'
        }
      }
    }
  },
  plugins: []
};
