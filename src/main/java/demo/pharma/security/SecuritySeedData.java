package demo.pharma.security;

import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecuritySeedData {
    private static final List<String> PERMISSIONS = List.of("USER_VIEW", "USER_CREATE", "USER_UPDATE", "USER_DELETE",
            "EMPLOYEE_VIEW", "EMPLOYEE_CREATE", "EMPLOYEE_UPDATE", "EMPLOYEE_DELETE", "MEDICINE_VIEW",
            "MEDICINE_CREATE", "MEDICINE_UPDATE", "MEDICINE_DELETE", "BATCH_VIEW", "BATCH_CREATE", "BATCH_UPDATE",
            "STOCK_VIEW", "STOCK_ADJUST", "SUPPLIER_VIEW", "SUPPLIER_CREATE", "SUPPLIER_UPDATE", "PURCHASE_VIEW",
            "PURCHASE_CREATE", "PURCHASE_UPDATE", "PURCHASE_APPROVE", "PURCHASE_RECEIVE", "PURCHASE_CANCEL",
            "PAYMENT_VIEW", "PAYMENT_CREATE", "REPORT_VIEW", "DASHBOARD_VIEW", "AUDIT_LOG_VIEW", "SETTINGS_MANAGE");

    @Bean
    CommandLineRunner seed(RoleRepository roles, PermissionRepository permissions, UserRepository users,
            PasswordEncoder encoder, @Value("${app.initial-admin.username}") String admin,
            @Value("${app.initial-admin.password}") String password,
            @Value("${app.initial-admin.email}") String email) {
        return args -> {
            for (String p : PERMISSIONS)
                permissions.findByCode(p).orElseGet(() -> permissions.save(new Permission(p, p.replace('_', ' '))));
            var all = new HashSet<>(permissions.findAll());
            for (String n : List.of("OWNER", "ADMIN", "PHARMACIST", "CASHIER", "INVENTORY_MANAGER", "ACCOUNTANT",
                    "STAFF")) {
                Role role = roles.findByName(n).orElseGet(() -> roles.save(new Role(n)));
                if (n.equals("OWNER") && role.getPermissions().isEmpty()) {
                    role.setPermissions(all);
                    roles.save(role);
                }
            }
            if (!admin.isBlank() && !password.isBlank() && !users.existsByUsernameIgnoreCase(admin)) {
                AppUser u = new AppUser();
                u.setUsername(admin);
                u.setEmail(email.isBlank() ? admin + "@local.invalid" : email);
                u.setPassword(encoder.encode(password));
                u.getRoles().add(roles.findByName("OWNER").orElseThrow());
                users.save(u);
            }
        };
    }
}
