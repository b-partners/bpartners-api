package app.bpartners.api.service.areapicture;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.exception.ServiceUnavailableException;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AreaPictureZoomValidator implements Consumer<AreaPicture> {
  @Override
  public void accept(AreaPicture areaPicture) {
    var currentLayer = areaPicture.getCurrentLayer();
    log.info("CurrentLayer={}", currentLayer);
    if (supportedLayers().contains(currentLayer.getName())) {
      if (currentLayer.getPrecisionLevelInCm() != 5) {
        throw new ServiceUnavailableException(
            "Layer " + currentLayer.getName() + " is temporarily unavailable");
      }
    } else {
      throw new NotImplementedException(
          "Layer " + currentLayer.getName() + " is not yet supported");
    }
  }

  private List<String> supportedLayers() {
    return List.of(
        "RHONE_2018_5CM",
        "RHONE_2023_5CM",
        "COTE_D_OR_2024_5cm",
        "PCRS.LAMB93",
        "Bas-Rhin_2023_5cm",
        "MARNES_2018_5cm",
        "MOSELLE_METZ_METROPOLE_2024_5CM",
        "Meurthe-et-moselle_Grand-Nancy_2023_5cm",
        "ALPES-MARITIMES_2020_5cm",
        "ALPES-MARITIMES_2024_5cm",
        "IGN_PHOTO_AERIENNE",
        "Ortho_Lisieux_Normandie_2022",
        "MANCHE_2021_5CM",
        "CHARENTE_2019_5cm",
        "GIRONDE_2023_5cm",
        "Pyrénées-Atlantique-Pau_Metropole_2024_5cm",
        "HERAULT_2020_5cm",
        "TARN-ET-GARONNE_2020_5CM",
        "Loire-Atlantique_Clisson_Sevre_Maine_2020_5cm",
        "INDRE_ET_LOIRE_2024_5CM",
        "Auvergne_Rhone_Alpes_All_Region_5cm",
        "Correze",
        "VENDEE_2025",
        "Eur_Et_Loir_Chartres",
        "ILE-DE-RE",
        "FINISTERE_2023_5cm",
        "HAUTE-GARONNE_2022_5cm",
        "HAUTE-SAVOIE_2023_5CM",
        "Thionville_2021_5cm",
        "Angouleme_2019",
        "Seine-Saint-Denis_2018_5cm",
        "Haut-De-Seine_2018_5cm",
        "Savoie-Enedis",
        "Auvergne_Rhone_Alpes_PCRS_5cm");
  }
}
