package app.bpartners.api.repository.implementation;

import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
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

  //TODO: persist the payment initiation as paymentRequest and particularly save the sessionId
  @Override
  public List<app.bpartners.api.model.PaymentRedirection> save(
      app.bpartners.api.model.PaymentInitiation toCreate) {
    PaymentInitiation paymentInitiation = mapper.toFintecturePaymentReq(toCreate);
    PaymentRedirection paymentRedirection =
        fintectureRepository.save(paymentInitiation, toCreate.getSuccessUrl());
    return List.of(mapper.toDomain(paymentRedirection, toCreate));
  }
}
