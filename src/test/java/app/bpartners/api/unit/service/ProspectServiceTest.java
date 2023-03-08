package app.bpartners.api.unit.service;

import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.ProspectService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.dataprocesser.ProspectDataProcesser;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.SWAN_ACCOUNTHOLDER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProspectServiceTest {
  ProspectRepository prospectRepositoryMock = mock(ProspectRepository.class);
  ProspectDataProcesser dataProcesserMock = mock(ProspectDataProcesser.class);
  AccountHolderJpaRepository accountHolderJpaRepositoryMock =
      mock(AccountHolderJpaRepository.class);
  SesService sesServiceMock = mock(SesService.class);
  ProspectService subject = new ProspectService(prospectRepositoryMock, dataProcesserMock,
      accountHolderJpaRepositoryMock, sesServiceMock);

  @BeforeEach
  void setup() {
    when(accountHolderJpaRepositoryMock.findAll()).thenReturn(
        List.of(HAccountHolder.builder()
                .id(SWAN_ACCOUNTHOLDER_ID)
                .build(),
            HAccountHolder.builder()
                .id("fake_accountholder_id")
                .build()));
    when(prospectRepositoryMock.needsProspects(any()))
        .thenAnswer(i -> Objects.equals(i.getArgument(0), SWAN_ACCOUNTHOLDER_ID));
    when(prospectRepositoryMock.isSogefiProspector(any()))
        .thenAnswer(i -> Objects.equals(i.getArgument(0), SWAN_ACCOUNTHOLDER_ID));
  }

  @Test
  void test_scheduler_ok() throws MessagingException, IOException {
    subject.prospect();

    verify(sesServiceMock, times(1)).sendEmail(any(), any(), any(), any());
  }
}
