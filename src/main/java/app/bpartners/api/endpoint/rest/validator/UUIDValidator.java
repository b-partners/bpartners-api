package app.bpartners.api.endpoint.rest.validator;

import java.util.UUID;

public class UUIDValidator {
  private UUIDValidator() {}

  public static boolean isValid(String candidate) {
    try {
      UUID.fromString(candidate);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
