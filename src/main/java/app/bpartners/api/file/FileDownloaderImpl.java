package app.bpartners.api.file;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.io.File.createTempFile;
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;

@Slf4j
@Component
final class FileDownloaderImpl implements FileDownloader {
  private final RestTemplate restTemplate;
  private final ObjectMapper om;
  private final BucketConf bucketConf;

  public FileDownloaderImpl(RestTemplate restTemplate, ObjectMapper objectMapper, BucketConf bucketConf) {
    this.restTemplate = restTemplate;
    this.om = objectMapper;
    this.bucketConf = bucketConf;
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
  public File getFromS3(String filename, URI uri) {
    log.info("GET downloading {} from {}", filename, uri);
    var response = restTemplate.getForObject(uri, s3Prop.class);
    return download(response.bucketName, response.s3Key);
  }

  public record s3Prop(String bucketName, String s3Key){};

  @SneakyThrows
  public File download(String bucketName, String bucketKey) {
    var destination =
            createTempFile(prefixFromBucketKey(bucketKey), suffixFromBucketKey(bucketKey));
    FileDownload download =
            bucketConf
                    .getS3TransferManager()
                    .downloadFile(
                            DownloadFileRequest.builder()
                                    .getObjectRequest(
                                            GetObjectRequest.builder()
                                                    .bucket(bucketName)
                                                    .key(bucketKey)
                                                    .build())
                                    .destination(destination)
                                    .build());
    download.completionFuture().join();
    return destination;
  }


  private String prefixFromBucketKey(String bucketKey) {
    return lastNameSplitByDot(bucketKey)[0];
  }

  private String suffixFromBucketKey(String bucketKey) {
    var splitByDot = lastNameSplitByDot(bucketKey);
    return splitByDot.length == 1 ? "" : splitByDot[splitByDot.length - 1];
  }

  private String[] lastNameSplitByDot(String bucketKey) {
    var splitByDash = bucketKey.split("/");
    var lastName = splitByDash[splitByDash.length - 1];
    return lastName.split("\\.");
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
