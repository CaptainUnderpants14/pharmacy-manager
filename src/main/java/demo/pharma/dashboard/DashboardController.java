package demo.pharma.dashboard;

import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.catalog.MedicineRepository;
import demo.pharma.catalog.RecordStatus;
import demo.pharma.employee.EmployeeRepository;
import demo.pharma.employee.EmployeeStatus;
import demo.pharma.inventory.MedicineBatchRepository;
import demo.pharma.purchase.PurchaseOrderRepository;
import demo.pharma.purchase.PurchaseOrderStatus;
import demo.pharma.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final MedicineRepository medicines;
    private final EmployeeRepository employees;
    private final SupplierRepository suppliers;
    private final PurchaseOrderRepository purchases;
    private final MedicineBatchRepository batches;

    record View(long totalMedicines, long activeMedicines, long lowStockMedicines, long expiringMedicines,
            long expiredMedicines, long totalEmployees, long activeEmployees, long totalSuppliers,
            long pendingPurchaseOrders) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    View dashboard() {
        var today = LocalDate.now();
        long active = medicines.count((r, q, b) -> b.equal(r.get("status"), RecordStatus.ACTIVE));
        return new View(medicines.count(), active, 0, batches.expiring(today, today.plusDays(30)).size(),
                batches.expired(today).size(), employees.count(), employees.countByStatus(EmployeeStatus.ACTIVE),
                suppliers.count(), purchases.countByStatus(PurchaseOrderStatus.PENDING_APPROVAL));
    }
}
