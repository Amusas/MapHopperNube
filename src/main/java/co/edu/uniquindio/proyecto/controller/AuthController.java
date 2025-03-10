package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.service.auth.VerificationService;
import co.edu.uniquindio.proyecto.service.implementations.UserServiceImplements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final VerificationService verificationService;
    private final UserServiceImplements userService;

    @GetMapping("/sessions")
    public ResponseEntity<String> verifyAccount(@RequestParam String token) {
        log.info("Solicitud de verificación de cuenta...");
        verificationService.verifyToken(token);
        return ResponseEntity.ok("Cuenta verificada exitosamente");
    }

}
