package demo.pharma.auth;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.audit.AuditService;
import demo.pharma.security.AppUser;
import demo.pharma.security.JwtService;
import demo.pharma.security.Role;
import demo.pharma.security.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final JwtService jwt;
    private final AuditService audit;

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record UserView(UUID id, String username, String employeeId, List<String> roles, List<String> permissions) {
    }

    record LoginResponse(String token, String tokenType, long expiresIn, UserView user) {
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUser u = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(request.username(), request.username())
                .orElseThrow();
        u.setLastLoginAt(Instant.now());
        users.save(u);
        audit.log(u, "LOGIN", "USER", u.getId(), null, null, http.getRemoteAddr());
        return new LoginResponse(jwt.generate(u), "Bearer", jwt.expiration(), view(u));
    }

    @GetMapping("/me")
    public UserView me(Authentication a) {
        AppUser u = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(a.getName(), a.getName()).orElseThrow();
        return view(u);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
    }

    private UserView view(AppUser u) {
        return new UserView(u.getId(), u.getUsername(), null,
                u.getRoles().stream().map(Role::getName).sorted().toList(),
                u.getRoles().stream().flatMap(role -> role.getPermissions().stream()).map(p -> p.getCode()).distinct().sorted().toList());
    }
}
