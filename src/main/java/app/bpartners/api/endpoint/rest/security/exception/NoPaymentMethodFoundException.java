package app.bpartners.api.endpoint.rest.security.exception;

import org.springframework.security.core.AuthenticationException;

public class NoPaymentMethodFoundException extends AuthenticationException {
  public NoPaymentMethodFoundException(String message) {
    super(message);
  }
}
