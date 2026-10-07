package demo.pharma.employee;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    boolean existsByEmployeeCodeIgnoreCase(String code);

    long countByStatus(EmployeeStatus status);
}
