package app.bpartners.api.model;

import app.bpartners.api.model.exception.BadRequestException;
import lombok.Getter;

public class BoundedPageSize {

  @Getter
  private final int value;

  private static final int MAX_SIZE = 500;

  public BoundedPageSize(int value) {
    if (value < 1) {
      throw new BadRequestException("page size must be >=1");
    }
    if (value > MAX_SIZE) {
      throw new BadRequestException("page size must be <" + MAX_SIZE);
    }
    this.value = value;
  }
}
