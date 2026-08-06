package app.bpartners.api.service.annotation.factory;

import static app.bpartners.api.service.annotation.factory.RoofSlopeBoundaryFactory.getRoofSlopeBoundaryTypeNames;
import static app.bpartners.api.service.annotation.utils.ImageUriUtils.base64ToUri;
import static app.bpartners.api.service.annotation.utils.ImageUriUtils.bufferedImageToUri;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationConf;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.model.Drawer;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import app.bpartners.api.service.annotation.model.custompage.CustomPage;
import app.bpartners.api.service.annotation.model.custompage.PageSection;
import app.bpartners.api.service.annotation.model.custompage.SectionPriority;
import app.bpartners.api.service.annotation.model.custompage.TableData;
import app.bpartners.api.service.annotation.utils.ImageUriUtils;
import app.bpartners.api.service.file.FileService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.context.Context;

@Slf4j
public class ExportAnnotationContextFactory {
  public static final String IMAGE_FORMAT = "png";

  public static Context createContext(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages,
      FileService fileService,
      ExportAreaPictureAnnotationImage3DGenerator annotationImage3DGenerator,
      AreaPictureAnnotationConfRestMapper areaPictureAnnotationConfRestMapper) {
    return createContext(
        user,
        logoBase64,
        annotation,
        annotationImages,
        annotation3DImages,
        null,
        fileService,
        annotationImage3DGenerator,
        areaPictureAnnotationConfRestMapper);
  }

  public static Context createContext(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages,
      Pair<String, List<String>> annotation3DFacadeImages,
      FileService fileService,
      ExportAreaPictureAnnotationImage3DGenerator annotationImage3DGenerator,
      AreaPictureAnnotationConfRestMapper areaPictureAnnotationConfRestMapper) {
    var context = new Context();

    var logoUri = logoBase64 == null ? null : base64ToUri(logoBase64);
    var mainImageUri = base64ToUri(annotationImages.first());
    var subImagesUris = annotationImages.second().stream().map(ImageUriUtils::base64ToUri).toList();
    var defaultAccountHolder = user.getDefaultHolder();
    var userAddress = defaultAccountHolder != null ? defaultAccountHolder.getAddress() : "-";
    var conf = areaPictureAnnotationConfRestMapper.toDomain(annotation.getConf());

    context.setVariable("user", user);
    context.setVariable("userWebsite", user.getDefaultWebsite());
    context.setVariable("logo", logoUri);
    context.setVariable("address", annotation.getAddress());
    context.setVariable("userAddress", userAddress);
    context.setVariable("mainImage", mainImageUri);
    context.setVariable("conf", conf);
    context.setVariable(
        "pages",
        groupByFirstPage(
            ExportAreaPictureAnnotationPDFGenerator.GroupedByKey.from(annotation.getAnnotations()),
            3,
            3));
    context.setVariable("subImagesPages", groupByFirstPage(subImagesUris, 3, 3));

    if (annotation.getLlm() != null) {
      configureLLMContext(context, annotation);
    }

    if (annotation.getGlobalRateValue() != null || annotation.getGlobalRateType() != null) {
      configureGlobalRateContext(context, annotation);
    }
    if (annotation.get3d() != null) {
      configureAnnotation3DContext(context, annotation.get3d(), annotation3DImages, fileService);
      configureAnnotationFacade3DContext(
          context, annotation.get3d(), annotation3DFacadeImages, fileService);
      configureAnnotationSummaryContext(context, annotation, annotationImage3DGenerator);
    }

    if (annotation.getCustomPages() != null) {
      context.setVariable("customPages", mapCustomPages(annotation.getCustomPages()));
    }

    configureLastSectionContext(context, annotation, conf);

    return context;
  }

