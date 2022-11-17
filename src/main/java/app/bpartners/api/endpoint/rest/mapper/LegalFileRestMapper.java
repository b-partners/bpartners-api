package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.LegalFile;
import org.springframework.stereotype.Component;

@Component
public class LegalFileRestMapper {
  public LegalFile toRest(app.bpartners.api.model.LegalFile domain) {
    return new LegalFile()
        .id(domain.getId())
        .name(domain.getName())
        .fileUrl(domain.getFileUrl())
        .approvalDatetime(domain.getApprovalDatetime());
  }
}
