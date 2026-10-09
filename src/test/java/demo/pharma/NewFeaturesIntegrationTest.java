package demo.pharma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import demo.pharma.analytics.AnalyticsController;
import demo.pharma.branch.Branch;
import demo.pharma.branch.BranchRepository;
import demo.pharma.catalog.Category;
import demo.pharma.catalog.CategoryRepository;
import demo.pharma.catalog.DosageForm;
import demo.pharma.catalog.Manufacturer;
import demo.pharma.catalog.ManufacturerRepository;
import demo.pharma.catalog.Medicine;
import demo.pharma.catalog.MedicineRepository;
import demo.pharma.customer.Customer;
import demo.pharma.customer.CustomerRepository;
import demo.pharma.customerpayment.CustomerPaymentController;
import demo.pharma.expense.ExpenseCategory;
import demo.pharma.expense.ExpenseCategoryRepository;
import demo.pharma.expense.ExpenseController;
import demo.pharma.inventory.MedicineBatch;
import demo.pharma.inventory.MedicineBatchRepository;
import demo.pharma.notification.Notification;
import demo.pharma.notification.NotificationService;
import demo.pharma.notification.NotificationType;
import demo.pharma.payment.PaymentMethod;
import demo.pharma.prescription.Prescription;
import demo.pharma.prescription.PrescriptionRepository;
import demo.pharma.purchasereturn.PurchaseReturn;
import demo.pharma.purchasereturn.PurchaseReturnService;
import demo.pharma.purchasereturn.PurchaseReturnStatus;
import demo.pharma.report.ReportController;
import demo.pharma.sale.Sale;
import demo.pharma.sale.SaleService;
import demo.pharma.supplier.Supplier;
import demo.pharma.supplier.SupplierRepository;

@SpringBootTest
@Transactional
public class NewFeaturesIntegrationTest {

    @Autowired private BranchRepository branchRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private PrescriptionRepository prescriptionRepo;
    @Autowired private CategoryRepository categoryRepo;
    @Autowired private ManufacturerRepository manufacturerRepo;
    @Autowired private SupplierRepository supplierRepo;
    @Autowired private MedicineRepository medicineRepo;
    @Autowired private MedicineBatchRepository batchRepo;
    @Autowired private SaleService saleService;
    @Autowired private PurchaseReturnService purchaseReturnService;
    @Autowired private CustomerPaymentController customerPaymentController;
    @Autowired private ExpenseCategoryRepository expenseCategoryRepo;
    @Autowired private ExpenseController expenseController;
    @Autowired private NotificationService notificationService;
    @Autowired private ReportController reportController;
    @Autowired private AnalyticsController analyticsController;

