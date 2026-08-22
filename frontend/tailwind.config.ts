import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // Brand accent based on coolors palette
        accent: {
          DEFAULT: '#145C9E',
          light: '#3A78B3',
          dark: '#0B4F6C',
        },
        background: '#DCC7BE',
      },
    },
  },
  plugins: [],
}

export default config
