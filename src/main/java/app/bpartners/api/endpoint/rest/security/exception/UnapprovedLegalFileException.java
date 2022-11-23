package app.bpartners.api.endpoint.rest.security.exception;

import org.springframework.security.core.AuthenticationException;

public class UnapprovedLegalFileException extends AuthenticationException {

  public UnapprovedLegalFileException(String msg) {
    super(msg);
  }
}
