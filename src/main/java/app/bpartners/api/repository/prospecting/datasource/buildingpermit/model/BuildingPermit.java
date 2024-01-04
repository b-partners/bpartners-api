package app.bpartners.api.repository.prospecting.datasource.buildingpermit.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.USE_DEFAULTS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class BuildingPermit {
  private long fileId;
  private String fileRef;
  private boolean sitadel;
  private String type;
  private String longType;
  private String insee;
  private int year;
  private String ref;
  private String suffix;
  // todo: deserialize with the corresponding java type
  private GeoJson<List<List<List<Object>>>> geoJson;
  private GeoJson<List<Object>> centroidGeoJson;
  private String idSitadel;

  @JsonProperty("sitadel_id")
  @JsonInclude(USE_DEFAULTS)
  public String getIdSitadel() {
    return idSitadel;
  }

  @JsonProperty("geojson_centroide")
  @JsonInclude(USE_DEFAULTS)
  public GeoJson<List<Object>> getCentroidGeoJson() {
    return centroidGeoJson;
  }

  @JsonProperty("sogefi_id_dossier")
  @JsonInclude(USE_DEFAULTS)
  public long getFileId() {
    return fileId;
  }

  @JsonProperty("sogefi_ref_dossier")
  @JsonInclude(USE_DEFAULTS)
  public String getFileRef() {
    return fileRef;
  }

  @JsonProperty("sogefi_sitadel")
  @JsonInclude(USE_DEFAULTS)
  public boolean isSitadel() {
    return sitadel;
  }

  @JsonProperty("type")
  @JsonInclude(USE_DEFAULTS)
  public String getType() {
    return type;
  }

  @JsonProperty("l_type")
  @JsonInclude(USE_DEFAULTS)
  public String getLongType() {
    return longType;
  }

  @JsonProperty("insee")
  @JsonInclude(USE_DEFAULTS)
  public String getInsee() {
    return insee;
  }

  @JsonProperty("annee")
  @JsonInclude(USE_DEFAULTS)
  public int getYear() {
    return year;
  }

  @JsonProperty("ref")
  @JsonInclude(USE_DEFAULTS)
  public String getRef() {
    return ref;
  }

  @JsonProperty("suffixe")
  @JsonInclude(USE_DEFAULTS)
  public String getSuffix() {
    return suffix;
  }

  @JsonProperty("geojson")
  @JsonInclude(USE_DEFAULTS)
  public GeoJson<List<List<List<Object>>>> getGeoJson() {
    return geoJson;
  }
}
