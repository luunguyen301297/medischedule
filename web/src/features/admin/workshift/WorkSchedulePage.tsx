import { CloseOutlined, LeftOutlined, PlusOutlined, RightOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, DatePicker, Empty, Form, Modal, Segmented, Select, Spin, Tag } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import 'dayjs/locale/vi'
import { useMemo, useState } from 'react'
import { ROLE_LABEL, type Role } from '@/features/auth'
import { errorMessage } from '@/shared/api/http'
import { shiftTime, workShiftApi, type Assignment, type WorkShift } from './api'

type Mode = 'day' | 'week' | 'month'
const UNIT: Record<Mode, 'day' | 'week' | 'month'> = { day: 'day', week: 'week', month: 'month' }
const ISO = 'YYYY-MM-DD'
const WEEKDAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']
// Quản trị viên không đi ca
const STAFF_ROLES = (Object.keys(ROLE_LABEL) as Role[]).filter((r) => r !== 'PATIENT' && r !== 'ADMIN')

/** Nhóm nhân viên theo vai trò, theo thứ tự ROLE_LABEL; vai trò không rõ (null) xếp cuối. */
function byRole(people: Assignment[]): [Role | null, Assignment[]][] {
  return [...STAFF_ROLES, null]
    .map((r): [Role | null, Assignment[]] => [r, people.filter((p) => p.role === r)])
    .filter(([, list]) => list.length > 0)
}

/** Thứ Hai của tuần chứa d (không phụ thuộc locale dayjs). */
const monday = (d: Dayjs) => d.subtract((d.day() + 6) % 7, 'day').startOf('day')

/** Các ngày hiển thị: 1 ngày, 7 ngày, hoặc lưới tháng 6 tuần bắt đầu thứ Hai. */
function visibleDays(mode: Mode, anchor: Dayjs): Dayjs[] {
  if (mode === 'day') return [anchor.startOf('day')]
  const start = mode === 'week' ? monday(anchor) : monday(anchor.startOf('month'))
  return Array.from({ length: mode === 'week' ? 7 : 42 }, (_, i) => start.add(i, 'day'))
}

