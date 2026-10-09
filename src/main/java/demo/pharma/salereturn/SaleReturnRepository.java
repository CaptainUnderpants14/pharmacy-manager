package demo.pharma.salereturn;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, UUID>, JpaSpecificationExecutor<SaleReturn> {
    boolean existsByReturnNumberIgnoreCase(String returnNumber);
}
