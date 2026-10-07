package demo.pharma.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    public UserDetails loadUserByUsername(String value) {
        AppUser u = users.findByUsernameIgnoreCaseOrEmailIgnoreCase(value, value)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
        return User.withUsername(u.getUsername()).password(u.getPassword()).disabled(!u.isEnabled())
                .accountLocked(u.isAccountLocked())
                .authorities(u.getRoles().stream().flatMap(r -> r.getPermissions().stream())
                        .map(p -> new SimpleGrantedAuthority(p.getCode())).toList())
                .build();
    }
}
