package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateAnnualRevenueTarget;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CreateAnnualRevenueTargetValidator implements Consumer<CreateAnnualRevenueTarget> {

  @Override
  public void accept(CreateAnnualRevenueTarget createAnnualRevenueTarget) {
    StringBuilder message = new StringBuilder();
    if (createAnnualRevenueTarget.getYear() == null) {
      message.append("Year is mandatory. ");
    }
    if (createAnnualRevenueTarget.getAmountTarget() == null) {
      message.append("Amount target is mandatory. ");
    }
    String errorMessage = message.toString();
    if (!errorMessage.isEmpty()) {
      throw new BadRequestException(errorMessage);
    }
  }

  public void accept(List<CreateAnnualRevenueTarget> createAnnualRevenueTargets) {
    createAnnualRevenueTargets.forEach(this);
    checkDuplicatedRevenues(createAnnualRevenueTargets);
  }

  private void checkDuplicatedRevenues(
      List<CreateAnnualRevenueTarget> createAnnualRevenueTargets) {
    for (CreateAnnualRevenueTarget revenue : createAnnualRevenueTargets) {
      List<CreateAnnualRevenueTarget> target = createAnnualRevenueTargets.stream()
          .filter(revenueTarget -> Objects.equals(revenueTarget.getYear(), revenue.getYear()))
          .collect(Collectors.toUnmodifiableList());
      if (target.size() >= 2) {
        throw new NotImplementedException(revenue.getYear() + " is duplicated.");
      }
    }
  }
}
