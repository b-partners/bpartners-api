package app.bpartners.api.repository;

import app.bpartners.api.model.UserAnalysisApiKey;
import java.util.List;

public interface UserAnalysisApiKeyRepository {
  List<UserAnalysisApiKey> getAllByUserId(String userId);

  UserAnalysisApiKey save(UserAnalysisApiKey userAnalysisApiKey);
}
