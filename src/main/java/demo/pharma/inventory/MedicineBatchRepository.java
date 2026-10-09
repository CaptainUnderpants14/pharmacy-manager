package demo.pharma.inventory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, UUID>, JpaSpecificationExecutor<MedicineBatch> {
    java.util.Optional<MedicineBatch> findByMedicineIdAndBatchNumber(UUID medicineId, String batchNumber);

    @Query("select b from MedicineBatch b where b.medicine.id=:medicineId and b.expiryDate > :today and b.quantity>b.reservedQuantity order by b.expiryDate")
    List<MedicineBatch> findAvailableFefo(@Param("medicineId") UUID id, @Param("today") LocalDate today);

    @Query("select b from MedicineBatch b where b.expiryDate < :today")
    List<MedicineBatch> expired(@Param("today") LocalDate today);

    @Query("select b from MedicineBatch b where b.expiryDate between :today and :until")
    List<MedicineBatch> expiring(@Param("today") LocalDate today, @Param("until") LocalDate until);
}
