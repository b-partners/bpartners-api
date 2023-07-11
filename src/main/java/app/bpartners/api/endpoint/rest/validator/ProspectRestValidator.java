package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ProspectRestValidator implements Consumer<UpdateProspect> {
  public static final String XLS_FILE = "application/vnd.ms-excel";
  public static final String XLSX_FILE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  @Override
  public void accept(UpdateProspect prospect) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (prospect.getId() == null) {
      exceptionMessageBuilder.append("Id is mandatory. ");
    }
    if (prospect.getStatus() == null) {
      exceptionMessageBuilder.append("Status is mandatory. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public String validateAccept(String headerValue) {
    if (headerValue == null) {
      throw new BadRequestException("Accept Header is mandatory. Types [application/json,"
          + XLS_FILE + "," + XLSX_FILE + "] are supported");
    }
    boolean isExcelFile = headerValue.equals(XLS_FILE) || headerValue.equals(XLSX_FILE);
    boolean isJsonFile = headerValue.equals(MediaType.APPLICATION_JSON_VALUE);
    if (!isExcelFile && !isJsonFile) {
      throw new NotImplementedException(headerValue + " is not supported."
          + "Only types [application/json," + XLS_FILE + "," + XLSX_FILE + "] are supported");
    }
    return headerValue;
  }
}
