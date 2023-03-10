package app.bpartners.api.repository.prospecting.datasource.buildingpermit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.USE_DEFAULTS;

@NoArgsConstructor
@ToString
@JsonIgnoreProperties("geojson_centroide")
@EqualsAndHashCode(callSuper = true)
public class SingleBuildingPermit extends BuildingPermit {
  private SogefiInformation sogefiInformation;
  private List<Parcel> parcels;

  @Builder(builderMethodName = "singleBuildingPermitBuilder")
  public SingleBuildingPermit(long fileId, String fileRef, boolean sitadel, String type,
                              String longType, String insee, int year, String ref, String suffix,
                              GeoJson<List<List<List<Object>>>> geoJson,
                              GeoJson<List<Object>> centroidGeoJson, String idSitadel,
                              SogefiInformation sogefiInformation, List<Parcel> parcels) {
    super(fileId, fileRef, sitadel, type, longType, insee, year, ref, suffix, geoJson,
        centroidGeoJson, idSitadel);
    this.sogefiInformation = sogefiInformation;
    this.parcels = parcels;
  }

  @JsonProperty("sogefi_infos_sitadel")
  @JsonInclude(USE_DEFAULTS)
  public SogefiInformation getSogefiInformation() {
    return sogefiInformation;
  }

  @JsonProperty("parcelles")
  @JsonInclude(USE_DEFAULTS)
  public List<Parcel> getParcels() {
    return parcels;
  }
}
