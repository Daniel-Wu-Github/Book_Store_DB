import React, { useRef, useEffect, useState } from 'react'

export default function AnimatedBook({ size = 48, style = {} }) {
  const root = useRef(null)
  const containerRef = useRef(null)
  const [pageRotation, setPageRotation] = useState(0)

  useEffect(() => {
    const handleMouseMove = (e) => {
      // Calculate cursor position as percentage across the entire viewport width
      const xPercent = Math.max(0, Math.min(1, e.clientX / window.innerWidth))
      // Map percentage to rotation: 0% (left edge) = -160deg, 100% (right edge) = 0deg
      const rotation = (1 - xPercent) * -160
      setPageRotation(rotation)
    }

    // Listen on the entire document
    document.addEventListener('mousemove', handleMouseMove)

    return () => {
      document.removeEventListener('mousemove', handleMouseMove)
    }
  }, [])

  return (
    <div ref={root}>
      <div
        ref={containerRef}
        className="book-container animated-book"
        style={{
          width: size,
          height: size,
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          transformStyle: 'preserve-3d',
          perspective: 800,
          willChange: 'transform',
          cursor: 'pointer',
          ...style
        }}
      >
        <svg viewBox="0 0 64 64" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
          {/* Book base/spine */}
          <g transform="translate(4,8)">
            {/* Back cover */}
            <rect x="4" y="4" width="48" height="44" rx="3" fill="#d35400" />
            
            {/* Pages stack */}
            <rect x="8" y="6" width="40" height="40" rx="2" fill="#fff7e6" />
            <rect x="9" y="7" width="38" height="38" rx="2" fill="#fef3dc" />
            <rect x="10" y="8" width="36" height="36" rx="2" fill="#fff9ed" />
            
            {/* Front cover (left side) */}
            <rect x="4" y="4" width="24" height="44" rx="3" fill="#e67e22" />
            
            {/* Spine highlight */}
            <rect x="26" y="4" width="4" height="44" fill="#d35400" />
            
            {/* Front cover decoration */}
            <rect x="8" y="12" width="16" height="4" rx="1" fill="#f5cba7" opacity="0.8" />
            <rect x="8" y="20" width="14" height="2" rx="0.5" fill="#fef9e7" opacity="0.6" />
            <rect x="8" y="24" width="12" height="2" rx="0.5" fill="#fef9e7" opacity="0.6" />
            <rect x="8" y="28" width="10" height="2" rx="0.5" fill="#fef9e7" opacity="0.6" />
            
            {/* Animated page (follows cursor) */}
            <g 
              className="animated-page" 
              style={{ 
                transformOrigin: '28px 26px', 
                transformStyle: 'preserve-3d',
                transform: `rotateY(${pageRotation}deg)`,
                transition: 'transform 0.15s ease-out'
              }}
            >
              <rect x="30" y="8" width="20" height="36" rx="1" fill="#fffef5" />
              <rect x="32" y="14" width="14" height="1.5" rx="0.5" fill="#ddd" opacity="0.5" />
              <rect x="32" y="18" width="12" height="1.5" rx="0.5" fill="#ddd" opacity="0.5" />
              <rect x="32" y="22" width="14" height="1.5" rx="0.5" fill="#ddd" opacity="0.5" />
              <rect x="32" y="26" width="10" height="1.5" rx="0.5" fill="#ddd" opacity="0.5" />
              <rect x="32" y="30" width="13" height="1.5" rx="0.5" fill="#ddd" opacity="0.5" />
            </g>
            
            {/* Right cover edge */}
            <rect x="50" y="8" width="3" height="36" rx="1" fill="#e67e22" />
          </g>
        </svg>
      </div>
    </div>
  )
}
