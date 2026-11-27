import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, ExportOutlined, PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Radio, Select, Space, Spin, Switch, Tabs } from 'antd'
import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { contentApi, FEATURE_ICONS, LIMITS, type HomepageContent, type ImageSection } from '@/features/landing'
import { errorMessage } from '@/shared/api/http'
import ImageField from '@/shared/ui/ImageField'

const iconOptions = Object.entries(FEATURE_ICONS).map(([value, { label, node }]) => ({
  value,
  label: <span style={{ display: 'inline-flex', gap: 8, alignItems: 'center' }}>{node} {label}</span>,
}))

const STAT_SOURCES = [
  { value: 'specialties', label: 'Số chuyên khoa' },
  { value: 'doctors', label: 'Số bác sĩ' },
]

const NEW_SECTION: ImageSection = { active: true, align: 'left', eyebrow: '', title: '', description: '', imageUrl: '', buttonText: '' }

/** Thiết lập trang giới thiệu, 3 loại: đặt lịch đầu trang, giới thiệu chung cạnh form đăng nhập, các cụm kèm ảnh nền. */
export default function HomepageSettingsPage() {
  const { message } = App.useApp()
  const qc = useQueryClient()
  const [form] = Form.useForm<HomepageContent>()
  const { data, isLoading } = useQuery({ queryKey: ['homepage'], queryFn: contentApi.homepage })
  const doctors = useQuery({ queryKey: ['public', 'doctors'], queryFn: contentApi.doctors })
  const doctorOptions = (doctors.data ?? []).map((d) => ({
    value: d.userId,
    label: d.specialtyName ? `${d.fullName} · ${d.specialtyName}` : d.fullName,
  }))

  useEffect(() => {
    if (data) form.setFieldsValue(data)
  }, [data, form])

  const save = useMutation({
    mutationFn: contentApi.saveHomepage,
    onSuccess: (saved) => {
      qc.setQueryData(['homepage'], saved)
      message.success('Đã lưu nội dung trang chủ')
    },
    onError: (e) => message.error(errorMessage(e)),
  })

  if (isLoading) return <Spin style={{ margin: 48 }} />

  const bookingTab = (
    <>
      <section className="settings-section">
        <h2>Khối đặt lịch đầu trang</h2>
        <p>Phần đầu tiên khách nhìn thấy: giới thiệu cơ bản bên trái, form đặt lịch nhanh bên phải.</p>
        <Form.Item name={['booking', 'badge']} label="Nhãn nhỏ phía trên">
          <Input maxLength={LIMITS.bookingBadge} showCount placeholder="Đặt lịch online · Xác nhận trong 15 phút" />
        </Form.Item>
        <Form.Item name={['booking', 'title']} label="Tiêu đề" rules={[{ required: true, whitespace: true, message: 'Nhập tiêu đề' }]}>
          <Input maxLength={LIMITS.bookingTitle} showCount />
        </Form.Item>
        <Form.Item name={['booking', 'subtitle']} label="Mô tả ngắn">
          <Input.TextArea maxLength={LIMITS.bookingSubtitle} showCount autoSize={{ minRows: 2, maxRows: 3 }} />
        </Form.Item>
        <Form.Item name={['booking', 'note']} label="Dòng lưu ý dưới nút đặt lịch">
          <Input maxLength={LIMITS.bookingNote} showCount />
        </Form.Item>
      </section>

      <section className="settings-section">
        <h2>Cam kết</h2>
        <p>Các dòng có dấu tích dưới mô tả, tối đa {LIMITS.highlights} dòng.</p>
        <Form.List name={['booking', 'highlights']}>
          {(fields, { add, remove }) => (
            <>
              {fields.map((f) => (
                <div key={f.key} style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
                  <Form.Item name={f.name} noStyle rules={[{ required: true, whitespace: true, message: 'Nhập nội dung' }]}>
                    <Input maxLength={LIMITS.highlight} showCount />
                  </Form.Item>
                  <Button type="text" danger icon={<DeleteOutlined />} aria-label="Xóa dòng" onClick={() => remove(f.name)} />
                </div>
              ))}
              {fields.length < LIMITS.highlights && (
                <Button type="dashed" icon={<PlusOutlined />} style={{ marginBottom: 16 }} onClick={() => add('')}>Thêm cam kết</Button>
              )}
            </>
          )}
        </Form.List>
      </section>

      <section className="settings-section">
        <h2>Số liệu</h2>
        <p>Dải số tạo niềm tin dưới phần cam kết, tối đa {LIMITS.bookingStats} mục.</p>
        <Form.List name={['booking', 'stats']}>
          {(fields, { add, remove }) => (
            <>
              {fields.map((f) => (
                <div key={f.key} className="list-row" style={{ gridTemplateColumns: '200px 140px 1fr 40px', marginBottom: 12 }}>
                  <Form.Item name={[f.name, 'source']} label="Lấy số từ">
                    <Select allowClear placeholder="Nhập tay" options={STAT_SOURCES} />
                  </Form.Item>
                  <Form.Item noStyle dependencies={[['booking', 'stats', f.name, 'source']]}>
                    {({ getFieldValue }) => (
                      <Form.Item
                        name={[f.name, 'value']}
                        label="Con số"
                        tooltip={getFieldValue(['booking', 'stats', f.name, 'source']) ? 'Đếm từ hệ thống, hiện kèm dấu +. Ô này chỉ dùng khi chưa tải được số.' : undefined}
                        rules={[{ required: true, whitespace: true, message: 'Nhập con số' }]}
                      >
                        <Input maxLength={LIMITS.statValue} placeholder="40+" />
                      </Form.Item>
                    )}
                  </Form.Item>
                  <Form.Item name={[f.name, 'label']} label="Nhãn" rules={[{ required: true, whitespace: true, message: 'Nhập nhãn' }]}>
                    <Input maxLength={LIMITS.statLabel} showCount placeholder="Bác sĩ" />
                  </Form.Item>
                  <Button type="text" danger icon={<DeleteOutlined />} aria-label="Xóa mục" style={{ marginTop: 30 }} onClick={() => remove(f.name)} />
                </div>
              ))}
              {fields.length < LIMITS.bookingStats && (
                <Button type="dashed" icon={<PlusOutlined />} style={{ marginBottom: 16 }} onClick={() => add({ value: '', label: '' })}>Thêm số liệu</Button>
              )}
            </>
          )}
        </Form.List>
      </section>

      <section className="settings-section">
        <h2>Các bác sĩ chuyên khoa hàng đầu</h2>
        <p>
          Hiện dạng ảnh tròn dưới dải số liệu, hiện 4 bác sĩ, cuộn ngang để xem tiếp, theo thứ tự chọn (tối đa {LIMITS.topDoctors}).
          Chỉ liệt kê bác sĩ đang hoạt động; không chọn ai thì ẩn khối này.
        </p>
        <Form.Item name={['booking', 'topDoctorIds']} label="Bác sĩ">
          <Select
            mode="multiple"
            loading={doctors.isLoading}
            options={doctorOptions}
            optionFilterProp="label"
            maxCount={LIMITS.topDoctors}
            placeholder="Chọn bác sĩ"
          />
        </Form.Item>
      </section>

      <section className="settings-section">
        <h2>Chuyên khoa trong form đặt lịch</h2>
        <p>
          Danh sách lấy từ các chuyên khoa đang hoạt động ở menu <Link to="/admin/specialties">Chuyên khoa</Link>.
        </p>
      </section>
    </>
  )

  const clinicTab = (
    <section className="settings-section">
      <h2>Tên phòng khám</h2>
      <p>Hiện ở logo trang chủ và đầu menu khu quản trị.</p>
      <Form.Item name={['intro', 'brandName']} label="Tên phòng khám" rules={[{ required: true, whitespace: true, message: 'Nhập tên phòng khám' }]}>
        <Input maxLength={LIMITS.brandName} showCount />
      </Form.Item>
    </section>
  )

  const introTab = (
    <>
      <section className="settings-section">
        <h2>Phần giới thiệu cạnh form đăng nhập</h2>
        <p>Hiện ở nửa trái màn hình đầu tiên, song song với form đăng nhập / đăng ký.</p>
        <Form.Item name={['intro', 'headline']} label="Tiêu đề" rules={[{ required: true, whitespace: true, message: 'Nhập tiêu đề' }]}>
          <Input maxLength={LIMITS.headline} showCount />
        </Form.Item>
        <Form.Item name={['intro', 'subheadline']} label="Mô tả ngắn">
          <Input.TextArea maxLength={LIMITS.subheadline} showCount autoSize={{ minRows: 2, maxRows: 3 }} />
        </Form.Item>
      </section>

      <section className="settings-section">
        <h2>Điểm nổi bật</h2>
        <p>Danh sách có biểu tượng dưới tiêu đề, tối đa {LIMITS.features} mục.</p>
        <Form.List name={['intro', 'features']}>
          {(fields, { add, remove }) => (
            <>
              {fields.map((f) => (
                <div key={f.key} className="list-row" style={{ gridTemplateColumns: '190px 1fr 1.3fr 40px', marginBottom: 12 }}>
                  <Form.Item name={[f.name, 'icon']} label="Biểu tượng" rules={[{ required: true }]}>
                    <Select options={iconOptions} />
                  </Form.Item>
                  <Form.Item name={[f.name, 'title']} label="Tiêu đề" rules={[{ required: true, whitespace: true, message: 'Nhập tiêu đề' }]}>
                    <Input maxLength={LIMITS.featureTitle} showCount />
                  </Form.Item>
                  <Form.Item name={[f.name, 'description']} label="Mô tả">
                    <Input maxLength={LIMITS.featureDesc} showCount />
                  </Form.Item>
                  <Button type="text" danger icon={<DeleteOutlined />} aria-label="Xóa mục" style={{ marginTop: 30 }} onClick={() => remove(f.name)} />
                </div>
              ))}
              {fields.length < LIMITS.features && (
                <Button type="dashed" icon={<PlusOutlined />} style={{ marginBottom: 16 }} onClick={() => add({ icon: 'heart', title: '', description: '' })}>
                  Thêm điểm nổi bật
                </Button>
              )}
            </>
          )}
        </Form.List>
      </section>

      <section className="settings-section">
        <h2>Liên hệ</h2>
        <p>Hiện ở chân trang.</p>
        <div className="settings-grid">
          <Form.Item name={['intro', 'hotline']} label="Hotline">
            <Input maxLength={LIMITS.hotline} placeholder="1900 6868" />
          </Form.Item>
          <Form.Item name={['intro', 'workingHours']} label="Giờ làm việc">
            <Input maxLength={LIMITS.workingHours} showCount />
          </Form.Item>
          <Form.Item name={['intro', 'address']} label="Địa chỉ">
            <Input maxLength={LIMITS.address} showCount />
          </Form.Item>
          <Form.Item name={['intro', 'footer']} label="Dòng bản quyền">
            <Input maxLength={LIMITS.footer} showCount />
          </Form.Item>
        </div>
      </section>
    </>
  )

  const sectionsTab = (
    <Form.List name="sections">
      {(fields, { add, remove, move }) => (
        <>
          {fields.map((f, idx) => (
            <section key={f.key} className="settings-section">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <h2>Cụm {idx + 1}</h2>
                <Space>
                  <Form.Item name={[f.name, 'active']} valuePropName="checked" noStyle>
                    <Switch checkedChildren="Hiển thị" unCheckedChildren="Ẩn" />
                  </Form.Item>
                  <Button icon={<ArrowUpOutlined />} aria-label="Đưa lên" disabled={idx === 0} onClick={() => move(idx, idx - 1)} />
                  <Button icon={<ArrowDownOutlined />} aria-label="Đưa xuống" disabled={idx === fields.length - 1} onClick={() => move(idx, idx + 1)} />
                  <Button danger icon={<DeleteOutlined />} onClick={() => remove(f.name)}>Xóa</Button>
                </Space>
              </div>
              <div className="settings-grid">
                <div>
                  <Form.Item name={[f.name, 'imageUrl']} label="Ảnh nền" rules={[{ required: true, message: 'Chọn ảnh nền' }]}>
                    <ImageField />
                  </Form.Item>
                  <Form.Item name={[f.name, 'align']} label="Vị trí khung chữ" htmlFor={`section_${f.key}_align_left`}>
                    <Radio.Group
                      name={`section_${f.key}_align`}
                      optionType="button"
                      options={[
                        { value: 'left', label: 'Bên trái', id: `section_${f.key}_align_left` },
                        { value: 'right', label: 'Bên phải', id: `section_${f.key}_align_right` },
                      ]}
                    />
                  </Form.Item>
                </div>
                <div>
                  <Form.Item name={[f.name, 'eyebrow']} label="Nhãn nhỏ phía trên">
                    <Input maxLength={LIMITS.eyebrow} placeholder="Ưu đãi tháng 11" />
                  </Form.Item>
                  <Form.Item name={[f.name, 'title']} label="Tiêu đề" rules={[{ required: true, whitespace: true, message: 'Nhập tiêu đề' }]}>
                    <Input maxLength={LIMITS.sectionTitle} showCount />
                  </Form.Item>
                  <Form.Item name={[f.name, 'description']} label="Nội dung">
                    <Input.TextArea maxLength={LIMITS.sectionDesc} showCount autoSize={{ minRows: 3, maxRows: 5 }} />
                  </Form.Item>
                  <Form.Item name={[f.name, 'buttonText']} label="Chữ trên nút" tooltip="Để trống thì không hiện nút. Bấm nút sẽ cuộn lên form đăng nhập.">
                    <Input maxLength={LIMITS.buttonText} placeholder="Đặt lịch ngay" />
                  </Form.Item>
                </div>
              </div>
            </section>
          ))}
          {fields.length < LIMITS.sections && (
            <Button type="dashed" block icon={<PlusOutlined />} onClick={() => add({ ...NEW_SECTION, align: fields.length % 2 === 0 ? 'left' : 'right' })}>
              Thêm cụm thông tin kèm ảnh nền
            </Button>
          )}
        </>
      )}
    </Form.List>
  )

  return (
    <div className="admin-page">
      <div className="admin-head">
        <h1>Thiết lập trang chủ</h1>
        <Space>
          <Button icon={<ExportOutlined />} href="/login" target="_blank">Xem trang</Button>
          <Button type="primary" loading={save.isPending} onClick={() => form.submit()}>Lưu thay đổi</Button>
        </Space>
      </div>

      <Form
        form={form}
        className="admin-fill"
        layout="vertical"
        requiredMark={false}
        onFinish={(v) => save.mutate(v)}
        onFinishFailed={() => message.error('Còn ô chưa hợp lệ, kiểm tra lại các tab')}
      >
        <Tabs
          items={[
            { key: 'clinic', label: 'Tên phòng khám', children: clinicTab, forceRender: true },
            { key: 'booking', label: 'Đặt lịch đầu trang', children: bookingTab, forceRender: true },
            { key: 'intro', label: 'Giới thiệu chung', children: introTab, forceRender: true },
            { key: 'sections', label: 'Thông tin kèm ảnh nền', children: sectionsTab, forceRender: true },
          ]}
        />
      </Form>
    </div>
  )
}
