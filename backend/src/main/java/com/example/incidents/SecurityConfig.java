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
import java.time.*;

@Configuration @EnableMethodSecurity
class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean CorsConfigurationSource corsConfigurationSource(){CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(List.of("http://localhost:5173","http://127.0.0.1:5173"));c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("Authorization","Content-Type"));UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;}
 @Bean SecurityFilterChain filterChain(HttpSecurity http,JwtFilter jwt) throws Exception {
  return http.csrf(c->c.disable()).cors(c->{}).headers(h->h.frameOptions(f->f.sameOrigin()))
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->error(res,401,"Authentification requise"))
    .accessDeniedHandler((req,res,ex)->error(res,403,"Accès interdit")))
   .authorizeHttpRequests(a->a.requestMatchers("/auth/**","/h2-console/**").permitAll().requestMatchers("/api/**").authenticated().anyRequest().denyAll())
   .addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class).build();
 }
 static void error(HttpServletResponse res,int status,String message)throws IOException {res.setStatus(status);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"status\":"+status+",\"message\":\""+message+"\"}");}
}
@Component class JwtService {
 private final SecretKey key; private final long expiration;
 JwtService(@Value("${app.jwt.secret}") String secret,@Value("${app.jwt.expiration-ms}") long expiration){this.key=Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));this.expiration=expiration;}
 String create(User u){Date now=new Date();return Jwts.builder().subject(u.email).claim("role",u.role.name()).issuedAt(now).expiration(new Date(now.getTime()+expiration)).signWith(key).compact();}
 Claims claims(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();}
}
@Component class JwtFilter extends OncePerRequestFilter {
 private final JwtService jwt; private final UserRepo users;
 JwtFilter(JwtService jwt,UserRepo users){this.jwt=jwt;this.users=users;}
 @Override protected boolean shouldNotFilter(HttpServletRequest req){String p=req.getRequestURI();return p.startsWith("/auth/")||p.startsWith("/h2-console");}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String header=req.getHeader("Authorization");
  if(header!=null&&header.startsWith("Bearer ")){
   try{Claims claims=jwt.claims(header.substring(7));String email=claims.getSubject();User u=users.findByEmailIgnoreCase(email).orElseThrow();
    if(!u.enabled||!u.role.name().equals(claims.get("role",String.class)))throw new IllegalArgumentException("Compte désactivé ou rôle modifié");
    Authentication auth=new UsernamePasswordAuthenticationToken(u.email,null,List.of(new SimpleGrantedAuthority("ROLE_"+u.role.name())));SecurityContextHolder.getContext().setAuthentication(auth);
   }catch(Exception ex){SecurityContextHolder.clearContext();SecurityConfig.error(res,401,"JWT invalide, expiré ou compte désactivé");return;}
  }
  chain.doFilter(req,res);
 }
}
