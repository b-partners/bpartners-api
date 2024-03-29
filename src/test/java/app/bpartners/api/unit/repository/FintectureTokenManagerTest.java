package app.bpartners.api.unit.repository;

import static app.bpartners.api.integration.conf.utils.TestUtils.OAUTH_URL;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.manager.FinctectureTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.model.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FintectureTokenManagerTest {
  private static final String APP_ID = "app_id";
  FintectureConf fintectureConf;
  FinctectureTokenManager fintectureTokenManager;

  @BeforeEach
  void setUp() {
    fintectureConf = mock(FintectureConf.class);
    fintectureTokenManager = new FinctectureTokenManager(fintectureConf);

    when(fintectureConf.getAppId()).thenReturn(APP_ID);
    when(fintectureConf.getOauthUrl()).thenReturn(OAUTH_URL);
  }

  @Test
  void read_fintecture_token_manager_ok() {
    TokenResponse actual = fintectureTokenManager.getProjectAccessToken();

    assertNotNull(actual);
  }
}
