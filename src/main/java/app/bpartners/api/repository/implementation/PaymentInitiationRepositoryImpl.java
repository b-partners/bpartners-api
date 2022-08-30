package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.implementation.FPaymentInitiationRepositoryImpl;
import app.bpartners.api.repository.mapper.FintectureMapper;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PaymentInitiationRepositoryImpl implements PaymentInitiationRepository {
  private final FPaymentInitiationRepositoryImpl fintectureRepository;
  private final FintectureMapper mapper;

  @Override
  public List<PaymentRedirection> save(PaymentInitiation toCreate) {
    app.bpartners.api.repository.fintecture.model.PaymentInitiation paymentInitiation =
        mapper.toFintecturePaymentReq(toCreate);
    return List.of(mapper.toDomain(fintectureRepository.save(paymentInitiation,
        toCreate.getSuccessUrl()), toCreate.getSuccessUrl()));
  }
}
