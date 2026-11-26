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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientCode;

    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    private Relation relation = Relation.SELF;

    private String fullName;

    private LocalDate dateOfBirth;

    private String gender;

    private String nationalId;

    private String phone;

    private String address;

    private String bhytNumber;

    private LocalDate bhytValidTo;

    private Short bhytRate;

    private String allergyNote;

    @Enumerated(EnumType.STRING)
    private Source source;

    @ManyToOne(fetch = FetchType.LAZY)
    private Patient mergedInto;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
