package demo.pharma.inventory;

import demo.pharma.catalog.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventory;
    private final MedicineBatchRepository batches;
    private final MedicineRepository medicines;

    record Adjustment(@NotNull UUID batchId, @NotNull @Positive BigDecimal quantity, @NotNull MovementType type,
            @NotBlank String reason) {
    }

    record BatchView(UUID id, UUID medicineId, String batchNumber, LocalDate expiryDate, BigDecimal quantity) {
    }

    record Summary(long lowStock, long expiring, long expired) {
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    BatchView adjust(@Valid @RequestBody Adjustment r) {
        return view(inventory.adjust(r.batchId(), r.quantity(), r.type(), r.reason()));
    }

    @GetMapping("/expired")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    List<BatchView> expired() {
        return batches.expired(LocalDate.now()).stream().map(this::view).toList();
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    List<BatchView> expiring(@RequestParam(defaultValue = "30") @Min(1) int days) {
        return batches.expiring(LocalDate.now(), LocalDate.now().plusDays(days)).stream().map(this::view).toList();
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    List<UUID> low() {
        return medicines.findAll().stream()
                .filter(m -> batches.findAvailableFefo(m.getId(), LocalDate.now()).stream()
                        .map(MedicineBatch::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .compareTo(m.getReorderLevel()) < 0)
                .map(Medicine::getId).toList();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    Summary summary() {
        long low = low().size(), exp = expiring(30).size(), expired = expired().size();
        return new Summary(low, exp, expired);
    }

    @GetMapping("/fefo/{medicineId}")
    @PreAuthorize("hasAuthority('STOCK_VIEW')")
    List<BatchView> fefo(@PathVariable UUID medicineId) {
        return inventory.selectFefo(medicineId).stream().map(this::view).toList();
    }

    private BatchView view(MedicineBatch b) {
        return new BatchView(b.getId(), b.getMedicine().getId(), b.getBatchNumber(), b.getExpiryDate(),
                b.getQuantity());
    }
}
