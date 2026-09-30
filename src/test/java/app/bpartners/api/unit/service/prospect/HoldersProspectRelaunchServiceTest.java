package app.bpartners.api.unit.service.prospect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.service.prospect.relaunch.HoldersProspectRelaunchService;
import app.bpartners.api.service.prospect.relaunch.ProspectRelaunchMailer;
import app.bpartners.api.service.prospect.relaunch.ProspectToRelaunchFinder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HoldersProspectRelaunchServiceTest {
  private static final String HOLDER_ID = "holder_id";
  private static final String OTHER_HOLDER_ID = "other_holder_id";
  private static final String UNKNOWN_HOLDER_ID = "unknown_holder_id";
  ProspectToRelaunchFinder finderMock = mock(ProspectToRelaunchFinder.class);
  AccountHolderJpaRepository accountHolderJpaRepositoryMock =
      mock(AccountHolderJpaRepository.class);
  ProspectRelaunchMailer mailerMock = mock(ProspectRelaunchMailer.class);
  HoldersProspectRelaunchService subject =
      new HoldersProspectRelaunchService(finderMock, accountHolderJpaRepositoryMock, mailerMock);

  @BeforeEach
  void setUp() {
    when(accountHolderJpaRepositoryMock.findById(HOLDER_ID))
        .thenReturn(Optional.of(accountHolder(HOLDER_ID)));
    when(accountHolderJpaRepositoryMock.findById(OTHER_HOLDER_ID))
        .thenReturn(Optional.of(accountHolder(OTHER_HOLDER_ID)));
    when(accountHolderJpaRepositoryMock.findById(UNKNOWN_HOLDER_ID)).thenReturn(Optional.empty());
  }

  @Test
  void sends_one_mail_per_holder_with_its_own_prospects() {
    when(finderMock.findGroupedByHolder())
        .thenReturn(
            Map.of(
                HOLDER_ID, prospects("prospect_1", "prospect_2"),
                OTHER_HOLDER_ID, prospects("prospect_3")));

    subject.relaunchHoldersProspects();

    verify(mailerMock).send(accountHolder(HOLDER_ID), prospects("prospect_1", "prospect_2"));
    verify(mailerMock).send(accountHolder(OTHER_HOLDER_ID), prospects("prospect_3"));
  }

  @Test
  void does_nothing_when_no_prospect_is_to_relaunch() {
    when(finderMock.findGroupedByHolder()).thenReturn(Map.of());

    subject.relaunchHoldersProspects();

    verify(mailerMock, never()).send(any(), any());
  }

  @Test
  void skips_unknown_holders_without_failing_the_others() {
    when(finderMock.findGroupedByHolder())
        .thenReturn(
            Map.of(
                UNKNOWN_HOLDER_ID, prospects("prospect_1"),
                HOLDER_ID, prospects("prospect_2")));

    assertDoesNotThrow(subject::relaunchHoldersProspects);

    verify(mailerMock).send(accountHolder(HOLDER_ID), prospects("prospect_2"));
  }

  @Test
  void keeps_relaunching_the_other_holders_when_one_mail_fails() {
    when(finderMock.findGroupedByHolder())
        .thenReturn(
            Map.of(
                HOLDER_ID, prospects("prospect_1"),
                OTHER_HOLDER_ID, prospects("prospect_2")));
    doThrow(new RuntimeException("mail is down"))
        .when(mailerMock)
        .send(accountHolder(HOLDER_ID), prospects("prospect_1"));

    assertDoesNotThrow(subject::relaunchHoldersProspects);

    verify(mailerMock).send(accountHolder(OTHER_HOLDER_ID), prospects("prospect_2"));
  }

  private static HAccountHolder accountHolder(String id) {
    return HAccountHolder.builder().id(id).email(id + "@mail.com").build();
  }

  private static List<Prospect> prospects(String... ids) {
    return List.of(ids).stream().map(id -> Prospect.builder().id(id).build()).toList();
  }
}
