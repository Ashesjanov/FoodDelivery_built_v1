/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{vue,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        canvas: '#f6f7f8',
        ink: '#18181b',
        brand: {
          400: '#fb923c',
          500: '#f97316',
          600: '#ea580c',
        },
        coral: {
          400: '#fb7185',
          500: '#f43f5e',
        },
        fresh: {
          400: '#4ade80',
          500: '#22c55e',
        },
        aqua: {
          400: '#22d3ee',
          500: '#06b6d4',
        },
      },
      fontFamily: {
        sans: ['PingFang SC', 'Microsoft YaHei', 'system-ui', 'sans-serif'],
      },
      aspectRatio: {
        '4/3': '4 / 3',
        '5/4': '5 / 4',
        '4/5': '4 / 5',
        '16/9': '16 / 9',
        '16/10': '16 / 10',
      },
      borderRadius: {
        xl: '0.875rem',
        '2xl': '1.25rem',
      },
      boxShadow: {
        bento: '0 10px 30px rgba(24, 24, 27, 0.07)',
      },
    },
  },
  plugins: [],
}
