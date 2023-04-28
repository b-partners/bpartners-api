package app.bpartners.api.unit.repository;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermitList;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.SingleBuildingPermit;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static app.bpartners.api.integration.conf.TestUtils.httpResponseMock;
import static app.bpartners.api.service.utils.URLUtils.URLEncodeMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
class BuildingPermitApiTest {
  public static final String BASE_URL = "https://localhost";
  public static final String BEARER = "bearer";
  //todo: move json to files
  public static final String SINGLE_BUILDING_PERMIT_JSON =
      "{\"sogefi_id_dossier\":112597101,\"sogefi_ref_dossier\":\"PC 092002 22 A0023\","
          + "\"sogefi_sitadel\":true,\"type\":\"PC\",\"l_type\":\"permis de construire\","
          + "\"insee\":\"92002\",\"annee\":2022,\"ref\":\"A0023\",\"suffixe\":null,"
          + "\"parcelles\":[{\"insee\":\"92002\",\"prefixe\":\"000\",\"section\":\"CE\","
          + "\"numero\":192}],\"geojson\":{\"type\":\"Polygon\",\"coordinates\":[[[2.311646,48"
          + ".744861],[2.311695,48.744832],[2.311737,48.744807],[2.311755,48.744796],[2.311798,48"
          + ".74477],[2.311839,48.744744],[2.31208,48.744598],[2.312212,48.744518],[2.312266,48"
          + ".744479],[2.312311,48.744446],[2.312344,48.744423],[2.312413,48.744373],[2.312431,48"
          + ".74436],[2.312451,48.744346],[2.312468,48.744334],[2.312477,48.744327],[2.312436,48"
          + ".744299],[2.312401,48.744276],[2.312391,48.744283],[2.312286,48.744357],[2.312177,48"
          + ".744434],[2.312024,48.744542],[2.311806,48.744696],[2.311719,48.744757],[2.311706,48"
          + ".744766],[2.311654,48.744803],[2.311605,48.744838],[2.311623,48.744848],[2.311646,48"
          + ".744861]]]},\"sogefi_infos_sitadel\":{\"etat\":\"autorisé\","
          + "\"date_reelle_autorisation\":\"2022-05-18\",\"dpc_prem\":\"2022-03-01\","
          + "\"sogefi_dpc_prem\":\"mars 2022\",\"dpc_aut\":\"2022-05-01\","
          + "\"sogefi_dpc_aut\":\"mai 2022\",\"dpc_dern\":\"2022-05-01\","
          + "\"sogefi_dpc_dern\":\"mai 2022\",\"dpc_doc\":null,\"sogefi_dpc_doc\":null,"
          + "\"date_reelle_doc\":null,\"date_reelle_daact\":null,\"rec_archi\":false,"
          + "\"superficie_terrain\":267,\"sogefi_superficie_terrain\":\"2 a 67 ca\","
          + "\"zone_op\":\"hors zone ou inconnu\",\"demandeur\":{\"ape\":\"47.21Z\",\"cj\":5499,"
          + "\"denom\":\"PETILLOT\",\"siren\":\"382757771\",\"sogefi_valid_siren\":true,"
          + "\"siret\":\"38275777100037\",\"sogefi_valid_siret\":true,\"codpost\":\"92160\","
          + "\"localite\":\"ANTONY\"},\"adresse_terrain\":{\"num\":null,\"typevoie\":\"Rue\","
          + "\"libvoie\":\"Frederic Chopin\",\"lieudit\":null,\"codpost\":null,"
          + "\"localite\":\"ANTONY\",\"sogefi_adresse\":\"Rue Frederic Chopin ANTONY\"},"
          + "\"parcelles\":[{\"insee\":\"92002\",\"prefixe\":\"000\",\"section\":\"CE\","
          + "\"numero\":192,\"partie\":false}],\"logements\":{\"cat_dem\":\"non déterminé (valeur"
          + " par défaut à la création du permis)\",\"nature_projet_declaree\":\"nouvelle "
          + "construction\",\"nature_projet_completee\":\"nouvelle construction\","
          + "\"destination_principale\":\"logements\",\"type_princip_logts_crees\":\"un logement "
          + "individuel\",\"type_transfo_principal\":null,"
          + "\"type_princip_locaux_transformes\":null,\"i_extension\":false,"
          + "\"i_surelevation\":false,\"i_nivsupp\":false,\"nb_niv_max\":0,\"utilisation\":\"non "
          + "rempli\",\"res_princip_ou_second\":\"résidence principale\",\"typ_annexe\":\"pas "
          + "d'annexe\",\"i_piscine\":false,\"i_garage\":false,\"i_veranda\":false,"
          + "\"i_abri_jardin\":false,\"i_autre_annexe\":false,\"residence\":\"non rempli\","
          + "\"res_pers_agees\":false,\"res_etudiants\":false,\"res_tourisme\":false,"
          + "\"res_hotel_sociale\":false,\"res_sociale\":false,\"res_handicapes\":false,"
          + "\"res_autre\":false,\"nb_lgt_tot_crees\":1,\"nb_lgt_ind_crees\":1,"
          + "\"nb_lgt_col_crees\":0,\"nb_lgt_indiv_purs\":1,\"nb_lgt_indiv_groupes\":null,"
          + "\"nb_lgt_res\":null,\"nb_lgt_col_hors_res\":0,\"nb_lgt_demolis\":0,\"nb_lgt_1p\":0,"
          + "\"nb_lgt_2p\":0,\"nb_lgt_3p\":0,\"nb_lgt_4p\":0,\"nb_lgt_5p\":0,"
          + "\"nb_lgt_6p_plus\":0,\"nb_lgt_pret_loc_social\":0,\"nb_lgt_acc_soc_hors_ptz\":0,"
          + "\"nb_lgt_ptz\":0,\"surf_hab_avant\":0,\"surf_hab_creee\":77,"
          + "\"surf_hab_issue_transfo\":0,\"surf_hab_demolie\":0,\"surf_hab_transformee\":0,"
          + "\"surf_loc_avant\":0,\"surf_loc_creee\":0,\"surf_loc_issue_transfo\":0,"
          + "\"surf_loc_demolie\":0,\"surf_loc_transformee\":0,\"surf_heb_transformee\":0,"
          + "\"surf_bur_transformee\":0,\"surf_com_transformee\":0,\"surf_art_transformee\":0,"
          + "\"surf_ind_transformee\":0,\"surf_agr_transformee\":0,\"surf_ent_transformee\":0,"
          + "\"surf_pub_transformee\":0}}}";
  public static final String BUILDING_PERMIT_LIST_JSON =
      "{\"total\":1,\"limit\":1000,\"records\":[{\"sogefi_id_dossier\":112597101,"
          + "\"sogefi_ref_dossier\":\"PC 092002 22 A0023\",\"type\":\"PC\",\"l_type\":\"permis de"
          + " construire\",\"insee\":\"92002\",\"annee\":2022,\"ref\":\"A0023\",\"suffixe\":null,"
          + "\"sogefi_sitadel\":true,\"sitadel_id\":2183595,\"geojson\":{\"type\":\"Polygon\","
          + "\"coordinates\":[[[2.311646,48.744861],[2.311695,48.744832],[2.311737,48.744807],[2"
          + ".311755,48.744796],[2.311798,48.74477],[2.311839,48.744744],[2.31208,48.744598],[2"
          + ".312212,48.744518],[2.312266,48.744479],[2.312311,48.744446],[2.312344,48.744423],[2"
          + ".312413,48.744373],[2.312431,48.74436],[2.312451,48.744346],[2.312468,48.744334],[2"
          + ".312477,48.744327],[2.312436,48.744299],[2.312401,48.744276],[2.312391,48.744283],[2"
          + ".312286,48.744357],[2.312177,48.744434],[2.312024,48.744542],[2.311806,48.744696],[2"
          + ".311719,48.744757],[2.311706,48.744766],[2.311654,48.744803],[2.311605,48.744838],[2"
          + ".311623,48.744848],[2.311646,48.744861]]]},"
          + "\"geojson_centroide\":{\"type\":\"Point\",\"coordinates\":[2.312055,48.74457]}}]}";
  private static final ObjectMapper om = new ObjectMapper().findAndRegisterModules();
  private static final String ENDPOINT = "audrso/v2/open/dossiers/filter";
  private static final String API_URL = "%s/%s/%s?%s";
  private static final String INSEE = "123456";
  private static final String AUTHORIZED_STATE = "autorisé";
  private static final String STARTED_STATE = "commencé";
  private static final String ID_SOGEFI = "id_sogefi";
  private static final String COMMON_DENOM_CHAR = "e";
  private static final String SOGEFI_EXCEPTION_MESSAGE_KEYWORD = "<!doctype html>";
  BuildingPermitConf conf = new BuildingPermitConf(BASE_URL, BEARER, COMMON_DENOM_CHAR);
  HttpClient mockHttpClient = mock(HttpClient.class);
  BuildingPermitApi api = new BuildingPermitApi(conf)
      .httpClient(mockHttpClient);

