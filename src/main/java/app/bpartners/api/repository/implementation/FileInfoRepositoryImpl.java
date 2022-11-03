package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.repository.jpa.FileInfoJpaRepository;
import app.bpartners.api.repository.jpa.model.HFileInfo;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class FileInfoRepositoryImpl implements FileRepository {
  private final FileInfoJpaRepository jpaRepository;
  private final FileMapper mapper;

  @Override
  public FileInfo getByAccountIdAndId(String accountId, String id) {
    Optional<HFileInfo> entity = jpaRepository.getByAccountIdAndId(accountId, id);
    return mapper.toDomain(entity
        .orElseThrow(
            () -> new NotFoundException("File." + id + " not found.")
        ));
  }

  @Override
  public FileInfo getById(String id) {
    return mapper.toDomain(jpaRepository.getById(id));
  }

  @Override
  public Optional<FileInfo> getOptionalById(String id) {
    Optional<HFileInfo> optional = jpaRepository.findById(id);
    return optional.map(mapper::toDomain);
  }

  @Override
  public FileInfo save(FileInfo file) {
    HFileInfo toSave = mapper.toEntity(file);
    return mapper.toDomain(jpaRepository.save(toSave));
  }
}