    @Test
    void testCompletePharmacyWorkflow() {
        // 1. Create Branch
        Branch branch = new Branch();
        branch.setCode("BR-TEST-01");
        branch.setName("Test Branch");
        branch.setMain(true);
        branchRepo.save(branch);
        assertNotNull(branch.getId());

        // 2. Create Customer
        Customer customer = new Customer();
        customer.setCustomerCode("CUST-100");
        customer.setName("John Doe");
        customer.setPhone("9876543210");
        customerRepo.save(customer);
        assertNotNull(customer.getId());

        // 3. Create Prescription
        Prescription rx = new Prescription();
        rx.setPrescriptionNumber("RX-100");
        rx.setCustomer(customer);
        rx.setDoctorName("Dr. Smith");
        rx.setPrescriptionDate(LocalDate.now());
        prescriptionRepo.save(rx);
        assertNotNull(rx.getId());

        // 4. Create Category, Manufacturer, Supplier, Medicine, and Batch
        Category cat = new Category();
        cat.setName("Painkillers");
        cat.setActive(true);
        categoryRepo.save(cat);

        Manufacturer mfg = new Manufacturer();
        mfg.setName("Pharma Corp");
        mfg.setActive(true);
        manufacturerRepo.save(mfg);

        Supplier supplier = new Supplier();
        supplier.setSupplierCode("SUP-100");
        supplier.setCompanyName("MedSupplier Ltd");
        supplierRepo.save(supplier);

        Medicine med = new Medicine();
        med.setMedicineCode("MED-PCM-500");
        med.setName("Paracetamol 500mg");
        med.setCategory(cat);
        med.setManufacturer(mfg);
        med.setDosageForm(DosageForm.TABLET);
        medicineRepo.save(med);

        MedicineBatch batch = new MedicineBatch();
        batch.setMedicine(med);
        batch.setSupplier(supplier);
        batch.setBatchNumber("BATCH-001");
        batch.setExpiryDate(LocalDate.now().plusMonths(12));
        batch.setPurchasePrice(new BigDecimal("10.00"));
        batch.setMrp(new BigDecimal("20.00"));
        batch.setSellingPrice(new BigDecimal("15.00"));
        batch.setQuantity(new BigDecimal("100.00"));
        batch.setReservedQuantity(BigDecimal.ZERO);
        batchRepo.save(batch);

        // 5. Perform POS Sale (FEFO stock reduction)
        SaleService.CreateSaleRequest saleReq = new SaleService.CreateSaleRequest(
                "SALE-TEST-001",
                customer.getId(),
                branch.getId(),
                rx.getId(),
                PaymentMethod.CASH,
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Test Sale Note",
                List.of(new SaleService.ItemRequest(med.getId(), null, new BigDecimal("10.00"), new BigDecimal("15.00"), BigDecimal.ZERO, BigDecimal.ZERO))
        );

        Sale sale = saleService.createSale(saleReq);
        assertNotNull(sale.getId());
        assertEquals(new BigDecimal("150.00"), sale.getTotalAmount());

        // Verify stock deducted from batch (100 - 10 = 90)
        MedicineBatch updatedBatch = batchRepo.findById(batch.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("90.00").compareTo(updatedBatch.getQuantity()));

        // 6. Perform Customer Payment
        CustomerPaymentController.Request payReq = new CustomerPaymentController.Request(
                "PAY-001", customer.getId(), sale.getId(), new BigDecimal("150.00"), PaymentMethod.CASH, "REF-123", LocalDate.now(), "Full Payment"
        );
        CustomerPaymentController.View payView = customerPaymentController.create(payReq);
        assertNotNull(payView.id());

        // 7. Perform Purchase Return
        PurchaseReturnService.CreateReturnRequest prReq = new PurchaseReturnService.CreateReturnRequest(
                "PR-001",
                supplier.getId(),
                null,
                LocalDate.now(),
                PurchaseReturnStatus.PENDING,
                "Overstocked",
                List.of(new PurchaseReturnService.ItemReturnRequest(batch.getId(), new BigDecimal("5.00"), new BigDecimal("10.00")))
        );
        PurchaseReturn pr = purchaseReturnService.createReturn(prReq);
        assertNotNull(pr.getId());
        assertEquals(0, new BigDecimal("50.00").compareTo(pr.getTotalAmount()));

        // Verify stock deducted by 5 (90 - 5 = 85)
        MedicineBatch afterReturnBatch = batchRepo.findById(batch.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("85.00").compareTo(afterReturnBatch.getQuantity()));

        // 8. Expense & Category
        ExpenseCategory expCat = new ExpenseCategory();
        expCat.setName("Utilities");
        expenseCategoryRepo.save(expCat);

        ExpenseController.ExpenseRequest expReq = new ExpenseController.ExpenseRequest(
                "EXP-001", expCat.getId(), new BigDecimal("50.00"), LocalDate.now(), PaymentMethod.CASH, "REF-EXP", "Electric Co", "Electricity Bill", branch.getId()
        );
        ExpenseController.ExpenseView expView = expenseController.createExpense(expReq);
        assertNotNull(expView.id());

        // 9. Notification
        Notification notif = notificationService.send("Test Title", "Test Message", NotificationType.LOW_STOCK, null);
        assertNotNull(notif.getId());

        // 10. Reports & Analytics
        ReportController.SalesReportView salesReport = reportController.salesReport(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        assertTrue(salesReport.totalSalesCount() >= 1);

        ReportController.StockValuationReportView stockVal = reportController.stockValuationReport();
        assertTrue(stockVal.totalBatchesCount() >= 1);

        List<AnalyticsController.DailyTrendPoint> trend = analyticsController.salesTrend(7);
        assertEquals(7, trend.size());
    }
}
