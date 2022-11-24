package app.bpartners.api.repository;

import app.bpartners.api.model.LegalFile;
import java.util.List;

public interface LegalFileRepository {

  LegalFile findByUserIdAndLegalFileId(String userId, String id);

  LegalFile save(String userId, String legalFileId);

  List<LegalFile> findAllByUserId(String userId);

  LegalFile findTopByUserId(String userId);
}
