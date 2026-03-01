<template>
  <section aria-label="Gandzi logo">
    <svg viewBox="0 0 960 260" role="img" aria-label="Gandzi Financial Treasure logo" width="100%" height="auto">
      <defs>
        <linearGradient id="synthStroke" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#7cf7ff" />
          <stop offset="50%" stop-color="#8c7bff" />
          <stop offset="100%" stop-color="#ff4fd8" />
        </linearGradient>
        <linearGradient id="pyramidFill" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#141a3a" />
          <stop offset="100%" stop-color="#22295f" />
        </linearGradient>
        <filter id="glow" x="-25%" y="-50%" width="150%" height="200%">
          <feGaussianBlur stdDeviation="3.2" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      <polygon
        points="118,38 40,194 196,194"
        fill="url(#pyramidFill)"
        stroke="url(#synthStroke)"
        stroke-width="3.5"
        filter="url(#glow)"
      />
      <path d="M72 130h92" stroke="#7cf7ff" stroke-opacity="0.65" stroke-width="2.2" />
      <path d="M58 160h120" stroke="#8c7bff" stroke-opacity="0.7" stroke-width="2.2" />
      <path d="M48 176h140" stroke="#ff4fd8" stroke-opacity="0.75" stroke-width="2.2" />
      <circle cx="118" cy="86" r="12" fill="#ff9a4f" stroke="#ffe4c8" stroke-width="2" />
      <path d="M106 214h24" stroke="#ffe7f8" stroke-opacity="0.55" stroke-width="2" />

      <text x="220" y="146" fill="url(#synthStroke)" font-size="108" font-weight="800" letter-spacing="2" style="font-family: 'Orbitron', 'Audiowide', sans-serif;" filter="url(#glow)">
        GANDZI
      </text>
      <text x="226" y="196" fill="#d7dcff" font-size="30" letter-spacing="4" style="font-family: 'Space Grotesk', 'Avenir Next', sans-serif;">
        FINANCIAL TREASURE
      </text>
    </svg>
  </section>
</template>
