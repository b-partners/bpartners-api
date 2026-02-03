package app.bpartners.api.integration;

import static app.bpartners.api.service.wms.imageSource.TileExtenderRequestBody.ShiftDirection.UP_DOWN_SIDE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.service.wms.imageSource.TileExtenderRequestBody;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class FileDownloaderIT extends MockedThirdParties {
  @Autowired FileDownloader fileDownloader;

  @Test
  void file_downloader_get_ok() {
    var downloaded =
        fileDownloader.get(
            "filename",
            URI.create("https://wms.openstreetmap.fr/tms/1.0.0/tous_fr/20/528244/381209.jpeg"));

    assertNotNull(downloaded);
  }

  @Test
  void file_downloader_post_ok() {
    var downloaded =
        fileDownloader.postJson(
            "filename",
            URI.create(
                "https://c2aa5siclrjcyzfpnrrh4hejyi0wsvwu.lambda-url.eu-west-3.on.aws/extend"),
            TileExtenderRequestBody.builder()
                .x(538596)
                .y(377561)
                .z(20)
                .server("geoserver")
                .layer("Auvergne_Rhone_Alpes_All_Region_5cm")
                .isCropped(false)
                .latitude(44.9120193)
                .longitude(4.9125046)
                .shiftNb(0)
                .build(),
            true);
    System.out.println(downloaded.getAbsolutePath());
  }

  @Test
  void file_downloader_post_shifted_down_ok() {
    var downloaded =
        fileDownloader.postJson(
            "filename",
            URI.create(
                "https://c2aa5siclrjcyzfpnrrh4hejyi0wsvwu.lambda-url.eu-west-3.on.aws/extend"),
            TileExtenderRequestBody.builder()
                .x(538596)
                .y(377561)
                .z(20)
                .server("geoserver")
                .layer("Auvergne_Rhone_Alpes_All_Region_5cm")
                .isCropped(false)
                .latitude(44.9120193)
                .longitude(4.9125046)
                .shiftNb(-1)
                .shiftDirection(UP_DOWN_SIDE)
                .build(),
            true);
    assertNotNull(downloaded.getAbsolutePath());
  }
}
