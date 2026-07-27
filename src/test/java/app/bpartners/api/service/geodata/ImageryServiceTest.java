package app.bpartners.api.service.geodata;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.model.exception.ImageryServiceException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ImageryServiceTest {
  private ImageryService subject;
  @Mock private HttpResponse<String> httpResponse;
  @Mock private HttpClient httpClient;

  @BeforeEach
  void setUp() {
    subject = new ImageryService("http://dummy.com", httpClient);
  }

  @Test
  void retrieve_area_picture_details_from_geodata_api_ok() throws Exception {
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body())
        .thenReturn(
            """
            {
              "id": "c247c3b4-9a5b-4c1b-8ecc-8340e1a30515",
              "xTile": 272351,
              "yTile": 191548,
              "xOffset": 981,
              "yOffset": 987,
              "currentTile": {
                "x": 272351,
                "y": 191548,
                "zoom": {
                  "level": "BUILDING",
                  "number": 19
                }
              },
              "referenceTile": {
                "x": 272348,
                "y": 191545,
                "zoom": {
                  "level": "BUILDING",
                  "number": 19
                }
              },
              "currentGeoPosition": {
                "score": 0.0,
                "longitude": 7.0089052,
                "latitude": 43.5503066
              },
              "actualLayer": {
                "id": "94a9a89c-6282-4921-aaef-219810a884fc",
                "name": "ALPES_MARITIMES_5cm",
                "year": 2025,
                "source": "GEOSERVER",
                "departementName": "alpes-maritimes",
                "maximumZoomLevel": "HOUSES_0",
                "maximumZoom": {
                  "level": "HOUSES_0",
                  "number": 20
                },
                "precisionLevelInCm": 5,
                "lastUpdatedAt": "2025-01-01",
                "creationDateTime": "2026-07-16T05:47:13.304Z",
                "expiredAt": null
              },
              "otherLayers": [],
              "geoPositions": [
                {
                  "score": 0.0,
                  "longitude": 7.0089052,
                  "latitude": 43.5503066
                }
              ],
              "imagePresignedUrl": {
                "value": "https://example.com/image",
                "expirationDelay": 86400,
                "updatedAt": "2026-07-16T18:47:01.938939901Z"
              },
              "address": "43.55027681708214, 7.0089592493070025",
              "zoomLevel": "BUILDING",
              "zoom": {
                "level": "BUILDING",
                "number": 19
              },
              "fileId": "f8e42205-0c25-46a1-bb4b-0ba2d3fd8df9",
              "filename": "ALPES_MARITIMES_5cm_BUILDING_272348_191545_extended",
              "createdAt": null,
              "updatedAt": null,
              "isExtended": true,
              "shiftNb": 0,
              "isOpaque": false,
              "shiftDirection": null
            }
            """);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    var actual =
        subject.downloadFromGeodataSource(
            new CrupdateAreaPictureDetails()
                .shiftNb(0)
                .address("43.55027681708214, 7.0089592493070025")
                .fileId("f8e42205-0c25-46a1-bb4b-0ba2d3fd8df9")
                .filename("Layer 43.55027681708214, 7.0089592493070025")
                .prospectId("8287ca6b-5d7d-4c11-8ec4-e085e2a81e73")
                .zoomLevel(ZoomLevel.BUILDING)
                .isExtended(true)
                .isOpaque(false));

    assertEquals(272351, actual.getxTile());
    assertEquals(191548, actual.getyTile());
    assertEquals("ALPES_MARITIMES_5cm", actual.getActualLayer().getName());
  }

  @Test
  void retrieve_area_picture_by_id_from_geodata_api_ok() throws Exception {
    String pcrsId = "726f5b3b-d23b-40c3-b38e-68a43d7ae155";
    String charenteId = "4b8e79bd-12ac-4c1b-8195-f9575d5fc4c8";
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenAnswer(
            invocation -> {
              HttpRequest request = invocation.getArgument(0);
              String uri = request.uri().toString();
              if (uri.endsWith(pcrsId)) {
                when(httpResponse.body())
                    .thenReturn(
                        """
                        {
                          "id": "726f5b3b-d23b-40c3-b38e-68a43d7ae155",
                          "name": "PCRS",
                          "year": 2025,
                          "source": "GEOSERVER",
                          "departementName": "ALL",
                          "maximumZoomLevel": "HOUSES_0",
                          "maximumZoom": {
                            "level": "HOUSES_0",
                            "number": 20
                          },
                          "precisionLevelInCm": 5,
                          "lastUpdatedAt": null,
                          "creationDateTime": "2026-07-16T05:47:13.304Z",
                          "expiredAt": null
                        }
                        """);
              } else if (uri.endsWith(charenteId)) {
                when(httpResponse.body())
                    .thenReturn(
                        """
                        {
                          "id": "4b8e79bd-12ac-4c1b-8195-f9575d5fc4c8",
                          "name": "CHARENTE_2025",
                          "year": 2025,
                          "source": "GEOSERVER",
                          "departementName": "Charente",
                          "maximumZoomLevel": "HOUSES_0",
                          "maximumZoom": {
                            "level": "HOUSES_0",
                            "number": 20
                          },
                          "precisionLevelInCm": 5,
                          "lastUpdatedAt": null,
                          "creationDateTime": "2026-07-16T05:47:13.304Z",
                          "expiredAt": null
                        }
                        """);
              }

              return httpResponse;
            });

    var actualPcrs = subject.getById(pcrsId);
    var actualCharente2025 = subject.getById(charenteId);

    assertEquals("PCRS", actualPcrs.getName());
    assertEquals("ALL", actualPcrs.getDepartementName());
    assertEquals("CHARENTE_2025", actualCharente2025.getName());
    assertEquals("Charente", actualCharente2025.getDepartementName());
  }

  @Test
  void should_throw_imager_service_exception_when_api_returns_an_error() throws Exception {
    when(httpResponse.statusCode()).thenReturn(500);
    when(httpResponse.body()).thenReturn("Internal Server Error");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);

    assertThatThrownBy(() -> subject.getById("area-picture-id"))
        .isInstanceOf(ImageryServiceException.class)
        .hasMessageContaining("GeoData Imagery API request failed");
  }
}
