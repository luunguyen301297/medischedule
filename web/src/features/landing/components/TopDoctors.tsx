import { UserOutlined } from '@ant-design/icons'
import { useEffect, useRef, useState } from 'react'
import type { PublicDoctor } from '../api'
import type { Booker } from './BookingWidget'
import DoctorProfileDialog from './DoctorProfileDialog'

/**
 * "Các bác sĩ chuyên khoa hàng đầu": ảnh tròn + tên + chuyên khoa, hiện 4 bác sĩ, cuộn ngang để xem tiếp
 * (kéo, trackpad, lăn chuột khi trỏ vào dải, hoặc bấm chấm trang). Số bác sĩ mỗi trang đặt ở landing.css (--per-view).
 */
export default function TopDoctors({ doctors, booker }: { doctors: PublicDoctor[]; booker?: Booker }) {
  const track = useRef<HTMLDivElement>(null)
  const [pages, setPages] = useState(1)
  const [page, setPage] = useState(0)
  const [open, setOpen] = useState<PublicDoctor | null>(null)

  useEffect(() => {
    const el = track.current
    if (!el) return
    const measure = () => {
      const n = Math.max(1, Math.ceil(el.scrollWidth / el.clientWidth - 0.01))
      setPages(n)
      setPage(Math.min(n - 1, Math.round(el.scrollLeft / el.clientWidth)))
    }
    // Lăn chuột dọc → cuộn ngang; tới mép dải thì trả lại cho trang cuộn bình thường
    const onWheel = (e: WheelEvent) => {
      if (Math.abs(e.deltaX) > Math.abs(e.deltaY)) return
      const max = el.scrollWidth - el.clientWidth
      if ((e.deltaY < 0 && el.scrollLeft <= 0) || (e.deltaY > 0 && el.scrollLeft >= max - 1)) return
      e.preventDefault()
      el.scrollBy({ left: e.deltaY, behavior: 'smooth' })
    }
    measure()
    const ro = new ResizeObserver(measure)
    ro.observe(el)
    el.addEventListener('scroll', measure, { passive: true })
    el.addEventListener('wheel', onWheel, { passive: false })
    return () => {
      ro.disconnect()
      el.removeEventListener('scroll', measure)
      el.removeEventListener('wheel', onWheel)
    }
  }, [doctors.length])

  const goTo = (i: number) => {
    const el = track.current
    if (el) el.scrollTo({ left: i * el.clientWidth, behavior: 'smooth' })
  }

  if (doctors.length === 0) return null
  return (
    <div className="top-doctors">
      <h2>Các bác sĩ chuyên khoa hàng đầu</h2>
      <div ref={track} className="doctor-track" tabIndex={0} aria-label="Danh sách bác sĩ, cuộn ngang để xem thêm">
        {doctors.map((d) => (
          <button key={d.userId} type="button" className="doctor-card" onClick={() => setOpen(d)} aria-label={`Xem hồ sơ ${d.fullName}`}>
            {d.avatarUrl
              ? <img src={d.avatarUrl} alt={d.fullName} loading="lazy" />
              : <span className="doctor-avatar-empty" aria-hidden="true"><UserOutlined /></span>}
            <span className="doctor-caption">
              <b>{d.fullName}</b>
              {d.specialtyName && <span>{d.specialtyName}</span>}
            </span>
          </button>
        ))}
      </div>
      {pages > 1 && (
        <div className="doctor-dots">
          {Array.from({ length: pages }, (_, i) => (
            <button
              key={i}
              type="button"
              className={i === page ? 'active' : undefined}
              aria-label={`Trang ${i + 1}`}
              aria-current={i === page}
              onClick={() => goTo(i)}
            />
          ))}
        </div>
      )}
      <DoctorProfileDialog doctor={open} booker={booker} onClose={() => setOpen(null)} />
    </div>
  )
}
