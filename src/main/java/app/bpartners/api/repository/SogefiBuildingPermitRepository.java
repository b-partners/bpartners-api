package app.bpartners.api.repository;

import app.bpartners.api.repository.jpa.model.HSogefiBuildingPermitProspect;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermit;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;

public interface SogefiBuildingPermitRepository {
  void saveByBuildingPermit(String idAccountHolder, BuildingPermit buildingPermit,
                            SingleBuildingPermit singleBuildingPermit);

  HSogefiBuildingPermitProspect findByIdProspect(String idProspect);
}
