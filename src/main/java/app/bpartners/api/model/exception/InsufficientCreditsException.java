package app.bpartners.api.model.exception;

import lombok.Getter;

@Getter
public class InsufficientCreditsException extends ApiException {
  private final long requiredCredits;
  private final long availableCredits;

  public InsufficientCreditsException(long requiredCredits, long availableCredits) {
    super(
        ExceptionType.CLIENT_EXCEPTION,
        "Insufficient credits, "
            + requiredCredits
            + " required but "
            + availableCredits
            + " available");
    this.requiredCredits = requiredCredits;
    this.availableCredits = availableCredits;
  }
}
