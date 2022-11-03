package app.bpartners.api.repository;

import app.bpartners.api.model.BusinessActivity;

public interface BusinessActivityRepository {
  BusinessActivity save(BusinessActivity businessActivity);

  BusinessActivity findByAccountHolderId(String accountHolderId);
}
