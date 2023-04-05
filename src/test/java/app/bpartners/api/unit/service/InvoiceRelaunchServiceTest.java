package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.ValidityStatus;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.model.validator.InvoiceRelaunchValidator;
import app.bpartners.api.repository.AccountInvoiceRelaunchConfRepository;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.AttachmentService;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.InvoiceRelaunchConfService;
import app.bpartners.api.service.InvoiceRelaunchService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.setUpProvider;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceRelaunchServiceTest {
  private static final String RANDOM_CONF_ID = "random conf id";
  private InvoiceRelaunchService invoiceRelaunchService;
  private AccountInvoiceRelaunchConfRepository accountInvoiceRelaunchRepository;
  private InvoiceRelaunchRepository invoiceRelaunchRepository;
  private InvoiceRelaunchValidator invoiceRelaunchValidator = new InvoiceRelaunchValidator();
  private InvoiceRepository invoiceRepository;
  private InvoiceJpaRepository invoiceJpaRepository;
  private InvoiceRelaunchConfService relaunchConfService;
  private AccountHolderService holderService;
  private EventProducer eventProducer;
  private PrincipalProvider auth;
  private FileService fileService;
  private AttachmentService attachmentService;

  @BeforeEach
  void setUp() {
    accountInvoiceRelaunchRepository = mock(AccountInvoiceRelaunchConfRepository.class);
    invoiceRelaunchRepository = mock(InvoiceRelaunchRepository.class);
    invoiceRepository = mock(InvoiceRepository.class);
    invoiceJpaRepository = mock(InvoiceJpaRepository.class);
    relaunchConfService = mock(InvoiceRelaunchConfService.class);
    holderService = mock(AccountHolderService.class);
    eventProducer = mock(EventProducer.class);
    auth = mock(PrincipalProvider.class);
    fileService = mock(FileService.class);
    attachmentService = mock(AttachmentService.class);
    setUpProvider(auth);
    invoiceRelaunchService = new InvoiceRelaunchService(
        accountInvoiceRelaunchRepository,
        invoiceRelaunchRepository,
        invoiceRelaunchValidator,
        invoiceRepository,
        invoiceJpaRepository,
        relaunchConfService,
        holderService,
        eventProducer,
        auth,
        fileService,
        attachmentService
    );
    when(invoiceJpaRepository.findAllByToBeRelaunched(true))
        .thenReturn(
            List.of(
                HInvoice.builder()
                    .id(INVOICE1_ID)
                    .toBeRelaunched(true)
                    .validityStatus(ValidityStatus.ENABLED)
                    .sendingDate(LocalDate.now().minusDays(10))
                    .build()
            )
        );
    when(relaunchConfService.findByIdInvoice(any(String.class)))
        .thenAnswer(i ->
            InvoiceRelaunchConf.builder()
                .id(RANDOM_CONF_ID)
                .idInvoice(i.getArgument(0))
                .delay(10)
                .rehearsalNumber(2)
                .build()
        );
    when(invoiceRepository.getById(INVOICE1_ID))
        .thenReturn(
            Invoice
                .builder()
                .id(INVOICE1_ID)
                .account(
                    Account.builder()
                        .id(JOE_DOE_ACCOUNT_ID)
                        .build()
                )
                .status(InvoiceStatus.PROPOSAL)
                .validityStatus(ValidityStatus.ENABLED)
                .build()
        );
    when(invoiceRelaunchRepository.getByInvoiceId(
        INVOICE1_ID,
        null,
        PageRequest.of(0, 500))
    ).thenReturn(
        List.of(
            InvoiceRelaunch.builder().build()
        )
    );
    when(invoiceRelaunchRepository.save(any(Invoice.class), any(), any(), eq(true)))
        .thenReturn(
            InvoiceRelaunch.builder()
                .invoice(
                    Invoice
                        .builder()
                        .status(InvoiceStatus.PROPOSAL)
                        .validityStatus(ValidityStatus.ENABLED)
                        .customer(
                            Customer.builder()
                                .firstName("someName")
                                .lastName("lastName")
                                .build()
                        )
                        .build()
                )
                .build()
        );
    when(holderService.getAccountHolderByAccountId(JOE_DOE_ACCOUNT_ID))
        .thenReturn(
            AccountHolder.builder().build()
        );
    when(fileService.downloadFile(any(), any(), any()))
        .thenReturn(new byte[0]);
    when(attachmentService.saveAll(any(), any())).thenReturn(List.of());
  }

  @Test
  void test_scheduler() {
    ArgumentCaptor<String> idInvoiceCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> idInvoiceCaptor2 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> idInvoiceCaptor3 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HInvoice> invoiceSaveCaptor = ArgumentCaptor.forClass(HInvoice.class);

    invoiceRelaunchService.relaunch();
    verify(relaunchConfService).findByIdInvoice(idInvoiceCaptor.capture());
    verify(invoiceRelaunchRepository).getByInvoiceId(
        idInvoiceCaptor2.capture(),
        eq(null),
        eq(PageRequest.of(0, 500))
    );
    verify(invoiceRepository).getById(idInvoiceCaptor3.capture());
    verify(invoiceJpaRepository).save(invoiceSaveCaptor.capture());

    assertEquals(INVOICE1_ID, idInvoiceCaptor.getValue());
    assertEquals(INVOICE1_ID, idInvoiceCaptor2.getValue());
    assertEquals(INVOICE1_ID, idInvoiceCaptor3.getValue());
    assertFalse(invoiceSaveCaptor.getValue().isToBeRelaunched());
  }
}
