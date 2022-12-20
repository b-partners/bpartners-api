package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.LegalFile;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.LegalFileMapper;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.jpa.LegalFileJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.UserLegalFileJpaRepository;
import app.bpartners.api.repository.jpa.model.HLegalFile;
import app.bpartners.api.repository.jpa.model.HUserLegalFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class LegalFileRepositoryImpl implements LegalFileRepository {
  private final LegalFileMapper mapper;
  private final UserJpaRepository userJpaRepository;
  private final LegalFileJpaRepository jpaRepository;
  private final UserLegalFileJpaRepository userLegalJpaRepository;

  @Override
  public LegalFile findByUserIdAndLegalFileId(String userId, String id) {
    Optional<HLegalFile> optionalLegalFile = jpaRepository.findById(id);
    if (optionalLegalFile.isEmpty()) {
      throw new NotFoundException("LegalFile." + id + " is not found");
    }
    HLegalFile legalFile = optionalLegalFile.get();
    HUserLegalFile userLegalFile = userLegalJpaRepository.findByLegalFile_IdAndUser_Id(id, userId);
    return mapper.toDomain(legalFile, userLegalFile);
  }

  @Override
  public LegalFile save(String userId, String legalFileId) {
    HLegalFile optionalLegalFile = jpaRepository.getById(legalFileId);
    HUserLegalFile userLegalFile = userLegalJpaRepository.save(HUserLegalFile.builder()
        .user(userJpaRepository.getById(userId))
        .legalFile(optionalLegalFile)
        .build());
    return mapper.toDomain(userLegalFile.getLegalFile(), userLegalFile);
  }

  @Override
  public List<LegalFile> findAllByUserId(String userId) {
    List<HLegalFile> files = jpaRepository.findAll();
    List<HUserLegalFile> userLegalFiles = userLegalJpaRepository.findAllByUser_Id(userId);
    List<LegalFile> legalFiles = new ArrayList<>();
    files.forEach(
        legalFile -> userLegalFiles.stream()
            .filter(userLegalFile -> legalFile.getId().equals(userLegalFile.getLegalFile().getId()))
            .findFirst()
            .ifPresentOrElse(
                userLegalFile -> legalFiles.add(mapper.toDomain(legalFile, userLegalFile)),
                () -> legalFiles.add(mapper.toDomain(legalFile, null)))
    );
    return legalFiles;
  }

  @Override
  public List<LegalFile> findAllToBeApprovedLegalFilesByUserId(String userId) {
    List<HLegalFile> legalFiles = jpaRepository.findAllByToBeConfirmedTrue();
    return legalFiles.stream().map(legalFile -> mapper.toDomain(legalFile,
        userLegalJpaRepository.findByLegalFile_IdAndUser_Id(legalFile.getId(), userId))).collect(
        Collectors.toUnmodifiableList());
  }
}
