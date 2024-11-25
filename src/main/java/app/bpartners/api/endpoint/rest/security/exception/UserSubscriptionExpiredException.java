package app.bpartners.api.endpoint.rest.security.exception;

import org.springframework.security.core.AuthenticationException;

public class UserSubscriptionExpiredException extends AuthenticationException {

  public UserSubscriptionExpiredException(String msg) {
    super(msg);
  }
}
