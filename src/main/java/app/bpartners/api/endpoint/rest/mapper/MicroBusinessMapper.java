package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.UpdateMicroBusiness;
import app.bpartners.api.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class MicroBusinessMapper {
  public Boolean toDomain(UpdateMicroBusiness rest) {
    if (rest == null) {
      throw new BadRequestException("microBusiness is mandatory");
    }
    if (rest.getIsMicroBusiness() == null) {
      throw new BadRequestException("microBusiness is mandatory");
    }
    return rest.getIsMicroBusiness();
  }
}
