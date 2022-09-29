package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.service.aws.S3Service;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FileService {
  private final S3Service s3Service;
  private final FileRepository repository;
  private final FileMapper mapper;

  private final AuthProvider authProvider;

  public byte[] uploadFile(String accountId, String fileId, byte[] toUpload) {
    String checksum = s3Service.uploadFile(accountId, fileId, toUpload);
    repository.save(mapper.toDomain(fileId, toUpload, checksum));
    return toUpload;
  }

  public byte[] downloadFile(String queryBearer, String accountId,
                             String fileId) {
    Principal principal = authProvider.getPrincipalByBearer(queryBearer);
    if (!principal.getAccount().getId().equals(accountId)) {
      throw new ForbiddenException();
    }
    return s3Service.downloadFile(accountId, fileId);
  }

  public FileInfo getFileByAccountIdAndId(String accountId, String fileId) {
    return repository.getByAccountIdAndId(accountId, fileId);
  }
}
