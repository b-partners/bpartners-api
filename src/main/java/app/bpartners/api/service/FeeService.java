package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Fee;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.Payment;
import app.bpartners.api.model.validator.FeeValidator;
import app.bpartners.api.repository.FeeRepository;
import java.time.Instant;
import java.util.List;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
@AllArgsConstructor
public class FeeService {

  private final FeeRepository feeRepository;
  private final FeeValidator feeValidator;

  public Fee getById(String id) {
    return resetPaymentRelatedInfo(feeRepository.getById(id));
  }

  public Fee getByStudentIdAndFeeId(String studentId, String feeId) {
    return resetPaymentRelatedInfo(feeRepository.getByStudentIdAndId(studentId, feeId));
  }

  @Transactional
  public List<Fee> saveAll(List<Fee> fees) {
    feeValidator.accept(fees);
    return resetPaymentRelatedInfo(feeRepository.saveAll(fees));
  }

  public List<Fee> getFeesByStudentId(
      String studentId, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(
        page.getValue() - 1,
        pageSize.getValue(),
        Sort.by(DESC, "dueDatetime"));
    return resetPaymentRelatedInfo(feeRepository.getByStudentId(studentId, pageable));
  }

  private app.bpartners.api.endpoint.rest.model.Fee.StatusEnum getFeeStatus(Fee fee) {
    if (computeRemainingAmount(fee) == 0) {
      return app.bpartners.api.endpoint.rest.model.Fee.StatusEnum.PAID;
    } else {
      if (Instant.now().isAfter(fee.getDueDatetime())) {
        return app.bpartners.api.endpoint.rest.model.Fee.StatusEnum.LATE;
      }
      return app.bpartners.api.endpoint.rest.model.Fee.StatusEnum.UNPAID;
    }
  }

  private int computeRemainingAmount(Fee fee) {
    List<Payment> payments = fee.getPayments();
    if (payments != null) {
      int amount = payments
          .stream()
          .mapToInt(Payment::getAmount)
          .sum();
      return fee.getTotalAmount() - amount;
    }
    return fee.getTotalAmount();
  }

  private Fee resetPaymentRelatedInfo(Fee initialFee) {
    return Fee.builder()
        .id(initialFee.getId())
        .student(initialFee.getStudent())
        .type(initialFee.getType())
        .remainingAmount(computeRemainingAmount(initialFee))
        .status(getFeeStatus(initialFee))
        .comment(initialFee.getComment())
        .creationDatetime(initialFee.getCreationDatetime())
        .dueDatetime(initialFee.getDueDatetime())
        .payments(initialFee.getPayments())
        .totalAmount(initialFee.getTotalAmount())
        .build();
  }

  private List<Fee> resetPaymentRelatedInfo(List<Fee> fees) {
    return fees.stream()
        .map(this::resetPaymentRelatedInfo)
        .collect(toUnmodifiableList());
  }
}
