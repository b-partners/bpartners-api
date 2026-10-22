package app.bpartners.api.service.annotation.factory;

import static app.bpartners.api.service.annotation.factory.RoofSlopeBoundaryFactory.getRoofSlopeBoundaryTypeNames;
import static app.bpartners.api.service.annotation.utils.ImageUriUtils.base64ToUri;
import static app.bpartners.api.service.annotation.utils.ImageUriUtils.bufferedImageToUri;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationConf;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.Drawer;
import app.bpartners.api.service.annotation.model.GroupedByKey;
import app.bpartners.api.service.annotation.model.RenderedImages;
import app.bpartners.api.service.annotation.model.ReportSection;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import app.bpartners.api.service.annotation.utils.ImageUriUtils;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportAnnotationContextFactory {
  private final CustomPageMapper customPageMapper;
  private final ItemImagesResolver itemImagesResolver;
  private final ExportAreaPictureAnnotationImage3DGenerator image3DGenerator;
  private final AreaPictureAnnotationConfRestMapper areaPictureAnnotationConfRestMapper;

  public static final String IMAGE_FORMAT = "png";
  // Items that fit on the first page of a section (below the section title) and on the following
  // pages; pans take more room per page than annotations and facades.
  private static final int FIRST_PAGE_ITEMS = 3;
  private static final int ITEMS_PER_PAGE = 3;
  private static final int PANS_PER_PAGE = 4;

  public Context createContext(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      RenderedImages annotationImages,
      RenderedImages annotation3DImages) {
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
          context, annotation.get3d(), annotation3DImages, conf.isShowAnnotation3dPages());
      if (conf.isShowAnnotation3dPages()) {
        configureAnnotationFacade3DContext(context, annotation.get3d());
      }
      configureAnnotationSummaryContext(context, annotation);
    }

    if (annotation.getCustomPages() != null) {
      context.setVariable("customPages", customPageMapper.map(annotation.getCustomPages()));
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

    // The last page group carries .last-page so no trailing blank page is emitted. Annotation
    // pages are always listed, whatever the conf, as the title page is never marked last.
    var enabled = new ArrayList<ReportSection>();
    enabled.add(ReportSection.ANNOTATIONS);
    if (show3dPages) enabled.add(ReportSection.PANS_3D);
    if (showFacades3D) enabled.add(ReportSection.FACADES_3D);
    if (showMeasurement) enabled.add(ReportSection.MEASUREMENT_SUMMARY);
    if (showPitch) enabled.add(ReportSection.PITCH_SUMMARY);
    if (showArea) enabled.add(ReportSection.AREA_SUMMARY);
    if (showOverall) {
      // The per-pan page renders under the same condition as the measures page and always
      // follows it, so the measures page is never the last one. Marking it as such made
      // openhtmltopdf keep it with the page after it, emitting a blank page before both.
      enabled.add(ReportSection.OVERALL_SUMMARY);
      enabled.add(ReportSection.FACES_SUMMARY);
    }
    if (showCustomPages) enabled.add(ReportSection.CUSTOM_PAGES);
    if (showLlm) enabled.add(ReportSection.LLM);

    context.setVariable("lastSection", enabled.getLast());
  }

  void configureAnnotationSummaryContext(Context context, ExportAreaPictureAnnotation annotation) {
    context.setVariable(
        "roofSummary", AnnotationSummaryFactory.create(annotation, image3DGenerator));

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

  void configureAnnotation3DContext(
      Context context,
      ExportAreaPictureAnnotation3D annotation3D,
      RenderedImages annotation3DImages,
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
    var pansScreenshootImages3D = itemImagesResolver.pans(annotation3D);

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

  void configureAnnotationFacade3DContext(
      Context context, ExportAreaPictureAnnotation3D annotation3D) {
    if (annotation3D.getFacades() == null) {
      return;
    }
    var pagesFacade3D =
        groupByFirstPage(annotation3D.getFacades(), FIRST_PAGE_ITEMS, ITEMS_PER_PAGE);
    var facadesImages3D = itemImagesResolver.facades(annotation3D);

    context.setVariable("pagesFacade3D", pagesFacade3D);
    context.setVariable(
        "facadesImages3DUris", groupByFirstPage(facadesImages3D, FIRST_PAGE_ITEMS, ITEMS_PER_PAGE));
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
