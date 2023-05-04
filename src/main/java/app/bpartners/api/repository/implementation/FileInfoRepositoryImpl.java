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
  public FileInfo findById(String id) {
    return mapper.toDomain(jpaRepository.findById(id)
        .orElseThrow(
            () -> new NotFoundException("File." + id + " not found.")
        ));
  }

  public Optional<FileInfo> findOptionalById(String id) {
    Optional<HFileInfo> optional = jpaRepository.findById(id);
    return optional.map(mapper::toDomain);
  }

  @Override
  public FileInfo save(FileInfo file) {
    HFileInfo toSave = mapper.toEntity(file);
    return mapper.toDomain(jpaRepository.save(toSave));
  }
}