  private static void configureLastSectionContext(
      Context context,
      ExportAreaPictureAnnotation annotation,
      ExportAreaPictureAnnotationConf conf) {
    boolean has3D = annotation.get3d() != null;
    boolean show3dPages = has3D && conf.isShowAnnotation3dPages();
    boolean showFacades3D =
        show3dPages && annotation.get3d() != null && annotation.get3d().getFacades() != null;
    boolean showMeasurement = has3D && conf.isShowMeasurementSummary();
    boolean showPitch = has3D && conf.isShowPitchSummary();
    boolean showArea = has3D && conf.isShowAreaSummary();
    boolean showOverall = has3D && conf.isShowOverallSummary();
    boolean showCustomPages =
        annotation.getCustomPages() != null && !annotation.getCustomPages().isEmpty();
    boolean showLlm = annotation.getLlm() != null && conf.isShowLlmSummary();
    boolean anySummary = showMeasurement || showPitch || showArea || showOverall;

    context.setVariable(
        "annotationPagesIsLast", !show3dPages && !anySummary && !showCustomPages && !showLlm);
    context.setVariable(
        "pans3DIsLast",
        show3dPages && !showFacades3D && !anySummary && !showCustomPages && !showLlm);
    context.setVariable(
        "facades3DIsLast", showFacades3D && !anySummary && !showCustomPages && !showLlm);
    context.setVariable(
        "measurementSummaryIsLast",
        showMeasurement && !showPitch && !showArea && !showOverall && !showCustomPages && !showLlm);
    context.setVariable(
        "pitchSummaryIsLast",
        showPitch && !showArea && !showOverall && !showCustomPages && !showLlm);
    context.setVariable(
        "areaSummaryIsLast", showArea && !showOverall && !showCustomPages && !showLlm);
    context.setVariable("overallSummaryIsLast", showOverall && !showCustomPages && !showLlm);
    context.setVariable("facesSummaryIsLast", showOverall && !showCustomPages && !showLlm);
    context.setVariable("customPagesIsLast", showCustomPages && !showLlm);
  }

  private static List<CustomPage> mapCustomPages(
      List<app.bpartners.api.endpoint.rest.model.CustomPage> customPages) {
    return customPages.stream()
        .map(
            page ->
                CustomPage.builder()
                    .pageTitle(page.getPageTitle())
                    .sections(
                        page.getSections().stream()
                            .map(ExportAnnotationContextFactory::mapSection)
                            .toList())
                    .build())
        .toList();
  }

