package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.PaymentRequestRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentRequestService {
  private final PaymentRequestRepository requestRepository;

  public List<PaymentRequest> getPaymentReqByAccountId(
      String accountId, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(),
            Sort.by("createdDatetime").descending());
    return requestRepository.findByAccountId(accountId, pageable);
  }
}
