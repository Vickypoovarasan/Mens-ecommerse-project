/* Atelier Tailwind theme — loaded after CDN */
if (typeof tailwind !== 'undefined') {
  tailwind.config = {
    darkMode: 'class',
    theme: {
      extend: {
        colors: {
          atelier: {
            bg: '#0f1419',
            surface: '#1a2332',
            surface2: '#243044',
            gold: '#c9a962',
            'gold-dim': '#8a7340',
            text: '#f0ebe3',
            muted: '#9aa5b5',
            border: '#2d3a4f',
            success: '#4ade80',
            danger: '#f87171'
          }
        },
        fontFamily: {
          display: ['"Cormorant Garamond"', 'Georgia', 'serif'],
          body: ['"DM Sans"', 'system-ui', 'sans-serif']
        },
        boxShadow: {
          card: '0 4px 24px rgba(0, 0, 0, 0.35), 0 0 0 1px rgba(45, 58, 79, 0.6)',
          glow: '0 0 40px rgba(201, 169, 98, 0.15)'
        },
        animation: {
          'fade-in': 'fadeIn 0.55s ease-out forwards',
          'slide-up': 'slideUp 0.5s ease-out forwards',
          'slide-down': 'slideDown 0.4s ease-out forwards',
          'scale-in': 'scaleIn 0.35s ease-out forwards',
          'shimmer': 'shimmer 1.5s ease-in-out infinite'
        },
        keyframes: {
          fadeIn: {
            '0%': { opacity: '0' },
            '100%': { opacity: '1' }
          },
          slideUp: {
            '0%': { opacity: '0', transform: 'translateY(16px)' },
            '100%': { opacity: '1', transform: 'translateY(0)' }
          },
          slideDown: {
            '0%': { opacity: '0', transform: 'translateY(-12px)' },
            '100%': { opacity: '1', transform: 'translateY(0)' }
          },
          scaleIn: {
            '0%': { opacity: '0', transform: 'scale(0.96)' },
            '100%': { opacity: '1', transform: 'scale(1)' }
          },
          shimmer: {
            '0%, 100%': { opacity: '0.45' },
            '50%': { opacity: '1' }
          }
        }
      }
    }
  };
}
