package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.repository.PaymentReqRepository;
import app.bpartners.api.repository.fintecture.implementation.FintecturePaymentReqRepositoryImpl;
import app.bpartners.api.repository.mapper.FintectureMapper;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PaymentReqRepositoryImpl implements PaymentReqRepository {
  private final FintecturePaymentReqRepositoryImpl fintectureRepository;
  private final FintectureMapper mapper;

  @Override
  public List<PaymentRedirection> generatePaymentUrl(PaymentInitiation paymentReq) {
    app.bpartners.api.repository.fintecture.schema.PaymentInitiation fintecturePaymentReq =
        mapper.toFintecturePaymentReq(paymentReq);
    return List.of(mapper.toDomain(fintectureRepository.generatePaymentUrl(fintecturePaymentReq,
        paymentReq.getSuccessUrl()), paymentReq.getSuccessUrl()));
  }
}
