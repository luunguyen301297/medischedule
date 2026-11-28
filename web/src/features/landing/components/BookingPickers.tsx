/** Dải chọn ngày và ô chọn buổi dùng chung cho form đặt lịch và đặt bác sĩ. */
import dayjs from 'dayjs'
import type { BookingShift } from '../api'

const WEEKDAY = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
export const hhmm = (t: string) => t.slice(0, 5)

/** Dải 7 ngày tới để chọn nhanh. */
export function DateStrip({ id, value, onChange }: { id?: string; value?: string; onChange?: (v: string) => void }) {
  const days = Array.from({ length: 7 }, (_, i) => dayjs().add(i, 'day'))
  return (
    <div className="date-strip" role="radiogroup" aria-label="Ngày khám">
      {days.map((d, i) => {
        const v = d.format('YYYY-MM-DD')
        return (
          <button
            key={v}
            id={i === 0 ? id : undefined}
            type="button"
            role="radio"
            aria-checked={value === v}
            className={`date-chip ${value === v ? 'is-active' : ''}`}
            onClick={() => onChange?.(v)}
          >
            <span className="date-dow">{i === 0 ? 'Hôm nay' : i === 1 ? 'Mai' : WEEKDAY[d.day()]}</span>
            <span className="date-num">{d.format('DD')}</span>
            <span className="date-mon">Th{d.format('M')}</span>
          </button>
        )
      })}
    </div>
  )
}

/** Ô buổi khám theo ca làm việc đang bật; ca đã kết thúc của hôm nay bị khóa. */
export function SessionTiles({ id, shifts, date, value, onChange }: {
  id?: string; shifts: BookingShift[]; date?: string; value?: number; onChange?: (v: number) => void
}) {
  const isToday = date === dayjs().format('YYYY-MM-DD')
  const now = dayjs().format('HH:mm')
  return (
    <div className="session-tiles" role="radiogroup" aria-label="Buổi khám">
      {shifts.map((s, i) => (
        <button
          key={s.id}
          id={i === 0 ? id : undefined}
          type="button"
          role="radio"
          aria-checked={value === s.id}
          disabled={isToday && hhmm(s.endTime) <= now}
          className={`session-tile ${value === s.id ? 'is-active' : ''}`}
          onClick={() => onChange?.(s.id)}
        >
          <b>{s.name}</b>
          <span>{hhmm(s.startTime)} – {hhmm(s.endTime)}</span>
        </button>
      ))}
    </div>
  )
}

