package yea.ecomservapi.modules.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import yea.ecomservapi.modules.auth.domain.User;
import yea.ecomservapi.modules.auth.dto.AuthRequest;
import yea.ecomservapi.modules.auth.dto.AuthResponse;
import yea.ecomservapi.modules.auth.dto.RegisterRequest;
import yea.ecomservapi.modules.auth.service.AuthService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints públicos para autenticación")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario y devuelve un token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("=== Login request recibido para usuario: {} ===", request.getUsername());
        AuthResponse response = authService.login(request);
        log.info("=== Login exitoso para usuario: {} ===", request.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario en el sistema")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("=== Register request recibido para usuario: {} ===", request.getUsername());
        AuthResponse response = authService.register(request);
        log.info("=== Registro exitoso para usuario: {} ===", request.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    @Operation(summary = "Actualizar perfil", description = "Actualiza el nombre del usuario autenticado")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<AuthResponse> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {
        String newName = body.get("name");
        String newUsername = body.get("username");
        log.info("=== Profile update para usuario: {} -> nombre: {}, username: {} ===", currentUser.getUsername(), newName, newUsername);
        AuthResponse response = authService.updateProfile(currentUser, newName, newUsername);
        return ResponseEntity.ok(response);
    }
}