  BuildingPermitList expectedPermits() throws IOException {
    return om.readValue(BUILDING_PERMIT_LIST_JSON, BuildingPermitList.class);
  }

  SingleBuildingPermit expectedPermit() throws IOException {
    return om.readValue(SINGLE_BUILDING_PERMIT_JSON, SingleBuildingPermit.class);
  }

  @Test
  void conf_values_ok() {
    String expectedString =
        String.format(API_URL, BASE_URL, BEARER, ENDPOINT, URLEncodeMap(conf.getFilter(INSEE)));

    assertEquals(expectedFilter(), conf.getFilter(INSEE));
    assertEquals(expectedFields(), conf.getFields());
    assertEquals(expectedString, conf.getApiWithFilterUrl(INSEE));
  }

  @Test
  void read_list_ok() throws IOException, InterruptedException {

    when(mockHttpClient.send(any(), any())).thenReturn(httpResponseMock(BUILDING_PERMIT_LIST_JSON));

    BuildingPermitList permits = api.getBuildingPermitList(INSEE);

    assertEquals(expectedPermits(), permits);
  }

  @Test
  void read_one_ok() throws IOException, InterruptedException {
    when(mockHttpClient.send(any(), any())).thenReturn(
        httpResponseMock(SINGLE_BUILDING_PERMIT_JSON));

    SingleBuildingPermit singleBuildingPermit = api.getSingleBuildingPermit(ID_SOGEFI);

    assertEquals(expectedPermit(), singleBuildingPermit);
  }

