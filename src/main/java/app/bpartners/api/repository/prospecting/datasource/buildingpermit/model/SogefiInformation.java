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
public class SogefiInformation {
  private String state;
  private String realAcceptanceDate;
  private String depositDate;
  private String sogefiDepositDate;
  private String initialAcceptanceDate;
  private String sogefiInitialAcceptanceDate;
  private String updatedAt;
  private String sogefiUpdatedAt;
  private String constructionSiteOpeningDate;
  private String sogefiConstructionSiteOpeningDate;
  private String realConstructionSiteOpeningDate;
  private String realEndOfConstructionDate;
  private boolean requireArchitect;
  private int landArea;
  private String sogefiSuperficieTerrain;
  private String operatingArea;
  private Applicant permitApplicant;
  private Address address;
  private List<Parcel> parcels;
  private Object local;

  @JsonProperty("etat")
  @JsonInclude(USE_DEFAULTS)
  public String getState() {
    return state;
  }

  @JsonProperty("date_reelle_autorisation")
  @JsonInclude(USE_DEFAULTS)
  public String getRealAcceptanceDate() {
    return realAcceptanceDate;
  }

  @JsonProperty("dpc_prem")
  @JsonInclude(USE_DEFAULTS)
  public String getDepositDate() {
    return depositDate;
  }

  @JsonProperty("sogefi_dpc_prem")
  @JsonInclude(USE_DEFAULTS)
  public String getSogefiDepositDate() {
    return sogefiDepositDate;
  }

  @JsonProperty("dpc_aut")
  @JsonInclude(USE_DEFAULTS)
  public String getInitialAcceptanceDate() {
    return initialAcceptanceDate;
  }

  @JsonProperty("sogefi_dpc_aut")
  @JsonInclude(USE_DEFAULTS)
  public String getSogefiInitialAcceptanceDate() {
    return sogefiInitialAcceptanceDate;
  }

  @JsonProperty("dpc_dern")
  @JsonInclude(USE_DEFAULTS)
  public String getUpdatedAt() {
    return updatedAt;
  }

  @JsonProperty("sogefi_dpc_dern")
  @JsonInclude(USE_DEFAULTS)
  public String getSogefiUpdatedAt() {
    return sogefiUpdatedAt;
  }

  @JsonProperty("dpc_doc")
  @JsonInclude(USE_DEFAULTS)
  public String getConstructionSiteOpeningDate() {
    return constructionSiteOpeningDate;
  }

  @JsonProperty("sogefi_dpc_doc")
  @JsonInclude(USE_DEFAULTS)
  public String getSogefiConstructionSiteOpeningDate() {
    return sogefiConstructionSiteOpeningDate;
  }

  @JsonProperty("date_reelle_doc")
  @JsonInclude(USE_DEFAULTS)
  public String getRealConstructionSiteOpeningDate() {
    return realConstructionSiteOpeningDate;
  }

  @JsonProperty("date_reelle_daact")
  @JsonInclude(USE_DEFAULTS)
  public String getRealEndOfConstructionDate() {
    return realEndOfConstructionDate;
  }

  @JsonProperty("rec_archi")
  @JsonInclude(USE_DEFAULTS)
  public boolean isRequireArchitect() {
    return requireArchitect;
  }

  @JsonProperty("superficie_terrain")
  @JsonInclude(USE_DEFAULTS)
  public int getLandArea() {
    return landArea;
  }

  @JsonProperty("sogefi_superficie_terrain")
  @JsonInclude(USE_DEFAULTS)
  public String getSogefiSuperficieTerrain() {
    return sogefiSuperficieTerrain;
  }

  @JsonProperty("zone_op")
  @JsonInclude(USE_DEFAULTS)
  public String getOperatingArea() {
    return operatingArea;
  }

  @JsonProperty("demandeur")
  @JsonInclude(USE_DEFAULTS)
  public Applicant getPermitApplicant() {
    return permitApplicant;
  }

  @JsonProperty("adresse_terrain")
  @JsonInclude(USE_DEFAULTS)
  public Address getAddress() {
    return address;
  }

  @JsonProperty("parcelles")
  @JsonInclude(USE_DEFAULTS)
  public List<Parcel> getParcels() {
    return parcels;
  }

  @JsonProperty("logements")
  @JsonInclude(USE_DEFAULTS)
  public Object getLocal() {
    return local;
  }
}
