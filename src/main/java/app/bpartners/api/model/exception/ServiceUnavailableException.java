package app.bpartners.api.model.exception;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

public class ServiceUnavailableException extends ApiException {
  public ServiceUnavailableException(String message) {
    super(SERVER_EXCEPTION, message);
  }
}
