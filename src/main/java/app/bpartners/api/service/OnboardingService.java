package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.gen.UserOnboarded;
import app.bpartners.api.endpoint.event.model.gen.UserUpserted;
import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.OnboardUser;
import app.bpartners.api.model.OnboardedUser;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.OPENED;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.VALID_IDENTITY;
import static java.util.UUID.randomUUID;
import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

@Service
@AllArgsConstructor
@Slf4j
public class OnboardingService {
  public static final boolean DEFAULT_SUBJECT_TO_VAT = true;
  public static final VerificationStatus DEFAULT_VERIFICATION_STATUS = VerificationStatus.VERIFIED;
  public static final AccountStatus DEFAULT_STATUS = OPENED;
  public static final Money DEFAULT_BALANCE = new Money();
  public static final Fraction DEFAULT_CASH_FLOW = new Fraction();
  public static final boolean DEFAULT_VERIFIED = true;
  public static final EnableStatus DEFAULT_USER_STATUS = EnableStatus.ENABLED;
  public static final IdentificationStatus DEFAULT_USER_IDENTIFICATION = VALID_IDENTITY;
  private final UserRepository userRepository;
  private final AccountRepository accountRepository;
  private final AccountHolderRepository accountHolderRepository;
  private final EventProducer eventProducer;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final EventConf eventConf;
  private final UserUpsertedService userUpsertedService;
  private final UserOnboardedService userOnboardedService;

  @Transactional(isolation = SERIALIZABLE)
  public OnboardedUser onboardUser(User toSave, String companyName) {
    String id = String.valueOf(randomUUID());
    String bridgePassword = encryptSequence(id);
    User savedUser = userRepository.create(userDefaultValues(toSave, id, bridgePassword));

    userUpsertedService.accept(toTypedUser(savedUser));

    AccountHolder accountHolderToSave = fromNewUser(companyName, savedUser);
    AccountHolder savedAccountHolder = accountHolderRepository.save(accountHolderToSave);
    Account accountToSave = fromNewUserAndAccountHolder(savedUser, savedAccountHolder);
    Account savedAccount = accountRepository.save(accountToSave);
    User updatedAccount = savedUser.toBuilder()
        .accounts(List.of(savedAccount.active(true)))
        .accountHolders(List.of(savedAccountHolder))
        .build();
    OnboardedUser onboardedUser = new OnboardedUser(updatedAccount, savedAccount,
        savedAccountHolder);

    userOnboardedService.accept(toTypedEvent(onboardedUser)); //TODO: add appropriate test

    return onboardedUser;
  }

  @Transactional(isolation = SERIALIZABLE)
  public List<OnboardedUser> onboardUsers(List<OnboardUser> toSave) {
    return toSave.stream()
        .map(onboardUser -> onboardUser(onboardUser.getUser(), onboardUser.getCompanyName()))
        .collect(Collectors.toList());
  }

  private UserUpserted toTypedUser(User user) {
    return
        new UserUpserted()
            .userId(user.getId())
            .email(user.getEmail()
            );
  }

  private UserOnboarded toTypedEvent(OnboardedUser onboardedUser) {
    String subject =
        "Inscription d'un nouvel artisan : " + onboardedUser.getOnboardedUser().getName();
    String recipient = eventConf.getAdminEmail();
    return new UserOnboarded()
        .subject(subject)
        .recipientEmail(recipient)
        .onboardedUser(onboardedUser);
  }

  private User userDefaultValues(User user, String id, String bridgePassword) {
    return user.toBuilder()
        .id(user.getId() == null ? id : user.getId())
        .bridgePassword(
            user.getBridgePassword() == null
                ? bridgePassword : user.getBridgePassword())
        .identificationStatus(DEFAULT_USER_IDENTIFICATION) //default value
        .status(DEFAULT_USER_STATUS) //default value
        .idVerified(DEFAULT_VERIFIED) //default value
        .accounts(List.of())
        .build();
  }

  private AccountHolder fromNewUser(String companyName, User user) {
    return AccountHolder.builder()
        .id(String.valueOf(randomUUID()))
        .userId(user.getId())
        .name(companyName)
        .initialCashflow(DEFAULT_CASH_FLOW)
        .email(user.getEmail())
        .website(null)
        .mobilePhoneNumber(user.getMobilePhoneNumber())
        .verificationStatus(DEFAULT_VERIFICATION_STATUS) //default value
        .subjectToVat(DEFAULT_SUBJECT_TO_VAT) //default value
        .build();
  }

  private Account fromNewUserAndAccountHolder(User savedUser, AccountHolder savedAccountHolder) {
    return Account.builder()
        .userId(savedUser.getId())
        .idAccountHolder(savedAccountHolder.getId())
        .name(savedUser.getName())
        .availableBalance(DEFAULT_BALANCE)
        .status(DEFAULT_STATUS)
        .active(true)
        .enableStatus(EnableStatus.ENABLED)
        .build();
  }

  private String encryptSequence(String sequence) {
    return encoder.encode(sequence);
  }

}
