package com.example.incidents;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional class IncidentService {
 final UserRepo users;final IncidentRepo incidents;final HistoryRepo histories;final CommentRepo comments;
 IncidentService(UserRepo users,IncidentRepo incidents,HistoryRepo histories,CommentRepo comments){this.users=users;this.incidents=incidents;this.histories=histories;this.comments=comments;}
 User current(String email){return users.findByEmailIgnoreCase(email).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Utilisateur introuvable"));}
 Incident get(Long id){return incidents.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Incident introuvable"));}
 void assertRead(Incident i,User u){if(u.role==Role.ADMIN)return;if(u.role==Role.EMPLOYEE&&i.createdBy.id.equals(u.id))return;if(u.role==Role.TECHNICIAN&&i.assignedTo!=null&&i.assignedTo.id.equals(u.id))return;throw new ApiException(HttpStatus.FORBIDDEN,"Incident non autorisé");}
 void log(Incident i,User u,String action){IncidentHistory h=new IncidentHistory();h.incident=i;h.actor=u;h.action=action;histories.save(h);}
 @PreAuthorize("hasRole('EMPLOYEE')") IncidentView create(IncidentCreate r,String email){User u=current(email);Incident i=new Incident();i.title=r.title();i.description=r.description();i.category=r.category();i.priority=r.priority();i.createdBy=u;i=incidents.save(i);log(i,u,"Incident créé");return IncidentView.of(i);}
 @Transactional(readOnly=true) IncidentView one(Long id,String email){Incident i=get(id);assertRead(i,current(email));return IncidentView.of(i);}
 @PreAuthorize("hasRole('EMPLOYEE')") @Transactional(readOnly=true) List<IncidentView> mine(String email){return incidents.findByCreatedByIdOrderByCreatedAtDesc(current(email).id).stream().map(IncidentView::of).toList();}
 @PreAuthorize("hasRole('TECHNICIAN')") @Transactional(readOnly=true) List<IncidentView> assigned(String email){return incidents.findByAssignedToIdOrderByCreatedAtDesc(current(email).id).stream().map(IncidentView::of).toList();}
 @PreAuthorize("hasRole('ADMIN')") @Transactional(readOnly=true) List<IncidentView> all(){return incidents.findAllByOrderByCreatedAtDesc().stream().map(IncidentView::of).toList();}
 @PreAuthorize("hasRole('ADMIN')") IncidentView assign(Long id,Long techId,String email){Incident i=get(id);User tech=users.findById(techId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Technicien introuvable"));if(tech.role!=Role.TECHNICIAN||!tech.enabled)throw new ApiException(HttpStatus.BAD_REQUEST,"Utilisateur non technicien ou désactivé");if(i.status==Status.CLOSED)throw new ApiException(HttpStatus.CONFLICT,"Incident fermé");i.assignedTo=tech;if(i.status==Status.OPEN)i.status=Status.ASSIGNED;i.updatedAt=java.time.Instant.now();log(i,current(email),"Affecté à "+tech.fullName);return IncidentView.of(i);}
 @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')") IncidentView status(Long id,Status status,String email){Incident i=get(id);User u=current(email);if(u.role==Role.TECHNICIAN&&(i.assignedTo==null||!i.assignedTo.id.equals(u.id)))throw new ApiException(HttpStatus.FORBIDDEN,"Incident non affecté au technicien");if(i.status==Status.CLOSED)throw new ApiException(HttpStatus.CONFLICT,"Incident fermé");Status old=i.status;i.status=status;i.updatedAt=java.time.Instant.now();log(i,u,old+" -> "+status);return IncidentView.of(i);}
 @PreAuthorize("hasRole('TECHNICIAN')") NoteView note(Long id,String content,String email){Incident i=get(id);User u=current(email);if(i.assignedTo==null||!i.assignedTo.id.equals(u.id))throw new ApiException(HttpStatus.FORBIDDEN,"Incident non affecté au technicien");if(i.status==Status.CLOSED)throw new ApiException(HttpStatus.CONFLICT,"Incident fermé");IncidentComment note=new IncidentComment();note.incident=i;note.author=u;note.content=content;note=comments.save(note);log(i,u,"Note de traitement ajoutée");return NoteView.of(note);}
 @Transactional(readOnly=true) Map<String,Object> detail(Long id,String email){Incident i=get(id);assertRead(i,current(email));return Map.of("history",histories.findByIncidentIdOrderByCreatedAtAsc(id).stream().map(HistoryView::of).toList(),"notes",comments.findByIncidentIdOrderByCreatedAtAsc(id).stream().map(NoteView::of).toList());}
}
