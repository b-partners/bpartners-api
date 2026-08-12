package app.bpartners.api.e2e;

import static java.lang.System.currentTimeMillis;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Test END-TO-END *live* du flux "Détection toiture" décrit dans {@code
 * src/test/resources/flow/detection-e2e-flow.yaml}.
 *
 * <p>Contrairement aux {@code *IT} classiques, ce test ne démarre PAS l'application en local et
 * n'utilise aucun mock : il tape les API RÉELLES (bpartners-api + geo-detection/geo-jobs). Il est
 * donc désactivé par défaut et ne s'exécute QUE si les variables d'environnement requises sont
 * présentes (voir {@link EnabledIfEnvironmentVariable} ci-dessous). Sans elles, JUnit le marque
 * "skipped" — il n'échouera jamais en CI.
 *
 * <p>Particularité demandée : plutôt que de délimiter une toiture à la main en pixels puis de la
 * convertir en GeoJSON (steps B3/C1 + convertisseurs Pixel/Mercator du flow), on part directement
 * des emprises de bâtiment de {@code
 * src/test/resources/geojson/BIRDIA_BP_Toiture_AMI_emprise_batiment.geojson}, DÉJÀ exprimées en
 * lon/lat. Le polygone d'emprise alimente donc directement le {@code geoJsonZone} de la détection.
 *
 * <h3>Variables d'environnement</h3>
 *
 * <ul>
 *   <li>{@code BP_ACCESS_TOKEN} (requis) — token d'accès Cognito (Bearer) de l'utilisateur.
 *   <li>{@code GEO_DETECTION_URL} (requis) — base URL du service geo-detection/geo-jobs, préfixe
 *       inclus (ex : {@code https://xxx/geo-detection}). Les endpoints {@code /detections/...} y
 *       sont ajoutés.
 *   <li>{@code GEO_API_KEY} (requis) — clé {@code x-api-key} du service geo-detection.
 *   <li>{@code BP_API_URL} (optionnel) — base URL bpartners-api. Défaut : {@code
 *       https://api-preprod.bpartners.app}.
 *   <li>{@code BP_ACCOUNT_ID} / {@code BP_ACCOUNT_HOLDER_ID} (optionnels) — sinon dérivés du token
 *       via {@code /whoami} puis {@code /users/{userId}/accounts/{accountId}/accountHolders}.
 *   <li>{@code BP_EMAIL} (optionnel) — email receveur de la détection. Défaut : email du prospect.
 * </ul>
 *
 * <p>Exemple de lancement :
 *
 * <pre>{@code
 * BP_ACCESS_TOKEN=... \
 * GEO_DETECTION_URL=https://.../geo-detection \
 * GEO_API_KEY=... \
 * ./gradlew test --tests 'app.bpartners.api.e2e.DetectionE2EFlowIT'
 * }</pre>
 */
@Slf4j
@EnabledIfEnvironmentVariable(named = "BP_ACCESS_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GEO_DETECTION_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GEO_API_KEY", matches = ".+")
class DetectionE2EFlowIT {
  private static final String GEOJSON_RESOURCE =
      "geojson/BIRDIA_BP_Toiture_AMI_emprise_batiment.geojson";
  private static final String DEFAULT_BP_API_URL = "https://api-preprod.bpartners.app";

  // Détection : modèle & constantes (cf. flow yaml, step C3).
  private static final String DETECTION_MODEL = "BP_TOITURE";
  // Taille de l'image de détection. Le ground-truth tourne en 3072 : à 1024 le modèle résout
  // beaucoup moins de régions (cf. imageProperties dans le résultat /sync).
  private static final int IMAGE_SIZE = 3072;
  // Taille de tuile (px) pour la conversion lon/lat -> pixels areaPicture (cf. convention front).
  private static final int TILE_SIZE_PX = 1024;
  private static final String GEO_SERVER_URL = "http://35.181.83.111/geoserver/cite/wms";

  // Polling des roof properties (pente/hauteur) après déclenchement du calcul (step C4).
  private static final Duration ROOF_POLL_INTERVAL = Duration.ofSeconds(5);
  private static final Duration ROOF_POLL_MAX = Duration.ofSeconds(120);

  // Couleur du polygone de toiture (contour vert transparent), alignée sur la palette du front.
  private static final String ROOF_FILL_COLOR = "#00ff0000";
  private static final String ROOF_STROKE_COLOR = "#00ff00";

  private final ObjectMapper om = new ObjectMapper();
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

  private final String bpApiUrl = env("BP_API_URL", DEFAULT_BP_API_URL);
  private final String accessToken = requireEnv("BP_ACCESS_TOKEN");
  private final String geoDetectionUrl = trimTrailingSlash(requireEnv("GEO_DETECTION_URL"));
  private final String geoApiKey = requireEnv("GEO_API_KEY");

  @Test
  void detects_roof_from_building_emprise() throws Exception {
    // ------------------------------------------------------------------ CONTEXTE
    // (accountId / accountHolderId) — dérivés du token si non fournis en env.
    var accountId = env("BP_ACCOUNT_ID", null);
    var accountHolderId = env("BP_ACCOUNT_HOLDER_ID", null);
    // whoami est toujours appelé : userId est requis pour créer les annotations (step C9).
    var whoami = bpGet("/whoami");
    var userId = text(whoami, "/user/id");
    assertNotNull(userId, "userId introuvable via /whoami");
    if (accountId == null) {
      accountId = text(whoami, "/user/activeAccount/id");
    }
    assertNotNull(accountId, "accountId introuvable via /whoami — fournir BP_ACCOUNT_ID");
    if (accountHolderId == null) {
      var holders = bpGet("/users/" + userId + "/accounts/" + accountId + "/accountHolders");
      assertTrue(holders.isArray() && holders.size() > 0, "aucun accountHolder pour ce compte");
      accountHolderId = holders.get(0).path("id").asText();
    }
    log.info(
        "contexte: userId={} accountId={} accountHolderId={}", userId, accountId, accountHolderId);

    // ---------------------------------------------------------------- EMPRISE
    // Première feature valide (adresse + polygone non vide) du geojson d'emprises.
    var emprise = firstValidEmprise();
    var address = emprise.address();
    var projectName =
        emprise.refBat(); // nom du projet ET de l'analyse (Feature.properties.ref_bat)
    var roofPolygon = emprise.polygon(); // [[[lon,lat],...]]
    log.info(
        "emprise retenue: ref_bat='{}' adresse='{}' ({} anneaux)",
        projectName,
        address,
        roofPolygon.size());

    // Identifiants client (UUID v4), idempotents par leur id côté serveur.
    var prospectId = randomUUID().toString();
    var pictureId = randomUUID().toString();
    var fileId = randomUUID().toString();
    var draftAnnotationId = randomUUID().toString();
    var detectionId = randomUUID().toString();
    var zoneId = randomUUID().toString();
    var email = env("BP_EMAIL", "e2e+" + prospectId.substring(0, 8) + "@birdia.fr");

    // ========================================================= PHASE A : Setup
    // A1 — créer le prospect (le serveur géocode l'adresse).
    var prospectBody =
        om.createArrayNode()
            .add(
                om.createObjectNode()
                    .put("id", prospectId)
                    .put("name", projectName)
                    .put("address", address)
                    .put("email", email)
                    .put("phone", "+33600000000")
                    .put("status", "TO_CONTACT"));
    var prospects = bpPut("/accountHolders/" + accountHolderId + "/prospects", prospectBody);
    assertTrue(prospects.isArray() && prospects.size() > 0, "création prospect: réponse vide");
    prospectId = prospects.get(0).path("id").asText(prospectId);
    log.info("A1 prospect créé: {}", prospectId);

    // A2 — créer l'areaPicture : le serveur géocode l'adresse et génère la tuile satellite.
    var areaPictureBody =
        om.createObjectNode()
            .put("address", address)
            .put("fileId", fileId)
            .put("filename", projectName) // nom du projet = Feature.properties.ref_bat
            .put("prospectId", prospectId)
            .put("zoomLevel", "BUILDING")
            .put("isExtended", true)
            .put("shiftNb", 0)
            .put("isOpaque", false);
    var areaPicture =
        bpPut("/accounts/" + accountId + "/areaPictures/" + pictureId, areaPictureBody);
    var layerName = text(areaPicture, "/actualLayer/name");
    assertNotNull(
        layerName, "areaPicture: actualLayer.name absent — image satellite indisponible ?");
    // Tuile + zoom de l'areaPicture : servent d'ORIGINE pour traduire l'emprise lon/lat en pixels
    // sur l'image de l'areaPicture (endpoint /annotations/convert).
    var tileX = areaPicture.at("/currentTile/x").asInt();
    var tileY = areaPicture.at("/currentTile/y").asInt();
    var zoomNumber =
        areaPicture.at("/zoom/number").isMissingNode()
            ? areaPicture.at("/currentTile/zoom/number").asInt()
            : areaPicture.at("/zoom/number").asInt();
    assertTrue(zoomNumber > 0, "areaPicture: zoom.number absent");
    log.info("A2 areaPicture: layer={} tile=({},{}) zoom={}", layerName, tileX, tileY, zoomNumber);

    // A3 — créer le "projet" = un draft annotation vide.
    var draftBody =
        om.createObjectNode()
            .put("id", draftAnnotationId)
            .put("idAreaPicture", pictureId)
            .put("isDraft", true);
    draftBody.set("annotations", om.createArrayNode());
    // nom du projet stocké dans les properties libres du draft (Feature.properties.ref_bat)
    draftBody.set("properties", om.createObjectNode().put("name", projectName));
    bpPut(
        "/accounts/"
            + accountId
            + "/areaPictures/"
            + pictureId
            + "/annotations/"
            + draftAnnotationId,
        draftBody);
    log.info("A3 projet (draft) créé: {}", draftAnnotationId);

    // B2 — vérifier que l'image satellite a bien été générée (téléchargement binaire).
    var rawImage =
        rawBytes(
            bpApiUrl
                + "/accounts/"
                + accountId
                + "/files/"
                + fileId
                + "/raw?accessToken="
                + accessToken
                + "&fileType=AREA_PICTURE");
    assertTrue(rawImage.length > 0, "image satellite vide");
    log.info("B2 image satellite téléchargée: {} octets", rawImage.length);

    // ====================================================== PHASE C : Détection
    // C3 — ★ détection toiture. On alimente geoJsonZone DIRECTEMENT avec l'emprise (déjà lon/lat).
    var detection = bpGeoJsonZone(roofPolygon, zoneId);
    var detectionBody = om.createObjectNode();
    var geoServerProps = om.createObjectNode();
    detectionBody.put("emailReceiver", email);
    detectionBody.set(
        "detectableObjectModel", om.createObjectNode().put("modelName", DETECTION_MODEL));
    detectionBody.put("zoneName", projectName); // nom de l'analyse = Feature.properties.ref_bat
    detectionBody.put("geoJsonDelimitationType", "ROOF");
    detectionBody.put("needsImageOutput", true);
    detectionBody.set("geoJsonZone", detection);

    var syncResult = geoPost("/detections/" + detectionId + "/sync", detectionBody);
    // C3 renvoie DÉJÀ le VGG (via vgg_file_url) : inutile de poller pour l'obtenir.
    var vggFileUrl = findFirstText(syncResult, "vgg_file_url");
    var originalImageUrl = findFirstText(syncResult, "original_image_url");
    // imageProperties = taille réelle de l'image analysée (doit refléter IMAGE_SIZE demandé).
    var imageProps = findFirstNode(syncResult, "imageProperties");
    var imageWidth = imageProps == null ? null : numberOrNull(imageProps, "width");
    var imageHeight = imageProps == null ? null : numberOrNull(imageProps, "height");
    log.info(
        "C3 détection synchrone: image={}x{} (demandé {}), vgg={}",
        imageWidth,
        imageHeight,
        IMAGE_SIZE,
        vggFileUrl);
    assertNotNull(vggFileUrl, "C3 /sync n'a pas renvoyé de vgg_file_url");

    // C6 — télécharger le VGG (régions + métriques toiture) renvoyé directement par C3.
    // Structure réelle : LISTE d'un objet indexé par "<uuid>_<zoom>_<xTile>_<yTile>.png",
    // contenant `properties` (métriques, OPTIONNELLES selon l'étape) et des `regions` labellisées
    // (HUMIDITE_CLAIR/INTENSE, OBSTACLE, VELUX, CHEMINEE, TOITURE_REVETEMENT).
    var vggJson = rawGetJson(vggFileUrl);
    var vggInner = vggInner(vggJson); // objet indexé par filename -> { properties, regions }
    var roof = roofPropertiesOf(vggInner);
    log.info(
        "C6 VGG initial: {} régions, pente={}° hauteur={} m (statuts {}/{})",
        roof.regionCount(),
        roof.roofSlopeInDegrees(),
        roof.roofHeightInMeters(),
        roof.roofSlopeStatus(),
        roof.roofHeightStatus());
    assertTrue(vggJson.isArray() && vggJson.size() > 0, "VGG: liste non vide attendue");
    assertTrue(roof.regionCount() > 0, "aucune région détectée dans le VGG");

    // C8 — TRADUIRE l'emprise (lon/lat) en pixels SUR L'IMAGE DE L'AREAPICTURE via le converter
    // bpartners-api, qui cale l'origine sur la tuile (tileX-1, tileY-1) et la taille de tuile.
    var roofPixelPoints = lonLatPolygonToPixel(accountId, roofPolygon, zoomNumber, tileX, tileY);
    assertTrue(roofPixelPoints.size() > 0, "conversion emprise -> pixels vide");
    log.info(
        "C8 emprise convertie en {} points pixel (tuile origine {},{})",
        roofPixelPoints.size(),
        tileX - 1,
        tileY - 1);

    // C9a — ★ SAUVEGARDE #1 : dès que le VGG est obtenu, persister l'UNIQUE annotation toiture
    // ("Polygon A") dans le draft pour un affichage immédiat, SANS attendre les roof properties.
    var annotations =
        buildAnnotations(vggInner, roofPixelPoints, pictureId, draftAnnotationId, userId, roof);
    assertTrue(annotations.size() > 0, "aucune annotation toiture construite");
    var roofMetadata =
        roofInstanceMetadata(annotations); // metadata de l'instance toiture (patch #2)
    var annotationsPath =
        "/accounts/"
            + accountId
            + "/areaPictures/"
            + pictureId
            + "/annotations/"
            + draftAnnotationId;
    // L'update passe creationDatetime en passthrough : sans lui, la colonne NOT NULL est réécrite
    // à null (le front renvoie le creationDatetime du draft chargé). On relit donc l'existant.
    var creationDatetime = text(bpGet(annotationsPath), "/creationDatetime");
    if (creationDatetime == null) {
      creationDatetime = java.time.Instant.now().toString();
    }
    var draftWithResults = om.createObjectNode();
    draftWithResults.put("id", draftAnnotationId);
    draftWithResults.put("idAreaPicture", pictureId);
    draftWithResults.put("isDraft", true);
    draftWithResults.put("creationDatetime", creationDatetime);
    draftWithResults.set("annotations", annotations);
    draftWithResults.set(
        "properties",
        om.createObjectNode()
            .put("name", projectName)
            // false -> image de fond = areaPicture 2D (fileId), pas l'image d'analyse : c'est le
            // repère dans lequel on a converti le polygone (cf. Annotator.tsx sourceFileId).
            .put("analyseImageGenerated", false)
            .put("roofAnalyseId", detectionId));
    bpPut(annotationsPath, draftWithResults);
    var reloadedCount = bpGet(annotationsPath).path("annotations").size();
    assertTrue(reloadedCount > 0, "le draft rechargé ne contient aucune annotation");
    log.info(
        "C9a ✔ save #1 : toiture 'Polygon A' persistée (draft {}, roofAnalyseId={}) -> affichable",
        draftAnnotationId,
        detectionId);

    // C4 — DÉCLENCHER le calcul des roof properties (pente/hauteur). geoPut échoue le test si le
    // statut n'est pas 2xx, donc un retour ici garantit le déclenchement.
    geoPut("/detections/" + detectionId + "/roofs/properties", null);
    log.info("C4 ✔ calcul des roof properties déclenché");

    // C5 — POLLER (5 s d'intervalle, 120 s max) en RE-TÉLÉCHARGEANT le VGG jusqu'à ce que les roof
    // properties y soient disponibles (roof_*_data_status = AVAILABLE), puis les lire dans le VGG.
    long deadline = currentTimeMillis() + ROOF_POLL_MAX.toMillis();
    int attempt = 0;
    while (!roofPropertiesReady(roof) && currentTimeMillis() < deadline) {
      attempt++;
      log.info(
          "C5 poll {} : roof properties pas prêtes (statuts {}/{}), attente {}s...",
          attempt,
          roof.roofSlopeStatus(),
          roof.roofHeightStatus(),
          ROOF_POLL_INTERVAL.toSeconds());
      Thread.sleep(ROOF_POLL_INTERVAL.toMillis());
      vggJson = rawGetJson(vggFileUrl);
      vggInner = vggInner(vggJson);
      roof = roofPropertiesOf(vggInner);
    }

    // C9b — ★ SAUVEGARDE #2 : quand les roof properties sont dispo, patcher JUSTE pente/hauteur sur
    // l'instance toiture (les régions restent inchangées) puis re-persister.
    if (roofPropertiesReady(roof) && roofMetadata != null) {
      if (roof.roofSlopeInDegrees() != null) {
        roofMetadata.put("slope", roof.roofSlopeInDegrees());
      }
      if (roof.roofHeightInMeters() != null) {
        roofMetadata.put("height", roof.roofHeightInMeters());
      }
      bpPut(annotationsPath, draftWithResults); // même corps, metadata toiture patchée en place
      log.info(
          "C5/C9b ✔ save #2 : roof properties enregistrées (pente={}° hauteur={} m)",
          roof.roofSlopeInDegrees(),
          roof.roofHeightInMeters());
    } else {
      log.warn(
          "C5 roof properties indisponibles après {}s — save #1 conservé sans pente/hauteur",
          ROOF_POLL_MAX.toSeconds());
    }

    log.info(
        "✅ E2E OK — projet='{}' adresse='{}' pente={}° hauteur={} m régions={}"
            + " (prospect={}, areaPicture={}, detection={})",
        projectName,
        address,
        roof.roofSlopeInDegrees(),
        roof.roofHeightInMeters(),
        annotations.size(),
        prospectId,
        pictureId,
        detectionId);
  }

  /** Metadata de l'instance toiture (labelType "Toit") dans la liste d'annotations, ou null. */
  private ObjectNode roofInstanceMetadata(ArrayNode annotations) {
    for (var instance : annotations) {
      if ("Toit".equals(instance.path("labelType").asText())) {
        return (ObjectNode) instance.path("metadata");
      }
    }
    return null;
  }

  // ================================================================ RÉSULTAT VGG

  private record RoofResult(
      Double usureRate,
      Double moisissureRate,
      Double humiditeRate,
      Double globalRateValue,
      String globalRateType,
      Double roofAreaInM2,
      String revetement1,
      String revetement2,
      String coveringPrimary,
      Double roofSlopeInDegrees,
      Double roofHeightInMeters,
      String roofSlopeStatus,
      String roofHeightStatus,
      int regionCount) {}

  /** VGG réel : liste -> objet indexé par filename -> { properties, regions }. Renvoie l'inner. */
  private JsonNode vggInner(JsonNode vgg) {
    assertTrue(vgg.isArray() && vgg.size() > 0, "VGG: liste non vide attendue");
    var byFilename = vgg.get(0); // { "<uuid>_<zoom>_<xTile>_<yTile>.png": { ... } }
    var fields = byFilename.fields();
    assertTrue(fields.hasNext(), "VGG: objet indexé par filename attendu");
    return fields.next().getValue();
  }

  /**
   * Toutes les propriétés sont optionnelles : le VGG évolue selon l'étape (rates puis
   * pente/hauteur).
   */
  private RoofResult roofPropertiesOf(JsonNode inner) {
    var props = inner.path("properties");
    var regions = inner.path("regions");
    var rev1 = textOrNull(props, "revetement_1");
    // covering est une chaîne JSON {"primary":...,"secondary":...} ; sinon repli sur revetement_1.
    var coveringPrimary = rev1 != null ? rev1 : coveringPrimaryOf(textOrNull(props, "covering"));
    return new RoofResult(
        numberOrNull(props, "usure_rate"),
        numberOrNull(props, "moisissure_rate"),
        numberOrNull(props, "humidite_rate"),
        numberOrNull(props, "global_rate_value"),
        textOrNull(props, "global_rate_type"),
        numberOrNull(props, "roof_area_in_m2"),
        rev1,
        textOrNull(props, "revetement_2"),
        coveringPrimary,
        numberOrNull(props, "roof_slope_in_degrees"),
        numberOrNull(props, "roof_height_in_meters"),
        textOrNull(props, "roof_slope_data_status"),
        textOrNull(props, "roof_height_data_status"),
        regions.isObject() ? regions.size() : 0);
  }

  private String coveringPrimaryOf(String coveringJson) {
    if (coveringJson == null) {
      return null;
    }
    try {
      return textOrNull(om.readTree(coveringJson), "primary");
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Roof properties prêtes = pente/hauteur présentes, ou statut de données "AVAILABLE" dans le VGG.
   */
  private static boolean roofPropertiesReady(RoofResult roof) {
    return roof.roofSlopeInDegrees() != null
        || roof.roofHeightInMeters() != null
        || "AVAILABLE".equalsIgnoreCase(roof.roofSlopeStatus())
        || "AVAILABLE".equalsIgnoreCase(roof.roofHeightStatus());
  }

  /**
   * Construit l'UNIQUE annotation à persister : le polygone de toiture nommé "Polygon A" (labelType
   * "roof"), sa géométrie étant l'emprise TRADUITE en pixels sur l'image de l'areaPicture ({@code
   * roofPixelPoints}), avec les métriques du VGG en metadata. Les résultats 2D (humidité/obstacles)
   * ne sont PAS persistés — le VGG complet est réutilisé côté front séparément.
   */
  private ArrayNode buildAnnotations(
      JsonNode inner,
      ArrayNode roofPixelPoints,
      String pictureId,
      String annotationId,
      String userId,
      RoofResult roof) {
    var out = om.createArrayNode();
    if (roofPixelPoints == null || roofPixelPoints.isEmpty()) {
      return out;
    }
    // Flag "obstacle" pour la metadata : OUI si le VGG contient une région de type obstacle.
    boolean hasObstacle = false;
    var regions = inner.path("regions");
    if (regions.isObject()) {
      var it = regions.fields();
      while (it.hasNext()) {
        if (isObstacleLabel(
            it.next().getValue().path("region_attributes").path("label").asText())) {
          hasObstacle = true;
          break;
        }
      }
    }
    var polygon = om.createObjectNode();
    polygon.set("points", roofPixelPoints);
    var metadata = roofMetadata(roof, hasObstacle);
    metadata.put("fillColor", ROOF_FILL_COLOR);
    metadata.put("strokeColor", ROOF_STROKE_COLOR);
    var instance = om.createObjectNode();
    // id de toiture 2D attendu par le front (suffixe __roof-polygon SANS __analyse-roof) : c'est
    // ce qui range le polygone dans la version 2D et non dans la partie analyse (cf. bpartners-web
    // createRoofPolygon vs AnnotatorComponent analyseRoofId).
    instance.put("id", randomUUID() + "__roof-polygon");
    instance.put("areaPictureId", pictureId);
    instance.put("annotationId", annotationId);
    instance.put("userId", userId);
    instance.put("labelName", "Polygone A");
    instance.put("labelType", "Toit");
    instance.set("polygon", polygon);
    instance.set("metadata", metadata);
    out.add(instance);
    return out;
  }

  /**
   * Traduit un polygone lon/lat en pixels sur l'image de l'areaPicture via {@code POST
   * /accounts/{accountId}/annotations/convert}. L'origine est calée sur la tuile : le converter
   * extrait (xTile, yTile) du filename {@code <hex>_<zoom>_<xTile>_<yTile>} et projette {@code
   * pixel = (mercator(lon/lat)·2^zoom - tile)·size}. Le front utilise tuile = currentTile-1 (image
   * étendue 3×3) et size = taille de tuile.
   */
  private ArrayNode lonLatPolygonToPixel(
      String accountId, ArrayNode lonLatPolygon, int zoom, int tileX, int tileY) throws Exception {
    var ring = lonLatPolygon.get(0); // anneau extérieur : [[lon,lat], ...]
    var lons = om.createArrayNode();
    var lats = om.createArrayNode();
    for (var point : ring) {
      lons.add(point.get(0).asDouble());
      lats.add(point.get(1).asDouble());
    }
    var filename =
        randomUUID().toString().replace("-", "")
            + "_"
            + zoom
            + "_"
            + (tileX - 1)
            + "_"
            + (tileY - 1)
            + ".jpg";
    var shape = om.createObjectNode();
    shape.set("all_points_x", lons);
    shape.set("all_points_y", lats);
    var region = om.createObjectNode();
    region.set("shape_attributes", shape);
    var regions = om.createObjectNode();
    regions.set(filename, region);
    var annotation = om.createObjectNode();
    annotation.put("filename", filename);
    annotation.put("size", TILE_SIZE_PX);
    annotation.put("zoom", zoom);
    annotation.set("regions", regions);
    var body = om.createObjectNode();
    body.set("roof", annotation);

    var response = bpPost("/accounts/" + accountId + "/annotations/convert", body);
    var shapeOut = findFirstNode(response, "shape_attributes");
    var out = om.createArrayNode();
    if (shapeOut == null) {
      return out;
    }
    var xs = shapeOut.path("all_points_x");
    var ys = shapeOut.path("all_points_y");
    int n = Math.min(xs.size(), ys.size());
    for (int i = 0; i < n; i++) {
      out.add(om.createObjectNode().put("x", xs.get(i).asDouble()).put("y", ys.get(i).asDouble()));
    }
    return out;
  }

  private ObjectNode roofMetadata(RoofResult roof, boolean hasObstacle) {
    var m = om.createObjectNode();
    // Toutes les métriques sont optionnelles (le VGG les livre selon l'étape).
    if (roof.roofAreaInM2() != null) {
      m.put("area", roof.roofAreaInM2());
    }
    if (roof.roofSlopeInDegrees() != null) {
      m.put("slope", roof.roofSlopeInDegrees());
    }
    if (roof.roofHeightInMeters() != null) {
      m.put("height", roof.roofHeightInMeters());
    }
    if (roof.usureRate() != null) {
      m.put("wearLevel", roof.usureRate());
      m.put("wearness", wearnessOf(roof.usureRate())); // ⚠️ seuils à confirmer
    }
    if (roof.moisissureRate() != null) {
      m.put("moldRate", roof.moisissureRate());
    }
    if (roof.humiditeRate() != null) {
      m.put("humidityLevel", roof.humiditeRate());
    }
    // covering = revêtement principal (le front lit `covering`, cf. golden payload).
    if (roof.coveringPrimary() != null) {
      m.put("covering", roof.coveringPrimary());
    }
    if (roof.revetement1() != null) {
      m.put("revetement1", roof.revetement1());
    }
    if (roof.revetement2() != null) {
      m.put("revetement2", roof.revetement2());
    }
    m.put("obstacle", hasObstacle ? "OUI" : "NON");
    return m;
  }

  private static boolean isObstacleLabel(String label) {
    return "OBSTACLE".equals(label) || "VELUX".equals(label) || "CHEMINEE".equals(label);
  }

  /**
   * Mappe le taux d'usure (%) sur l'enum Wearness (LOW/PARTIAL/ADVANCED/EXTREME). Seuils à valider.
   */
  private static String wearnessOf(Double wearLevel) {
    double w = wearLevel == null ? 0 : wearLevel;
    if (w < 25) {
      return "LOW";
    }
    if (w < 50) {
      return "PARTIAL";
    }
    if (w < 75) {
      return "ADVANCED";
    }
    return "EXTREME";
  }

  // ===================================================================== EMPRISE

  private record Emprise(String refBat, String address, ArrayNode polygon) {}

  private Emprise firstValidEmprise() throws Exception {
    JsonNode root;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(GEOJSON_RESOURCE)) {
      assertNotNull(in, "ressource introuvable: " + GEOJSON_RESOURCE);
      root = om.readTree(in);
    }
    int i = 0;
    for (var feature : root.path("features")) {
      if (i == 0) {
        i++;
        continue;
      }
      var props = feature.path("properties");
      var address = props.path("adresse").asText(null);
      var refBat = props.path("ref_bat").asText(null);
      var geom = feature.path("geometry");
      var coords = geom.path("coordinates");
      boolean isPolygon = "Polygon".equals(geom.path("type").asText());
      boolean hasRing = coords.isArray() && coords.size() > 0 && coords.get(0).size() >= 4;
      if (address != null && !address.isBlank() && isPolygon && hasRing) {
        // ref_bat sert de nom au projet et à l'analyse ; repli sur l'adresse s'il est absent.
        var name =
            (refBat != null && !refBat.isBlank())
                ? refBat + "_" + currentTimeMillis()
                : address + "_" + currentTimeMillis();
        return new Emprise(name, address, (ArrayNode) coords);
      }
    }
    return fail("aucune emprise valide (adresse + polygone) dans " + GEOJSON_RESOURCE);
  }

  /**
   * geoJsonZone = un seul Feature Polygon (l'emprise), tel qu'attendu par la détection (step C3).
   */
  private ArrayNode bpGeoJsonZone(ArrayNode polygonCoordinates, String zoneId) {
    var geometry = om.createObjectNode();
    geometry.put("type", "Polygon");
    geometry.set("coordinates", polygonCoordinates);
    var feature = om.createObjectNode();
    feature.put("type", "Feature");
    feature.set("geometry", geometry);
    feature.set("properties", om.createObjectNode());
    return om.createArrayNode().add(feature);
  }

  // ==================================================================== HTTP BP

  private JsonNode bpGet(String path) throws Exception {
    return send(
        HttpRequest.newBuilder(URI.create(bpApiUrl + path))
            .header("Authorization", "Bearer " + accessToken)
            .GET(),
        path);
  }

  private JsonNode bpPut(String path, JsonNode body) throws Exception {
    return send(
        HttpRequest.newBuilder(URI.create(bpApiUrl + path))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .PUT(bodyOf(body)),
        path);
  }

  private JsonNode bpPost(String path, JsonNode body) throws Exception {
    return send(
        HttpRequest.newBuilder(URI.create(bpApiUrl + path))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(bodyOf(body)),
        path);
  }

  // ================================================================ HTTP GEO

  private JsonNode geoPost(String path, JsonNode body) throws Exception {
    return send(
        HttpRequest.newBuilder(URI.create(geoDetectionUrl + path))
            .header("x-api-key", geoApiKey)
            .header("Content-Type", "application/json")
            .POST(bodyOf(body)),
        path);
  }

  private JsonNode geoPut(String path, JsonNode body) throws Exception {
    return send(
        HttpRequest.newBuilder(URI.create(geoDetectionUrl + path))
            .header("x-api-key", geoApiKey)
            .header("Content-Type", "application/json")
            .PUT(bodyOf(body)),
        path);
  }

  // ================================================================ HTTP util

  private JsonNode send(HttpRequest.Builder builder, String label) throws Exception {
    var request = builder.timeout(Duration.ofSeconds(120)).build();
    var response = http.send(request, ofString(UTF_8));
    var status = response.statusCode();
    var raw = response.body();
    if (status < 200 || status >= 300) {
      fail(label + " -> HTTP " + status + " : " + raw);
    }
    return (raw == null || raw.isBlank()) ? om.nullNode() : om.readTree(raw);
  }

  private byte[] rawBytes(String url) throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .timeout(Duration.ofSeconds(120))
            .GET()
            .build();
    var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode(), "GET raw file -> " + response.statusCode());
    return response.body();
  }

  /** GET sur une URL absolue (S3 signée) renvoyant du JSON. */
  private JsonNode rawGetJson(String url) throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(120)).GET().build();
    var response = http.send(request, ofString(UTF_8));
    assertEquals(200, response.statusCode(), "GET vgg result -> " + response.statusCode());
    return om.readTree(response.body());
  }

  private HttpRequest.BodyPublisher bodyOf(JsonNode body) {
    try {
      var json = (body == null || body.isNull()) ? "{}" : om.writeValueAsString(body);
      return HttpRequest.BodyPublishers.ofString(json, UTF_8);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // ================================================================ JSON util

  private static String text(JsonNode node, String jsonPointer) {
    var found = node.at(jsonPointer);
    return found.isMissingNode() || found.isNull() ? null : found.asText();
  }

  /** Recherche récursive de la première valeur textuelle non nulle pour un nom de champ donné. */
  private static String findFirstText(JsonNode node, String field) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isObject()) {
      var direct = node.get(field);
      if (direct != null
          && direct.isValueNode()
          && !direct.isNull()
          && !direct.asText().isBlank()) {
        return direct.asText();
      }
      for (var child : node) {
        var found = findFirstText(child, field);
        if (found != null) {
          return found;
        }
      }
    } else if (node.isArray()) {
      for (var child : node) {
        var found = findFirstText(child, field);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  /** Recherche récursive du premier nœud OBJET portant le nom de champ donné. */
  private static JsonNode findFirstNode(JsonNode node, String field) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isObject()) {
      var direct = node.get(field);
      if (direct != null && direct.isObject()) {
        return direct;
      }
    }
    if (node.isContainerNode()) {
      for (var child : node) {
        var found = findFirstNode(child, field);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static Double numberOrNull(JsonNode node, String field) {
    var value = node.path(field);
    return value.isNumber() ? value.asDouble() : null;
  }

  private static String textOrNull(JsonNode node, String field) {
    var value = node.path(field);
    return value.isValueNode() && !value.isNull() ? value.asText() : null;
  }

  // ================================================================ env util

  private static String requireEnv(String name) {
    var value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("variable d'environnement requise manquante: " + name);
    }
    return value;
  }

  private static String env(String name, String defaultValue) {
    var value = System.getenv(name);
    return (value == null || value.isBlank()) ? defaultValue : value;
  }

  private static String trimTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
