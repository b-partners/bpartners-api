package app.bpartners.api.unit.validator;

import static app.bpartners.api.model.WhiteListScope.CREDIT_ANALYSIS_NOT_REQUIRED;
import static app.bpartners.api.model.WhiteListScope.PAYMENT_METHOD_NOT_REQUIRED;
import static app.bpartners.api.model.WhiteListScope.SUBSCRIPTION_VALIDATION_NOT_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.UserWhiteListed;
import app.bpartners.api.model.WhiteListScope;
import app.bpartners.api.model.credit.CreditBalance;
import app.bpartners.api.model.exception.InsufficientCreditsException;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import app.bpartners.api.service.areapicture.AreaPictureConsumptionValidator;
import app.bpartners.api.service.credit.CreditService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AreaPictureConsumptionValidatorTest {
  CreditService creditServiceMock = mock();
  UserWhiteListedJpaRepository userWhiteListedRepositoryMock = mock();
  AreaPictureConsumptionValidator subject =
      new AreaPictureConsumptionValidator(creditServiceMock, userWhiteListedRepositoryMock);

  @Test
  void does_not_throw_when_spendable_credits_cover_analysis_cost() {
    var userId = "userId";
    when(userWhiteListedRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());
    when(creditServiceMock.getCreditBalance(userId))
        .thenReturn(CreditBalance.builder().spendableCredits(10).creditCostPerAnalysis(10).build());

    assertDoesNotThrow(() -> subject.accept(AreaPicture.builder().idUser(userId).build()));
  }

  @Test
  void throws_insufficient_credits_when_spendable_credits_below_analysis_cost() {
    var userId = "userId";
    when(userWhiteListedRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());
    when(creditServiceMock.getCreditBalance(userId))
        .thenReturn(CreditBalance.builder().spendableCredits(9).creditCostPerAnalysis(10).build());

    var actual =
        assertThrows(
            InsufficientCreditsException.class,
            () -> subject.accept(AreaPicture.builder().idUser(userId).build()));

    assertEquals(10, actual.getRequiredCredits());
    assertEquals(9, actual.getAvailableCredits());
  }

  @Test
  void does_not_check_credits_when_white_listed_with_credit_analysis_not_required() {
    var userId = "userId";
    givenWhiteListedScopes(userId, CREDIT_ANALYSIS_NOT_REQUIRED);

    assertDoesNotThrow(() -> subject.accept(AreaPicture.builder().idUser(userId).build()));

    verifyNoInteractions(creditServiceMock);
  }

  @Test
  void does_not_check_credits_when_white_listed_with_subscription_validation_not_required() {
    var userId = "userId";
    givenWhiteListedScopes(userId, SUBSCRIPTION_VALIDATION_NOT_REQUIRED);

    assertDoesNotThrow(() -> subject.accept(AreaPicture.builder().idUser(userId).build()));

    verifyNoInteractions(creditServiceMock);
  }

  @Test
  void checks_credits_when_white_listed_without_a_relevant_scope() {
    var userId = "userId";
    givenWhiteListedScopes(userId, PAYMENT_METHOD_NOT_REQUIRED);
    when(creditServiceMock.getCreditBalance(userId))
        .thenReturn(CreditBalance.builder().spendableCredits(0).creditCostPerAnalysis(10).build());

    assertThrows(
        InsufficientCreditsException.class,
        () -> subject.accept(AreaPicture.builder().idUser(userId).build()));
  }

  private void givenWhiteListedScopes(String userId, WhiteListScope... scopes) {
    var userWhiteListed = UserWhiteListed.builder().userId(userId).scopes(List.of(scopes)).build();
    when(userWhiteListedRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userWhiteListed));
  }
}
