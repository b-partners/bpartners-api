package app.bpartners.api.service.llm;

import org.springframework.web.bind.annotation.BindParam;

public record LegacyLlmRoofQuery(
    String adresse,
    String gps,
    Integer millesimeImage1,
    Integer millesimeImage2,
    Double surfaceEnM2,
    String typeToiture,
    Double penteMin,
    Double penteMax,
    String revetement,
    @BindParam("humidité") Double humidite,
    Double moisissure,
    Double usure,
    String obstacles,
    String mutation,
    Boolean fissureCassure,
    Boolean risqueFeu,
    Double noteDegradationGlobale,
    String categorie,
    String revetement2,
    Double hauteurBatiment,
    String commentaireCouvreur) {}
