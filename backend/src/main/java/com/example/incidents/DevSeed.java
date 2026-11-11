package com.example.incidents;
import org.springframework.boot.*;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.env.Environment;
@Configuration class DevSeed {
 // Demo accounts only. Switch off with DEMO_SEED=false outside local coursework testing.
 @Bean CommandLineRunner seed(UserRepo users,PasswordEncoder encoder,Environment env){return args->{if(!Boolean.parseBoolean(env.getProperty("DEMO_SEED","true")))return;
  create(users,encoder,"admin@demo.local","Admin Demo",Role.ADMIN);
  create(users,encoder,"tech@demo.local","Technicien Demo",Role.TECHNICIAN);
 };}
 void create(UserRepo repo,PasswordEncoder enc,String email,String name,Role role){if(repo.existsByEmailIgnoreCase(email))return;User u=new User();u.email=email;u.fullName=name;u.role=role;u.password=enc.encode("Demo12345!");repo.save(u);}
}
