package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateFee;
import app.bpartners.api.endpoint.rest.model.Fee.TypeEnum;
import app.bpartners.api.endpoint.rest.validator.CreateFeeValidator;
import app.bpartners.api.model.Fee;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.service.UserService;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toUnmodifiableList;

@Component
@AllArgsConstructor
public class FeeMapper {

  private final UserService userService;
  private final CreateFeeValidator createFeeValidator;

  public app.bpartners.api.endpoint.rest.model.Fee toRestFee(Fee fee) {
    return new app.bpartners.api.endpoint.rest.model.Fee()
        .id(fee.getId())
        .studentId(fee.getStudent().getId())
        .status(fee.getStatus())
        .type(fee.getType())
        .totalAmount(fee.getTotalAmount())
        .remainingAmount(fee.getRemainingAmount())
        .comment(fee.getComment())
        .creationDatetime(fee.getCreationDatetime())
        .dueDatetime(fee.getDueDatetime());
  }

  private Fee toDomainFee(User student, CreateFee createFee) {
    createFeeValidator.accept(createFee);
    if (!student.getRole().equals(User.Role.STUDENT)) {
      throw new BadRequestException("Only students can have fees");
    }
    return Fee.builder()
        .student(student)
        .type(toDomainFeeType(Objects.requireNonNull(createFee.getType())))
        .totalAmount(createFee.getTotalAmount())
        .comment(createFee.getComment())
        .creationDatetime(createFee.getCreationDatetime())
        .dueDatetime(createFee.getDueDatetime())
        .build();
  }

  public List<Fee> toDomainFee(String studentId, List<CreateFee> toCreate) {
    User student = userService.getById(studentId);
    if (student == null) {
      throw new NotFoundException("Student.id=" + studentId + " is not found");
    }
    return toCreate
        .stream()
        .map(createFee -> toDomainFee(student, createFee))
        .collect(toUnmodifiableList());
  }

  private TypeEnum toDomainFeeType(CreateFee.TypeEnum createFeeType) {
    switch (createFeeType) {
      case TUITION:
        return TypeEnum.TUITION;
      case HARDWARE:
        return TypeEnum.HARDWARE;
      default:
        throw new BadRequestException("Unexpected feeType: " + createFeeType.getValue());
    }
  }
}
