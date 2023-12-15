package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CompanyInfoRestValidator implements Consumer<CompanyInfo> {
  @Override
  public void accept(CompanyInfo companyInfo) {
    // todo: merge with companyInfo validator when it will be used
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (companyInfo.getLocation() != null) {
      if (companyInfo.getLocation().getLatitude() == null) {
        exceptionMessageBuilder.append("latitude is mandatory. ");
      }
      if (companyInfo.getLocation().getLongitude() == null) {
        exceptionMessageBuilder.append("longitude is mandatory. ");
      }
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
