package app.bpartners.api.model.mapper;

import app.bpartners.api.model.LegalFile;
import app.bpartners.api.repository.jpa.model.HLegalFile;
import app.bpartners.api.repository.jpa.model.HUserLegalFile;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class LegalFileMapper {
  public LegalFile toDomain(HLegalFile legalFile, HUserLegalFile userLegalFile) {
    String userId = userLegalFile == null ? null : userLegalFile.getUser().getId();
    Instant approvalDatetime = userLegalFile == null ? null : userLegalFile.getApprovalDatetime();
    return LegalFile.builder()
        .id(legalFile.getId())
        .fileUrl(legalFile.getFileUrl())
        .name(legalFile.getName())
        .userId(userId)
        .approvalDatetime(approvalDatetime)
        .toBeConfirmed(legalFile.isToBeConfirmed())
        .build();
  }
}
