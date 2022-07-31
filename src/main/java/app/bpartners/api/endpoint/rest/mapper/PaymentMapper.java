package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePayment;
import app.bpartners.api.endpoint.rest.model.Payment.TypeEnum;
import app.bpartners.api.endpoint.rest.validator.CreatePaymentValidator;
import app.bpartners.api.model.Fee;
import app.bpartners.api.model.Payment;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.service.FeeService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toUnmodifiableList;

@Component
@AllArgsConstructor
public class PaymentMapper {
  private final FeeService feeService;
  private final CreatePaymentValidator createPaymentValidator;

  public app.bpartners.api.endpoint.rest.model.Payment toRestPayment(Payment payment) {
    return new app.bpartners.api.endpoint.rest.model.Payment()
        .id(payment.getId())
        .feeId(payment.getFee().getId())
        .type(payment.getType())
        .amount(payment.getAmount())
        .comment(payment.getComment())
        .creationDatetime(payment.getCreationDatetime());
  }

  private Payment toDomainPayment(
      Fee associatedFee, CreatePayment createPayment) {
    createPaymentValidator.accept(createPayment);
    return Payment.builder()
        .fee(associatedFee)
        .type(toDomainPaymentType(createPayment.getType()))
        .amount(createPayment.getAmount())
        .comment(createPayment.getComment())
        .build();
  }

  public List<Payment> toDomainPayment(
      String feeId, List<CreatePayment> createPayment) {
    Fee associatedFee = feeService.getById(feeId);
    if (associatedFee == null) {
      throw new NotFoundException("Fee.id=" + feeId + " is not found");
    }
    return createPayment.stream()
        .map(payment -> toDomainPayment(associatedFee, payment))
        .collect(toUnmodifiableList());
  }

  private TypeEnum toDomainPaymentType(CreatePayment.TypeEnum createPaymentType) {
    switch (createPaymentType) {
      case CASH:
        return TypeEnum.CASH;
      case SCHOLARSHIP:
        return TypeEnum.SCHOLARSHIP;
      case MOBILE_MONEY:
        return TypeEnum.MOBILE_MONEY;
      case FIX:
        return TypeEnum.FIX;
      default:
        throw new BadRequestException("Unexpected paymentType: " + createPaymentType.getValue());
    }
  }
}
