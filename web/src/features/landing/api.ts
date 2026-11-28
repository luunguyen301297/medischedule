import { http } from '@/shared/api/http'

export type FeatureIcon = 'calendar' | 'clock' | 'file' | 'bell' | 'heart' | 'team' | 'safety' | 'medicine'

/** Loại 1: giới thiệu chung, đặt song song với form đăng nhập / đăng ký. */
export interface IntroContent {
  brandName: string
  headline: string
  subheadline: string
  features: { icon: FeatureIcon; title: string; description: string }[]
  hotline: string
  workingHours: string
  address: string
  footer: string
}

/** Loại 2: khối thông tin kèm ảnh nền, xếp bên dưới, trượt vào khi cuộn. */
export interface ImageSection {
  active: boolean
  align: 'left' | 'right'      // khung chữ nằm bên trái hay phải ảnh
  eyebrow: string
  title: string
  description: string
  imageUrl: string
  buttonText: string           // để trống thì không hiện nút
}

/** Loại 3: khối đặt lịch khám + giới thiệu cơ bản, đặt ở đầu trang. */
export interface BookingContent {
  badge: string
  title: string
  subtitle: string
  highlights: string[]                       // 3 cam kết ngắn có dấu tích
  stats: BookingStat[]                       // số liệu tạo niềm tin
  note: string                               // dòng nhỏ dưới nút đặt lịch
  topDoctorIds: number[]                     // users.id bác sĩ hiện ở "Các bác sĩ chuyên khoa hàng đầu", theo thứ tự
}

/** source có giá trị thì con số đếm từ hệ thống (vd. "12+"), value chỉ là dự phòng khi chưa tải được. */
export interface BookingStat { value: string; label: string; source?: 'specialties' | 'doctors' }

/** Bác sĩ đang hoạt động (public); specialtyName null = chưa có khoa. */
export interface PublicDoctor {
  userId: number; fullName: string; degree: string | null; specialtyName: string | null; avatarUrl: string | null
  licenseNo: string | null; acceptsDesignation: boolean
}

export interface HomepageContent {
  booking: BookingContent
  intro: IntroContent
  sections: ImageSection[]
}

/** Giới hạn độ dài để bố cục không vỡ. Dùng chung cho form thiết lập. */
export const LIMITS = {
  brandName: 30, headline: 160, subheadline: 160, featureTitle: 45, featureDesc: 70, features: 4,
  hotline: 20, workingHours: 50, address: 90, footer: 70,
  eyebrow: 30, sectionTitle: 70, sectionDesc: 220, buttonText: 30, sections: 8,
  bookingBadge: 50, bookingTitle: 70, bookingSubtitle: 180, highlight: 50, highlights: 3,
  bookingStats: 3, statValue: 10, statLabel: 30, bookingNote: 120, topDoctors: 12,
} as const

const img = (id: string) => `https://images.unsplash.com/photo-${id}?w=1920&q=70&auto=format&fit=crop`

