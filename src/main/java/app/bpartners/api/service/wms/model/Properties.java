package app.bpartners.api.service.wms.model;

import lombok.Data;

@Data
public class Properties {
  private String acquisitionDate;
  private String acquisitionIdentifier;
  private String activityId;
  private double azimuthAngle;
  private double cloudCover;
  private String commercialReference;
  private String constellation;
  private String contractId;
  private String correlationId;
  private String customerReference;
  private String expirationDate;
  private String format;
  private GeometryCentroid geometryCentroid;
  private String id;
  private double illuminationAzimuthAngle;
  private double illuminationElevationAngle;
  private double incidenceAngle;
  private double incidenceAngleAcrossTrack;
  private double incidenceAngleAlongTrack;
  private String internalLine;
  private String internalReference;
  private String lastUpdateDate;
  private String organisationName;
  private String platform;
  private String processingCenter;
  private String processingDate;
  private String processingLevel;
  private String processorName;
  private String productCategory;
  private String productType;
  private String productionStatus;
  private String publicationDate;
  private boolean qualified;
  private double resolution;
  private String sensorType;
  private double snowCover;
  private String spectralRange;
  private double surfaceArea;
  private String workspaceId;
  private String workspaceName;
  private String workspaceTitle;
}
