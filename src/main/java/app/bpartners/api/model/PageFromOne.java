package app.bpartners.api.model;

import app.bpartners.api.model.exception.BadRequestException;
import lombok.Getter;

public class PageFromOne {

  @Getter
  private final int value;

  public PageFromOne(int value) {
    if (value < 1) {
      throw new BadRequestException("page must be >=1");
    }
    this.value = value;
  }
}
