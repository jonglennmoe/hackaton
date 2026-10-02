package se.hildur.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login and logout. These endpoints are open – they are how you get a token. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record GuestLogin(@NotBlank String bookingNumber, @NotBlank String lastName) {
    }

    public record StaffLogin(@NotBlank String username, @NotBlank String pin) {
    }

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/guest")
    public AuthService.LoginResult guest(@Valid @RequestBody GuestLogin body) {
        return authService.loginGuest(body.bookingNumber(), body.lastName());
    }

    @PostMapping("/staff")
    public AuthService.LoginResult staff(@Valid @RequestBody StaffLogin body) {
        return authService.loginStaff(body.username(), body.pin());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = AuthInterceptor.bearerToken(request);
        if (token != null) {
            authService.logout(token);
        }
        return ResponseEntity.noContent().build();
    }
}
