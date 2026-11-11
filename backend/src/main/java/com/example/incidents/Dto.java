package com.example.incidents;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
record RegisterRequest(@NotBlank @Size(max=255) String fullName,@NotBlank @Email @Size(max=255) String email,@NotBlank @Size(min=8,max=100) String password){}
record LoginRequest(@NotBlank String email,@NotBlank String password){}
record LoginResponse(String token,String role,String fullName){}
record ProfileUpdate(@NotBlank @Size(max=255) String fullName,@NotBlank @Email @Size(max=255) String email){}
record PasswordUpdate(@NotBlank String oldPassword,@NotBlank @Size(min=8,max=100) String newPassword,@NotBlank String confirmPassword){}
record IncidentCreate(@NotBlank @Size(max=255) String title,@NotBlank @Size(max=10000) String description,@NotNull Category category,@NotNull Priority priority){}
record StatusUpdate(@NotNull Status status){}
record NoteRequest(@NotBlank @Size(max=2000) String content){}
record UserView(Long id,String fullName,String email,Role role,boolean enabled,Instant createdAt){static UserView of(User u){return new UserView(u.id,u.fullName,u.email,u.role,u.enabled,u.createdAt);}}
record IncidentView(Long id,String title,String description,Category category,Priority priority,Status status,Instant createdAt,Instant updatedAt,Long createdById,String createdByName,Long assignedToId,String assignedToName){static IncidentView of(Incident i){return new IncidentView(i.id,i.title,i.description,i.category,i.priority,i.status,i.createdAt,i.updatedAt,i.createdBy.id,i.createdBy.fullName,i.assignedTo==null?null:i.assignedTo.id,i.assignedTo==null?null:i.assignedTo.fullName);}}
record HistoryView(Long id,String action,String actor,Instant createdAt){static HistoryView of(IncidentHistory h){return new HistoryView(h.id,h.action,h.actor.fullName,h.createdAt);}}
record NoteView(Long id,String content,String author,Instant createdAt){static NoteView of(IncidentComment n){return new NoteView(n.id,n.content,n.author.fullName,n.createdAt);}}
