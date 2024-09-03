package app.bpartners.api.integration.event;

import app.bpartners.api.conf.FacadeIT;
import app.bpartners.api.endpoint.event.model.DisconnectionInitiated;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.DbTransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.event.DisconnectionInitiatedService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.USER1_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@AutoConfigureMockMvc
public class DisconnectionInitiatedServiceTest extends MockedThirdParties {
  @MockBean
  AccountService accountServiceMock;
  @MockBean
  UserRepository userRepositoryMock;
  @MockBean
  TransactionsSummaryRepository transactionsSummaryRepositoryMock;
  @MockBean
  DbTransactionRepository transactionRepositoryMock;
  @Autowired DisconnectionInitiatedService subject;

  Account account(){
    return Account.builder()
        .id("accountId")
        .name("Account name")
        .build();
  }
  @Test
  void accept_ok(){
    MockedStatic<AuthProvider> authProviderMock = mockStatic(AuthProvider.class);
    authProviderMock.when(AuthProvider::getAuthenticatedUser).thenReturn(User.builder().accounts(List.of(account())).build());

    when(accountServiceMock.getAccountsByUserId(any())).thenReturn(List.of(account()));
    when(accountServiceMock.getActive(any())).thenReturn(account());
    when(transactionRepositoryMock.findByAccountId(any())).thenReturn(List.of(new Transaction()));

    subject.accept(new DisconnectionInitiated(USER1_ID));

    verify(transactionsSummaryRepositoryMock, times(1)).removeAll(any());
    verify(transactionRepositoryMock, times(1)).saveAll(any());
    verify(accountServiceMock, times(1)).saveAll(any());
    verify(userRepositoryMock, times(1)).save(any());
    verify(accountServiceMock, times(1)).save(any());
  }
}
