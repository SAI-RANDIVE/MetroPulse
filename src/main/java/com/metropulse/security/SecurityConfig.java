package com.metropulse.security;

import com.metropulse.enums.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/**
 * Two security filter chains:
 * <ol>
 *     <li><b>/api/**</b> - JSON API for programs. Stateless: HTTP Basic on every request, no session cookie is
 *     read or created, so CSRF protection is not needed there (a browser's login cookie cannot be abused).</li>
 *     <li><b>everything else</b> - the website. Form login with a session, CSRF tokens on every form.</li>
 * </ol>
 * Role checks: commuters may submit crowd reports; only administrators reach /admin and /api/admin.
 */
@Configuration
public class SecurityConfig {

    private static final String ADMIN = Role.ADMIN.name();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurity(HttpSecurity http, JsonSecurityErrorHandler errors) throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole(ADMIN)
                        .requestMatchers("/api/crowd-reports/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/lines/**", "/api/stations/**", "/api/routes/**", "/api/alerts/**").permitAll()
                        .anyRequest().denyAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(errors))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/vendor/**", "/img/**", "/favicon.svg").permitAll()
                        .requestMatchers("/", "/plan", "/routes", "/stations/**", "/lines/**", "/alerts/**",
                                "/login", "/register", "/about", "/error", "/error/**").permitAll()
                        .requestMatchers("/admin/**").hasRole(ADMIN)
                        .requestMatchers("/reports/**").authenticated()
                        .anyRequest().permitAll()) // unknown paths reach the 404 page
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?signedOut")
                        .deleteCookies("JSESSIONID"))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; font-src 'self'; frame-ancestors 'none'; form-action 'self'"))
                        .referrerPolicy(ref -> ref.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }
}
