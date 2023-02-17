package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.aws.S3Service;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.integration.conf.TestUtils.FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileServiceTest {
  public static final String RANDOM_CHECKSUM = "random_checksum";
  FileService fileService;
  S3Service s3Service;
  FileRepository fileRepository;
  FileMapper fileMapper;
  UserJpaRepository userJpaRepository;

  @BeforeEach
  void setUp() {
    s3Service = mock(S3Service.class);
    fileRepository = mock(FileRepository.class);
    fileMapper = mock(FileMapper.class);
    userJpaRepository = mock(UserJpaRepository.class);
    fileService =
        new FileService(s3Service, fileRepository, fileMapper, userJpaRepository);
  }

  @Test
  void upload_ok() {
    String fileId = FILE_ID;
    FileType fileType = INVOICE;
    String accountId = JOE_DOE_ACCOUNT_ID;
    byte[] fileAsBytes = new byte[0];
    String userId = JOE_DOE_ID;
    when(s3Service.uploadFile(fileType, accountId, fileId, fileAsBytes))
        .thenReturn(RANDOM_CHECKSUM);
    when(userJpaRepository.getById(userId)).thenReturn(HUser.builder().build());
    when(userJpaRepository.save(any())).thenReturn(HUser.builder().build());
    when(fileMapper.toDomain(any(String.class), any(), any(String.class),
        any(String.class))).thenReturn(fileInfo());
    when(fileRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    FileInfo actual = fileService.upload(fileId, fileType, accountId, fileAsBytes, userId);

    assertEquals(RANDOM_CHECKSUM, actual.getSha256());
  }

  @Test
  void download_optional_ko() {
    when(fileRepository.getOptionalByIdAndAccountId(any(String.class),
        any(String.class))).thenReturn(Optional.empty());

    FileType anyFileType = FileType.LOGO;
    String anyAccountId = String.valueOf(randomUUID());
    String anyFileId = String.valueOf(randomUUID());
    List<byte[]> downloadedFile =
        fileService.downloadOptionalFile(anyFileType, anyAccountId, anyFileId + ".jpeg");

    assertTrue(downloadedFile.isEmpty());
  }


  FileInfo fileInfo() {
    return FileInfo.builder()
        .id(FILE_ID)
        .sha256(RANDOM_CHECKSUM)
        .sizeInKb(0)
        .build();
  }
}
