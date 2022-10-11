package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
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

  @Override
  public List<PaymentRedirection> save(PaymentInitiation toCreate) {
    app.bpartners.api.repository.fintecture.model.PaymentInitiation fintecturePaymentInit =
        mapper.toFintecturePaymentReq(toCreate);
    app.bpartners.api.repository.fintecture.model.PaymentRedirection fintecturePaymentRedirection =
        fintectureRepository.save(fintecturePaymentInit,
            toCreate.getSuccessUrl());
    return List.of(mapper.toDomain(fintecturePaymentRedirection, toCreate.getSuccessUrl()));
  }
}
