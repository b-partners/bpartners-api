package app.bpartners.api.repository.implementation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.mapper.UserAnalysisApiKeyMapper;
import app.bpartners.api.repository.jpa.UserAnalysisApiKeyJpaRepository;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyRepositoryImplTest {

  UserAnalysisApiKeyJpaRepository jpaRepositoryMock = mock();
  UserAnalysisApiKeyMapper mapper = new UserAnalysisApiKeyMapper();
  UserAnalysisApiKeyRepositoryImpl subject =
      new UserAnalysisApiKeyRepositoryImpl(jpaRepositoryMock, mapper);

  @Test
  void get_all_ok() {
    when(jpaRepositoryMock.findAllByUserId(any(String.class)))
        .thenReturn(List.of(hUserAnalysisApiKey()));

    List<UserAnalysisApiKey> actual = subject.getAllByUserId("id");

    assertTrue(actual.contains(userAnalysisApiKey()));
  }

  @Test
  void save_ok() {
    when(jpaRepositoryMock.save(any())).thenReturn(hUserAnalysisApiKey());

    UserAnalysisApiKey actual = subject.save(userAnalysisApiKey());

    assertEquals(userAnalysisApiKey(), actual);
  }

  private HUserAnalysisApiKey hUserAnalysisApiKey() {
    return HUserAnalysisApiKey.builder().build();
  }

  private UserAnalysisApiKey userAnalysisApiKey() {
    return UserAnalysisApiKey.builder().build();
  }
}
