package app.bpartners.api.model.exception;

public class TooManyRequestsException extends ApiException {
  public TooManyRequestsException(String message) {
    super(ExceptionType.CLIENT_EXCEPTION, message);
  }

  public TooManyRequestsException(Exception source) {
    super(ExceptionType.CLIENT_EXCEPTION, source);
  }
}
