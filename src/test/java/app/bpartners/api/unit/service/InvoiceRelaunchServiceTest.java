package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpProvider;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.*;
import app.bpartners.api.model.validator.InvoiceRelaunchValidator;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserInvoiceRelaunchConfRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.service.accountholder.AccountHolderService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.InvoiceRelaunchSavedService;
import app.bpartners.api.service.file.AttachmentService;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.invoice.InvoicePDFGenerator;
import app.bpartners.api.service.invoice.InvoiceRelaunchConfService;
import app.bpartners.api.service.invoice.InvoiceRelaunchService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

class InvoiceRelaunchServiceTest {
  private static final String RANDOM_CONF_ID = "random conf id";
  private InvoiceRelaunchService subject = mock();
  private UserInvoiceRelaunchConfRepository accountInvoiceRelaunchRepository = mock();
  private InvoiceRelaunchRepository invoiceRelaunchRepository = mock();
  private InvoiceRelaunchValidator invoiceRelaunchValidator = mock();
  private InvoiceRepository invoiceRepository = mock();
  private InvoiceJpaRepository invoiceJpaRepository = mock();
  private InvoiceRelaunchConfService relaunchConfService = mock();
  private AccountHolderService holderService = mock();
  private PrincipalProvider auth = mock();
  private FileService fileService = mock();
  private AttachmentService attachmentService = mock();
  private SesConf sesConf = mock();
  private SesService sesServiceMock = mock();
  InvoicePDFGenerator invoicePDFGeneratorMock = mock();
  InvoiceRelaunchSavedService invoiceRelaunchSavedServiceMock = mock();
  FileWriter fileWriterMock = mock();

  @SneakyThrows
  @BeforeEach
  void setUp() {
    setUpProvider(auth);

    subject =
        new InvoiceRelaunchService(
            accountInvoiceRelaunchRepository,
            invoiceRelaunchRepository,
            invoiceRelaunchValidator,
            invoiceRepository,
            invoiceJpaRepository,
            relaunchConfService,
            holderService,
            auth,
            fileService,
            attachmentService,
            sesConf,
            invoicePDFGeneratorMock,
            sesServiceMock,
            invoiceRelaunchSavedServiceMock,
            fileWriterMock,
            mock());
    when(invoiceJpaRepository.findAllByToBeRelaunched(true))
        .thenReturn(
            List.of(
                HInvoice.builder()
                    .id(INVOICE1_ID)
                    .toBeRelaunched(true)
                    .archiveStatus(ENABLED)
                    .sendingDate(LocalDate.now().minusDays(10))
                    .build()));
    when(relaunchConfService.findByIdInvoice(any(String.class)))
        .thenAnswer(
            i ->
                InvoiceRelaunchConf.builder()
                    .id(RANDOM_CONF_ID)
                    .idInvoice(i.getArgument(0))
                    .delay(10)
                    .rehearsalNumber(2)
                    .build());
    when(invoiceRepository.getById(INVOICE1_ID))
        .thenReturn(
            Invoice.builder()
                .id(INVOICE1_ID)
                .user(
                    User.builder()
                        .accounts(List.of(Account.builder().id(JOE_DOE_ACCOUNT_ID).build()))
                        .build())
                .status(InvoiceStatus.PROPOSAL)
                .archiveStatus(ENABLED)
                .build());
    when(invoiceRelaunchRepository.getByInvoiceId(INVOICE1_ID, null, PageRequest.of(0, MAX_SIZE)))
        .thenReturn(List.of(InvoiceRelaunch.builder().build()));
    when(invoiceRelaunchRepository.save(any(Invoice.class), any(), any(), eq(true)))
        .thenReturn(
            InvoiceRelaunch.builder()
                .invoice(
                    Invoice.builder()
                        .status(InvoiceStatus.PROPOSAL)
                        .archiveStatus(ENABLED)
                        .customer(
                            Customer.builder().firstName("someName").lastName("lastName").build())
                        .build())
                .build());
    when(holderService.getDefaultByAccountId(JOE_DOE_ACCOUNT_ID))
        .thenReturn(AccountHolder.builder().build());
    when(fileService.downloadFile(any(), any(), any()))
        .thenReturn(File.createTempFile(randomUUID().toString(), randomUUID().toString()));
    when(attachmentService.saveAll(any(), any())).thenReturn(List.of());
  }

