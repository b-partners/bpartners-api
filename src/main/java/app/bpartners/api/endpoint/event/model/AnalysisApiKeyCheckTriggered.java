package app.bpartners.api.endpoint.event.model;

import java.time.Duration;
import java.util.List;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@Getter
public class AnalysisApiKeyCheckTriggered extends PojaEvent {
  private String userId;

  private List<String> nextUserIds;

  // offset of the next chunk of users to chain once nextUserIds is exhausted, null on the last
  // chunk
  private Integer nextChunkOffset;

  public AnalysisApiKeyCheckTriggered(String userId) {
    this(userId, List.of());
  }

  public AnalysisApiKeyCheckTriggered(String userId, List<String> nextUserIds) {
    this(userId, nextUserIds, null);
  }

  // ids are ~40 bytes each, keeps events far below the 256KB EventBridge/SQS limit
  public static final int CHUNK_SIZE = 1000;

  public static AnalysisApiKeyCheckTriggered ofChunk(List<String> allIds, int offset) {
    var end = Math.min(offset + CHUNK_SIZE, allIds.size());
    var chunk = allIds.subList(offset, end);
    return new AnalysisApiKeyCheckTriggered(
        chunk.getFirst(), chunk.subList(1, chunk.size()), end < allIds.size() ? end : null);
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(2L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
