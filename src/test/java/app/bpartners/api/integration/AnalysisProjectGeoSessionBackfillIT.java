package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.project.AnalysisProject;
import app.bpartners.api.model.project.AnalysisProjectPhase;
import app.bpartners.api.repository.jpa.AnalysisProjectRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

class AnalysisProjectGeoSessionBackfillIT extends MockedThirdParties {
  private static final String BACKFILL =
      "db/migration/V43_150__Backfill_analysis_project_steps_from_draft_geo_sessions.sql";
  private static final String ANALYSE = "\"analyse\": {\"properties\": {}, \"detectedCount\": 3}";
  private static final String CITY_JSON = "\"cityJson\": {\"type\": \"CityJSON\"}";

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private AnalysisProjectRepository analysisProjectRepository;
  private final List<String> areaPictureIds = new ArrayList<>();

  @AfterEach
  void tearDown() {
    analysisProjectRepository.deleteAll();
    areaPictureIds.forEach(
        id -> {
          jdbcTemplate.update("delete from area_picture_annotation where id_area_picture = ?", id);
          jdbcTemplate.update("delete from area_picture where id = ?", id);
        });
    areaPictureIds.clear();
  }

  @Test
  void draft_holding_both_results_becomes_done_without_debiting() {
    var areaPictureId =
        draftWithGeoSession(randomUUID().toString(), objectSession(ANALYSE, CITY_JSON));
    var projectId = migratedProject(areaPictureId);

    runBackfill();

    var project = analysisProjectRepository.findById(projectId).orElseThrow();
    assertEquals(AnalysisProjectPhase.DONE, project.phase());
    assertNotNull(project.getCreditConsumedDatetime());
    assertNull(project.getCreditTransactionId());
    assertNull(project.getDetectionIdentifier());
  }

  @Test
  void geo_session_saved_as_a_string_is_read() {
    var session = objectSession(ANALYSE, "\"cityJson\": null");
    var areaPictureId = draftWithGeoSession(randomUUID().toString(), quoted(session));
    var projectId = migratedProject(areaPictureId);

    runBackfill();

    var project = analysisProjectRepository.findById(projectId).orElseThrow();
    assertNotNull(project.getAnalysis2dDatetime());
    assertNull(project.getModel3dDatetime());
    assertEquals(AnalysisProjectPhase.IN_PROGRESS, project.phase());
  }

  @Test
  void project_keyed_by_session_id_is_matched_through_the_geo_session() {
    var sessionId = randomUUID().toString();
    draftWithGeoSession(randomUUID().toString(), sessionOf(sessionId, CITY_JSON));
    var projectId = project(sessionId, null);

    runBackfill();

    var project = analysisProjectRepository.findById(projectId).orElseThrow();
    assertNull(project.getAnalysis2dDatetime());
    assertNotNull(project.getModel3dDatetime());
  }

  @Test
  void draft_without_results_or_with_unreadable_session_stays_draft() {
    var emptyDraft = migratedProject(draftWithGeoSession(randomUUID().toString(), objectSession()));
    var unreadable = migratedProject(draftWithGeoSession(randomUUID().toString(), "\"{not json\""));

    runBackfill();

    assertEquals(AnalysisProjectPhase.DRAFT, phaseOf(emptyDraft));
    assertEquals(AnalysisProjectPhase.DRAFT, phaseOf(unreadable));
    assertNull(
        analysisProjectRepository.findById(emptyDraft).orElseThrow().getCreditConsumedDatetime());
  }

  @Test
  void steps_already_registered_keep_their_datetime_and_rerun_is_harmless() {
    var areaPictureId =
        draftWithGeoSession(randomUUID().toString(), objectSession(ANALYSE, CITY_JSON));
    var registered = Instant.parse("2026-11-01T10:00:00Z");
    var projectId = migratedProject(areaPictureId);
    var initial = analysisProjectRepository.findById(projectId).orElseThrow();
    analysisProjectRepository.save(
        initial.toBuilder()
            .analysis2dDatetime(registered)
            .creditConsumedDatetime(registered)
            .build());

    runBackfill();
    var afterFirstRun = analysisProjectRepository.findById(projectId).orElseThrow();
    runBackfill();
    var afterSecondRun = analysisProjectRepository.findById(projectId).orElseThrow();

    assertEquals(registered, afterFirstRun.getAnalysis2dDatetime());
    assertEquals(registered, afterFirstRun.getCreditConsumedDatetime());
    assertNotNull(afterFirstRun.getModel3dDatetime());
    assertEquals(afterFirstRun, afterSecondRun);
  }

  @SneakyThrows
  private void runBackfill() {
    jdbcTemplate.execute(
        new ClassPathResource(BACKFILL).getContentAsString(StandardCharsets.UTF_8));
  }

  private AnalysisProjectPhase phaseOf(String projectId) {
    return analysisProjectRepository.findById(projectId).orElseThrow().phase();
  }

  private String objectSession(String... stateEntries) {
    return sessionOf(randomUUID().toString(), stateEntries);
  }

  private String sessionOf(String sessionId, String... stateEntries) {
    return "{\"version\": 2, \"sessionId\": \""
        + sessionId
        + "\", \"latitude\": 45.6, \"longitude\": 0.14, \"savedAt\": \"2026-10-01T08:00:00Z\","
        + " \"state\": {\"screen\": \"map\", \"polygons\": []"
        + (stateEntries.length == 0 ? "" : ", " + String.join(", ", stateEntries))
        + "}}";
  }

  private String quoted(String json) {
    return "\"" + json.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  private String draftWithGeoSession(String areaPictureId, String geoSessionJson) {
    areaPictureIds.add(areaPictureId);
    jdbcTemplate.update(
        "insert into area_picture (id, id_user, address, zoom_level, is_extended)"
            + " values (?, ?, 'Backfill Address', 'HOUSES_0', false)",
        areaPictureId,
        JOE_DOE_ID);
    jdbcTemplate.update(
        "insert into area_picture_annotation (id, id_user, id_area_picture, is_draft, properties)"
            + " values (?, ?, ?, true, jsonb_build_object('geoSession', ?::jsonb))",
        randomUUID().toString(),
        JOE_DOE_ID,
        areaPictureId,
        geoSessionJson);
    return areaPictureId;
  }

  private String migratedProject(String areaPictureId) {
    return project(areaPictureId, areaPictureId);
  }

  private String project(String projectKey, String areaPictureId) {
    return analysisProjectRepository
        .save(
            AnalysisProject.builder()
                .id(randomUUID().toString())
                .userId(JOE_DOE_ID)
                .projectKey(projectKey)
                .areaPictureId(areaPictureId)
                .address("Backfill Address")
                .creationDatetime(Instant.parse("2026-10-01T08:00:00Z"))
                .build())
        .getId();
  }
}
