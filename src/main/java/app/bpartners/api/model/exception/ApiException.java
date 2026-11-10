package app.bpartners.api.model.exception;

import lombok.Getter;

public class ApiException extends RuntimeException {

  @Getter private final ExceptionType type;
  @Getter private final Integer status;

  public ApiException(ExceptionType type, String message) {
    this(type, message, null);
  }

  public ApiException(ExceptionType type, String message, Integer status) {
    super(message);
    this.type = type;
    this.status = status;
  }

  public ApiException(ExceptionType type, Exception source) {
    super(source);
    this.type = type;
    this.status = null;
  }

  public enum ExceptionType {
    CLIENT_EXCEPTION,
    SERVER_EXCEPTION
  }
}
