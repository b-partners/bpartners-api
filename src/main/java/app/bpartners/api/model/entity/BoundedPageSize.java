package app.bpartners.api.model.entity;

import lombok.Getter;
import app.bpartners.api.model.exception.BadRequestException;

//TODO: should not be in this package
public class BoundedPageSize {

  @Getter
  private final int value;

  private static final int MAX_SIZE = 500;

  public BoundedPageSize(int value) {
    if (value > MAX_SIZE) {
      throw new BadRequestException("page size must be <" + MAX_SIZE);
    }
    this.value = value;
  }
}
