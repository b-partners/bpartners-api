package app.bpartners.api.unit.repository;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.RelaunchType.PROPOSAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.mapper.InvoiceRelaunchMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.implementation.InvoiceRelaunchRepositoryImpl;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceRelaunchJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvoiceRelaunchRepositoryTest {
  private static final String OBJECT = "relaunch_object";
  private static final String EMAILBODY = "<p>Hello World!</p>";
  public static final boolean IS_USER_RELAUNCHED = true;
  InvoiceRelaunchJpaRepository invoiceRelaunchJpaRepositoryMock;
  InvoiceRelaunchMapper invoiceRelaunchMapperMock;
  InvoiceRepository invoiceRepositoryMock;
  InvoiceJpaRepository invoiceJpaRepositoryMock;
  InvoiceRelaunchRepositoryImpl subject;

  @BeforeEach
  void setUp() {
    invoiceRelaunchJpaRepositoryMock = mock(InvoiceRelaunchJpaRepository.class);
    invoiceRelaunchMapperMock = mock(InvoiceRelaunchMapper.class);
    invoiceRepositoryMock = mock(InvoiceRepository.class);
    invoiceJpaRepositoryMock = mock(InvoiceJpaRepository.class);
    subject =
        new InvoiceRelaunchRepositoryImpl(
            invoiceRelaunchJpaRepositoryMock,
            invoiceRelaunchMapperMock,
            invoiceRepositoryMock,
            invoiceJpaRepositoryMock);

    when(invoiceRelaunchMapperMock.toEntity(
            any(HInvoice.class), any(String.class), any(String.class), any(Boolean.class)))
        .thenReturn(invoiceRelaunch());
    when(invoiceJpaRepositoryMock.findById("invoice_id")).thenReturn(Optional.of(invoiceEntity()));
  }

  HInvoice invoiceEntity() {
    return HInvoice.builder()
        .id("invoice_id")
        .ref("invoice_ref")
        .title("invoice_title")
        .fileId("logo.pdf")
        .status(DRAFT)
        .comment("commment")
        .sendingDate(LocalDate.now())
        .toPayAt(LocalDate.now())
        .products(List.of(new HInvoiceProduct()))
        .build();
  }

  @Test
  void save_object_and_emailbody_on_relaunching_invoice() {
    ArgumentCaptor<HInvoice> invoiceCaptor = ArgumentCaptor.forClass(HInvoice.class);
    ArgumentCaptor<String> objectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> emailBodyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HInvoiceRelaunch> invoiceRelaunchCaptor =
        ArgumentCaptor.forClass(HInvoiceRelaunch.class);
    ArgumentCaptor<Boolean> userRelaunchedCaptor = ArgumentCaptor.forClass(Boolean.class);

    subject.save(invoice(), OBJECT, EMAILBODY, IS_USER_RELAUNCHED);
    verify(invoiceRelaunchMapperMock)
        .toEntity(
            invoiceCaptor.capture(),
            objectCaptor.capture(),
            emailBodyCaptor.capture(),
            userRelaunchedCaptor.capture());
    verify(invoiceRelaunchJpaRepositoryMock).save(invoiceRelaunchCaptor.capture());

    assertEquals(invoiceEntity(), invoiceCaptor.getValue());
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
        .products(List.of(new InvoiceProduct()))
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
