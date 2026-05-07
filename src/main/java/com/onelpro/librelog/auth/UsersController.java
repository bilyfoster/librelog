package com.onelpro.librelog.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public record UserDto(String id, String email, String name, String role, boolean active) {
        static UserDto from(AppUser u) {
            return new UserDto(u.getId().toString(), u.getEmail(), u.getName(), u.getRole(), u.isActive());
        }
    }

    public record CreateUserRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String password,
            String name,
            @NotBlank String role
    ) {}

    public record UpdateUserRequest(String name, String role, Boolean active, String password) {}

    @GetMapping
    public List<UserDto> list() {
        return users.findAll().stream().map(UserDto::from).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserRequest req) {
        if (users.existsByEmail(req.email().toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }
        if (!"ADMIN".equals(req.role()) && !"EDITOR".equals(req.role())) {
            return ResponseEntity.badRequest().body(Map.of("error", "role must be ADMIN or EDITOR"));
        }
        AppUser u = AppUser.builder()
                .email(req.email().toLowerCase())
                .passwordHash(encoder.encode(req.password()))
                .name(req.name())
                .role(req.role())
                .active(true)
                .build();
        return ResponseEntity.ok(UserDto.from(users.save(u)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody UpdateUserRequest req) {
        var u = users.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        if (req.name() != null) u.setName(req.name());
        if (req.role() != null) {
            if (!"ADMIN".equals(req.role()) && !"EDITOR".equals(req.role())) {
                return ResponseEntity.badRequest().body(Map.of("error", "role must be ADMIN or EDITOR"));
            }
            u.setRole(req.role());
        }
        if (req.active() != null) u.setActive(req.active());
        if (req.password() != null && !req.password().isBlank()) {
            u.setPasswordHash(encoder.encode(req.password()));
        }
        return ResponseEntity.ok(UserDto.from(users.save(u)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!users.existsById(id)) return ResponseEntity.notFound().build();
        users.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
