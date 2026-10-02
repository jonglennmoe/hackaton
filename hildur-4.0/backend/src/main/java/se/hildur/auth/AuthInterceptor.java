package se.hildur.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import se.hildur.common.ApiException;

/**
 * The "bouncer" in front of the API. It runs before every controller method:
 * /api/guest/** needs a GUEST token, /api/staff/** needs a STAFF token.
 * A guest token can never open a staff door, and the other way round.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** Request attribute where controllers find the logged-in {@link Session}. */
    public static final String SESSION = "hildur.session";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true; // CORS pre-flight carries no token
        }
        Session session = authService.find(bearerToken(request))
                .orElseThrow(() -> ApiException.unauthorized("notLoggedIn"));
        Session.Role required = request.getRequestURI().startsWith("/api/staff") ? Session.Role.STAFF : Session.Role.GUEST;
        if (session.role() != required) {
            throw ApiException.forbidden("wrongRole");
        }
        request.setAttribute(SESSION, session);
        return true;
    }

    public static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }
}
