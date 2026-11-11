package com.example.incidents;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
class ApiException extends RuntimeException {final HttpStatus status;ApiException(HttpStatus status,String message){super(message);this.status=status;}}
@RestControllerAdvice class ApiError {
 @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException ex){return ResponseEntity.status(ex.status).body(Map.of("status",ex.status.value(),"message",ex.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalid(MethodArgumentNotValidException ex){return ResponseEntity.badRequest().body(Map.of("status",400,"message",ex.getBindingResult().getFieldErrors().stream().map(e->e.getField()+": "+e.getDefaultMessage()).toList()));}
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<?> denied(AccessDeniedException ex){return ResponseEntity.status(403).body(Map.of("status",403,"message","Accès interdit"));}
 @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class) ResponseEntity<?> malformed(Exception ex){return ResponseEntity.badRequest().body(Map.of("status",400,"message","JSON ou valeur enum invalide"));}
}
