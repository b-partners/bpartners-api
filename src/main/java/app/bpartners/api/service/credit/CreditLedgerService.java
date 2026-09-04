package app.bpartners.api.service.credit;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditWallet;
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
    var spendableCredits = spendableCreditsInCredits(userIdentifier);
    var movement = draft.isCredit() ? draft.creditsOrZero() : -draft.creditsOrZero();
    if (spendableCredits + movement < 0) {
      throw new InsufficientCreditsException(draft.creditsOrZero(), spendableCredits);
    }
    return creditTransactionRepository.save(
        draft.toBuilder()
            .id(draft.getId() == null ? randomUUID().toString() : draft.getId())
            .creationDatetime(
                draft.getCreationDatetime() == null ? now() : draft.getCreationDatetime())
            .build());
  }

  private long spendableCreditsInCredits(String userId) {
    return CreditWallet.of(creditTransactionRepository.findAllByUserId(userId), now())
        .spendableCredits();
  }
}
