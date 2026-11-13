package vn.medischedule.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import vn.medischedule.auth.domain.User;
import vn.medischedule.common.config.AppProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Tạo và đọc access token (HS256). Token chứa id, email, role. */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(AppProperties props) {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET phải dài ít nhất 32 ký tự");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.ttl = Duration.ofMinutes(props.jwt().ttlMinutes());
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().getCode())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }

    /** Trả về rỗng nếu token sai chữ ký, hết hạn hoặc sai định dạng. */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthUser(Long.valueOf(c.getSubject()), c.get("email", String.class), c.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
