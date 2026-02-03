package app.bpartners.api.service.transaction;

import static app.bpartners.api.endpoint.rest.model.FileType.TRANSACTION_SUPPORTING_DOCS;
import static app.bpartners.api.endpoint.rest.model.TransactionStatus.BOOKED;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.JustifyTransaction;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionExportDetails;
import app.bpartners.api.model.TransactionInvoiceDetails;
import app.bpartners.api.model.TransactionSupportingDocs;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.DbTransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.TransactionSupportingDocsJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionSupportingDocs;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {
  public static final long ONE_HOUR_IN_SECONDS = 3600L;
  private final DbTransactionRepository dbTransactionRepository;
  private final TransactionSupportingDocsJpaRepository docsJpaRepository;
  private final TransactionsSummaryRepository summaryRepository;
  private final InvoiceService invoiceService;
  private final S3Service s3Service;
  private final UserService userService;
  private final FileService fileService;
  private final FileWriter fileWriter;
  private final CustomDateFormatter customDateFormatter;

  public List<TransactionSupportingDocs> getSupportingDocuments(String transactionId) {
    Transaction transaction = dbTransactionRepository.findById(transactionId);
    return transaction.getSupportingDocuments();
  }

  public List<TransactionSupportingDocs> addSupportingDocuments(
      String idUser, String transactionId, File documentFile) {
    Transaction transaction = dbTransactionRepository.findById(transactionId);

    String fileId = String.valueOf(randomUUID());
    String supportingDocsId = String.valueOf(randomUUID());

    FileInfo uploadedFileInfo =
        fileService.upload(TRANSACTION_SUPPORTING_DOCS, fileId, idUser, documentFile);

    /*
    TODO: historize by ENABLE and DISABLE status instead of replacing old
    List<TransactionSupportingDocs> actualDocs =
        new ArrayList<>(transaction.getSupportingDocuments());

    actualDocs.add(
        TransactionSupportingDocs.builder()
            .id(supportingDocsId)
            .fileInfo(uploadedFileInfo)
            .build());*/

    List<TransactionSupportingDocs> newSupportingDocs =
        List.of(
            TransactionSupportingDocs.builder()
                .id(supportingDocsId)
                .fileInfo(uploadedFileInfo)
                .build());

    Transaction savedTransaction =
        dbTransactionRepository
            .saveAll(
                List.of(
                    transaction.toBuilder()
                        .invoiceDetails(null)
                        .supportingDocuments(newSupportingDocs)
                        .build()))
            .get(0);

    return savedTransaction.getSupportingDocuments();
  }

  public TransactionExportDetails generateTransactionSummaryLink(
      String idAccount, Instant from, Instant to, TransactionStatus transactionStatus) {
    if (from.isAfter(to)) {
      throw new BadRequestException(
          String.format("Min interval (%s) must be after max interval (%s)", from, to));
    }
    if (transactionStatus == null) {
      transactionStatus = BOOKED;
    }
    List<Transaction> transactions =
        dbTransactionRepository.findByAccountIdAndStatusBetweenInstants(
            idAccount, transactionStatus, from, to);
    User user = userService.getByIdAccount(idAccount);

    Map<byte[], Map<String, String>> excelFileWithAssociatedInvoices =
        convertToExcelFileWithAssociatedInvoices(transactions);
    byte[] transactionExcelBytes = excelFileWithAssociatedInvoices.keySet().iterator().next();
    Map<String, byte[]> pdfInvoices =
        convertToFileNameAndBytes(user, excelFileWithAssociatedInvoices.get(transactionExcelBytes));

    String transactionExcelFileName =
        String.format(
            "Transactions du %s au %s.xlsx",
            customDateFormatter.formatFrenchDateUnderscore(from),
            customDateFormatter.formatFrenchDateUnderscore(to));
    byte[] compressed =
        compressedFiles(transactionExcelFileName, transactionExcelBytes, pdfInvoices);
    String compressedFileId = String.valueOf(randomUUID());
    s3Service.uploadFile(
        FileType.TRANSACTION, compressedFileId, user.getId(), fileWriter.apply(compressed, null));

    Instant createdAt = now();
    Instant expiredAt = createdAt.plusSeconds(ONE_HOUR_IN_SECONDS);
    String presignedUrl =
        s3Service.presignURL(
            FileType.TRANSACTION, compressedFileId, user.getId(), ONE_HOUR_IN_SECONDS);
    return TransactionExportDetails.builder()
        .downloadLink(presignedUrl)
        .createdAt(createdAt)
        .expiredAt(expiredAt)
        .build();
  }

  private Map<String, byte[]> convertToFileNameAndBytes(
      User user, Map<String, String> invoiceFileInfos) {
    Map<String, byte[]> pdfInvoices = new HashMap<>();
    invoiceFileInfos.forEach(
        (fileName, fileId) -> {
          File file = s3Service.downloadFile(FileType.INVOICE, user.getId(), fileId);
          pdfInvoices.put(fileName, fileWriter.writeAsByte(file));
        });
    return pdfInvoices;
  }

  private Map<String, String> removeDuplications(Map<String, String> map) {
    Map<String, String> mapWithoutDuplicates = new HashMap<>();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String fileName = entry.getKey();
      String fileId = entry.getValue();
      if (!mapWithoutDuplicates.containsValue(fileId)) {
        mapWithoutDuplicates.put(fileName, fileId);
      }
    }
    return mapWithoutDuplicates;
  }

  private byte[] compressedFiles(
      String transactionExcelFileName,
      byte[] transactionExcelFile,
      Map<String, byte[]> filesWithName) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos)) {

      zos.putNextEntry(new ZipEntry(transactionExcelFileName));
      zos.write(transactionExcelFile, 0, transactionExcelFile.length);
      zos.closeEntry();

      for (Map.Entry<String, byte[]> entry : filesWithName.entrySet()) {
        String fileName = entry.getKey();
        byte[] byteArray = entry.getValue();
        zos.putNextEntry(new ZipEntry("Factures/" + fileName + ".pdf"));
        zos.write(byteArray, 0, byteArray.length);
        zos.closeEntry();
      }

      zos.finish();
      zos.flush();

      return baos.toByteArray();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private Map<byte[], Map<String, String>> convertToExcelFileWithAssociatedInvoices(
      List<Transaction> transactions) {
    ByteArrayOutputStream outputStream;
    Map<String, String> invoiceNameAndFileIds = new HashMap<>();
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("ID");
      headerRow.createCell(1).setCellValue("Label");
      headerRow.createCell(2).setCellValue("Type");
      headerRow.createCell(3).setCellValue("Montant €");
      headerRow.createCell(4).setCellValue("Categorie");
      headerRow.createCell(5).setCellValue("Facture associée");
      headerRow.createCell(6).setCellValue("Date de paiement");

      List<Transaction> transactionsByPaymentDateDesc =
          transactions.stream()
              .sorted(Comparator.comparing(Transaction::getPaymentDatetime).reversed())
              .toList();
      int row = 1;
      for (Transaction transaction : transactionsByPaymentDateDesc) {
        TransactionInvoiceDetails invoiceDetails = transaction.getInvoiceDetails();
        Invoice invoice =
            invoiceDetails == null || invoiceDetails.getIdInvoice() == null
                ? null
                : invoiceService.getById(invoiceDetails.getIdInvoice());
        if (invoice != null && invoice.getFileId() != null) {
          invoiceNameAndFileIds.put(invoice.getRef(), invoice.getFileId());
        }

        Row currentRow = sheet.createRow(row);
        currentRow.createCell(0).setCellValue(transaction.getId());
        currentRow.createCell(1).setCellValue(transaction.getLabel());
        currentRow.createCell(2).setCellValue(transaction.getSide());
        currentRow
            .createCell(3)
            .setCellValue(transaction.getAmount().getValue().getCentsAsDecimal());
        currentRow
            .createCell(4)
            .setCellValue(
                transaction.getCategory() == null
                    ? ""
                    : transaction.getCategory().getDescription());
        currentRow.createCell(5).setCellValue(invoice == null ? "" : invoice.getRef());
        currentRow
            .createCell(6)
            .setCellValue(
                transaction.getPaymentDatetime() == null
                    ? ""
                    : customDateFormatter.formatFrenchDate(transaction.getPaymentDatetime()));
        row++;
      }
      outputStream = new ByteArrayOutputStream();
      workbook.write(outputStream);
      workbook.close();
      outputStream.close();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return Map.of(outputStream.toByteArray(), removeDuplications(invoiceNameAndFileIds));
  }

  public List<Transaction> getPersistedByIdAccount(
      String idAccount,
      String label,
      TransactionStatus status,
      String category,
      PageFromOne page,
      BoundedPageSize pageSize) {
    int pageValue = page == null ? 0 : page.getValue() - 1;
    int pageSizeValue = pageSize == null ? 30 : pageSize.getValue();
    if (category != null) {
      throw new NotImplementedException("prospect conversion not implemented yet");
    }
    return dbTransactionRepository.findByIdAccount(
        idAccount, label, status, pageValue, pageSizeValue);
  }

  public Transaction getById(String transactionId) {
    return dbTransactionRepository.findById(transactionId);
  }

  public TransactionsSummary getTransactionsSummary(String idUser, Integer year) {
    if (year == null) {
      year = LocalDate.now().getYear();
    }
    return summaryRepository.getByIdUserAndYear(idUser, year);
  }

  // TODO: change to transactionRepository.save(Transaction toSsave)
  public Transaction justifyTransaction(String idTransaction, String idInvoice) {
    List<HTransactionSupportingDocs> supportingDocs =
        docsJpaRepository.findAllByIdTransaction(idTransaction);
    docsJpaRepository.deleteAllById(
        supportingDocs.stream().map(HTransactionSupportingDocs::getId).toList());

    return dbTransactionRepository.save(
        JustifyTransaction.builder().idTransaction(idTransaction).idInvoice(idInvoice).build());
  }
}
