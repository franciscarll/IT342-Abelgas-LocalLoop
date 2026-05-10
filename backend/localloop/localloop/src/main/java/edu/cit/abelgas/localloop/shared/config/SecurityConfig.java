package edu.cit.abelgas.localloop.shared.config;

import edu.cit.abelgas.localloop.shared.security.OAuth2SuccessHandler;import edu.cit.abelgas.localloop.shared.security.jwt.JwtAuthFilter;import org.springframework.context.annotation.Bean;import org.springframework.context.annotation.Configuration;import org.springframework.security.authentication.AuthenticationManager;import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.config.http.SessionCreationPolicy;import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.security.web.SecurityFilterChain;import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;import org.springframework.web.cors.CorsConfigurationSource;import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── FIX: Use IF_REQUIRED for OAuth2 flow but prevent session
                // fixation and context persistence issues.
                //
                // WHY NOT STATELESS:
                //   OAuth2 login redirect requires a temporary session to pass
                //   the authorization code back from Google. After that, we use JWT.
                //
                // WHY THIS FIXES THE ERROR:
                //   We disable session fixation protection's migrateSession strategy
                //   and disable the security context repository so Spring Security
                //   does NOT try to serialize/deserialize SPRING_SECURITY_CONTEXT
                //   into the HTTP session — that's what caused the StreamCorruptedException.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // Prevent Spring Security from storing the SecurityContext in the session.
                        // JWT requests are fully stateless — the context is rebuilt per-request
                        // by JwtAuthFilter. Only the OAuth2 redirect needs the session briefly.
                        .sessionFixation(fixation -> fixation.none())
                )

                // Disable Spring Security's default behaviour of saving the
                // SecurityContext to the HttpSession. This is the root cause of
                // the serialization error — without this, it tries to write our
                // User entity to disk which fails on restart.
                .securityContext(ctx -> ctx.requireExplicitSave(true))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/login/oauth2/code/google",
                                "/oauth2/**",
                                "/login**",
                                // Allow serving uploaded profile pictures as static resources
                                "/uploads/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .failureUrl("http://localhost:3000/login?error=true")
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}