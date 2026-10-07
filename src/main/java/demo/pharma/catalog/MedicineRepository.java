package demo.pharma.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicineRepository extends JpaRepository<Medicine, UUID>, JpaSpecificationExecutor<Medicine> {
    boolean existsByMedicineCodeIgnoreCase(String medicineCode);

    @Query("select m from Medicine m where lower(m.name) like lower(concat('%',:q,'%')) or lower(coalesce(m.genericName,'')) like lower(concat('%',:q,'%')) or lower(coalesce(m.brandName,'')) like lower(concat('%',:q,'%')) or lower(m.medicineCode) like lower(concat('%',:q,'%'))")
    List<Medicine> search(@Param("q") String q);
}
