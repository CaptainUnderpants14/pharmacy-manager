package demo.pharma.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final CustomUserDetailsService details;

    public JwtAuthenticationFilter(JwtService j, CustomUserDetailsService d) {
        jwt = j;
        details = d;
    }

    protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c)
            throws ServletException, IOException {
        String h = r.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            c.doFilter(r, s);
            return;
        }
        try {
            String token = h.substring(7);
            String username = jwt.username(token);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = details.loadUserByUsername(username);
                if (jwt.valid(token, user)) {
                    var a = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(a);
                }
            }
        } catch (Exception ignored) {
        }
        c.doFilter(r, s);
    }
}
