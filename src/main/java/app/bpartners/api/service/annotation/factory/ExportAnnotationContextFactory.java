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
import app.bpartners.api.service.annotation.ImageCompressor;
import app.bpartners.api.service.annotation.model.Drawer;
import app.bpartners.api.service.annotation.model.GroupedByKey;
import app.bpartners.api.service.annotation.model.RenderedImages;
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
  // Items that fit on the first page of a section (below the section title) and on the following
  // pages; pans take more room per page than annotations and facades.
  private static final int FIRST_PAGE_ITEMS = 3;
  private static final int ITEMS_PER_PAGE = 3;
  private static final int PANS_PER_PAGE = 4;

  public static Context createContext(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      RenderedImages annotationImages,
      RenderedImages annotation3DImages,
      FileService fileService,
      ExportAreaPictureAnnotationImage3DGenerator annotationImage3DGenerator,
      AreaPictureAnnotationConfRestMapper areaPictureAnnotationConfRestMapper) {
    var context = new Context();

    var conf = areaPictureAnnotationConfRestMapper.toDomain(annotation.getConf());
    var logoUri = logoBase64 == null ? null : base64ToUri(logoBase64);
    var mainImageUri =
        annotationImages.main() == null ? null : base64ToUri(annotationImages.main());
    var subImagesUris = annotationImages.subs().stream().map(ImageUriUtils::base64ToUri).toList();
    var defaultAccountHolder = user.getDefaultHolder();
    var userAddress = defaultAccountHolder != null ? defaultAccountHolder.getAddress() : "-";

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
            GroupedByKey.from(annotation.getAnnotations()), FIRST_PAGE_ITEMS, ITEMS_PER_PAGE));
    context.setVariable(
        "subImagesPages", groupByFirstPage(subImagesUris, FIRST_PAGE_ITEMS, ITEMS_PER_PAGE));

    if (annotation.getLlm() != null) {
      configureLLMContext(context, annotation);
    }

    if (annotation.getGlobalRateValue() != null || annotation.getGlobalRateType() != null) {
      configureGlobalRateContext(context, annotation);
    }
    if (annotation.get3d() != null) {
      configureAnnotation3DContext(
          context,
          annotation.get3d(),
          annotation3DImages,
          fileService,
          conf.isShowAnnotation3dPages());
      if (conf.isShowAnnotation3dPages()) {
        configureAnnotationFacade3DContext(context, annotation.get3d(), fileService);
      }
      configureAnnotationSummaryContext(context, annotation, annotationImage3DGenerator);
    }

    if (annotation.getCustomPages() != null) {
      context.setVariable("customPages", mapCustomPages(annotation.getCustomPages(), fileService));
    }

    configureLastSectionContext(context, annotation, conf);

    return context;
  }

  static void configureLastSectionContext(
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
    // The per-pan page renders under the same condition as the measures page and always
    // follows it, so the measures page is never the last one. Marking it as such made
    // openhtmltopdf keep it with the page after it, emitting a blank page before both.
    context.setVariable("overallSummaryIsLast", false);
    context.setVariable("facesSummaryIsLast", showOverall && !showCustomPages && !showLlm);
    context.setVariable("customPagesIsLast", showCustomPages && !showLlm);
  }

  private static List<CustomPage> mapCustomPages(
      List<app.bpartners.api.endpoint.rest.model.CustomPage> customPages, FileService fileService) {
    return customPages.stream()
        .map(
            page ->
                CustomPage.builder()
                    .pageTitle(page.getPageTitle())
                    .sections(
                        page.getSections().stream()
                            .map(section -> mapSection(section, fileService))
                            .toList())
                    .build())
        .toList();
  }

  private static PageSection mapSection(
      app.bpartners.api.endpoint.rest.model.PageSection restSection, FileService fileService) {
    SectionPriority priority = SectionPriority.valueOf(restSection.getPriority().name());
    if (restSection instanceof app.bpartners.api.endpoint.rest.model.TextSection textRestSection) {
      return app.bpartners.api.service.annotation.model.custompage.TextSection.builder()
          .priority(priority)
          .text(textRestSection.getText())
          .build();
    } else if (restSection
        instanceof app.bpartners.api.endpoint.rest.model.ImageSection imageRestSection) {
      return app.bpartners.api.service.annotation.model.custompage.ImageSection.builder()
          .priority(priority)
          .url(resolveImageSectionUri(imageRestSection, fileService))
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
          mapSection(splitRestSection.getLeftSection(), fileService),
          mapSection(splitRestSection.getRightSection(), fileService));
    } else if (restSection
        instanceof app.bpartners.api.endpoint.rest.model.ThreeSplitSection threeSplitRestSection) {
      return new app.bpartners.api.service.annotation.model.custompage.ThreeSplitSection(
          priority,
          mapSection(threeSplitRestSection.getLeftSection(), fileService),
          mapSection(threeSplitRestSection.getMiddleSection(), fileService),
          mapSection(threeSplitRestSection.getRightSection(), fileService));
    }
    throw new IllegalArgumentException("Unknown section type: " + restSection.getClass());
  }

  private static String resolveImageSectionUri(
      app.bpartners.api.endpoint.rest.model.ImageSection imageRestSection,
      FileService fileService) {
    String fileId = imageRestSection.getFileId();
    if (fileId != null && !fileId.isBlank()) {
      try {
        var fileInfo = fileService.findById(fileId);
        if (fileInfo == null) {
          log.warn("Can't get image file for file id: {}", fileId);
        } else {
          var file =
              fileService.downloadFile(
                  FileType.IMAGE, fileInfo.getUserUploaderId(), fileInfo.getId());
          if (file == null) {
            log.warn("Can't download image file for file id: {}", fileId);
          } else {
            var image = ImageIO.read(file);
            if (image != null) {
              return ImageUriUtils.bufferedImageToUri(image);
            }
            log.warn("Could not read image from file id: {}", fileId);
          }
        }
      } catch (IOException e) {
        log.error("Could not download image from file id: {}", fileId, e);
      }
    }

    String imageUri = String.valueOf(imageRestSection.getUrl());
    try {
      URI uri = new URI(imageUri);
      String scheme = uri.getScheme();
      if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
        var image = ImageIO.read(uri.toURL());
        if (image != null) {
          return ImageUriUtils.bufferedImageToUri(image);
        }
        log.warn("Could not read image from url: {}", imageUri);
      } else {
        log.warn("Blocked non-http/https image url: {}", imageUri);
      }
    } catch (IOException | URISyntaxException | IllegalArgumentException e) {
      log.error("Could not download image from url: {}", imageRestSection.getUrl(), e);
    }
    return imageUri;
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
      RenderedImages annotation3DImages,
      FileService fileService,
      boolean showAnnotation3dPages) {
    var pages3D = groupByFirstPage(annotation3D.getPans(), FIRST_PAGE_ITEMS, PANS_PER_PAGE);
    context.setVariable("pages3D", pages3D);
    context.setVariable("roofSlopeBoundariesPerPage", getRoofSlopeBoundaryPerPage(pages3D));
    context.setVariable("roofSlopeBoundariesImages", getRoofSlopeBoundaryMap());

    if (!showAnnotation3dPages) {
      context.setVariable("mainImage3D", null);
      context.setVariable("topViewPanImagesUris", List.of());
      context.setVariable("pansImages3DUris", List.of());
      return;
    }

    var mainImage3DUri = base64ToUri(annotation3DImages.main());
    var subImages3DUris =
        annotation3DImages.subs().stream().map(ImageUriUtils::base64ToUri).toList();
    var pansScreenshootImages3D = getPansImages3DContext(annotation3D, fileService);

    context.setVariable("mainImage3D", mainImage3DUri);
    context.setVariable(
        "topViewPanImagesUris", groupByFirstPage(subImages3DUris, FIRST_PAGE_ITEMS, PANS_PER_PAGE));
    context.setVariable(
        "pansImages3DUris",
        groupByFirstPage(pansScreenshootImages3D, FIRST_PAGE_ITEMS, PANS_PER_PAGE));
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
    return getItemsImages3DContext(annotation3D.getPans(), "pan", fileService);
  }

  static void configureAnnotationFacade3DContext(
      Context context, ExportAreaPictureAnnotation3D annotation3D, FileService fileService) {
    if (annotation3D.getFacades() == null) {
      return;
    }
    var pagesFacade3D =
        groupByFirstPage(annotation3D.getFacades(), FIRST_PAGE_ITEMS, ITEMS_PER_PAGE);
    var facadesImages3D = getFacadesImages3DContext(annotation3D, fileService);

    context.setVariable("pagesFacade3D", pagesFacade3D);
    context.setVariable(
        "facadesImages3DUris", groupByFirstPage(facadesImages3D, FIRST_PAGE_ITEMS, ITEMS_PER_PAGE));
  }

  static List<String> getFacadesImages3DContext(
      ExportAreaPictureAnnotation3D annotation3D, FileService fileService) {
    if (annotation3D.getFacades() == null) {
      return List.of();
    }
    return getItemsImages3DContext(annotation3D.getFacades(), "facade", fileService);
  }

  // Pans and facades share the same rendering: a highlighted top view, replaced by the item's own
  // uploaded image when there is one.
  private static List<String> getItemsImages3DContext(
      List<ExportAreaPictureAnnotation3DPan> items, String itemLabel, FileService fileService) {
    var exportAreaPictureAnnotationImage3DGenerator =
        new ExportAreaPictureAnnotationImage3DGenerator();
    var imageCompressor = new ImageCompressor();

    var overallTopView = exportAreaPictureAnnotationImage3DGenerator.generateBaseImage(items);
    return items.stream()
        .map(
            item -> {
              var image =
                  exportAreaPictureAnnotationImage3DGenerator
                      .generateBaseImageWithHighlightedPanWithSlopeBoundary(
                          overallTopView.second(), overallTopView.first(), item);
              try {
                if (item.getImageUri() == null || item.getImageUri().isBlank()) {
                  log.warn(
                      "No image provided for {}: {}. Falling back to top view image.",
                      itemLabel,
                      item.getName());
                  return bufferedImageToUri(image);
                }
                var fileInfo = fileService.findById(item.getImageUri());
                if (fileInfo == null) {
                  log.warn(
                      "Can't get image file for {}: {} from file id {}. Falling back to top view"
                          + " image.",
                      itemLabel,
                      item.getName(),
                      item.getImageUri());
                  return bufferedImageToUri(image);
                }
                var fileFromFileService =
                    fileService.downloadFile(
                        FileType.IMAGE, fileInfo.getUserUploaderId(), fileInfo.getId());

                if (fileFromFileService == null) {
                  log.warn(
                      "Can't get image file for {}: {}. Falling back to top view image.",
                      itemLabel,
                      item.getName());
                  return bufferedImageToUri(image);
                }

                image = ImageIO.read(fileFromFileService);
              } catch (IOException e) {
                log.error(
                    "Error while downloading {} image: {}. Falling back to top view image.",
                    itemLabel,
                    item.getName(),
                    e);
              }

              image = imageCompressor.compressImage(image);
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
