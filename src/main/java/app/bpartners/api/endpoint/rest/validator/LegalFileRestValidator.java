package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.model.LegalFile;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.LegalFileRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class LegalFileRestValidator {
  private final LegalFileRepository repository;

  public void accept(String userId, String legalFileId) {
    LegalFile legalFile = repository.findByUserIdAndLegalFileId(userId, legalFileId);
    if (legalFile.isApproved()) {
      throw new BadRequestException("LegalFile." + legalFileId + " was already approved on "
          + legalFile.getApprovalDatetime());
    }
  }
}