/** Nội dung mẫu: dùng khi backend chưa có dữ liệu hoặc không gọi được (khớp migration V3 + V4). */
export const DEFAULT_HOMEPAGE: HomepageContent = {
  booking: {
    badge: 'Đặt lịch sớm để được phục vụ tốt hơn',
    title: 'Đặt lịch khám trong 1 phút, đến là được khám',
    subtitle: '',
    highlights: ['Đặt lịch đơn giản', 'Xác nhận qua điện thoại trong 15 phút', 'Có thể lựa chọn các bác sĩ hàng đầu'],
    stats: [
      { value: '10+', label: 'Chuyên khoa', source: 'specialties' },
      { value: '10+', label: 'Bác sĩ', source: 'doctors' },
      { value: '4.8/5', label: 'Bệnh nhân đánh giá' },
    ],
    note: '',
    topDoctorIds: [],
  },
  intro: {
    brandName: 'MediSchedule',
    headline: 'Một tài khoản để đặt lịch, theo dõi lượt khám và xem lại hồ sơ sức khỏe của cả gia đình.',
    subheadline: '',
    features: [
      { icon: 'calendar', title: 'Chọn chuyên khoa, bác sĩ và giờ khám', description: 'Xem khung giờ còn trống và giữ chỗ ngay.' },
      { icon: 'bell', title: 'Biết trước thời gian chờ', description: 'Nhận thông báo khi sắp đến lượt khám.' },
      { icon: 'file', title: 'Kết quả và đơn thuốc ở một nơi', description: 'Tra cứu lại lịch sử khám bất cứ lúc nào.' },
    ],
    hotline: '1900 6868',
    workingHours: 'T2 – T7: 7:00 – 20:00 · CN: 7:00 – 12:00',
    address: '125 Nguyễn Văn Linh, Phường Tân Phong, Quận 7, TP.HCM',
    footer: '© 2026 MediSchedule',
  },
  sections: [
    {
      active: true, align: 'left', eyebrow: 'Đặt lịch khám',
      title: 'Chọn chuyên khoa, chọn đúng bác sĩ bạn tin',
      description: 'Xem khung giờ còn trống theo từng ngày, chỉ định đích danh bác sĩ quen và nhận mã lịch ngay sau khi đặt.',
      imageUrl: img('1532938911079-1b06ac7ceec7'), buttonText: 'Đặt lịch ngay',
    },
    {
      active: true, align: 'right', eyebrow: 'Không cần tài khoản',
      title: 'Đặt nhanh chỉ với số điện thoại',
      description: 'Chưa có tài khoản vẫn đặt được lịch: nhập số điện thoại, chọn giờ khám, tra cứu và hủy lịch bằng mã lịch được gửi qua tin nhắn.',
      imageUrl: img('1576091160550-2173dba999ef'), buttonText: 'Đặt lịch nhanh',
    },
    {
      active: true, align: 'left', eyebrow: 'Thời gian chờ',
      title: 'Biết còn bao lâu đến lượt, không phải ngồi chờ ở sảnh',
      description: 'Theo dõi số thứ tự đang gọi và thời gian chờ ước tính ngay trên điện thoại. App báo trước khi còn 3 người.',
      imageUrl: img('1519494026892-80bbd2d6fd0d'), buttonText: '',
    },
    {
      active: true, align: 'right', eyebrow: 'Kết quả khám',
      title: 'Kết quả xét nghiệm và chẩn đoán hình ảnh trên app',
      description: 'Xem lại bệnh án, kết quả xét nghiệm, phim X-quang và siêu âm của từng lần khám, tải bản PDF khi cần.',
      imageUrl: img('1666214280557-f1b5022eb634'), buttonText: 'Đăng nhập để xem',
    },
    {
      active: true, align: 'left', eyebrow: 'Đơn thuốc',
      title: 'Đơn thuốc điện tử và nhắc giờ uống thuốc',
      description: 'Đơn thuốc có hướng dẫn liều dùng sáng, trưa, chiều, tối. App nhắc đúng giờ để không quên liều nào.',
      imageUrl: img('1587854692152-cbe660dbde88'), buttonText: '',
    },
    {
      active: true, align: 'right', eyebrow: 'Bệnh mạn tính',
      title: 'Bác sĩ đồng hành cùng bạn giữa các lần tái khám',
      description: 'Người bệnh tiểu đường, huyết áp tự nhập chỉ số hằng ngày. Chỉ số vượt ngưỡng, bác sĩ phụ trách được báo để liên hệ kịp thời.',
      imageUrl: img('1576765608535-5f04d1e3f289'), buttonText: 'Tìm hiểu chương trình',
    },
    {
      active: true, align: 'left', eyebrow: 'Hồ sơ gia đình',
      title: 'Một tài khoản chăm sóc cả nhà',
      description: 'Thêm hồ sơ cho con nhỏ và bố mẹ, đặt lịch hộ người thân và xem lại lịch sử khám của từng người.',
      imageUrl: img('1584515933487-779824d29309'), buttonText: 'Tạo tài khoản',
    },
    {
      active: true, align: 'right', eyebrow: 'Trợ lý AI',
      title: 'Chưa biết nên khám khoa nào? Hỏi trợ lý',
      description: 'Mô tả triệu chứng, trợ lý gợi ý chuyên khoa và bác sĩ phù hợp rồi điền sẵn phiếu đặt lịch. Gợi ý chỉ để tham khảo, không thay chẩn đoán của bác sĩ.',
      imageUrl: img('1631217868264-e5b90bb7e133'), buttonText: 'Thử trợ lý',
    },
  ],
}

