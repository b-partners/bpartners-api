package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceCustomer;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.validator.InvoiceValidator;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.ProductRepository;
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.InvoiceService;
import app.bpartners.api.service.PaymentInitiationService;
import app.bpartners.api.service.aws.SesService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvoiceServiceTest {
  public static final String INVOICE_ID = "invoice_id";
  public static final String INVOICE_REF = "F2022-01-10";
  InvoiceService invoiceService;
  InvoiceRepository repository;
  ProductRepository productRepository;
  PaymentInitiationService pis;
  FileService fileService;
  AccountHolderService holderService;
  PrincipalProvider auth;
  InvoiceValidator validator;
  SesService sesService;
  EventProducer eventProducer;

  @BeforeEach
  void setUp() {
    repository = mock(InvoiceRepository.class);
    productRepository = mock(ProductRepository.class);
    pis = mock(PaymentInitiationService.class);
    fileService = mock(FileService.class);
    holderService = mock(AccountHolderService.class);
    auth = mock(PrincipalProvider.class);
    validator = mock(InvoiceValidator.class);
    sesService = mock(SesService.class);
    eventProducer = mock(EventProducer.class);

    invoiceService = new InvoiceService(repository, productRepository, pis, fileService,
        holderService, auth, validator, sesService, eventProducer);
  }

  @Test
  void persist_file_id_ok() {
    when(repository.getById(INVOICE_ID)).thenReturn(invoice());
    when(repository.crupdate(any())).thenAnswer(i -> i.getArguments()[0]);
    when(pis.initiateInvoicePayment(any())).thenReturn(PaymentRedirection.builder().build());
    Invoice before = repository.getById(INVOICE_ID);

    Invoice actual = invoiceService.persistFileId(INVOICE_ID);

    assertNotEquals(before.getFileId(), actual.getFileId());
    assertEquals(actual.getFileId(), INVOICE_REF + ".pdf");
  }

  Invoice invoice() {
    return Invoice.builder()
        .id(INVOICE_ID)
        .ref(INVOICE_REF)
        .fileId(null)
        .status(CONFIRMED)
        .invoiceCustomer(InvoiceCustomer.customerTemplateBuilder().build())
        .products(List.of())
        .build();
  }
}
