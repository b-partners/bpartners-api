package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CompanyInfoMapper {


  public app.bpartners.api.model.CompanyInfo toDomain(
      CompanyInfo companyInfo) {
    return app.bpartners.api.model.CompanyInfo.builder()
        .email(companyInfo.getEmail())
        .tvaNumber(companyInfo.getTvaNumber())
        .phone(companyInfo.getPhone())
        .socialCapital(companyInfo.getSocialCapital())
        .build();
  }
}
