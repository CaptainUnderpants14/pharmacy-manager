package demo.pharma.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.inventory.MedicineBatchRepository;
import demo.pharma.catalog.MedicineRepository;
import demo.pharma.payment.PaymentMethod;
import demo.pharma.sale.Sale;
import demo.pharma.sale.SaleItem;
import demo.pharma.sale.SaleRepository;
import demo.pharma.sale.SaleStatus;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final SaleRepository saleRepo;
    private final MedicineRepository medicineRepo;
    private final MedicineBatchRepository batchRepo;

    public record DailyTrendPoint(String date, long salesCount, BigDecimal revenue) {
    }

    public record TopMedicineView(UUID medicineId, String medicineName, String medicineCode, BigDecimal totalQuantitySold,
            BigDecimal totalRevenue) {
    }

    public record CategoryShareView(String categoryName, BigDecimal totalRevenue, long totalMedicinesCount) {
    }

    public record PaymentMethodShareView(PaymentMethod paymentMethod, long count, BigDecimal totalAmount) {
    }

    @GetMapping("/sales-trend")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('REPORT_VIEW')")
    public List<DailyTrendPoint> salesTrend(@RequestParam(defaultValue = "30") int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);

        Instant startInst = start.atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Sale> sales = saleRepo.findAll().stream()
                .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                .filter(s -> !s.getSaleDate().isBefore(startInst))
                .toList();

        Map<String, List<Sale>> byDate = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        for (Sale s : sales) {
            String dateStr = s.getSaleDate().atZone(ZoneId.systemDefault()).toLocalDate().format(fmt);
            byDate.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(s);
        }

        List<DailyTrendPoint> points = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            String ds = d.format(fmt);
            List<Sale> daySales = byDate.getOrDefault(ds, List.of());
            long count = daySales.size();
            BigDecimal rev = daySales.stream().map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            points.add(new DailyTrendPoint(ds, count, rev));
        }

        return points;
    }

    @GetMapping("/top-medicines")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public List<TopMedicineView> topMedicines(@RequestParam(defaultValue = "10") int limit) {
        List<Sale> sales = saleRepo.findAll().stream()
                .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                .toList();

        Map<UUID, BigDecimal> qtyMap = new HashMap<>();
        Map<UUID, BigDecimal> revMap = new HashMap<>();
        Map<UUID, String> nameMap = new HashMap<>();
        Map<UUID, String> codeMap = new HashMap<>();

        for (Sale s : sales) {
            for (SaleItem item : s.getItems()) {
                UUID medId = item.getMedicine().getId();
                qtyMap.put(medId, qtyMap.getOrDefault(medId, BigDecimal.ZERO).add(item.getQuantity()));
                revMap.put(medId, revMap.getOrDefault(medId, BigDecimal.ZERO).add(item.getTotal()));
                nameMap.putIfAbsent(medId, item.getMedicine().getName());
                codeMap.putIfAbsent(medId, item.getMedicine().getMedicineCode());
            }
        }

        return qtyMap.keySet().stream()
                .map(medId -> new TopMedicineView(medId, nameMap.get(medId), codeMap.get(medId), qtyMap.get(medId),
                        revMap.get(medId)))
                .sorted(Comparator.comparing(TopMedicineView::totalRevenue).reversed())
                .limit(limit)
                .toList();
    }

    @GetMapping("/category-distribution")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public List<CategoryShareView> categoryDistribution() {
        List<Sale> sales = saleRepo.findAll().stream()
                .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                .toList();

        Map<String, BigDecimal> revMap = new HashMap<>();
        for (Sale s : sales) {
            for (SaleItem item : s.getItems()) {
                String catName = item.getMedicine().getCategory() != null ? item.getMedicine().getCategory().getName()
                        : "Uncategorized";
                revMap.put(catName, revMap.getOrDefault(catName, BigDecimal.ZERO).add(item.getTotal()));
            }
        }

        List<CategoryShareView> result = new ArrayList<>();
        revMap.forEach((catName, rev) -> result.add(new CategoryShareView(catName, rev, 0)));
        result.sort(Comparator.comparing(CategoryShareView::totalRevenue).reversed());
        return result;
    }

    @GetMapping("/payment-methods")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('REPORT_VIEW')")
    public List<PaymentMethodShareView> paymentMethods() {
        List<Sale> sales = saleRepo.findAll().stream()
                .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                .toList();

        Map<PaymentMethod, List<Sale>> byMethod = new HashMap<>();
        for (Sale s : sales) {
            byMethod.computeIfAbsent(s.getPaymentMethod(), k -> new ArrayList<>()).add(s);
        }

        List<PaymentMethodShareView> list = new ArrayList<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            List<Sale> mSales = byMethod.getOrDefault(pm, List.of());
            long count = mSales.size();
            BigDecimal total = mSales.stream().map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            list.add(new PaymentMethodShareView(pm, count, total));
        }

        return list;
    }
}
