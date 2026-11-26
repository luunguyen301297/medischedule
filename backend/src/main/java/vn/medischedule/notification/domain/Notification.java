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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String phone;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    private String content;

    @Enumerated(EnumType.STRING)
    private Status status = Status.QUEUED;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    private OffsetDateTime sentAt;
}
