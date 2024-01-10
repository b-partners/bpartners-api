package app.bpartners.api.repository.implementation;

import static app.bpartners.api.endpoint.rest.security.AuthProvider.getAuthenticatedUser;

import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.PaymentInitiationRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.model.FPaymentInitiation;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import app.bpartners.api.repository.mapper.FintectureMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class PaymentInitiationRepositoryImpl implements PaymentInitiationRepository {
  private final FintecturePaymentInitiationRepository repository;
  private final FintectureMapper mapper;
  private final PaymentRequestJpaRepository paymentRequestRepository;
  private final PaymentRequestMapper paymentRequestMapper;
  private final UserRepository userRepository;

  @Override
  public List<PaymentRedirection> saveAll(
      List<PaymentInitiation> paymentInitiations, String invoice, User user) {
    return paymentInitiations.stream()
        .map(
            domain -> {
              FPaymentRedirection paymentRedirection = initiatePayment(domain, invoice, user);
              return mapper.toDomain(paymentRedirection, domain);
            })
        .toList();
  }

  @Override
  public List<HPaymentRequest> retrievePaymentEntities(
      List<PaymentInitiation> paymentInitiations, String invoice) {
    return paymentInitiations.stream()
        .map(payment -> paymentRequestMapper.toEntity(null, payment, invoice))
        .collect(Collectors.toList());
  }

  @Override
  public List<HPaymentRequest> retrievePaymentEntitiesWithUrl(
      List<PaymentInitiation> paymentInitiations, String invoice, User user) {
    User authenticatedUser = getAuthenticatedUser() == null ? user : getAuthenticatedUser();

    return paymentInitiations.stream()
        .map(
            payment -> {
              FPaymentInitiation paymentInitiation =
                  mapper.toFintectureResource(
                      payment,
                      authenticatedUser.getDefaultAccount(),
                      authenticatedUser.getDefaultHolder());
              FPaymentRedirection paymentRedirection =
                  repository.save(paymentInitiation, payment.getSuccessUrl());
              return paymentRequestMapper.toEntity(paymentRedirection, payment, invoice);
            })
        .collect(Collectors.toList());
  }

  private FPaymentRedirection initiatePayment(PaymentInitiation domain, String invoice, User user) {
    User authenticatedUser = getAuthenticatedUser() == null ? user : getAuthenticatedUser();
    FPaymentInitiation paymentInitiation =
        mapper.toFintectureResource(
            domain, authenticatedUser.getDefaultAccount(), authenticatedUser.getDefaultHolder());
    FPaymentRedirection paymentRedirection =
        repository.save(paymentInitiation, domain.getSuccessUrl());
    HPaymentRequest entity = paymentRequestMapper.toEntity(paymentRedirection, domain, invoice);
    paymentRequestRepository.save(entity);
    return paymentRedirection;
  }
}
