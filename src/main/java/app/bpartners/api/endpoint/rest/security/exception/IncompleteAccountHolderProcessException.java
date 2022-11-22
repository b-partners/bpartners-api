package app.bpartners.api.endpoint.rest.security.exception;

import org.springframework.security.core.AuthenticationException;

public class IncompleteAccountHolderProcessException extends AuthenticationException {

  public IncompleteAccountHolderProcessException(String msg) {
    super(msg);
  }
}
