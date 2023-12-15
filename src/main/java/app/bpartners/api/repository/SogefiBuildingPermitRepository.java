package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermit;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;

public interface SogefiBuildingPermitRepository {
  void saveByBuildingPermit(
      String idAccountHolder,
      BuildingPermit buildingPermit,
      SingleBuildingPermit singleBuildingPermit);

  Geojson findLocationByIdProspect(String idProspect);
}
