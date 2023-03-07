package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.model.FPaymentInitiation;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import app.bpartners.api.repository.mapper.FintectureMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PaymentInitiationRepositoryImpl implements PaymentInitiationRepository {
  private final FintecturePaymentInitiationRepository
      fintectureRepository;
  private final FintectureMapper mapper;
  private final PaymentRequestJpaRepository paymentRequestRepository;
  private final PaymentRequestMapper paymentRequestMapper;

  @Override
  public List<PaymentRedirection> saveAll(
      List<PaymentInitiation> paymentInitiations, String invoice) {
    return paymentInitiations.stream()
        .map(domain -> {
          FPaymentRedirection paymentRedirection = initiatePayment(domain, invoice);
          return mapper.toDomain(paymentRedirection, domain);
        }).collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<HPaymentRequest> retrievePaymentEntities(
      List<PaymentInitiation> paymentInitiations, String invoice, InvoiceStatus status) {
    return paymentInitiations.stream()
        .map(payment -> paymentRequestMapper.toEntity(null, payment, invoice))
        .collect(Collectors.toList());
  }

  @Override
  public List<HPaymentRequest> retrievePaymentEntities(
      List<PaymentInitiation> paymentInitiations, String invoice) {
    return paymentInitiations.stream()
        .map(payment -> {
          FPaymentInitiation paymentInitiation = mapper.toFintectureResource(payment);
          FPaymentRedirection paymentRedirection =
              fintectureRepository.save(paymentInitiation, payment.getSuccessUrl());
          return paymentRequestMapper.toEntity(paymentRedirection, payment, invoice);
        })
        .collect(Collectors.toList());
  }

  private FPaymentRedirection initiatePayment(PaymentInitiation domain, String invoice) {
    FPaymentInitiation paymentInitiation = mapper.toFintectureResource(domain);
    FPaymentRedirection paymentRedirection =
        fintectureRepository.save(paymentInitiation, domain.getSuccessUrl());
    HPaymentRequest entity = paymentRequestMapper.toEntity(paymentRedirection, domain, invoice);
    paymentRequestRepository.save(entity);
    return paymentRedirection;
  }
}
