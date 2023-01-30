package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.model.FPaymentInitiation;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.mapper.FintectureMapper;
import java.util.List;
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
  public List<app.bpartners.api.model.PaymentRedirection> save(PaymentInitiation domain) {
    FPaymentInitiation paymentInitiation = mapper.toFintecturePaymentReq(domain);
    FPaymentRedirection paymentRedirection =
        fintectureRepository.save(paymentInitiation, domain.getSuccessUrl());
    paymentRequestRepository
        .save(paymentRequestMapper.toEntity(paymentRedirection, domain, null));
    return List.of(mapper.toDomain(paymentRedirection, domain));
  }

  @Override
  public List<PaymentRedirection> save(PaymentInitiation domain, String idInvoice) {
    FPaymentInitiation paymentInitiation = mapper.toFintecturePaymentReq(domain);
    FPaymentRedirection paymentRedirection =
        fintectureRepository.save(paymentInitiation, domain.getSuccessUrl());
    paymentRequestRepository
        .save(paymentRequestMapper.toEntity(paymentRedirection, domain, idInvoice));
    return List.of(mapper.toDomain(paymentRedirection, domain));
  }
}
