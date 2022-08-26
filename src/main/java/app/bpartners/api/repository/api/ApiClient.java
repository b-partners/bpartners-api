package app.bpartners.api.repository.api;

import app.bpartners.api.endpoint.rest.client.Pair;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ApiClient {
  private HttpClient.Builder builder;
  private ObjectMapper mapper;
  private String scheme;
  private String host;
  private int port;
  private String basePath;
  private Consumer<HttpRequest.Builder> interceptor;
  private Consumer<HttpResponse<InputStream>> responseInterceptor;
  private Consumer<HttpResponse<String>> asyncResponseInterceptor;
  private Duration readTimeout;

  private static String valueToString(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof OffsetDateTime) {
      return ((OffsetDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
    return value.toString();
  }


  public static String urlEncode(String s) {
    return URLEncoder.encode(s, UTF_8);
  }


  public static List<Pair> parameterToPairs(String name, Object value) {
    if (name == null || name.isEmpty() || value == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(new Pair(urlEncode(name), urlEncode(valueToString(value))));
  }


  public static List<Pair> parameterToPairs(
          String collectionFormat, String name, Collection<?> values) {
    if (name == null || name.isEmpty() || values == null || values.isEmpty()) {
      return Collections.emptyList();
    }


    String format = collectionFormat == null || collectionFormat.isEmpty() ? "csv" : collectionFormat;


    if ("multi".equals(format)) {
      return values.stream()
              .map(value -> new Pair(urlEncode(name), urlEncode(valueToString(value))))
              .collect(Collectors.toList());
    }

    String delimiter;
    switch(format) {
      case "csv":
        delimiter = urlEncode(",");
        break;
      case "ssv":
        delimiter = urlEncode(" ");
        break;
      case "tsv":
        delimiter = urlEncode("\t");
        break;
      case "pipes":
        delimiter = urlEncode("|");
        break;
      default:
        throw new IllegalArgumentException("Illegal collection format: " + collectionFormat);
    }

    StringJoiner joiner = new StringJoiner(delimiter);
    for (Object value : values) {
      joiner.add(urlEncode(valueToString(value)));
    }

    return Collections.singletonList(new Pair(urlEncode(name), joiner.toString()));
  }


  public ApiClient() {
    this.builder = createDefaultHttpClientBuilder();
    this.mapper = createDefaultObjectMapper();
    updateBaseUri(getDefaultBaseUri());
    interceptor = null;
    readTimeout = null;
    responseInterceptor = null;
    asyncResponseInterceptor = null;
  }

  public ApiClient(HttpClient.Builder builder, ObjectMapper mapper, String baseUri) {
    this.builder = builder;
    this.mapper = mapper;
    updateBaseUri(baseUri != null ? baseUri : getDefaultBaseUri());
    interceptor = null;
    readTimeout = null;
    responseInterceptor = null;
    asyncResponseInterceptor = null;
  }

  protected ObjectMapper createDefaultObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new JsonNullableModule());
    return mapper;
  }

  protected String getDefaultBaseUri() {
    return "https://api-dev.bpartners.app";
  }

  protected HttpClient.Builder createDefaultHttpClientBuilder() {
    return HttpClient.newBuilder();
  }

  public void updateBaseUri(String baseUri) {
    URI uri = URI.create(baseUri);
    scheme = uri.getScheme();
    host = uri.getHost();
    port = uri.getPort();
    basePath = uri.getRawPath();
  }

  public ApiClient setHttpClientBuilder(HttpClient.Builder builder) {
    this.builder = builder;
    return this;
  }

  public HttpClient getHttpClient() {
    return builder.build();
  }

  public ApiClient setObjectMapper(ObjectMapper mapper) {
    this.mapper = mapper;
    return this;
  }

  public ObjectMapper getObjectMapper() {
    return mapper.copy();
  }

  public ApiClient setHost(String host) {
    this.host = host;
    return this;
  }

  public ApiClient setPort(int port) {
    this.port = port;
    return this;
  }

  public ApiClient setBasePath(String basePath) {
    this.basePath = basePath;
    return this;
  }


  public String getBaseUri() {
    return scheme + "://" + host + (port == -1 ? "" : ":" + port) + basePath;
  }


  public ApiClient setScheme(String scheme){
    this.scheme = scheme;
    return this;
  }

  public ApiClient setRequestInterceptor(Consumer<HttpRequest.Builder> interceptor) {
    this.interceptor = interceptor;
    return this;
  }

  public Consumer<HttpRequest.Builder> getRequestInterceptor() {
    return interceptor;
  }

  public ApiClient setResponseInterceptor(Consumer<HttpResponse<InputStream>> interceptor) {
    this.responseInterceptor = interceptor;
    return this;
  }

  public Consumer<HttpResponse<InputStream>> getResponseInterceptor() {
    return responseInterceptor;
  }

  public ApiClient setAsyncResponseInterceptor(Consumer<HttpResponse<String>> interceptor) {
    this.asyncResponseInterceptor = interceptor;
    return this;
  }

  public Consumer<HttpResponse<String>> getAsyncResponseInterceptor() {
    return asyncResponseInterceptor;
  }

  public ApiClient setReadTimeout(Duration readTimeout) {
    this.readTimeout = readTimeout;
    return this;
  }

  public Duration getReadTimeout() {
    return readTimeout;
  }
}
