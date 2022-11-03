package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CompanyInfoValidator implements Consumer<CompanyInfo> {
  @Override
  public void accept(CompanyInfo companyInfo) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (companyInfo.getEmail() == null) {
      exceptionMessageBuilder.append("Email is mandatory. ");
    }
    if (companyInfo.getPhone() == null) {
      exceptionMessageBuilder.append("Phone is mandatory. ");
    }
    if (companyInfo.getTvaNumber() == null) {
      exceptionMessageBuilder.append("Tva number is mandatory. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isBlank()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
