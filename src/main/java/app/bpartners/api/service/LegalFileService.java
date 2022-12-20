package app.bpartners.api.service;

import app.bpartners.api.model.LegalFile;
import app.bpartners.api.repository.LegalFileRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LegalFileService {
  private final LegalFileRepository repository;

  public List<LegalFile> getLegalFiles(String userId) {
    return repository.findAllByUserId(userId);
  }

  public LegalFile approveLegalFile(String userId, String legalFileId) {
    return repository.save(userId, legalFileId);
  }

  public List<LegalFile> getAllToBeApprovedLegalFilesByUserId(String userId) {
    return repository.findAllToBeConfirmedLegalFilesByUserId(userId);
  }
}
