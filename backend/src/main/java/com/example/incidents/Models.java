package com.example.incidents;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

enum Role {EMPLOYEE,TECHNICIAN,ADMIN}
enum Category {NETWORK,MALWARE,PHISHING,ACCESS,SOFTWARE,HARDWARE,DATA_LEAK,OTHER}
enum Priority {LOW,MEDIUM,HIGH,CRITICAL}
enum Status {OPEN,ASSIGNED,IN_PROGRESS,RESOLVED,CLOSED}
@Entity @Table(name="app_users") class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String fullName;
 @Column(unique=true,nullable=false) public String email;
 public String password;
 @Enumerated(EnumType.STRING) public Role role=Role.EMPLOYEE;
 public boolean enabled=true;
 public Instant createdAt=Instant.now();
}
@Entity class Incident {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String title;
 @Column(length=10000) public String description;
 @Enumerated(EnumType.STRING) public Category category;
 @Enumerated(EnumType.STRING) public Priority priority;
 @Enumerated(EnumType.STRING) public Status status=Status.OPEN;
 public Instant createdAt=Instant.now();
 public Instant updatedAt=Instant.now();
 @ManyToOne(fetch=FetchType.EAGER,optional=false) public User createdBy;
 @ManyToOne(fetch=FetchType.EAGER) public User assignedTo;
}
@Entity class IncidentHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) public Incident incident;
 @ManyToOne(fetch=FetchType.EAGER,optional=false) public User actor;
 public Instant createdAt=Instant.now();
 @Column(length=1500) public String action;
}
@Entity class IncidentComment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) public Incident incident;
 @ManyToOne(fetch=FetchType.EAGER,optional=false) public User author;
 public Instant createdAt=Instant.now();
 @Column(length=2000) public String content;
}
