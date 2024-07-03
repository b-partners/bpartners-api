package app.bpartners.api.endpoint.event;

import static java.lang.System.getenv;

import app.bpartners.api.PojaGenerated;
import lombok.Getter;

@PojaGenerated
@SuppressWarnings("all")
public enum EventStack {
  EVENT_STACK_1(getenv("AWS_EVENT_STACK_1_SQS_QUEUE_URL")),
  EVENT_STACK_2(getenv("AWS_EVENT_STACK_2_SQS_QUEUE_URL"));

  @Getter private final String sqsQueueUrl;

  EventStack(String sqsQueueUrl) {
    this.sqsQueueUrl = sqsQueueUrl;
  }
}
