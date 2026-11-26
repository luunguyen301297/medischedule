package vn.medischedule.patient.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Hồ sơ người đi khám; khách đặt bằng SĐT thì ownerUserId NULL tới khi đăng ký. */
@Entity
@Table(name = "patient")
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    public enum Relation { SELF, CHILD, PARENT, OTHER }

    public enum Source { GUEST, APP, RECEPTION }

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã bệnh nhân in trên giấy tờ, duy nhất (vd. BN000123). */
    private String patientCode;

    /** users.id của tài khoản quản lý hồ sơ; NULL với khách đặt bằng SĐT chưa đăng ký. */
    private Long ownerUserId;

    /** Quan hệ với chủ tài khoản; mỗi tài khoản tối đa một hồ sơ SELF. */
    @Enumerated(EnumType.STRING)
    private Relation relation = Relation.SELF;

    /** Họ tên người đi khám. */
    private String fullName;

    /** Ngày sinh. */
    private LocalDate dateOfBirth;

    /** Giới tính. */
    private String gender;

    /** Số CCCD, duy nhất. */
    private String nationalId;

    /** SĐT liên hệ; dùng tìm hồ sơ khi check-in và phát hiện hồ sơ trùng. */
    private String phone;

    /** Địa chỉ. */
    private String address;

    /** Số thẻ BHYT. */
    private String bhytNumber;

    /** Hạn thẻ BHYT. */
    private LocalDate bhytValidTo;

    /** Mức hưởng BHYT (%): 80, 95 hoặc 100. */
    private Short bhytRate;

    /** Dị ứng (dạng chữ); hiện cảnh báo khi bác sĩ kê đơn. */
    private String allergyNote;

    /** Nơi tạo hồ sơ: GUEST (khách đặt bằng SĐT), APP, RECEPTION (lễ tân). */
    @Enumerated(EnumType.STRING)
    private Source source;

    /** Hồ sơ chính mà hồ sơ trùng này đã được gộp vào; NULL nếu chưa gộp. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Patient mergedInto;

    /** Thời điểm tạo hồ sơ. */
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
