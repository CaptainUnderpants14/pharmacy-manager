package demo.pharma.payment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import demo.pharma.audit.AuditService;
import demo.pharma.common.exception.BusinessException;
import demo.pharma.security.UserRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PurchaseInvoiceRepository invoices;
    @Mock SupplierPaymentRepository payments;
    @Mock DemoPaymentTransactionRepository terminal;
    @Mock UserRepository users;
    @Mock AuditService audit;
    @InjectMocks PaymentService service;

    @Test
    void rejectsPaymentOverOutstandingBalance() {
        UUID invoiceId = UUID.randomUUID();
        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setTotal(new BigDecimal("100.00"));
        when(invoices.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(payments.totalPaid(invoiceId)).thenReturn(new BigDecimal("75.00"));

        assertThrows(BusinessException.class, () -> service.pay(invoiceId, new BigDecimal("25.01"), PaymentMethod.UPI, null, null));
        verify(payments, never()).save(any());
    }

    @Test
    void demoTerminalApprovesValidPayment() {
        when(terminal.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DemoPaymentTransaction transaction = service.charge("POS_CHECKOUT", UUID.randomUUID(), new BigDecimal("250.00"), PaymentMethod.CARD, "test");

        assertEquals(DemoPaymentStatus.APPROVED, transaction.getStatus());
        assertEquals(PaymentMethod.CARD, transaction.getPaymentMethod());
        assertTrue(transaction.getTerminalReference().startsWith("DEMO-"));
        verify(terminal).save(transaction);
    }
}
