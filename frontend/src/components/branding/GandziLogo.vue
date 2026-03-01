<template>
  <section aria-label="Gandzi logo">
    <svg viewBox="0 0 960 260" role="img" aria-label="Gandzi Financial Treasure logo" width="100%" height="auto">
      <defs>
        <linearGradient id="synthStroke" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#7cf7ff" />
          <stop offset="50%" stop-color="#8c7bff" />
          <stop offset="100%" stop-color="#ff4fd8" />
        </linearGradient>
        <linearGradient id="sunset" x1="50%" y1="0%" x2="50%" y2="100%">
          <stop offset="0%" stop-color="#ff9a4f" />
          <stop offset="100%" stop-color="#ff4fd8" />
        </linearGradient>
        <filter id="glow" x="-25%" y="-50%" width="150%" height="200%">
          <feGaussianBlur stdDeviation="3.2" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      <circle cx="110" cy="122" r="72" fill="url(#sunset)" opacity="0.85" />
      <path d="M36 152h148" stroke="#ffe7f8" stroke-opacity="0.6" stroke-width="2" />
      <path d="M48 168h124" stroke="#ffe7f8" stroke-opacity="0.45" stroke-width="2" />
      <path d="M58 184h104" stroke="#ffe7f8" stroke-opacity="0.3" stroke-width="2" />

      <text x="220" y="146" fill="url(#synthStroke)" font-size="108" font-weight="800" letter-spacing="2" style="font-family: 'Orbitron', 'Audiowide', sans-serif;" filter="url(#glow)">
        GANDZI
      </text>
      <text x="226" y="196" fill="#d7dcff" font-size="30" letter-spacing="4" style="font-family: 'Space Grotesk', 'Avenir Next', sans-serif;">
        FINANCIAL TREASURE
      </text>
    </svg>
  </section>
</template>
