import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react'

type From = 'left' | 'right' | 'up'

/**
 * Trượt phần tử vào khi nó xuất hiện trong khung nhìn (một lần).
 * Người dùng bật "giảm chuyển động" trong hệ điều hành thì hiện ngay, không trượt.
 */
export default function Reveal({
  from = 'up',
  delay = 0,
  className = '',
  style,
  children,
}: {
  from?: From
  delay?: number
  className?: string
  style?: CSSProperties
  children: ReactNode
}) {
  const ref = useRef<HTMLDivElement>(null)
  const [shown, setShown] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || !('IntersectionObserver' in window)) {
      setShown(true)
      return
    }
    const io = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setShown(true)
          io.disconnect()
        }
      },
      // threshold 0: khối cao (vd. panel giới thiệu cao cả màn hình) vẫn kích hoạt ngay khi mép trên lọt vào,
      // rootMargin -12%: đợi khối vào hẳn khung nhìn một chút rồi mới trượt để người dùng kịp thấy
      { threshold: 0, rootMargin: '0px 0px -12% 0px' },
    )
    io.observe(el)
    return () => io.disconnect()
  }, [])

  return (
    <div
      ref={ref}
      className={`reveal reveal-${from} ${shown ? 'is-shown' : ''} ${className}`}
      style={{ ...style, transitionDelay: `${delay}ms` }}
    >
      {children}
    </div>
  )
}
