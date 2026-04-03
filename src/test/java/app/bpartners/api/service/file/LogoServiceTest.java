package app.bpartners.api.service.file;

import static app.bpartners.api.file.hash.FileHashAlgorithm.SHA256;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.LogoCompressionTriggered;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.file.hash.FileHash;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.aws.S3Service;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LogoServiceTest {
  static final String USER_ID = randomUUID().toString();
  static final String FILE_ID = randomUUID().toString();
  FileRepository fileRepository = mock();
  FileWriter fileWriter = mock();
  FileMapper fileMapper = mock();
  S3Service s3Service = mock();
  UserJpaRepository userJpaRepository = mock();
  EventProducer eventProducer = mock();
  AccountRepository accountRepository = mock();

  LogoService subject =
      new LogoService(
          fileRepository,
          fileWriter,
          fileMapper,
          s3Service,
          userJpaRepository,
          eventProducer,
          accountRepository);

  @Test
  void trigger_logo_compression_call_event_producer() {
    subject.triggerLogoCompression(USER_ID, FILE_ID);

    verify(eventProducer)
        .accept(
            argThat(
                list -> {
                  LogoCompressionTriggered event =
                      (LogoCompressionTriggered) list.iterator().next();
                  return list.size() == 1
                      && USER_ID.equals(event.getUserId())
                      && FILE_ID.equals(event.getUserLogoFileId());
                }));
  }

  @Test
  void compression_integrity() throws IOException {
    String userId = randomUUID().toString();
    String fileId = randomUUID().toString();
    File logoFile = new ClassPathResource("files/logo_company.jpeg").getFile();
    FileInfo fileInfo = mock();
    String hashValue = randomUUID().toString();
    byte[] bytes = new byte[10];
    HUser hUser = HUser.builder().id(userId).logoFileId(fileId).build();
    String expectedCompressedLogoFileId = "compressed_logo_" + fileId;
    when(s3Service.uploadFile(any(), any(), any(), any()))
        .thenReturn(new FileHash(SHA256, hashValue));
    when(fileWriter.writeAsByte(logoFile)).thenReturn(bytes);
    when(fileMapper.toDomain(any(), any(), any(), any())).thenReturn(fileInfo);
    when(fileRepository.save(any())).thenReturn(fileInfo);
    when(fileInfo.getSha256()).thenReturn(hashValue);
    when(userJpaRepository.getById(userId)).thenReturn(hUser);
    when(userJpaRepository.save(any())).then(i -> i.getArgument(0));
    when(accountRepository.findByUserId(userId))
        .thenReturn(List.of(new Account().toBuilder().id(userId).build()));

    var actual = subject.compressUserLogo(userId, logoFile, fileId);

    assertEquals(hashValue, actual.getSha256());
    verify(userJpaRepository)
        .save(hUser.toBuilder().logoFileId(expectedCompressedLogoFileId).build());
  }

  @Test
  void is_compressed() {
    String compressedFileId = "compressed_logo_" + randomUUID();

    assertTrue(subject.isCompressedLogo(compressedFileId));
    assertFalse(subject.isCompressedLogo(randomUUID().toString()));
  }
}