  @Test
  void handle_jsonProcessingException() throws IOException, InterruptedException {
    when(mockHttpClient.send(any(), any())).thenThrow(
        new IOException(SOGEFI_EXCEPTION_MESSAGE_KEYWORD));

    try {
      api.getData(BASE_URL, SingleBuildingPermit.class);
    } catch (ApiException e) {
      assertThrows(ApiException.class , () -> api.getSingleBuildingPermit(ID_SOGEFI));
      verify(mockHttpClient, times(3)).send(any(), any());
    }
  }

  @Test
  void read_from_sogefi_ko() throws IOException, InterruptedException {
//    TODO: add test for the retrier
    when(mockHttpClient.send(any(), any())).thenThrow(new InterruptedException());

    assertThrows(ApiException.class, () -> api.getBuildingPermitList(INSEE));
    assertThrows(ApiException.class, () -> api.getSingleBuildingPermit(ID_SOGEFI));
  }

  Map<String, String> expectedFilter() {
    Map<String, String> filter = new HashMap<>();
    filter.put("insee[in]", INSEE);
    filter.put("annee[gte]", String.valueOf(Year.now().minusYears(1).getValue()));
    filter.put("type[eq]", "PC");
    filter.put("sitadel_etat[in]", AUTHORIZED_STATE + "," + STARTED_STATE);
    filter.put("sitadel_demandeur_denom[like]", COMMON_DENOM_CHAR);
    filter.put("onlytotal", String.valueOf(false));
    return filter;
  }

  Map<String, String> expectedFields() {
    Map<String, String> field = new HashMap<>();
    field.put("fields", "_full_");
    return field;
  }
}
