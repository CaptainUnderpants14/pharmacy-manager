package demo.pharma.catalog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, UUID> {
}
