package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class PaginationValidator {
  public void accept(PageFromOne page, BoundedPageSize pageSize) {
    StringBuilder sb = new StringBuilder();
    if (page == null) {
      sb.append("page is mandatory. ");
    }
    if (pageSize == null) {
      sb.append("page_size is mandatory. ");
    }
    String exceptionMessage = sb.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
