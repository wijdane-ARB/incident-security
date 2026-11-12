
package com.example.incidents;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.http.*;

import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.*;

// =====================================
// SPRING SECURITY CONFIGURATION
// =====================================

@Configuration
@EnableMethodSecurity
class SecurityConfig {

 @Bean
 PasswordEncoder passwordEncoder() {
  return new BCryptPasswordEncoder();
 }

 // =====================================
 // CORS CONFIGURATION
 // =====================================

 @Bean
 CorsConfigurationSource corsConfigurationSource(
         @Value("${APP_CORS_ALLOWED_ORIGIN:}")
         String frontendOrigin
 ) {

  CorsConfiguration config = new CorsConfiguration();

  List<String> allowedOrigins = new ArrayList<>();

  allowedOrigins.add("http://localhost:5173");
  allowedOrigins.add("http://127.0.0.1:5173");

  if (frontendOrigin != null && !frontendOrigin.isBlank()) {
   String origin = frontendOrigin.trim();

   if (origin.endsWith("/")) {
    origin = origin.substring(0, origin.length() - 1);
   }

   if (!allowedOrigins.contains(origin)) {
    allowedOrigins.add(origin);
   }
  }

  config.setAllowedOrigins(allowedOrigins);

  config.setAllowedMethods(
          List.of(
                  "GET",
                  "POST",
                  "PUT",
                  "DELETE",
                  "OPTIONS"
          )
  );

  config.setAllowedHeaders(
          List.of(
                  "Authorization",
                  "Content-Type"
          )
  );

  UrlBasedCorsConfigurationSource source =
          new UrlBasedCorsConfigurationSource();

  source.registerCorsConfiguration("/**", config);

  return source;
 }

 // =====================================
 // HTTP SECURITY
 // =====================================

 @Bean
 SecurityFilterChain filterChain(
         HttpSecurity http,
         JwtFilter jwt
 ) throws Exception {

  return http

          .csrf(csrf -> csrf.disable())

          .cors(cors -> {})

          .headers(headers ->
                  headers.frameOptions(frame ->
                          frame.deny()
                  )
          )

          .sessionManagement(session ->
                  session.sessionCreationPolicy(
                          SessionCreationPolicy.STATELESS
                  )
          )

          .exceptionHandling(exceptions ->
                  exceptions

                          .authenticationEntryPoint(
                                  (req, res, ex) ->
                                          error(
                                                  res,
                                                  401,
                                                  "Authentification requise"
                                          )
                          )

                          .accessDeniedHandler(
                                  (req, res, ex) ->
                                          error(
                                                  res,
                                                  403,
                                                  "Accès interdit"
                                          )
                          )
          )

          .authorizeHttpRequests(auth ->
                  auth

                          .requestMatchers(
                                  "/auth/register",
                                  "/auth/login"
                          )
                          .permitAll()

                          .requestMatchers("/api/**")
                          .authenticated()

                          .anyRequest()
                          .denyAll()
          )

          .addFilterBefore(
                  jwt,
                  UsernamePasswordAuthenticationFilter.class
          )

          .build();
 }

 // =====================================
 // JSON ERROR RESPONSE
 // =====================================

 static void error(
         HttpServletResponse response,
         int status,
         String message
 ) throws IOException {

  response.setStatus(status);

  response.setContentType(
          "application/json;charset=UTF-8"
  );

  response.getWriter().write(
          "{\"status\":" + status +
                  ",\"message\":\"" + message + "\"}"
  );
 }
}


// =====================================
// JWT SERVICE
// =====================================

@Component
class JwtService {

 private final SecretKey key;
 private final long expiration;

 JwtService(
         @Value("${app.jwt.secret}") String secret,
         @Value("${app.jwt.expiration-ms}") long expiration
 ) {

  this.key = Keys.hmacShaKeyFor(
          Decoders.BASE64.decode(secret)
  );

  this.expiration = expiration;
 }

 String create(User user) {

  Date now = new Date();

  return Jwts.builder()

          .subject(user.email)

          .claim(
                  "role",
                  user.role.name()
          )

          .issuedAt(now)

          .expiration(
                  new Date(
                          now.getTime() + expiration
                  )
          )

          .signWith(key)

          .compact();
 }

 Claims claims(String token) {

  return Jwts.parser()

          .verifyWith(key)

          .build()

          .parseSignedClaims(token)

          .getPayload();
 }
}


// =====================================
// JWT AUTHENTICATION FILTER
// =====================================

@Component
class JwtFilter extends OncePerRequestFilter {

 private final JwtService jwt;
 private final UserRepo users;

 JwtFilter(
         JwtService jwt,
         UserRepo users
 ) {
  this.jwt = jwt;
  this.users = users;
 }

 // Skip JWT processing only for public
 // registration and login endpoints.

 @Override
 protected boolean shouldNotFilter(
         HttpServletRequest request
 ) {

  String path = request.getRequestURI();

  return path.equals("/auth/register")
          || path.equals("/auth/login");
 }

 @Override
 protected void doFilterInternal(
         HttpServletRequest request,
         HttpServletResponse response,
         FilterChain chain
 ) throws ServletException, IOException {

  String header = request.getHeader(
          "Authorization"
  );

  if (
          header != null &&
                  header.startsWith("Bearer ")
  ) {

   try {

    String token = header.substring(7);

    Claims claims = jwt.claims(token);

    String email = claims.getSubject();

    User user = users
            .findByEmailIgnoreCase(email)
            .orElseThrow();

    if (
            !user.enabled ||
                    !user.role.name().equals(
                            claims.get("role", String.class)
                    )
    ) {

     throw new IllegalArgumentException(
             "Compte désactivé ou rôle modifié"
     );
    }

    Authentication authentication =
            new UsernamePasswordAuthenticationToken(
                    user.email,
                    null,
                    List.of(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + user.role.name()
                            )
                    )
            );

    SecurityContextHolder
            .getContext()
            .setAuthentication(authentication);

   } catch (Exception ex) {

    SecurityContextHolder.clearContext();

    SecurityConfig.error(
            response,
            401,
            "JWT invalide, expiré ou compte désactivé"
    );

    return;
   }
  }

  chain.doFilter(request, response);
 }
}
