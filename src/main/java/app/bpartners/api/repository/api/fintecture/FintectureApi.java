package app.bpartners.api.repository.api.fintecture;

import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.client.ApiResponse;
import app.bpartners.api.repository.api.ApiClient;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import app.bpartners.api.repository.fintecture.schema.PaymentInitiation;
import app.bpartners.api.repository.fintecture.schema.PaymentInitiation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

public class FintectureApi {
  private final HttpClient memberVarHttpClient;
  private final ObjectMapper memberVarObjectMapper;
  private final String memberVarBaseUri;
  private final Consumer<HttpRequest.Builder> memberVarInterceptor;
  private final Duration memberVarReadTimeout;
  private final Consumer<HttpResponse<InputStream>> memberVarResponseInterceptor;
  private final Consumer<HttpResponse<String>> memberVarAsyncResponseInterceptor;


  public FintectureApi() {
    this(new ApiClient());
  }

  public FintectureApi(ApiClient apiClient) {
    memberVarHttpClient = apiClient.getHttpClient();
    memberVarObjectMapper = apiClient.getObjectMapper();
    memberVarBaseUri = apiClient.getBaseUri();
    memberVarInterceptor = apiClient.getRequestInterceptor();
    memberVarReadTimeout = apiClient.getReadTimeout();
    memberVarAsyncResponseInterceptor = apiClient.getAsyncResponseInterceptor();
    memberVarResponseInterceptor = apiClient.getResponseInterceptor();
  }

  protected ApiException getApiException(String operationId, HttpResponse<InputStream> response) throws IOException {
    String body = response.body() == null ? null : new String(response.body().readAllBytes());
    String message = formatExceptionMessage(operationId, response.statusCode(), body);
    return new ApiException(response.statusCode(), message, response.headers(), body);
  }

  private String formatExceptionMessage(String operationId, int statusCode, String body) {
    if (body == null || body.isEmpty()) {
      body = "[no body]";
    }
    return operationId + " call failed with: " + statusCode + " - " + body;
  }

  public ApiResponse<PaymentInitiation> generatePaymentInitiation(PaymentInitiation paymentReq) throws ApiException, JsonProcessingException {
    HttpRequest.Builder localVarRequestBuilder = generatePaymentInitiationRequestBuilder(paymentReq);
    try {
      HttpResponse<InputStream> localVarResponse = memberVarHttpClient.send(
              localVarRequestBuilder.build(),
              HttpResponse.BodyHandlers.ofInputStream());
      if (memberVarResponseInterceptor != null) {
        memberVarResponseInterceptor.accept(localVarResponse);
      }
      if (localVarResponse.statusCode() / 100 != 2) {
        throw getApiException("generatePaymentInitiation", localVarResponse);
      }
      return new ApiResponse<PaymentRedirection>(
              localVarResponse.statusCode(),
              localVarResponse.headers().map(),
              memberVarObjectMapper.readValue(localVarResponse.body(), null)
      );
    } catch (IOException e) {
      throw new ApiException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(e);
    }
  }

  private HttpRequest.Builder generatePaymentInitiationRequestBuilder(PaymentInitiation paymentReq) throws ApiException, JsonProcessingException {

    if (paymentReq == null) {
      throw new ApiException(400, "Missing the required parameter 'payementUrlParams' when calling generatePaymentInitiation");
    }
    HttpRequest.Builder localVarRequestBuilder = HttpRequest.newBuilder();

    String localVarPath = "/paymentRequests";

    localVarRequestBuilder.uri(URI.create(memberVarBaseUri + localVarPath));

    localVarRequestBuilder.header("Content-Type", "application/json");
    localVarRequestBuilder.header("Accept", "application/json");

    byte[] localVarPostBody = memberVarObjectMapper.writeValueAsBytes(paymentReq);
    localVarRequestBuilder.method("POST", HttpRequest.BodyPublishers.ofByteArray(localVarPostBody));
    if (memberVarReadTimeout != null) {
      localVarRequestBuilder.timeout(memberVarReadTimeout);
    }
    if (memberVarInterceptor != null) {
      memberVarInterceptor.accept(localVarRequestBuilder);
    }
    return localVarRequestBuilder;
  }
}