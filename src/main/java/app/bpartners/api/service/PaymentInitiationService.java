package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.PaymentRequestRepository;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
public class PaymentInitiationService {
  private final PaymentInitiationRepository repository;
  private final PaymentRequestRepository requestRepository;

  public List<PaymentRedirection> createPaymentReq(List<PaymentInitiation> paymentReqs) {
    if (paymentReqs.size() > 1) {
      throw new NotImplementedException("Only one payment request is supported.");
    }
    return repository.save(paymentReqs.get(0));
  }

  public PaymentRedirection initiateInvoicePayment(Invoice invoice) {
    if (Objects.equals(invoice.getTotalPriceWithVat(), new Fraction())) {
      return new PaymentRedirection();
    }
    PaymentInitiation paymentInitiation = PaymentInitiation.builder()
        .id(String.valueOf(randomUUID()))
        .reference(invoice.getRef())
        .label(invoice.getTitle())
        .amount(invoice.getTotalPriceWithVat())
        .payerName(invoice.getInvoiceCustomer().getName())
        .payerEmail(invoice.getInvoiceCustomer().getEmail())
        .successUrl("https://dashboard-dev.bpartners.app") //TODO: to change
        .failureUrl("https://dashboard-dev.bpartners.app") //TODO: to change
        .build();
    return repository.save(paymentInitiation, invoice.getId()).get(0);
  }

  public List<PaymentRequest> getPaymentReqByAccountId(
      String accountId, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(),
            Sort.by("createdDatetime").descending());
    return requestRepository.findByAccountId(accountId, pageable);
  }
}
