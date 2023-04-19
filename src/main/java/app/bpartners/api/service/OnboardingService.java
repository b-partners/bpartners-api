package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedUserUpserted;
import app.bpartners.api.endpoint.event.model.gen.UserUpserted;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Onboarding;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.OnboardingRepository;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.UUID.randomUUID;
import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

@Service
@AllArgsConstructor
public class OnboardingService {
  private final OnboardingRepository repository;
  private final UserRepository userRepository;
  private final AccountRepository accountRepository;
  private final AccountHolderRepository accountHolderRepository;
  private final EventProducer eventProducer;

  public Onboarding generateOnboarding(String successUrl, String failureUrl) {
    Onboarding onboarding = repository.save(successUrl);
    onboarding.setFailureUrl(failureUrl);
    return onboarding;
  }

  @Transactional(isolation = SERIALIZABLE)
  public User onboardUser(User userToSave, String companyName) {
    User savedUser = userRepository.save(userToSave);
    eventProducer.accept(List.of(toTypedUser(savedUser)));
    Account accountToSave = fromNewUser(savedUser);
    Account accountSaved = accountRepository.save(accountToSave, savedUser.getId());
    AccountHolder accountHolderToSave = fromNewAccountAndUser(companyName, accountSaved, savedUser);
    accountHolderRepository.save(accountHolderToSave);
    return savedUser;
  }

  private TypedUserUpserted toTypedUser(User user) {
    return new TypedUserUpserted(
        new UserUpserted()
            .userId(user.getId())
            .email(user.getEmail())
    );
  }

  private AccountHolder fromNewAccountAndUser(String companyName, Account account, User user) {
    return AccountHolder.builder()
        .id(String.valueOf(randomUUID()))
        .accountId(account.getId())
        .name(companyName)
        .initialCashflow(new Fraction())
        .email(user.getEmail())
        .mobilePhoneNumber(user.getMobilePhoneNumber())
        .build();
  }

  private Account fromNewUser(User user) {
    return Account.builder()
        .id(String.valueOf(randomUUID()))
        .name(user.getName())
        .userId(user.getId())
        .availableBalance(new Fraction())
        .build();
  }
}