  @Test
  @Disabled
  // TODO: check if should be removed or not as scheduler is disabled
  void test_scheduler() {
    ArgumentCaptor<String> idInvoiceCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> idInvoiceCaptor2 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> idInvoiceCaptor3 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HInvoice> invoiceSaveCaptor = ArgumentCaptor.forClass(HInvoice.class);

    subject.relaunch();
    verify(relaunchConfService).findByIdInvoice(idInvoiceCaptor.capture());
    verify(invoiceRelaunchRepository)
        .getByInvoiceId(idInvoiceCaptor2.capture(), eq(null), eq(PageRequest.of(0, MAX_SIZE)));
    verify(invoiceRepository).getById(idInvoiceCaptor3.capture());
    verify(invoiceJpaRepository).save(invoiceSaveCaptor.capture());

    assertEquals(INVOICE1_ID, idInvoiceCaptor.getValue());
    assertEquals(INVOICE1_ID, idInvoiceCaptor2.getValue());
    assertEquals(INVOICE1_ID, idInvoiceCaptor3.getValue());
    assertFalse(invoiceSaveCaptor.getValue().isToBeRelaunched());
  }

  @Test
  void restart_last_relaunch() throws IOException {
    var invoiceId = "invoiceId";
    var attachment = Attachment.builder().build();
    var invoiceRelaunch =
        InvoiceRelaunch.builder()
            .creationDatetime(now())
            .emailObject("emailObject")
            .emailBody("emailBody")
            .attachments(List.of(attachment))
            .build();
    when(invoiceRelaunchRepository.getByInvoiceId(any(), any(), any()))
        .thenReturn(List.of(invoiceRelaunch));
    var account =
        Account.builder()
            .enableStatus(EnableStatus.ENABLED)
            .id("preferredAccountId")
            .iban("iban")
            .build();
    var user =
        User.builder()
            .accounts(List.of(account))
            .preferredAccountId("preferredAccountId")
            .accountHolders(
                List.of(AccountHolder.builder().email("accountHolder@gmail.com").build()))
            .build();
    var customer = Customer.builder().email("email@gmail.com").build();
    var invoice =
        Invoice.builder()
            .archiveStatus(ENABLED)
            .customer(customer)
            .user(user)
            .status(InvoiceStatus.PROPOSAL)
            .ref("REF")
            .build();
    when(invoiceRepository.getById(any())).thenReturn(invoice);
    doNothing().when(invoiceRelaunchValidator).accept(any());
    when(holderService.getDefaultByAccountId(any())).thenReturn(AccountHolder.builder().build());
    when(invoiceRelaunchRepository.save(any(), any(), any(), anyBoolean()))
        .thenReturn(invoiceRelaunch);
    when(fileWriterMock.apply(any(), any())).thenReturn(File.createTempFile("temp", "file"));
    when(fileService.upload(any(), any(), any(), any()))
        .thenReturn(FileInfo.builder().id("fileId").build());
    when(attachmentService.saveAll(any(), any())).thenReturn(List.of(attachment));
    when(sesConf.getAdminEmail()).thenReturn("admin@email.com");
    doNothing()
        .when(invoiceRelaunchSavedServiceMock)
        .relaunchInvoiceAction(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any());

    subject.restartLastRelaunch(List.of(invoiceId));

    verify(invoiceRelaunchRepository).getByInvoiceId(any(), any(), any());
    verify(invoiceRepository).getById(any());
    verify(invoiceRelaunchValidator).accept(any());
    verify(holderService).getDefaultByAccountId(any());
    verify(invoiceRelaunchRepository).save(any(), any(), any(), anyBoolean());
    verify(attachmentService).saveAll(any(), any());
    verify(sesConf).getAdminEmail();
    verify(invoiceRelaunchSavedServiceMock)
        .relaunchInvoiceAction(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any());
  }
}
