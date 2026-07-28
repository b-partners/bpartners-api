package app.bpartners.api.service.annotation.factory;

import static app.bpartners.api.service.annotation.factory.RoofSlopeBoundaryFactory.getRoofSlopeBoundaryTypeNames;
import static app.bpartners.api.service.annotation.utils.ImageUriUtils.base64ToUri;
import static app.bpartners.api.service.annotation.utils.ImageUriUtils.bufferedImageToUri;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.AreaAnnotation3D;
import app.bpartners.api.service.annotation.AreaAnnotation3DPan;
import app.bpartners.api.service.annotation.AreaAnnotationExportPayload;
import app.bpartners.api.service.annotation.export.AreaAnnotationExportConf;
import app.bpartners.api.service.annotation.export.AreaAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.export.AreaAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.model.Drawer;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import app.bpartners.api.service.annotation.model.custompage.CustomPage;
import app.bpartners.api.service.annotation.model.custompage.ImageSection;
import app.bpartners.api.service.annotation.model.custompage.PageSection;
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
      AreaAnnotationExportPayload annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages,
      FileService fileService,
      AreaAnnotationImage3DGenerator annotationImage3DGenerator) {
    return createContext(
        user,
        logoBase64,
        annotation,
        annotationImages,
        annotation3DImages,
        null,
        fileService,
        annotationImage3DGenerator);
  }

  public static Context createContext(
      User user,
      String logoBase64,
      AreaAnnotationExportPayload annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages,
      Pair<String, List<String>> annotation3DFacadeImages,
      FileService fileService,
      AreaAnnotationImage3DGenerator annotationImage3DGenerator) {
    var context = new Context();

    var logoUri = logoBase64 == null ? null : base64ToUri(logoBase64);
    var mainImageUri = base64ToUri(annotationImages.first());
    var subImagesUris = annotationImages.second().stream().map(ImageUriUtils::base64ToUri).toList();
    var defaultAccountHolder = user.getDefaultHolder();
    var userAddress = defaultAccountHolder != null ? defaultAccountHolder.getAddress() : "-";
    var conf =
        annotation.getConf() != null ? annotation.getConf() : AreaAnnotationExportConf.DEFAULT;

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
            AreaAnnotationPDFGenerator.GroupedByKey.from(annotation.getAnnotations()), 3, 3));
    context.setVariable("subImagesPages", groupByFirstPage(subImagesUris, 3, 3));

    if (annotation.getLlm() != null) {
      configureLLMContext(context, annotation);
    }

    if (annotation.getGlobalRateValue() != null || annotation.getGlobalRateType() != null) {
      configureGlobalRateContext(context, annotation);
    }
    if (annotation.getAnnotation3d() != null) {
      configureAnnotation3DContext(
          context, annotation.getAnnotation3d(), annotation3DImages, fileService);
      configureAnnotationFacade3DContext(
          context, annotation.getAnnotation3d(), annotation3DFacadeImages, fileService);
      configureAnnotationSummaryContext(context, annotation, annotationImage3DGenerator);
    }

    if (annotation.getCustomPages() != null) {
      context.setVariable("customPages", downloadCustomPageImages(annotation.getCustomPages()));
    }

    return context;
  }

  static void configureAnnotationSummaryContext(
      Context context,
      AreaAnnotationExportPayload annotation,
      AreaAnnotationImage3DGenerator annotationImage3DGenerator) {
    context.setVariable(
        "roofSummary", AnnotationSummaryFactory.create(annotation, annotationImage3DGenerator));

    context.setVariable(
        "allEdgeTypes",
        allRoofSlopeBoundaryTypes(
            (Map<Integer, List<String>>) context.getVariable("roofSlopeBoundariesPerPage")));
  }

  static void configureLLMContext(Context context, AreaAnnotationExportPayload annotation) {
    context.setVariable("llm", annotation.getLlm());
  }

  static void configureGlobalRateContext(Context context, AreaAnnotationExportPayload annotation) {
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
      AreaAnnotation3D annotation3D,
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
      List<List<AreaAnnotation3DPan>> paged3DPans) {
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
      AreaAnnotation3D annotation3D, FileService fileService) {
    var exportAreaPictureAnnotationImage3DGenerator = new AreaAnnotationImage3DGenerator();

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
                      "Can't get image file for pan: {} from file id {}. Falling back to top"
                          + " view image.",
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
      AreaAnnotation3D annotation3D,
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
      AreaAnnotation3D annotation3D, FileService fileService) {
    if (annotation3D.getFacades() == null) {
      return List.of();
    }
    var exportAreaPictureAnnotationImage3DGenerator = new AreaAnnotationImage3DGenerator();

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

  static List<CustomPage> downloadCustomPageImages(List<CustomPage> customPages) {
    return customPages.stream()
        .map(
            page -> {
              var processedSections =
                  page.getSections().stream()
                      .map(ExportAnnotationContextFactory::processSectionImages)
                      .toList();
              return CustomPage.builder()
                  .pageTitle(page.getPageTitle())
                  .sections(processedSections)
                  .build();
            })
        .toList();
  }

  private static PageSection processSectionImages(PageSection section) {
    if (section instanceof ImageSection imageSection) {
      var imageUrl = imageSection.getUrl();
      if (imageUrl != null) {
        try {
          URI uri = new URI(imageUrl);
          String scheme = uri.getScheme();
          if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            var image = ImageIO.read(uri.toURL());
            if (image != null) {
              var dataUri = ImageUriUtils.bufferedImageToUri(image);
              return ImageSection.builder()
                  .priority(imageSection.getPriority())
                  .url(dataUri)
                  .caption(imageSection.getCaption())
                  .build();
            } else {
              log.warn("Could not read image from url: {}", imageUrl);
            }
          } else {
            log.warn("Blocked non-http/https image url: {}", imageUrl);
          }
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
          log.error("Could not download image from url: {}", imageUrl, e);
        }
      }
      return imageSection;
    }
    if (section
        instanceof
        app.bpartners.api.service.annotation.model.custompage.SplitSection splitSection) {
      return new app.bpartners.api.service.annotation.model.custompage.SplitSection(
          splitSection.getPriority(),
          processSectionImages(splitSection.getLeftSection()),
          processSectionImages(splitSection.getRightSection()));
    }
    if (section
        instanceof
        app.bpartners.api.service.annotation.model.custompage.ThreeSplitSection threeSplitSection) {
      return new app.bpartners.api.service.annotation.model.custompage.ThreeSplitSection(
          threeSplitSection.getPriority(),
          processSectionImages(threeSplitSection.getLeftSection()),
          processSectionImages(threeSplitSection.getMiddleSection()),
          processSectionImages(threeSplitSection.getRightSection()));
    }
    return section;
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
