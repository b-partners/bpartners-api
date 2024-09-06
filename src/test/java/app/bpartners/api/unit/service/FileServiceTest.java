package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.file.FileHashAlgorithm.SHA256;
import static app.bpartners.api.integration.conf.utils.TestUtils.FILE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.file.FileHash;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.aws.S3Service;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    fileService = new FileService(s3Service, fileRepository, fileMapper, userJpaRepository, mock());
  }

  @Test
  void upload_ok() {
    String fileId = FILE_ID;
    FileType fileType = INVOICE;
    File file = mock();
    String idUser = JOE_DOE_ID;
    when(s3Service.uploadFile(fileType, fileId, idUser, file))
        .thenReturn(new FileHash(SHA256, RANDOM_CHECKSUM));
    when(userJpaRepository.getById(idUser)).thenReturn(HUser.builder().build());
    when(userJpaRepository.save(any())).thenReturn(HUser.builder().build());
    when(fileMapper.toDomain(any(String.class), any(), any(String.class), any(String.class)))
        .thenReturn(fileInfo());
    when(fileRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    FileInfo actual = fileService.upload(fileType, fileId, idUser, file);

    assertEquals(RANDOM_CHECKSUM, actual.getSha256());
  }

  FileInfo fileInfo() {
    return FileInfo.builder().id(FILE_ID).sha256(RANDOM_CHECKSUM).sizeInKb(0).build();
  }
}
