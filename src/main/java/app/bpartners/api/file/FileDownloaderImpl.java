package app.bpartners.api.file;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import app.bpartners.api.model.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public final class FileDownloaderImpl implements FileDownloader {
  private final RestTemplate restTemplate;
  private final ObjectMapper om;

  public FileDownloaderImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.om = objectMapper;
  }

  @Override
  @SneakyThrows
  public File get(String filename, URI uri) {
    log.info("GET downloading {} from {}", filename, uri);
    var response = restTemplate.getForObject(uri, byte[].class);
    return createFileFrom(filename, response);
  }

  @Override
  @SneakyThrows
  public File postJson(String filename, URI uri, Serializable body, boolean isBase64Encoded) {
    log.info("POST downloading {} from {}", filename, uri);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);
    headers.setAccept(List.of(ALL));
    log.info("body {}", body);
    RequestEntity<byte[]> request =
        new RequestEntity<>(om.writeValueAsBytes(body), headers, POST, uri);
    byte[] bytes;
    if (isBase64Encoded) {
      var base64Response = restTemplate.postForObject(uri, request, String.class);
      bytes = Base64.getDecoder().decode(base64Response);
    } else {
      bytes = restTemplate.postForObject(uri, request, byte[].class);
    }

    if (bytes == null) {
      throw new ApiException(SERVER_EXCEPTION, "unable to POST download from " + uri);
    }
    return createFileFrom(filename, bytes);
  }

  @NotNull
  private static File createFileFrom(String filename, byte[] response) throws IOException {
    File res = File.createTempFile(filename, null);
    StreamUtils.copy(response, new FileOutputStream(res));
    return res;
  }
}
