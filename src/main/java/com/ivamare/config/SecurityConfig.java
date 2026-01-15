package com.ivamare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** Security configuration for SQL Runner application. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error")
                    .permitAll()
                    .requestMatchers("/h2-console/**")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/export", "/api/import")
                    .hasRole("ADMIN")
                    .requestMatchers("/queries/*/execute/update/**")
                    .hasAnyRole("ADMIN", "UPDATE_RUNNER")
                    .requestMatchers("/queries/**")
                    .authenticated()
                    .requestMatchers("/history/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            form ->
                form.loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?error=true")
                    .permitAll())
        .logout(
            logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout=true")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll())
        .sessionManagement(
            session ->
                session
                    .invalidSessionUrl("/login?expired=true")
                    .maximumSessions(-1)
                    .expiredUrl("/login?expired=true"))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

    return http.build();
  }

  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withDefaultRolePrefix()
        .role("ADMIN")
        .implies("UPDATE_RUNNER")
        .role("UPDATE_RUNNER")
        .implies("SELECT_RUNNER")
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** In-memory users for development and testing. */
  @Bean
  @Profile({"default", "dev", "test"})
  public UserDetailsService inMemoryUserDetailsService(PasswordEncoder passwordEncoder) {
    UserDetails admin =
        User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin"))
            .roles("ADMIN")
            .build();

    UserDetails updater =
        User.builder()
            .username("updater")
            .password(passwordEncoder.encode("updater"))
            .roles("UPDATE_RUNNER")
            .build();

    UserDetails reader =
        User.builder()
            .username("reader")
            .password(passwordEncoder.encode("reader"))
            .roles("SELECT_RUNNER")
            .build();

    return new InMemoryUserDetailsManager(admin, updater, reader);
  }
}