/** Quản trị › Lịch làm việc: ai làm ca nào theo ngày / tuần / tháng; admin phân ca ở chế độ Ngày. */
export default function WorkSchedulePage() {
  const { message } = App.useApp()
  const qc = useQueryClient()
  const [mode, setMode] = useState<Mode>('week')
  const [anchor, setAnchor] = useState(() => dayjs().startOf('day'))
  const [role, setRole] = useState<Role>()
  // Ca đang mở hộp thoại thêm nhân viên (chế độ Ngày)
  const [adding, setAdding] = useState<WorkShift>()
  const [picked, setPicked] = useState<number[]>([])

  const days = useMemo(() => visibleDays(mode, anchor), [mode, anchor])
  const from = days[0].format(ISO)
  const to = days[days.length - 1].format(ISO)
  const today = dayjs().startOf('day')

  const shifts = useQuery({ queryKey: ['admin', 'work-shifts'], queryFn: workShiftApi.list })
  const schedule = useQuery({ queryKey: ['admin', 'work-schedule', from, to], queryFn: () => workShiftApi.schedule(from, to) })
  const staff = useQuery({ queryKey: ['admin', 'work-staff'], queryFn: workShiftApi.staff, enabled: !!adding })

  // ngày → ca → nhân viên (đã lọc vai trò)
  const byDay = useMemo(() => {
    const map = new Map<string, Map<number, Assignment[]>>()
    for (const a of schedule.data ?? []) {
      if (role && a.role !== role) continue
      const day = map.get(a.date) ?? new Map<number, Assignment[]>()
      day.set(a.shiftId, [...(day.get(a.shiftId) ?? []), a])
      map.set(a.date, day)
    }
    return map
  }, [schedule.data, role])

  const refresh = () => qc.invalidateQueries({ queryKey: ['admin', 'work-schedule'] })
  const assign = useMutation({
    mutationFn: workShiftApi.assign,
    onSuccess: (created) => {
      message.success(created.length ? `Đã thêm ${created.length} nhân viên` : 'Nhân viên đã có trong ca')
      setAdding(undefined)
    },
    onError: (e) => { message.error(errorMessage(e)) },
    onSettled: refresh,
  })
  const unassign = useMutation({
    mutationFn: workShiftApi.unassign,
    onSuccess: () => { message.success('Đã gỡ khỏi ca') },
    onError: (e) => { message.error(errorMessage(e)) },
    onSettled: refresh,
  })

  const allShifts = shifts.data ?? []
  /** Ca hiện trong một ngày: ca đang bật, cộng ca đã tắt nhưng ngày đó có người làm. */
  const shiftsOf = (day?: Map<number, Assignment[]>) => allShifts.filter((s) => s.active || day?.has(s.id))
  const openDay = (d: Dayjs) => {
    setAnchor(d)
    setMode('day')
  }

  const title =
    mode === 'day'
      ? anchor.locale('vi').format('dddd, DD/MM/YYYY')
      : mode === 'week'
        ? `${days[0].format('DD/MM')} – ${days[6].format('DD/MM/YYYY')}`
        : `Tháng ${anchor.format('MM/YYYY')}`

  const renderDay = () => {
    const day = byDay.get(from)
    const past = anchor.isBefore(today)
    const list = shiftsOf(day)
    if (!list.length) return <Empty description="Chưa có ca làm việc. Hãy thêm ở Cài đặt › Ca làm việc." />
    return (
      <>
        {past && <Alert className="ws-past" type="info" showIcon message="Ngày đã qua: chỉ xem, không sửa được lịch." />}
        {/* Mỗi ca một cột, nhân viên nhóm theo vai trò như chế độ Tuần */}
        <div className="ws-day">
        {list.map((s) => {
          const people = day?.get(s.id) ?? []
          return (
            <section key={s.id} className="ws-shift">
              <div className="ws-shift-head">
                <div>
                  <span className="ws-dot ws-dot-lead" style={{ background: s.color }} aria-hidden="true" />
                  <b>{s.name}</b> <span className="ws-sub">{shiftTime(s)} · {people.length} người</span>
                  {!s.active && <Tag className="ws-tag">Đang tắt</Tag>}
                </div>
                {!past && s.active && (
                  <Button
                    type="primary"
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={() => {
                      setPicked([])
                      setAdding(s)
                    }}
                  >
                    Thêm nhân viên
                  </Button>
                )}
              </div>
              {people.length ? (
                byRole(people).map(([r, group]) => (
                  <div key={r} className="ws-role-group">
                    <span className="ws-role-label">{r ? ROLE_LABEL[r] : 'Khác'}</span>
                    <ul className="ws-people">
                      {group.map((p) => (
                        <li key={p.id}>
                          <span className="ws-person">{p.fullName}</span>
                          {!past && (
                            <Button
                              type="text"
                              size="small"
                              icon={<CloseOutlined />}
                              disabled={unassign.isPending}
                              aria-label={`Gỡ ${p.fullName} khỏi ca ${s.name}`}
                              onClick={() => unassign.mutate(p.id)}
                            />
                          )}
                        </li>
                      ))}
                    </ul>
                  </div>
                ))
              ) : (
                <div className="ws-sub">Chưa phân</div>
              )}
            </section>
          )
        })}
        </div>
      </>
    )
  }

  const renderGrid = () => (
    <div className={`ws-grid ws-grid-${mode}`}>
      {WEEKDAYS.map((w) => (
        <div key={w} className="ws-weekday">{w}</div>
      ))}
      {days.map((d) => {
        const key = d.format(ISO)
        const day = byDay.get(key)
        const outside = mode === 'month' && d.month() !== anchor.month()
        const cls = ['ws-cell', outside && 'is-outside', d.isSame(today) && 'is-today'].filter(Boolean).join(' ')
        return (
          <button key={key} type="button" className={cls} onClick={() => openDay(d)} aria-label={`Xem ngày ${d.format('DD/MM/YYYY')}`}>
            <span className="ws-date">{mode === 'week' ? d.format('DD/MM') : d.date()}</span>
            {allShifts
              .filter((s) => day?.has(s.id))
              .map((s) => {
                const people = day!.get(s.id)!
                return mode === 'month' ? (
                  <span key={s.id} className="ws-chip">
                    <span className="ws-dot" style={{ background: s.color }} aria-hidden="true" />
                    {s.name} · {people.length}
                  </span>
                ) : (
                  <span key={s.id} className="ws-block" style={{ borderInlineStartColor: s.color }}>
                    <span className="ws-block-head">
                      <b>{s.name}</b> <span className="ws-sub">{shiftTime(s)}</span>
                    </span>
                    {byRole(people).map(([r, list]) => (
                      <span key={r} className="ws-role-group">
                        <span className="ws-role-label">{r ? ROLE_LABEL[r] : 'Khác'}</span>
                        {list.map((p) => (
                          <span key={p.id} className="ws-person">{p.fullName}</span>
                        ))}
                      </span>
                    ))}
                  </span>
                )
              })}
          </button>
        )
      })}
    </div>
  )

  const roleOf = new Map(staff.data?.map((s) => [s.userId, s.role]))
  const assigned = new Set(adding ? (byDay.get(from)?.get(adding.id) ?? []).map((a) => a.userId) : [])
  const loading = shifts.isLoading || schedule.isLoading
  const error = shifts.error ?? schedule.error

  return (
    <div className="admin-page">
      <div className="admin-head">
        <div>
          <h1>Lịch làm việc</h1>
          <p>Nhân viên đi làm theo ca. Chọn một ngày để phân ca.</p>
        </div>
      </div>

      <div className="ws-toolbar">
        <Segmented<Mode>
          value={mode}
          onChange={setMode}
          options={[
            { value: 'day', label: 'Ngày' },
            { value: 'week', label: 'Tuần' },
            { value: 'month', label: 'Tháng' },
          ]}
        />
        <Button icon={<LeftOutlined />} aria-label="Trước" onClick={() => setAnchor(anchor.subtract(1, UNIT[mode]))} />
        <Button onClick={() => setAnchor(today)}>Hôm nay</Button>
        <Button icon={<RightOutlined />} aria-label="Sau" onClick={() => setAnchor(anchor.add(1, UNIT[mode]))} />
        <DatePicker
          picker={mode === 'day' ? 'date' : mode}
          value={anchor}
          allowClear={false}
          format={mode === 'month' ? 'MM/YYYY' : mode === 'week' ? () => title : 'DD/MM/YYYY'}
          onChange={(d) => d && setAnchor(d.startOf('day'))}
          aria-label="Chọn thời gian"
        />
        <Select
          allowClear
          placeholder="Tất cả vai trò"
          className="ws-role"
          value={role}
          onChange={setRole}
          options={STAFF_ROLES.map((r) => ({ value: r, label: ROLE_LABEL[r] }))}
          aria-label="Lọc vai trò"
        />
        <span className="ws-title">{title}</span>
      </div>

      <div className="admin-scroll">
        {error && <Alert type="error" showIcon message={errorMessage(error)} />}
        <Spin spinning={loading}>{mode === 'day' ? renderDay() : renderGrid()}</Spin>
      </div>

      <Modal
        title={adding && `Thêm nhân viên · ${adding.name} ${anchor.format('DD/MM/YYYY')}`}
        open={!!adding}
        okText="Thêm"
        cancelText="Hủy"
        okButtonProps={{ disabled: !picked.length }}
        confirmLoading={assign.isPending}
        onOk={() => adding && assign.mutate({ date: from, shiftId: adding.id, userIds: picked })}
        onCancel={() => setAdding(undefined)}
      >
        {/* Mỗi vai trò một ô chọn; vai trò không có tài khoản nhân viên nào thì ẩn */}
        <Spin spinning={staff.isLoading}>
          <Form layout="vertical" component="div">
            {(role ? [role] : STAFF_ROLES)
              .filter((r) => staff.data?.some((s) => s.role === r))
              .map((r) => (
                <Form.Item key={r} label={ROLE_LABEL[r]} htmlFor={`ws-pick-${r}`}>
                  <Select
                    id={`ws-pick-${r}`}
                    mode="multiple"
                    allowClear
                    placeholder={`Chọn ${ROLE_LABEL[r].toLowerCase()}`}
                    optionFilterProp="label"
                    value={picked.filter((id) => roleOf.get(id) === r)}
                    onChange={(ids) => setPicked([...picked.filter((id) => roleOf.get(id) !== r), ...ids])}
                    notFoundContent={<Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Không còn ai để thêm" />}
                    options={staff.data
                      ?.filter((s) => s.role === r && !assigned.has(s.userId))
                      .map((s) => ({ value: s.userId, label: s.fullName }))}
                  />
                </Form.Item>
              ))}
            {staff.isSuccess && !staff.data.length && <Empty description="Không có nhân viên" />}
          </Form>
        </Spin>
      </Modal>
    </div>
  )
}
