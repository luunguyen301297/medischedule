package vn.medischedule.auth.domain;

/**
 * Mã vai trò, khớp cột roles.code.
 * Dùng trong controller: @PreAuthorize("hasRole('" + RoleCode.ADMIN + "')")
 */
public final class RoleCode {
    public static final String ADMIN = "ADMIN";
    public static final String DOCTOR = "DOCTOR";
    public static final String RECEPTIONIST = "RECEPTIONIST";
    public static final String PHARMACIST = "PHARMACIST";
    public static final String PATIENT = "PATIENT";

    private RoleCode() {}
}
