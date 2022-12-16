package app.bpartners.api.unit.repository;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.Product;
import app.bpartners.api.model.mapper.InvoiceRelaunchMapper;
import app.bpartners.api.repository.implementation.InvoiceRelaunchRepositoryImpl;
import app.bpartners.api.repository.jpa.InvoiceRelaunchJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.RelaunchType.PROPOSAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceRelaunchRepositoryTest {
  private static final String OBJECT = "relaunch_object";
  private static final String EMAILBODY = "<p>Hello World!</p>";
  public static final boolean IS_USER_RELAUNCHED = true;
  InvoiceRelaunchJpaRepository invoiceRelaunchJpaRepository;
  InvoiceRelaunchMapper invoiceRelaunchMapper;
  InvoiceRelaunchRepositoryImpl invoiceRelaunchRepository;

  @BeforeEach
  void setUp() {
    invoiceRelaunchJpaRepository = mock(InvoiceRelaunchJpaRepository.class);
    invoiceRelaunchMapper = mock(InvoiceRelaunchMapper.class);
    invoiceRelaunchRepository =
        new InvoiceRelaunchRepositoryImpl(invoiceRelaunchJpaRepository, invoiceRelaunchMapper);

    when(invoiceRelaunchMapper.toEntity(any(Invoice.class), any(String.class),
        any(String.class), any(Boolean.class))).thenReturn(invoiceRelaunch());
  }

  @Test
  void save_object_and_emailbody_on_relaunching_invoice() {
    ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
    ArgumentCaptor<String> objectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> emailBodyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HInvoiceRelaunch> invoiceRelaunchCaptor =
        ArgumentCaptor.forClass(HInvoiceRelaunch.class);
    ArgumentCaptor<Boolean> userRelaunchedCaptor =
        ArgumentCaptor.forClass(Boolean.class);


    invoiceRelaunchRepository.save(invoice(), OBJECT, EMAILBODY, IS_USER_RELAUNCHED);
    verify(invoiceRelaunchMapper).toEntity(invoiceCaptor.capture(), objectCaptor.capture(),
        emailBodyCaptor.capture(), userRelaunchedCaptor.capture());
    verify(invoiceRelaunchJpaRepository).save(invoiceRelaunchCaptor.capture());

    assertEquals(invoice(), invoiceCaptor.getValue());
    assertEquals(OBJECT, objectCaptor.getValue());
    assertEquals(EMAILBODY, emailBodyCaptor.getValue());
    assertEquals(invoiceRelaunch(), invoiceRelaunchCaptor.getValue());
    assertEquals(IS_USER_RELAUNCHED, userRelaunchedCaptor.getValue());
  }


  private Invoice invoice() {
    return Invoice.builder()
        .id("invoice_id")
        .ref("invoice_ref")
        .title("invoice_title")
        .fileId("logo.pdf")
        .status(DRAFT)
        .comment("commment")
        .sendingDate(LocalDate.now())
        .toPayAt(LocalDate.now())
        .products(List.of(new Product()))
        .build();
  }

  private HInvoiceRelaunch invoiceRelaunch() {
    return HInvoiceRelaunch.builder()
        .isUserRelaunched(true)
        .invoice(new HInvoice())
        .type(PROPOSAL)
        .emailBody(EMAILBODY)
        .object(OBJECT)
        .build();
  }
}
