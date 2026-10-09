package demo.pharma.customerpayment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, UUID>, JpaSpecificationExecutor<CustomerPayment> {
    boolean existsByPaymentNumberIgnoreCase(String paymentNumber);
}
