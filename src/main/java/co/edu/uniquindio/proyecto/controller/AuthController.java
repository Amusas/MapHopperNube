package co.edu.uniquindio.proyecto.controller;

import co.edu.uniquindio.proyecto.dto.response.SuccessResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import co.edu.uniquindio.proyecto.dto.user.*;
import co.edu.uniquindio.proyecto.entity.auth.VerificationCodeType;
import co.edu.uniquindio.proyecto.service.interfaces.AuthService;
import co.edu.uniquindio.proyecto.service.interfaces.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import java.time.Duration;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador que gestiona los endpoints relacionados con autenticación,
 * verificación de cuentas y recuperación de contraseñas.
 */
@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final VerificationService verificationService;
    private final AuthService authService;

    /**
     * Verifica la cuenta del usuario mediante el código recibido por correo electrónico.
     *
     * @param code Código de verificación enviado previamente al correo del usuario.
     * @return Respuesta con mensaje de éxito si el código es válido.
     */
    @PatchMapping("/activations")
    public ResponseEntity<String> verifyAccount(@RequestParam String code) {
        log.info("🛂 Verificando cuenta con código: {}", code);
        verificationService.validateCodeActivation(code);
        return ResponseEntity.ok("Cuenta verificada exitosamente");
    }

    /**
     * Reenvía un nuevo código de activación al usuario especificado por su ID.
     *
     * @param userId ID del usuario que solicita un nuevo código.
     * @return HTTP 204 si el código fue enviado exitosamente.
     */
    @PostMapping("/activations/{userId}")
    public ResponseEntity<Void> sendCodeAgain(@PathVariable String userId) {
        log.info("📩 Reenviando código de activación al usuario con ID: {}", userId);
        verificationService.resendCode(userId, VerificationCodeType.ACTIVATION);
        return ResponseEntity.noContent().build();
    }

    /**
     * Realiza el proceso de autenticación del usuario, devolviendo un token JWT si es exitoso.
     *
     * @param request Objeto con las credenciales de inicio de sesión.
     * @return JWT válido para sesiones autenticadas.
     */
    @PostMapping("/sessions")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔐 Iniciando sesión para el usuario: {}", request.userName());

        // Ejecutar la autenticación y obtener los tokens
        JwtResponse jwtResponse = authService.authenticate(request);
        log.info("✅ Autenticación exitosa para el usuario: {}", request.userName());

        // Crear cookies para access y refresh
        ResponseCookie accessTokenCookie  = buildCookie("access_token",  jwtResponse.token(),        Duration.ofHours(1));
        ResponseCookie refreshTokenCookie = buildCookie("refresh_token", jwtResponse.refreshToken(), Duration.ofDays(7));
        log.debug("Cookies generadas: access_token ({}s), refresh_token ({}s)",
                  accessTokenCookie.getMaxAge(), refreshTokenCookie.getMaxAge());

        // Devolver respuesta con headers Set-Cookie
        return ResponseEntity.ok()
                             .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                             .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                             .body(jwtResponse);
    }


    /**
     * Solicita un código de recuperación de contraseña para el correo electrónico dado.
     *
     * @param email Correo del usuario que desea recuperar su contraseña.
     * @return HTTP 204 si el correo fue enviado correctamente.
     */
    @PostMapping("/passwordCodes")
    public ResponseEntity<Void> requestPasswordReset(@RequestParam String email) {
        log.info("🔁 Solicitud de código de recuperación para: {}", email);
        verificationService.sendPasswordResetCode(email);
        return ResponseEntity.noContent().build();
    }

    /**
     * Confirma la recuperación de contraseña usando un código enviado por correo electrónico.
     *
     * @param request DTO que contiene el código de validación y la nueva contraseña.
     * @return Respuesta indicando que la contraseña fue actualizada exitosamente.
     */
    @PatchMapping("/users/password")
    public ResponseEntity<SuccessResponse> confirmReset(@Valid @RequestBody PasswordResetRequest request) {
        log.info("🔄 Confirmando restablecimiento de contraseña con código: {}", request.code());
        verificationService.resetPasswordWithCode(request.code(), request.newPassword());
        return ResponseEntity.ok(new SuccessResponse("Contraseña actualizada exitosamente"));
    }


    /**
     * Endpoint para refrescar el token de acceso mediante refresh token.
     *
     * @param refreshToken Objeto que contiene el refresh token.
     * @return Un {@link JwtResponse} con el nuevo token de acceso.
     */
    @PostMapping("/accessTokens")
    public ResponseEntity<JwtAccessResponse> refreshToken(
        @CookieValue(name = "refresh_token", required = true) String refreshToken) {

    log.info("Recibida solicitud de refresco de token.");

    JwtAccessResponse response = authService.refreshAccessToken(refreshToken);
    return ResponseEntity.ok(response);
}



      /**
     * Construye una {@link ResponseCookie} con las siguientes propiedades:
     * <ul>
     *   <li>HttpOnly: true</li>
     *   <li>Secure: true</li>
     *   <li>Path: "/"</li>
     *   <li>SameSite: Strict</li>
     *   <li>Max-Age: según parámetro</li>
     * </ul>
     *
     * @param name   Nombre de la cookie.
     * @param value  Valor de la cookie.
     * @param maxAge Duración de la cookie.
     * @return ResponseCookie ya configurada.
     */
    private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
        log.debug("🔧 Construyendo cookie '{}', duración {} segundos", name, maxAge.getSeconds());
        return ResponseCookie.from(name, value)
                             .httpOnly(true)
                             .secure(true)
                             .path("/")
                             .sameSite("Strict")
                             .maxAge(maxAge)
                             .build();
    }

}

