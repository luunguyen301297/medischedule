import { CheckCircleFilled, ClockCircleOutlined, EnvironmentOutlined, PhoneOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button } from 'antd'
import { useEffect, type ReactNode } from 'react'
import { contentApi, DEFAULT_HOMEPAGE } from './api'
import BookingWidget from './components/BookingWidget'
import FeatureIcon from './components/FeatureIcon'
import Reveal from '@/shared/ui/Reveal'
import './landing.css'

function Logo({ name }: { name: string }) {
  return (
    <div className="brand-logo">
      <span className="brand-mark" aria-hidden="true">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M9 2h6v7h7v6h-7v7H9v-7H2V9h7z" /></svg>
      </span>
      {name}
    </div>
  )
}

const scrollToId = (id: string) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })

/**
 * Trang giới thiệu công khai kèm form đăng nhập (đăng ký mở dialog). Nội dung ở /admin/homepage, 3 loại:
 *  1. booking  — đặt lịch khám + giới thiệu cơ bản, đầu trang
 *  2. intro    — giới thiệu chung song song form đăng nhập / đăng ký
 *  3. sections — nhiều cụm thông tin kèm ảnh nền
 * Mỗi cụm trượt vào khi cuộn tới (components/Reveal).
 */
export default function LandingLayout({
  title,
  subtitle,
  onRegister,
  children,
}: {
  title: string
  subtitle: string
  /** Nút Đăng ký trên thanh điều hướng (trang đăng nhập mở dialog đăng ký) */
  onRegister: () => void
  children: ReactNode
}) {
  const { data = DEFAULT_HOMEPAGE } = useQuery({ queryKey: ['homepage'], queryFn: contentApi.homepage, staleTime: 60_000 })
  const specialties = useQuery({ queryKey: ['public', 'specialties'], queryFn: contentApi.specialties })
  const { booking, intro, sections } = data
  const shown = sections.filter((s) => s.active && s.title)

  // Tải lại trang (F5) luôn mở ở đầu trang, không để trình duyệt khôi phục vị trí cuộn cũ
  useEffect(() => {
    if ('scrollRestoration' in history) history.scrollRestoration = 'manual'
    window.scrollTo(0, 0)
  }, [])

  return (
    <div className="landing">
      {/* ---------- Thanh điều hướng ---------- */}
      <header className="site-nav">
        <button type="button" className="nav-home" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })} aria-label="Về đầu trang">
          <Logo name={intro.brandName} />
        </button>
        <nav aria-label="Điều hướng trang">
          <button type="button" onClick={() => scrollToId('dat-lich')}>Đặt lịch khám</button>
          {shown.length > 0 && <button type="button" onClick={() => scrollToId('kham-pha')}>Dịch vụ</button>}
          <button type="button" onClick={() => scrollToId('lien-he')}>Liên hệ</button>
        </nav>
        <div className="nav-right">
          {intro.hotline && <span className="nav-hotline"><PhoneOutlined /> {intro.hotline}</span>}
          <Button onClick={onRegister}>Đăng ký</Button>
          <Button type="primary" onClick={() => scrollToId('tai-khoan')}>Đăng nhập</Button>
        </div>
      </header>

      {/* ---------- 1. Đặt lịch khám + giới thiệu cơ bản ---------- */}
      <section id="dat-lich" className="booking-hero">
        <Reveal from="left" className="booking-copy">
          {booking.badge && <span className="booking-badge">{booking.badge}</span>}
          <h1>{booking.title}</h1>
          {booking.subtitle && <p className="booking-sub">{booking.subtitle}</p>}
          <ul className="booking-highlights">
            {booking.highlights.filter(Boolean).map((h, i) => (
              <li key={i}><CheckCircleFilled aria-hidden="true" /> {h}</li>
            ))}
          </ul>
          {booking.stats.length > 0 && (
            <dl className="booking-stats">
              {booking.stats.map((s, i) => (
                <div key={i}>
                  <dt>{s.value}</dt>
                  <dd>{s.label}</dd>
                </div>
              ))}
            </dl>
          )}
        </Reveal>
        <Reveal from="right" delay={120} className="booking-card">
          <BookingWidget
            specialties={specialties.data?.map((s) => s.name)}
            loadFailed={specialties.isError && !specialties.data}
            note={booking.note}
          />
        </Reveal>
      </section>

      {/* ---------- 2. Giới thiệu chung + đăng nhập / đăng ký ---------- */}
      <section id="tai-khoan" className="hero">
        <Reveal from="left" className="hero-brand">
          <Logo name={intro.brandName} />
          <div className="hero-copy">
            <h2>{intro.headline}</h2>
            {intro.subheadline && <p>{intro.subheadline}</p>}
            <ul className="hero-features">
              {intro.features.map((f, i) => (
                <li key={i}>
                  <span className="feature-icon" aria-hidden="true"><FeatureIcon name={f.icon} /></span>
                  <div>
                    <b>{f.title}</b>
                    {f.description && <span>{f.description}</span>}
                  </div>
                </li>
              ))}
            </ul>
          </div>
          <div className="hero-foot">{intro.footer}</div>
        </Reveal>

        <Reveal from="right" delay={120} className="hero-form">
          <div className="auth-panel">
            <h2>{title}</h2>
            <p className="auth-sub">{subtitle}</p>
            {children}
          </div>
        </Reveal>
      </section>

      {/* ---------- 3. Các cụm thông tin kèm ảnh nền ---------- */}
      <div id="kham-pha">
        {shown.map((s, i) => (
          <section
            key={i}
            className={`img-section img-${s.align}`}
            style={s.imageUrl ? { backgroundImage: `url("${s.imageUrl}")` } : undefined}
          >
            <Reveal from={s.align === 'right' ? 'right' : 'left'} className="img-card">
              {s.eyebrow && <span className="img-eyebrow">{s.eyebrow}</span>}
              <h2>{s.title}</h2>
              {s.description && <p>{s.description}</p>}
              {s.buttonText && (
                <Button size="large" className="btn-accent" onClick={() => scrollToId('dat-lich')}>
                  {s.buttonText}
                </Button>
              )}
            </Reveal>
          </section>
        ))}
      </div>

      {/* ---------- Liên hệ ---------- */}
      <footer id="lien-he" className="site-foot">
        <Reveal from="up" className="foot-grid">
          <Logo name={intro.brandName} />
          {intro.address && <div><EnvironmentOutlined /> {intro.address}</div>}
          {intro.hotline && <div><PhoneOutlined /> {intro.hotline}</div>}
          {intro.workingHours && <div><ClockCircleOutlined /> {intro.workingHours}</div>}
        </Reveal>
      </footer>
    </div>
  )
}
