package demo.pharma.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.expense.Expense;
import demo.pharma.expense.ExpenseRepository;
import demo.pharma.inventory.MedicineBatch;
import demo.pharma.inventory.MedicineBatchRepository;
import demo.pharma.payment.PaymentMethod;
import demo.pharma.purchase.PurchaseOrder;
import demo.pharma.purchase.PurchaseOrderRepository;
import demo.pharma.sale.Sale;
import demo.pharma.sale.SaleItem;
import demo.pharma.sale.SaleRepository;
import demo.pharma.sale.SaleStatus;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final SaleRepository saleRepo;
    private final PurchaseOrderRepository purchaseRepo;
    private final ExpenseRepository expenseRepo;
    private final MedicineBatchRepository batchRepo;

    public record SalesReportView(long totalSalesCount, BigDecimal grossRevenue, BigDecimal totalDiscount,
            BigDecimal totalTax, BigDecimal netRevenue, BigDecimal cashSales, BigDecimal cardSales, BigDecimal upiSales,
            BigDecimal otherSales) {
    }

    public record PurchasesReportView(long totalOrdersCount, BigDecimal totalPurchaseAmount, BigDecimal totalTax) {
    }

    public record CategoryExpenseSummary(String categoryName, BigDecimal totalAmount) {
    }

    public record ExpensesReportView(long totalExpensesCount, BigDecimal totalExpenseAmount,
            List<CategoryExpenseSummary> categoryBreakdown) {
    }

    public record ProfitLossReportView(BigDecimal salesRevenue, BigDecimal costOfGoodsSold, BigDecimal grossProfit,
            BigDecimal operatingExpenses, BigDecimal netProfit) {
    }

    public record StockValuationReportView(long totalBatchesCount, BigDecimal totalStockQuantity,
            BigDecimal totalPurchaseValue, BigDecimal totalSellingValue, BigDecimal potentialProfit) {
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public SalesReportView salesReport(@RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Instant start = startDate != null ? startDate.atStartOfDay().toInstant(ZoneOffset.UTC)
                : Instant.ofEpochMilli(0);
        Instant end = endDate != null ? endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                : Instant.now();

        List<Sale> sales = saleRepo.findAll().stream()
                .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                .filter(s -> !s.getSaleDate().isBefore(start) && !s.getSaleDate().isAfter(end))
                .toList();

        long count = sales.size();
        BigDecimal gross = sales.stream().map(Sale::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disc = sales.stream().map(Sale::getDiscount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = sales.stream().map(Sale::getTax).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = sales.stream().map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cash = sales.stream().filter(s -> s.getPaymentMethod() == PaymentMethod.CASH)
                .map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal card = sales.stream().filter(s -> s.getPaymentMethod() == PaymentMethod.CARD)
                .map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal upi = sales.stream().filter(s -> s.getPaymentMethod() == PaymentMethod.UPI)
                .map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal other = sales.stream()
                .filter(s -> s.getPaymentMethod() != PaymentMethod.CASH && s.getPaymentMethod() != PaymentMethod.CARD
                        && s.getPaymentMethod() != PaymentMethod.UPI)
                .map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SalesReportView(count, gross, disc, tax, net, cash, card, upi, other);
    }

    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public PurchasesReportView purchasesReport(@RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<PurchaseOrder> orders = purchaseRepo.findAll().stream()
                .filter(po -> !po.getOrderDate().isBefore(start) && !po.getOrderDate().isAfter(end))
                .toList();

        long count = orders.size();
        BigDecimal totalAmount = orders.stream().map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = orders.stream().map(po -> po.getTax() != null ? po.getTax() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PurchasesReportView(count, totalAmount, tax);
    }

    @GetMapping("/expenses")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ExpensesReportView expensesReport(@RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<Expense> expenses = expenseRepo.findAll().stream()
                .filter(e -> !e.getExpenseDate().isBefore(start) && !e.getExpenseDate().isAfter(end))
                .toList();

        long count = expenses.size();
        BigDecimal total = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        var byCat = expenses.stream()
                .collect(java.util.stream.Collectors.groupingBy(e -> e.getCategory().getName(),
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        List<CategoryExpenseSummary> catSummaries = new ArrayList<>();
        byCat.forEach((catName, amt) -> catSummaries.add(new CategoryExpenseSummary(catName, amt)));

        return new ExpensesReportView(count, total, catSummaries);
    }

    @GetMapping("/profit-loss")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ProfitLossReportView profitLossReport(@RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Instant startInst = startDate != null ? startDate.atStartOfDay().toInstant(ZoneOffset.UTC) : Instant.ofEpochMilli(0);
        Instant endInst = endDate != null ? endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : Instant.now();
        LocalDate startDt = startDate != null ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate endDt = endDate != null ? endDate : LocalDate.now();

        List<Sale> sales = saleRepo.findAll().stream()
                .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                .filter(s -> !s.getSaleDate().isBefore(startInst) && !s.getSaleDate().isAfter(endInst))
                .toList();

        BigDecimal revenue = sales.stream().map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cogs = BigDecimal.ZERO;
        for (Sale s : sales) {
            for (SaleItem item : s.getItems()) {
                BigDecimal purchasePrice = item.getMedicineBatch() != null ? item.getMedicineBatch().getPurchasePrice() : BigDecimal.ZERO;
                cogs = cogs.add(purchasePrice.multiply(item.getQuantity()));
            }
        }

        BigDecimal grossProfit = revenue.subtract(cogs);

        List<Expense> expenses = expenseRepo.findAll().stream()
                .filter(e -> !e.getExpenseDate().isBefore(startDt) && !e.getExpenseDate().isAfter(endDt))
                .toList();
        BigDecimal totalExpenses = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = grossProfit.subtract(totalExpenses);

        return new ProfitLossReportView(revenue, cogs, grossProfit, totalExpenses, netProfit);
    }

    @GetMapping("/stock-valuation")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('STOCK_VIEW')")
    public StockValuationReportView stockValuationReport() {
        List<MedicineBatch> batches = batchRepo.findAll();

        long count = batches.size();
        BigDecimal totalQty = batches.stream().map(MedicineBatch::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purchaseVal = batches.stream().map(b -> b.getPurchasePrice().multiply(b.getQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sellingVal = batches.stream().map(b -> b.getSellingPrice().multiply(b.getQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal potentialProfit = sellingVal.subtract(purchaseVal);

        return new StockValuationReportView(count, totalQty, purchaseVal, sellingVal, potentialProfit);
    }
}
