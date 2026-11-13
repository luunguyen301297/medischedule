package vn.medischedule.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vn.medischedule.common.config.AppProperties;

import java.util.List;

/**
 * Stateless JWT. Phân quyền theo vai trò:
 *  - URL chung ở đây (public / cần đăng nhập)
 *  - Quyền chi tiết đặt bằng @PreAuthorize("hasRole('...')") trên controller.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC = {
            "/api/auth/login", "/api/auth/register", "/api/public/**",
            "/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final AppProperties props;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writeError(res, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Chưa đăng nhập hoặc phiên đã hết hạn"))
                        .accessDeniedHandler((req, res, ex) -> writeError(res, HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource corsSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.corsOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private static void writeError(jakarta.servlet.http.HttpServletResponse res, HttpStatus status, String code, String message)
            throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
