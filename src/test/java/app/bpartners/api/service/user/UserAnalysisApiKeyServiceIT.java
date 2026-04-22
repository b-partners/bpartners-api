package app.bpartners.api.service.user;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.conf.FacadeIT;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class UserAnalysisApiKeyServiceIT extends FacadeIT {
  @MockBean AnalysisApiKeyApi analysisApiKeyApi;
  @Autowired UserAnalysisApiKeyService subject;
}
