package com.example.incidents;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface UserRepo extends JpaRepository<User,Long> {Optional<User> findByEmailIgnoreCase(String email);boolean existsByEmailIgnoreCase(String email);long countByRole(Role role);}
interface IncidentRepo extends JpaRepository<Incident,Long> {List<Incident> findByCreatedByIdOrderByCreatedAtDesc(Long id);List<Incident> findByAssignedToIdOrderByCreatedAtDesc(Long id);List<Incident> findAllByOrderByCreatedAtDesc();long countByStatus(Status status);}
interface HistoryRepo extends JpaRepository<IncidentHistory,Long> {List<IncidentHistory> findByIncidentIdOrderByCreatedAtAsc(Long id);}
interface CommentRepo extends JpaRepository<IncidentComment,Long> {List<IncidentComment> findByIncidentIdOrderByCreatedAtAsc(Long id);}
