package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.TransactionStatus.BOOKED;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.file.FileWriter;
import app.bpartners.api.file.hash.FileHash;
import app.bpartners.api.model.*;
import app.bpartners.api.repository.BridgeTransactionRepository;
import app.bpartners.api.repository.DbTransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.TransactionSupportingDocsJpaRepository;
import app.bpartners.api.service.account.AccountService;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.transaction.TransactionService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionServiceTest {
  DbTransactionRepository dbTransactionRepositoryMock = mock();
  BridgeTransactionRepository bridgeTransactionRepositoryMock = mock();
  TransactionSupportingDocsJpaRepository docsJpaRepositoryMock = mock();
  TransactionsSummaryRepository summaryRepositoryMock = mock();
  AccountService accountServiceMock = mock();
  InvoiceService invoiceServiceMock = mock();
  S3Service s3ServiceMock = mock();
  UserService userServiceMock = mock();
  FileService fileServiceMock = mock();
  FileWriter fileWriterMock = mock();
  CustomDateFormatter customDateFormatterMock = mock();
  TransactionService subject =
      new TransactionService(
          dbTransactionRepositoryMock,
          bridgeTransactionRepositoryMock,
          docsJpaRepositoryMock,
          summaryRepositoryMock,
          accountServiceMock,
          invoiceServiceMock,
          s3ServiceMock,
          userServiceMock,
          fileServiceMock,
          fileWriterMock,
          customDateFormatterMock);

  @Test
  void get_supporting_documents() {
    var transactionId = "transactionId";
    var supportingDocuments = TransactionSupportingDocs.builder().build();
    var transaction =
        Transaction.builder().supportingDocuments(List.of(supportingDocuments)).build();
    when(dbTransactionRepositoryMock.findById(any())).thenReturn(transaction);

    var actual = subject.getSupportingDocuments(transactionId);

    assertEquals(List.of(supportingDocuments), actual);
  }

  @Test
  void add_supporting_document() throws IOException {
    var transactionId = "transactionId";
    var idUser = "idUser";
    var documentFile = File.createTempFile("temp", "file");
    var transactionSupportingDocs = TransactionSupportingDocs.builder().build();
    var transaction =
        Transaction.builder().supportingDocuments(List.of(transactionSupportingDocs)).build();
    when(dbTransactionRepositoryMock.findById(any())).thenReturn(transaction);
    var fileInfo = FileInfo.builder().build();
    when(fileServiceMock.upload(any(), any(), anyString(), any())).thenReturn(fileInfo);
    when(dbTransactionRepositoryMock.saveAll(anyList())).thenReturn(List.of(transaction));

    var actual = subject.addSupportingDocuments(idUser, transactionId, documentFile);

    assertEquals(List.of(transactionSupportingDocs), actual);
  }

  @Test
  void generate_transaction_summary_link() throws IOException {
    var idAccount = "idAccount";
    var now = Instant.now();
    var from = now.minus(1, HOURS);
    var to = now.minus(1, MINUTES);
    var invoiceDetails = TransactionInvoiceDetails.builder().idInvoice("idInvoice").build();
    var amount =
        Money.builder()
            .value(
                Fraction.builder()
                    .denominator(BigInteger.valueOf(2))
                    .numerator(BigInteger.valueOf(3))
                    .build())
            .build();
    var category = TransactionCategory.builder().description("description").build();
    var transaction =
        Transaction.builder()
            .id("idTransaction")
            .label("label")
            .side("side")
            .category(category)
            .amount(amount)
            .paymentDatetime(now.minus(2, MINUTES))
            .invoiceDetails(invoiceDetails)
            .build();
    when(dbTransactionRepositoryMock.findByAccountIdAndStatusBetweenInstants(
            anyString(), any(), any(), any()))
        .thenReturn(List.of(transaction));
    var user = User.builder().build();
    when(userServiceMock.getByIdAccount(anyString())).thenReturn(user);
    var invoice = Invoice.builder().fileId("fileId").ref("ref").build();
    when(invoiceServiceMock.getById(any())).thenReturn(invoice);
    var file = File.createTempFile("temp", "file");
    when(fileWriterMock.writeAsByte(any(File.class))).thenReturn("dummy".getBytes());
    when(s3ServiceMock.downloadFile(any(), any(), any())).thenReturn(file);
    when(s3ServiceMock.uploadFile(any(), any(), any(), any())).thenReturn(mock(FileHash.class));
    when(s3ServiceMock.presignURL(any(), any(), any(), any())).thenReturn("presignedUrl");

    var actual = subject.generateTransactionSummaryLink(idAccount, from, to, BOOKED);

    assertEquals("presignedUrl", actual.getDownloadLink());
  }
}
