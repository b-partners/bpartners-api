package app.bpartners.api.model.entity.validator;

import app.bpartners.api.model.entity.HUser;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserValidator implements Consumer<HUser> {
  private final Validator validator;

  public void accept(List<HUser> users) {
    users.forEach(this);
  }

  @Override
  public void accept(HUser user) {
    Set<ConstraintViolation<HUser>> violations = validator.validate(user);
    if (!violations.isEmpty()) {
      String constraintMessages = violations
          .stream()
          .map(ConstraintViolation::getMessage)
          .collect(Collectors.joining(". "));
      throw new BadRequestException(constraintMessages);
    }
  }
}