  private static PageSection mapSection(
      app.bpartners.api.endpoint.rest.model.PageSection restSection) {
    SectionPriority priority = SectionPriority.valueOf(restSection.getPriority().name());
    if (restSection instanceof app.bpartners.api.endpoint.rest.model.TextSection textRestSection) {
      return app.bpartners.api.service.annotation.model.custompage.TextSection.builder()
          .priority(priority)
          .text(textRestSection.getText())
          .build();
    } else if (restSection
        instanceof app.bpartners.api.endpoint.rest.model.ImageSection imageRestSection) {
      String imageUri = String.valueOf(imageRestSection.getUrl());
      try {
        URI uri = new URI(imageUri);
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
          var image = ImageIO.read(uri.toURL());
          if (image != null) {
            imageUri = ImageUriUtils.bufferedImageToUri(image);
          } else {
            log.warn("Could not read image from url: {}", imageUri);
          }
        } else {
          log.warn("Blocked non-http/https image url: {}", imageUri);
        }
      } catch (IOException | URISyntaxException | IllegalArgumentException e) {
        log.error("Could not download image from url: {}", imageRestSection.getUrl(), e);
      }
      return app.bpartners.api.service.annotation.model.custompage.ImageSection.builder()
          .priority(priority)
          .url(imageUri)
          .caption(imageRestSection.getCaption())
          .build();
    } else if (restSection
        instanceof app.bpartners.api.endpoint.rest.model.TableSection tableRestSection) {
      app.bpartners.api.endpoint.rest.model.TableData restTableData =
          tableRestSection.getTableData();
      return app.bpartners.api.service.annotation.model.custompage.TableSection.builder()
          .priority(priority)
          .tableData(
              TableData.builder()
                  .headers(restTableData.getHeaders())
                  .rows(restTableData.getRows())
                  .build())
          .build();
    } else if (restSection
        instanceof app.bpartners.api.endpoint.rest.model.SplitSection splitRestSection) {
      return new app.bpartners.api.service.annotation.model.custompage.SplitSection(
          priority,
          mapSection(splitRestSection.getLeftSection()),
          mapSection(splitRestSection.getRightSection()));
    } else if (restSection
        instanceof app.bpartners.api.endpoint.rest.model.ThreeSplitSection threeSplitRestSection) {
      return new app.bpartners.api.service.annotation.model.custompage.ThreeSplitSection(
          priority,
          mapSection(threeSplitRestSection.getLeftSection()),
          mapSection(threeSplitRestSection.getMiddleSection()),
          mapSection(threeSplitRestSection.getRightSection()));
    }
    throw new IllegalArgumentException("Unknown section type: " + restSection.getClass());
  }

  static void configureAnnotationSummaryContext(
      Context context,
      ExportAreaPictureAnnotation annotation,
      ExportAreaPictureAnnotationImage3DGenerator annotationImage3DGenerator) {
    context.setVariable(
        "roofSummary", AnnotationSummaryFactory.create(annotation, annotationImage3DGenerator));

    context.setVariable(
        "allEdgeTypes",
        allRoofSlopeBoundaryTypes(
            (Map<Integer, List<String>>) context.getVariable("roofSlopeBoundariesPerPage")));
  }

  static void configureLLMContext(Context context, ExportAreaPictureAnnotation annotation) {
    context.setVariable("llm", annotation.getLlm());
  }

  static void configureGlobalRateContext(Context context, ExportAreaPictureAnnotation annotation) {
    var degradationLevels = DegradationLevel.values();
    var activeDegradationLevel = DegradationLevel.valueOf(annotation);

    context.setVariable("globalRateType", annotation.getGlobalRateType());
    context.setVariable("globalRateValue", annotation.getGlobalRateValue());
    context.setVariable(
        "degradationLevels",
        degradationLevels.length > 0 ? Arrays.asList(degradationLevels) : null);
    context.setVariable("activeDegradationLevel", activeDegradationLevel);
  }

  static void configureAnnotation3DContext(
      Context context,
      ExportAreaPictureAnnotation3D annotation3D,
      Pair<String, List<String>> annotation3DImages,
      FileService fileService) {
    var pages3D = groupByFirstPage(annotation3D.getPans(), 3, 4);
    var mainImage3DUri = base64ToUri(annotation3DImages.first());
    var subImages3DUris =
        annotation3DImages.second().stream().map(ImageUriUtils::base64ToUri).toList();
    var pansScreenshootImages3D = getPansImages3DContext(annotation3D, fileService);

    context.setVariable("pages3D", pages3D);
    context.setVariable("mainImage3D", mainImage3DUri);
    context.setVariable("roofSlopeBoundariesPerPage", getRoofSlopeBoundaryPerPage(pages3D));
    context.setVariable("roofSlopeBoundariesImages", getRoofSlopeBoundaryMap());
    context.setVariable("topViewPanImagesUris", groupByFirstPage(subImages3DUris, 3, 4));
    context.setVariable("pansImages3DUris", groupByFirstPage(pansScreenshootImages3D, 3, 4));
  }

  static HashSet<String> allRoofSlopeBoundaryTypes(Map<Integer, List<String>> perPage) {
    var allTypes = new ArrayList<String>();
    perPage.forEach((page, types) -> allTypes.addAll(types));

    return new HashSet<>(allTypes);
  }

  static Map<Integer, List<String>> getRoofSlopeBoundaryPerPage(
      List<List<ExportAreaPictureAnnotation3DPan>> paged3DPans) {
    var map = new HashMap<Integer, List<String>>();
    for (int i = 0; i < paged3DPans.size(); i++) {
      var page = paged3DPans.get(i);
      var typesInPage = new ArrayList<String>();
      for (var pan : page) {
        List<String> boundariesTypes = getRoofSlopeBoundaryTypeNames(pan);
        typesInPage.addAll(boundariesTypes);
      }
      map.put(i, new ArrayList<>(new LinkedHashSet<>(typesInPage)));
    }
    return map;
  }

  static Map<String, String> getRoofSlopeBoundaryMap() {
    var allRoofSlopeBoundaries = RoofSlopeBoundaryType.values();
    var map = new HashMap<String, String>();
    for (var boundary : allRoofSlopeBoundaries) {
      var image = Drawer.createStrokeIllustration(boundary);
      map.put(boundary.getLabel().replace("_", "-"), bufferedImageToUri(image));
    }
    return map;
  }

  static List<String> getPansImages3DContext(
      ExportAreaPictureAnnotation3D annotation3D, FileService fileService) {
    var exportAreaPictureAnnotationImage3DGenerator =
        new ExportAreaPictureAnnotationImage3DGenerator();

    var overallPansTopView =
        exportAreaPictureAnnotationImage3DGenerator.generateBaseImage(annotation3D.getPans());
    return annotation3D.getPans().stream()
        .map(
            pan -> {
              var image =
                  exportAreaPictureAnnotationImage3DGenerator
                      .generateBaseImageWithHighlightedPanWithSlopeBoundary(
                          overallPansTopView.second(), overallPansTopView.first(), pan);
              try {
                if (pan.getImageUri() == null || pan.getImageUri().isBlank()) {
                  log.warn(
                      "No image provided for pan: {}. Falling back to top view image.",
                      pan.getName());
                  return bufferedImageToUri(image);
                }
                var fileInfo = fileService.findById(pan.getImageUri());
                if (fileInfo == null) {
                  log.warn(
                      "Can't get image file for pan: {} from file id {}. Falling back to top view"
                          + " image.",
                      pan.getName(),
                      pan.getImageUri());
                  return bufferedImageToUri(image);
                }
                var fileFromFileService =
                    fileService.downloadFile(
                        FileType.IMAGE, fileInfo.getUserUploaderId(), fileInfo.getId());

                if (fileFromFileService == null) {
                  log.warn(
                      "Can't get image file for pan: {}. Falling back to top view image.",
                      pan.getName());
                  return bufferedImageToUri(image);
                }

                image = ImageIO.read(fileFromFileService);
              } catch (IOException e) {
                log.error(
                    "Error while downloading pan image: {}. Falling back to top view image.",
                    pan.getName(),
                    e);
              }

              return bufferedImageToUri(image);
            })
        .toList();
  }

  static void configureAnnotationFacade3DContext(
      Context context,
      ExportAreaPictureAnnotation3D annotation3D,
      Pair<String, List<String>> annotation3DFacadeImages,
      FileService fileService) {
    if (annotation3D.getFacades() == null) {
      return;
    }
    var pagesFacade3D = groupByFirstPage(annotation3D.getFacades(), 3, 4);
    var subImages3DUris =
        annotation3DFacadeImages != null && annotation3DFacadeImages.second() != null
            ? annotation3DFacadeImages.second().stream().map(ImageUriUtils::base64ToUri).toList()
            : List.<URI>of();
    var facadesImages3D = getFacadesImages3DContext(annotation3D, fileService);

    context.setVariable("pagesFacade3D", pagesFacade3D);
    context.setVariable("topViewFacadeImagesUris", groupByFirstPage(subImages3DUris, 3, 4));
    context.setVariable("facadesImages3DUris", groupByFirstPage(facadesImages3D, 3, 4));
  }

  static List<String> getFacadesImages3DContext(
      ExportAreaPictureAnnotation3D annotation3D, FileService fileService) {
    if (annotation3D.getFacades() == null) {
      return List.of();
    }
    var exportAreaPictureAnnotationImage3DGenerator =
        new ExportAreaPictureAnnotationImage3DGenerator();

    var overallFacadesTopView =
        exportAreaPictureAnnotationImage3DGenerator.generateBaseImage(annotation3D.getFacades());
    return annotation3D.getFacades().stream()
        .map(
            facade -> {
              var image =
                  exportAreaPictureAnnotationImage3DGenerator
                      .generateBaseImageWithHighlightedPanWithSlopeBoundary(
                          overallFacadesTopView.second(), overallFacadesTopView.first(), facade);
              try {
                if (facade.getImageUri() == null || facade.getImageUri().isBlank()) {
                  log.warn(
                      "No image provided for facade: {}. Falling back to top view image.",
                      facade.getName());
                  return bufferedImageToUri(image);
                }
                var fileInfo = fileService.findById(facade.getImageUri());
                if (fileInfo == null) {
                  log.warn(
                      "Can't get image file for facade: {} from file id {}. Falling back to top"
                          + " view image.",
                      facade.getName(),
                      facade.getImageUri());
                  return bufferedImageToUri(image);
                }
                var fileFromFileService =
                    fileService.downloadFile(
                        FileType.IMAGE, fileInfo.getUserUploaderId(), fileInfo.getId());

                if (fileFromFileService == null) {
                  log.warn(
                      "Can't get image file for facade: {}. Falling back to top view image.",
                      facade.getName());
                  return bufferedImageToUri(image);
                }

                image = ImageIO.read(fileFromFileService);
              } catch (IOException e) {
                log.error(
                    "Error while downloading facade image: {}. Falling back to top view image.",
                    facade.getName(),
                    e);
              }

              return bufferedImageToUri(image);
            })
        .toList();
  }

  static <T> List<List<T>> groupByFirstPage(List<T> list, int firstPageMax, int limit) {
    List<List<T>> pages = new ArrayList<>();
    var iterator = list.iterator();

    List<T> firstPage = new ArrayList<>();
    for (int i = 0; i < firstPageMax && iterator.hasNext(); i++) {
      firstPage.add(iterator.next());
    }
    if (!firstPage.isEmpty()) {
      pages.add(firstPage);
    }

    while (iterator.hasNext()) {
      List<T> page = new ArrayList<>();
      for (int i = 0; i < limit && iterator.hasNext(); i++) {
        page.add(iterator.next());
      }
      pages.add(page);
    }

    return pages;
  }
}
