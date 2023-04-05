package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.validator.CompanyInfoRestValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CompanyInfoMapper {
  private final CompanyInfoRestValidator validator;

  public app.bpartners.api.model.CompanyInfo toDomain(CompanyInfo rest) {
    validator.accept(rest);
    return app.bpartners.api.model.CompanyInfo.builder()
        .email(rest.getEmail())
        .tvaNumber(rest.getTvaNumber())
        .phone(rest.getPhone())
        .socialCapital(rest.getSocialCapital())
        .subjectToVat(rest.getIsSubjectToVat() != null && rest.getIsSubjectToVat())
        .location(rest.getLocation())
        .townCode(rest.getTownCode())
        .build();
  }
}
