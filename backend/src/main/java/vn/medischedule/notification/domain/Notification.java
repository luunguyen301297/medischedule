package vn.medischedule.notification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** Hàng đợi gửi Push / Zalo / SMS; phone dùng cho khách không có tài khoản. */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    public enum Channel { PUSH, ZALO, SMS }

    public enum Status { QUEUED, SENT, FAILED }

    /** Khóa chính. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** users.id người nhận; NULL khi gửi cho khách không có tài khoản. */
    private Long userId;

    /** SĐT nhận (Zalo/SMS); ít nhất một trong userId hoặc phone. */
    private String phone;

    /** PUSH (FCM), ZALO, SMS. */
    @Enumerated(EnumType.STRING)
    private Channel channel;

    /** Nội dung đã dựng sẵn: mã lịch, sắp đến lượt, nhắc thuốc, link khảo sát... */
    private String content;

    /** QUEUED (chờ gửi), SENT, FAILED. */
    @Enumerated(EnumType.STRING)
    private Status status = Status.QUEUED;

    /** Thời điểm xếp hàng. */
    @CreationTimestamp
    private OffsetDateTime createdAt;

    /** Thời điểm gửi thành công. */
    private OffsetDateTime sentAt;
}
