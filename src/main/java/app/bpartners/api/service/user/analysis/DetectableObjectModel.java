package app.bpartners.api.service.user.analysis;

public record DetectableObjectModel(String modelName) {
  public static DetectableObjectModel ofName(String modelName) {
    return new DetectableObjectModel(modelName);
  }
}
