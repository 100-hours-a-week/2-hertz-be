package com.hertz.hertz_be.global.config;

import com.hertz.hertz_be.global.auth.filter.JwtAuthenticationFilter;
import com.hertz.hertz_be.global.auth.handler.CustomAuthenticationEntryPoint;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll() // TODO: 추후에 프론트엔드 서버와 AI 서버 도메인만 남기기
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