/** Ghép dữ liệu backend với mặc định: thiếu phần nào (vd. DB chưa chạy migration) thì lấy mẫu. */
export function withDefaults(data: Partial<HomepageContent> | undefined): HomepageContent {
  return {
    booking: { ...DEFAULT_HOMEPAGE.booking, ...(data?.booking ?? {}) },
    intro: { ...DEFAULT_HOMEPAGE.intro, ...(data?.intro ?? {}) },
    sections: Array.isArray(data?.sections) ? data!.sections : DEFAULT_HOMEPAGE.sections,
  }
}

/** Ca làm việc đang bật (giờ dạng HH:mm:ss), cấu hình ở trang Ca làm việc của admin. */
export interface BookingShift { id: number; name: string; startTime: string; endTime: string }

/** Bác sĩ được phân vào ca; specialtyId null = chưa gán khoa. */
export interface BookingDoctor { userId: number; fullName: string; specialtyId: number | null; specialtyName: string | null }

export const bookingApi = {
  shifts: () => http.get<BookingShift[]>('/public/booking/shifts').then((r) => r.data),
  doctors: (date: string, shiftId: number, specialtyId?: number) =>
    http.get<BookingDoctor[]>('/public/booking/doctors', { params: { date, shiftId, specialtyId } }).then((r) => r.data),
}

/** Yêu cầu đặt đích danh bác sĩ (ngày + buổi), chờ nhân sự xếp ca. */
export interface DoctorRequestBody {
  doctorUserId: number; date: string; workShiftId: number; fullName?: string; phone?: string; symptoms?: string
}
export interface DoctorRequestResult {
  id: number; doctorUserId: number; doctorName: string; date: string; workShiftName: string
  startTime: string; endTime: string; fullName: string; phone: string; symptoms: string | null; status: string; createdAt: string
}

export const doctorRequestApi = {
  /** Khách: kèm họ tên + SĐT */
  guest: (body: DoctorRequestBody) => http.post<DoctorRequestResult>('/public/doctor-requests', body).then((r) => r.data),
  /** Bệnh nhân đã đăng nhập: họ tên + SĐT lấy từ tài khoản */
  patient: (body: DoctorRequestBody) => http.post<DoctorRequestResult>('/doctor-requests', body).then((r) => r.data),
}

export const contentApi = {
  /** Chuyên khoa đang hoạt động (A→Z), quản lý ở /admin/specialties. */
  specialties: () => http.get<{ id: number; name: string }[]>('/public/specialties').then((r) => r.data),
  /** Bác sĩ đang hoạt động (A→Z) kèm khoa và ảnh. */
  doctors: () => http.get<PublicDoctor[]>('/public/doctors').then((r) => r.data),
  homepage: () =>
    http.get<Partial<HomepageContent>>('/public/site-content/homepage')
      .then((r) => withDefaults(r.data))
      .catch(() => DEFAULT_HOMEPAGE),
  saveHomepage: (body: HomepageContent) =>
    http.put<HomepageContent>('/admin/site-content/homepage', body).then((r) => withDefaults(r.data)),
}
