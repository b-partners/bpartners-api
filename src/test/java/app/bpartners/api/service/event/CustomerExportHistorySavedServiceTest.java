package app.bpartners.api.service.event;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.model.CustomerExportHistorySaved;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.CustomerExportHistory;
import app.bpartners.api.repository.jpa.CustomerExportHistoryJpaRepository;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CustomerExportHistorySavedServiceTest {
  private static final String ADMIN_EMAIL = "dummy@example.com";
  SesService emailServiceMock = mock(SesService.class);
  CustomerExportHistoryJpaRepository customerExportHistoryJpaRepositoryMock =
      mock(CustomerExportHistoryJpaRepository.class);
  BucketComponent bucketComponentMock = mock(BucketComponent.class);
  FileWriter fileWriterMock = mock(FileWriter.class);
  TemplateResolverEngine templateResolverEngine = new TemplateResolverEngine();
  SesConf sesConfMock = mock(SesConf.class);
  CustomerExportHistorySavedService subject =
      new CustomerExportHistorySavedService(
          emailServiceMock,
          customerExportHistoryJpaRepositoryMock,
          bucketComponentMock,
          fileWriterMock,
          templateResolverEngine,
          sesConfMock);

  @BeforeEach
  void setUp() {
    when(sesConfMock.getAdminEmail()).thenReturn(ADMIN_EMAIL);
  }

  @SneakyThrows
  @Test
  void send_mail_with_full_subject() {
    var customerExportHistoryIdentifier = randomUUID().toString();
    var fileKey = randomUUID().toString();
    var customerExportHistoryMock = mock(CustomerExportHistory.class);
    var attachmentFileMock = mock(File.class);
    var emailSubject = "Liste des clients exportés pour la période de 5/2026";
    var emailBody = getBody();
    var emptyBytes = new byte[0];

    when(customerExportHistoryMock.getFileKey()).thenReturn(fileKey);
    when(customerExportHistoryMock.getAdditionalProperties())
        .thenReturn(new HashMap<>(Map.of("month", 5, "year", 2026)));
    when(customerExportHistoryJpaRepositoryMock.findById(customerExportHistoryIdentifier))
        .thenReturn(Optional.of(customerExportHistoryMock));
    when(bucketComponentMock.download(fileKey, true)).thenReturn(attachmentFileMock);
    when(fileWriterMock.writeAsByte(attachmentFileMock)).thenReturn(emptyBytes);

    assertDoesNotThrow(
        () -> subject.accept(new CustomerExportHistorySaved(customerExportHistoryIdentifier)));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(emailServiceMock)
        .sendEmail(
            eq(ADMIN_EMAIL),
            eq("tech@birdia.fr"),
            eq(emailSubject),
            eq(emailBody),
            listCaptor.capture());
    var attachment = (Attachment) listCaptor.getValue().getFirst();
    assertEquals(
        Attachment.builder().name(attachment.getName()).content(emptyBytes).build(), attachment);
    assertTrue(
        attachment.getName().contains("Birdia - Liste des clients exportés pour suivi Stripe "));
    assertTrue(attachment.getName().contains(".xlsx"));
  }

  private static @NotNull String getBody() {
    return """
<html xmlns="http://www.w3.org/1999/html">
<head>
    <title>Customer export history</title>
    <style>
        * {
            font-family: Arial, Verdana, Georgia, and Courier, serif;
            color: black;
        }

        h1, section, footer {
            margin: 2vh 2vw;
        }

        h1 {
            font-size: 1.2em;
            font-weight: 500;
        }
    </style>
</head>
<body>
<p>
    Bonjour,
</p>
<p> Veuillez trouver en pièce jointe le résultat de l'export des clients facturés <span> pour le <span>5</span>/<span>2026</span></span></p>
<p>Cordialement,</p>
<p>L'équipe Birdia.</p>
</body>
</html>""";
  }

  @SneakyThrows
  @Test
  void send_mail_with_minimal_subject() {
    var customerExportHistoryIdentifier = randomUUID().toString();
    var fileKey = randomUUID().toString();
    var customerExportHistoryMock = mock(CustomerExportHistory.class);
    var attachmentFileMock = mock(File.class);
    var emailSubject = "Liste des clients exportés le " + now();
    var emailBody = getEmailBody();
    var emptyBytes = new byte[0];

    when(customerExportHistoryMock.getFileKey()).thenReturn(fileKey);
    when(customerExportHistoryMock.getAdditionalProperties()).thenReturn(new HashMap<>());
    when(customerExportHistoryJpaRepositoryMock.findById(customerExportHistoryIdentifier))
        .thenReturn(Optional.of(customerExportHistoryMock));
    when(bucketComponentMock.download(fileKey, true)).thenReturn(attachmentFileMock);
    when(fileWriterMock.writeAsByte(attachmentFileMock)).thenReturn(emptyBytes);

    assertDoesNotThrow(
        () -> subject.accept(new CustomerExportHistorySaved(customerExportHistoryIdentifier)));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(emailServiceMock)
        .sendEmail(
            eq(ADMIN_EMAIL),
            eq("tech@birdia.fr"),
            eq(emailSubject),
            eq(emailBody),
            listCaptor.capture());
    var attachment = (Attachment) listCaptor.getValue().getFirst();
    assertEquals(
        Attachment.builder().name(attachment.getName()).content(emptyBytes).build(), attachment);
    assertTrue(
        attachment.getName().contains("Birdia - Liste des clients exportés pour suivi Stripe "));
    assertTrue(attachment.getName().contains(".xlsx"));
  }

  private String getEmailBody() {
    return """
           <html xmlns="http://www.w3.org/1999/html">
           <head>
               <title>Customer export history</title>
               <style>
                   * {
                       font-family: Arial, Verdana, Georgia, and Courier, serif;
                       color: black;
                   }

                   h1, section, footer {
                       margin: 2vh 2vw;
                   }

                   h1 {
                       font-size: 1.2em;
                       font-weight: 500;
                   }
               </style>
           </head>
           <body>
           <p>
               Bonjour,
           </p>
           <p> Veuillez trouver en pièce jointe le résultat de l'export des clients facturés </p>
           <p>Cordialement,</p>
           <p>L'équipe Birdia.</p>
           </body>
           </html>""";
  }

  @SneakyThrows
  @Test
  void do_nothing_when_customer_export_history_not_found() {
    var customerExportHistoryIdentifier = randomUUID().toString();

    when(customerExportHistoryJpaRepositoryMock.findById(customerExportHistoryIdentifier))
        .thenReturn(Optional.empty());

    assertDoesNotThrow(
        () -> subject.accept(new CustomerExportHistorySaved(customerExportHistoryIdentifier)));

    verify(emailServiceMock, never()).sendEmail(any(), any(), any(), any());
    verify(bucketComponentMock, never()).download(any(), anyBoolean());
    verify(fileWriterMock, never()).writeAsByte(any());
  }
}
