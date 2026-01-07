package app.bpartners.api.integration.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.ProspectCreated;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.ProspectCreatedService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProspectCreatedServiceTest {

  AccountHolderRepository accountHolderRepositoryMock = mock();
  SesService sesServiceMock = mock();
  TemplateResolverEngine templateResolverEngineMock = mock();
  BucketComponent bucketComponentMock = mock();
  FileWriter fileWriterMock = mock();

  ProspectCreatedService subject =
      new ProspectCreatedService(
          accountHolderRepositoryMock,
          sesServiceMock,
          templateResolverEngineMock,
          bucketComponentMock,
          fileWriterMock);

  @SneakyThrows
  @Test
  void trigger_mail_with_attachment() {
    var prospectMock = mock(Prospect.class);
    var attachmentFileKey = randomUUID().toString();
    var accountHolderId = randomUUID().toString();
    var prospectId = randomUUID().toString();
    var updatedAt = now();
    var accountHolderMock = mock(AccountHolder.class);
    var downloadedFileMock = mock(File.class);
    var fileAttachmentMockContent = new byte[0];
    var accountHolderEmail = randomUUID() + "@bpartners.app";
    var prospectName = "name " + randomUUID();
    var emailBody = randomUUID().toString();

    when(prospectMock.getIdHolderOwner()).thenReturn(accountHolderId);
    when(prospectMock.getId()).thenReturn(prospectId);
    when(prospectMock.getName()).thenReturn(prospectName);
    when(accountHolderRepositoryMock.findById(prospectMock.getIdHolderOwner()))
        .thenReturn(accountHolderMock);
    when(accountHolderMock.getEmail()).thenReturn(accountHolderEmail);
    when(bucketComponentMock.download(
            String.format(
                "prospects/%s/notifications/attachments/%s", prospectId, attachmentFileKey),
            true))
        .thenReturn(downloadedFileMock);
    when(fileWriterMock.writeAsByte(downloadedFileMock)).thenReturn(fileAttachmentMockContent);
    when(templateResolverEngineMock.parseTemplateResolver(
            eq("prospect_account_holder_notification"), any()))
        .thenReturn(emailBody);
    doNothing().when(sesServiceMock).sendEmail(any(), any(), any(), any(), any());

    assertDoesNotThrow(
        () -> subject.accept(new ProspectCreated(prospectMock, attachmentFileKey, updatedAt)));

    var attachmentCaptor = ArgumentCaptor.forClass(List.class);
    verify(sesServiceMock)
        .sendEmail(
            eq(accountHolderEmail),
            eq("contact@birdia.fr"),
            eq(
                "[BIRDIA] Notification - Un nouveau prospect \""
                    + prospectName
                    + " \" a besoin de vos services"),
            eq(emailBody),
            attachmentCaptor.capture(),
            eq("tech@birdia.fr"));
    var actualAttachment = (Attachment) attachmentCaptor.getValue().getFirst();
    assertEquals(
        Attachment.builder()
            .name(actualAttachment.getName())
            .content(fileAttachmentMockContent)
            .build(),
        actualAttachment);
    assertNotNull(actualAttachment.getName());
  }

  @SneakyThrows
  @Test
  void trigger_mail_without_attachment() {
    var prospectMock = mock(Prospect.class);
    var accountHolderId = randomUUID().toString();
    var prospectId = randomUUID().toString();
    var updatedAt = now();
    var accountHolderMock = mock(AccountHolder.class);
    var downloadedFileMock = mock(File.class);
    var fileAttachmentMockContent = new byte[0];
    var accountHolderEmail = randomUUID() + "@bpartners.app";
    var prospectName = "name " + randomUUID();
    var emailBody = randomUUID().toString();

    when(prospectMock.getIdHolderOwner()).thenReturn(accountHolderId);
    when(prospectMock.getId()).thenReturn(prospectId);
    when(prospectMock.getName()).thenReturn(prospectName);
    when(accountHolderRepositoryMock.findById(prospectMock.getIdHolderOwner()))
        .thenReturn(accountHolderMock);
    when(accountHolderMock.getEmail()).thenReturn(accountHolderEmail);

    when(fileWriterMock.writeAsByte(downloadedFileMock)).thenReturn(fileAttachmentMockContent);
    when(templateResolverEngineMock.parseTemplateResolver(
            eq("prospect_account_holder_notification"), any()))
        .thenReturn(emailBody);
    doNothing().when(sesServiceMock).sendEmail(any(), any(), any(), any(), any());

    assertDoesNotThrow(() -> subject.accept(new ProspectCreated(prospectMock, null, updatedAt)));

    verify(sesServiceMock)
        .sendEmail(
            eq(accountHolderEmail),
            eq("contact@birdia.fr"),
            eq(
                "[BIRDIA] Notification - Un nouveau prospect \""
                    + prospectName
                    + " \" a besoin de vos services"),
            eq(emailBody),
            eq(new ArrayList<>()),
            eq("tech@birdia.fr"));
  }
}
