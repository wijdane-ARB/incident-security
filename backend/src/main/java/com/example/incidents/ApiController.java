package com.example.incidents;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;
@RestController @RequestMapping("/api") class ApiController {
 private final IncidentService svc;private final UserRepo users;private final IncidentRepo incidents;private final PasswordEncoder encoder;
 ApiController(IncidentService svc,UserRepo users,IncidentRepo incidents,PasswordEncoder encoder){this.svc=svc;this.users=users;this.incidents=incidents;this.encoder=encoder;}
 @GetMapping("/profile") UserView profile(Principal p){return UserView.of(svc.current(p.getName()));}
 @PutMapping("/profile") UserView update(@Valid @RequestBody ProfileUpdate r,Principal p){User u=svc.current(p.getName());if(!u.email.equalsIgnoreCase(r.email())&&users.existsByEmailIgnoreCase(r.email()))throw new ApiException(HttpStatus.CONFLICT,"Email déjà utilisé");u.fullName=r.fullName();u.email=r.email().trim().toLowerCase();return UserView.of(users.save(u));}
 @PutMapping("/profile/password") Map<String,String> password(@Valid @RequestBody PasswordUpdate r,Principal p){User u=svc.current(p.getName());if(!encoder.matches(r.oldPassword(),u.password))throw new ApiException(HttpStatus.BAD_REQUEST,"Ancien mot de passe incorrect");if(!r.newPassword().equals(r.confirmPassword()))throw new ApiException(HttpStatus.BAD_REQUEST,"Confirmation incorrecte");u.password=encoder.encode(r.newPassword());users.save(u);return Map.of("message","Mot de passe modifié");}
 @PostMapping("/incidents") IncidentView create(@Valid @RequestBody IncidentCreate r,Principal p){return svc.create(r,p.getName());}
 @GetMapping("/incidents/my") List<IncidentView> mine(Principal p){return svc.mine(p.getName());}
 @GetMapping("/incidents/assigned") List<IncidentView> assigned(Principal p){return svc.assigned(p.getName());}
 @GetMapping("/incidents/{id}") IncidentView one(@PathVariable Long id,Principal p){return svc.one(id,p.getName());}
 @GetMapping("/incidents/{id}/activity") Map<String,Object> activity(@PathVariable Long id,Principal p){return svc.detail(id,p.getName());}
 @PutMapping("/incidents/{id}/status") IncidentView status(@PathVariable Long id,@Valid @RequestBody StatusUpdate r,Principal p){return svc.status(id,r.status(),p.getName());}
 @PutMapping("/incidents/{id}/assign/{technicianId}") IncidentView assign(@PathVariable Long id,@PathVariable Long technicianId,Principal p){return svc.assign(id,technicianId,p.getName());}
 @PostMapping("/incidents/{id}/notes") NoteView note(@PathVariable Long id,@Valid @RequestBody NoteRequest r,Principal p){return svc.note(id,r.content(),p.getName());}
 @GetMapping("/incidents") List<IncidentView> all(){return svc.all();}
 @PreAuthorize("hasRole('ADMIN')") @GetMapping("/admin/users") List<UserView> users(){return users.findAll().stream().map(UserView::of).toList();}
 @PreAuthorize("hasRole('ADMIN')") @PutMapping("/admin/users/{id}/enable") UserView enable(@PathVariable Long id){User u=users.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Utilisateur introuvable"));u.enabled=true;return UserView.of(users.save(u));}
 @PreAuthorize("hasRole('ADMIN')") @PutMapping("/admin/users/{id}/disable") UserView disable(@PathVariable Long id,Principal p){User u=users.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Utilisateur introuvable"));if(u.email.equalsIgnoreCase(p.getName()))throw new ApiException(HttpStatus.BAD_REQUEST,"Impossible de désactiver votre propre compte");u.enabled=false;return UserView.of(users.save(u));}
 @PreAuthorize("hasRole('ADMIN')") @GetMapping("/admin/statistics") Map<String,Long> stats(){return Map.of("users",users.count(),"employees",users.countByRole(Role.EMPLOYEE),"technicians",users.countByRole(Role.TECHNICIAN),"incidents",incidents.count(),"open",incidents.countByStatus(Status.OPEN),"inProgress",incidents.countByStatus(Status.IN_PROGRESS),"closed",incidents.countByStatus(Status.CLOSED));}
}
