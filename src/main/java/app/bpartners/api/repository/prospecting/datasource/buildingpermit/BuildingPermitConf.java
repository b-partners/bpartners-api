package app.bpartners.api.repository.prospecting.datasource.buildingpermit;

import static app.bpartners.api.service.utils.URLUtils.URLEncodeMap;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class BuildingPermitConf {
  // apiURL objects baseURL/bearer/endpoint/filter
  private static final String API_URL = "%s/%s/%s?%s";
  private static final String AUTHORIZED_STATE = "autorisé";
  private static final String STARTED_STATE = "commencé";
  private static final String LIST_ENDPOINT = "audrso/v2/open/dossiers/filter";
  private static final String SINGLE_PERMIT_ENDPOINT = "audrso/v2/open/dossiers/%s";
  private String baseUrl;
  private String bearer;
  private String denomChar;

  public BuildingPermitConf(
      @Value("${ads.baseUrl}") String baseUrl,
      @Value("${ads.bearer}") String bearer,
      @Value("${ads.denom.char}") String denomChar) {
    this.baseUrl = baseUrl;
    this.bearer = bearer;
    this.denomChar = denomChar;
  }

  public String getApiWithFilterUrl(String insee) {
    return String.format(API_URL, baseUrl, bearer, LIST_ENDPOINT, URLEncodeMap(getFilter(insee)));
  }

  public String getSinglePermitUrl(String idFile) {
    String permitEndpoint = String.format(SINGLE_PERMIT_ENDPOINT, idFile);
    return String.format(API_URL, baseUrl, bearer, permitEndpoint, URLEncodeMap(getFields()));
  }

  public Map<String, String> getFilter(String insee) {
    Map<String, String> filter = new HashMap<>();
    filter.put("insee[in]", insee);
    filter.put("annee[gte]", String.valueOf(Year.now().minusYears(1).getValue()));
    filter.put("type[eq]", "PC");
    filter.put("sitadel_etat[in]", AUTHORIZED_STATE + "," + STARTED_STATE);
    filter.put("sitadel_demandeur_denom[like]", denomChar);
    filter.put("onlytotal", String.valueOf(false));
    return filter;
  }

  public Map<String, String> getFields() {
    Map<String, String> field = new HashMap<>();
    field.put("fields", "_full_");
    return field;
  }
}
