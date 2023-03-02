package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HCustomer;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.OTHER_CUSTOMER_ID;
import static java.util.UUID.randomUUID;

@SpringBootTest(classes = InvoiceJpaRepositoryTest.class)
@AutoConfigureMockMvc
class InvoiceJpaRepositoryTest {
  @Autowired
  MockMvc mockMvc;
  @Autowired
  InvoiceJpaRepository invoiceJpaRepository;

  @Test
  void crupdate_invoice_ok() {
    HInvoice actual = invoiceJpaRepository.save(toBeCrupdated());


  }

  HInvoice toBeCrupdated() {
    return HInvoice.builder()
        .id(String.valueOf((randomUUID())))
        .title("invoice_title")
        .ref(String.valueOf((randomUUID())))
        .status(InvoiceStatus.DRAFT)
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .customer(HCustomer.builder()
            .id("customer1_id")
            .build())
        .paymentRequests(List.of(
            HPaymentRequest.builder()
                .amount("1000/1")
                .label("Test label")
                .payerName("Joe Doe")
                .payerEmail("exemple@gmail.com")
                .accountId(JOE_DOE_ACCOUNT_ID)
                .paymentDueDate(LocalDate.of(2023, 3, 2))
                .build(),
            HPaymentRequest.builder()
                .build()
        ))
        .build();
  }
}
