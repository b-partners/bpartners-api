package app.bpartners.api.model.exception;

public class NotFoundException extends ApiException {
  public NotFoundException(String message) {
    super(ExceptionType.CLIENT_EXCEPTION, message);
  }
}
