package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ExportTest {
   private static BufferedImage image;
   private ExportAreaPictureAnnotationPDFGenerator pdfGenerator = new ExportAreaPictureAnnotationPDFGenerator(new TemplateResolverEngine());
   private ExportAreaPictureAnnotationImageGenerator imageGenerator = new ExportAreaPictureAnnotationImageGenerator();

   private ExportAreaPictureAnnotationPDFProcessor subject = new ExportAreaPictureAnnotationPDFProcessor(
       pdfGenerator, imageGenerator
   );

   @BeforeAll
   static void setUp() throws IOException {
       image = ImageIO.read(Objects.requireNonNull(ExportTest.class.getClassLoader().getResourceAsStream("files/file.png")));
   }

    @Test
    public void test() throws IOException {
        var pdf = subject.process(payload(), image);
        save(pdf, "/home/ricka/test.pdf");
    }

    private void save(byte[] content, String path) {
        try {
            Files.write(Path.of(path), content);
            System.out.println("Saved successfully to " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ExportAreaPictureAnnotation payload(){
       return new ExportAreaPictureAnnotation()
       .imageUrl("dump")
       .globalRateValue(50d)
           .globalRateType("D")
           .address("23 Av. du Dr Raymond Picaud, 06400 Cannes ")
           .annotations(List.of(instanceRectangle()))
           .llm(llm());
    }

    private static ExportAreaPictureAnnotationInstance instanceRectangle() {
        List<Point> points = List.of(new Point().x(100d).y(100d),
            new Point().x(400d).y(100d),
            new Point().x(400d).y(300d),
            new Point().x(100d).y(300d),
            new Point().x(100d).y(100d)
        );

        List<ExportAreaPictureAnnotationMeasurement> measurements = List.of(new ExportAreaPictureAnnotationMeasurement().isInvisible(false).unit("m").value(30d), // côté haut
                new ExportAreaPictureAnnotationMeasurement().isInvisible(false).unit("m").value(20d), // côté droit
                new ExportAreaPictureAnnotationMeasurement().isInvisible(false).unit("m").value(30d), // côté bas
                new ExportAreaPictureAnnotationMeasurement().isInvisible(false).unit("m").value(20d)  // côté gauche
        );

        return new ExportAreaPictureAnnotationInstance().fillColor("#00000000") // transparent
                .strokeColor("#000000FF") // bleu
                .labelName("Rectangle").polygon(new Polygon().points(points)).measurements(measurements);
    }

        private static String llm(){
        return """
        <section>
          <h2>COMPRENDRE VOTRE RAPPORT</h2>
          <h3><span>🟢</span> CATÉGORIE A : Excellent état général</h3>
          <ul>
            <li>L'analyse a montré que la toiture est dans un excellent état général. Le faible taux d'humidité de 8.04 % et la quasi-inexistence de moisissure à 0.74 % sont des signes très positifs. Avec un revêtement de gravier, ces niveaux indiquent qu'il n'y a pas de stagnation d'eau ni d'accumulation favorisant la prolifération de mousses ou de champignons. L'absence de fissures et de risque d'incendie atteste de la solidité et de la sécurisation optimale de la structure.</li>
          </ul>
        
          <ul>
            <li>Le revêtement en gravier est très bien adapté pour limiter les problèmes d'humidité et d'usure. L'absence totale d'usure (0.0 %) confirme que ce matériau fait preuve d'une remarquable longévité. Le faible taux de moisissure met en évidence l'efficacité du revêtement pour empêcher l'infiltration d'eau et la prolifération de végétations indésirables, même avec des obstacles présents.</li>
          </ul>
        </section>
        <section>
          <h2>CONSEILS DE L’ARTISAN COUVREUR</h2>
          <ul>
            <li>🔍 Inspection ciblée : Il est crucial de vérifier régulièrement les zones autour des obstacles tels que les pénétrations et les angles rentrants, où l'eau peut s'accumuler. </li>
            <li>🧼 Entretien recommandé : Procédez à un nettoyage préventif pour enlever les mousses et autres dépôts. Assurez-vous que les évacuations d'eau sont bien dégagées pour éviter tout risque lié à l'humidité.</li>
            <li>🛠️ Travaux à envisager : Bien que la toiture soit en excellent état, il est prudent de surveiller les joints périphériques et garantir leur étanchéité, surtout autour des obstacles.</li>
            <li>📸 Suivi : Un contrôle annuel, par inspection visuelle ou utilisation de drones, est préconisé pour détecter tout changement ou intrusion d'eau non visible à l'œil nu.</li>
            <li>🧪 Vérifications complémentaires : Envisagez des tests supplémentaires comme l'arrosage ciblé pour détecter d'éventuelles infiltrations, ainsi que l'utilisation de caméras thermiques pour vérifier l'intégrité thermique de la toiture.</li>
          </ul>
        </section>
        """;
    }
}
