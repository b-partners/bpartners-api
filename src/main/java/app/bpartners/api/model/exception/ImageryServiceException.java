package app.bpartners.api.model.exception;

public class ImageryServiceException extends RuntimeException {
  public ImageryServiceException(String message) {
    super(message);
  }

  public ImageryServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
