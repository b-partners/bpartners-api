package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.api.file.FileDownloader;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.service.WMS.imageSource.TileExtenderRequestBody;
import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled
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
            URI.create("https://93pm3wtg9e.execute-api.eu-west-3.amazonaws.com/Prod/extend"),
            TileExtenderRequestBody.builder()
                .x(528671)
                .y(383099)
                .z(20)
                .server("geoserver")
                .layer("cannes_labege")
                .build(),
            true);

    System.out.println(downloaded.getAbsolutePath());
  }
}
