package com.example.incidents;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/auth") class AuthController {
 private final UserRepo users;private final PasswordEncoder encoder;private final JwtService jwt;
 AuthController(UserRepo users,PasswordEncoder encoder,JwtService jwt){this.users=users;this.encoder=encoder;this.jwt=jwt;}
 @PostMapping("/register") UserView register(@Valid @RequestBody RegisterRequest r){String email=r.email().trim().toLowerCase();if(users.existsByEmailIgnoreCase(email))throw new ApiException(HttpStatus.CONFLICT,"Email déjà utilisé");User u=new User();u.fullName=r.fullName().trim();u.email=email;u.password=encoder.encode(r.password());u.role=Role.EMPLOYEE;return UserView.of(users.save(u));}
 @PostMapping("/login") LoginResponse login(@Valid @RequestBody LoginRequest r){User u=users.findByEmailIgnoreCase(r.email()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Identifiants invalides"));if(!u.enabled||!encoder.matches(r.password(),u.password))throw new ApiException(HttpStatus.UNAUTHORIZED,"Identifiants invalides ou compte désactivé");return new LoginResponse(jwt.create(u),u.role.name(),u.fullName);}
}
