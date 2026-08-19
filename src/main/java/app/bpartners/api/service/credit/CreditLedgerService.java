package app.bpartners.api.service.credit;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.exception.InsufficientCreditsException;
import app.bpartners.api.model.validator.CreditTransactionValidator;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreditLedgerService {
  private final CreditTransactionRepository creditTransactionRepository;
  private final CreditTransactionValidator creditTransactionValidator;

  @Transactional
  public CreditTransaction append(CreditTransaction draft) {
    creditTransactionValidator.accept(draft);

    var userIdentifier = draft.getUserId();
    creditTransactionRepository.acquireWalletLock(userIdentifier);
    var currentBalance = currentBalanceInCredits(userIdentifier);
    var movement = draft.isCredit() ? draft.creditsOrZero() : -draft.creditsOrZero();
    if (currentBalance + movement < 0) {
      throw new InsufficientCreditsException(draft.creditsOrZero(), currentBalance);
    }
    return creditTransactionRepository.save(
        draft.toBuilder()
            .id(draft.getId() == null ? randomUUID().toString() : draft.getId())
            .creationDatetime(
                draft.getCreationDatetime() == null ? now() : draft.getCreationDatetime())
            .build());
  }

  private long currentBalanceInCredits(String userId) {
    return creditTransactionRepository.findAllByUserId(userId).stream()
        .mapToLong(
            transaction ->
                transaction.isCredit() ? transaction.creditsOrZero() : -transaction.creditsOrZero())
        .sum();
  }
}
