package app.bpartners.api.endpoint.rest.controller;

import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

import app.bpartners.api.endpoint.rest.model.LlmRoof;
import app.bpartners.api.service.llm.LlmBridge;
import java.util.LinkedHashMap;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bridge to the genai service (https://github.com/b-partners/genai), same input and output. */
@RestController
@AllArgsConstructor
public class LlmController {
  private final LlmBridge llmBridge;

  @GetMapping(value = "/llm/toiture", produces = TEXT_HTML_VALUE)
  public ResponseEntity<String> getLegacyLlmRoofReport(
      @RequestParam(name = "adresse", required = false) String adresse,
      @RequestParam(name = "gps", required = false) String gps,
      @RequestParam(name = "millesimeImage1", required = false) Integer millesimeImage1,
      @RequestParam(name = "millesimeImage2", required = false) Integer millesimeImage2,
      @RequestParam(name = "surfaceEnM2") Double surfaceEnM2,
      @RequestParam(name = "typeToiture", required = false) String typeToiture,
      @RequestParam(name = "penteMin", required = false) Double penteMin,
      @RequestParam(name = "penteMax", required = false) Double penteMax,
      @RequestParam(name = "revetement") String revetement,
      @RequestParam(name = "humidité") Double humidite,
      @RequestParam(name = "moisissure") Double moisissure,
      @RequestParam(name = "usure") Double usure,
      @RequestParam(name = "obstacles", required = false) String obstacles,
      @RequestParam(name = "mutation", required = false) String mutation,
      @RequestParam(name = "fissureCassure", required = false) Boolean fissureCassure,
      @RequestParam(name = "risqueFeu", required = false) Boolean risqueFeu,
      @RequestParam(name = "noteDegradationGlobale", required = false)
          Double noteDegradationGlobale,
      @RequestParam(name = "categorie", required = false) String categorie,
      @RequestParam(name = "revetement2", required = false) String revetement2,
      @RequestParam(name = "hauteurBatiment", required = false) Double hauteurBatiment,
      @RequestParam(name = "commentaireCouvreur", required = false) String commentaireCouvreur) {
    var params = new LinkedHashMap<String, Object>();
    params.put("adresse", adresse);
    params.put("gps", gps);
    params.put("millesimeImage1", millesimeImage1);
    params.put("millesimeImage2", millesimeImage2);
    params.put("surfaceEnM2", surfaceEnM2);
    params.put("typeToiture", typeToiture);
    params.put("penteMin", penteMin);
    params.put("penteMax", penteMax);
    params.put("revetement", revetement);
    params.put("humidité", humidite);
    params.put("moisissure", moisissure);
    params.put("usure", usure);
    params.put("obstacles", obstacles);
    params.put("mutation", mutation);
    params.put("fissureCassure", fissureCassure);
    params.put("risqueFeu", risqueFeu);
    params.put("noteDegradationGlobale", noteDegradationGlobale);
    params.put("categorie", categorie);
    params.put("revetement2", revetement2);
    params.put("hauteurBatiment", hauteurBatiment);
    params.put("commentaireCouvreur", commentaireCouvreur);
    return llmBridge.getToiture(params);
  }

  @PostMapping(value = "/llm/toiture", produces = TEXT_HTML_VALUE)
  public ResponseEntity<String> getLlmRoofReport(@RequestBody LlmRoof roof) {
    return llmBridge.postToiture(roof);
  }
}
