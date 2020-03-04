package nl.knaw.huc.resources;

import com.jayway.jsonpath.JsonPath;
import io.dropwizard.testing.junit.DropwizardAppRule;
import nl.knaw.huc.FullTextApplication;
import nl.knaw.huc.FullTextConfiguration;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static nl.knaw.huc.TestUtils.getResourceAsBytes;
import static org.assertj.core.api.Assertions.assertThat;

public class FullTextResourceTest {

  @ClassRule
  public static DropwizardAppRule<FullTextConfiguration> RULE =
      new DropwizardAppRule<>(
          FullTextApplication.class, System.getProperty("user.dir") + "/src/test/resources/test-config.yml");

  @Before
  public void setUp() {
    client = RULE.client();
    client.register(MultiPartFeature.class);
  }

  private Client client;

  @Test
  public void testMapping_returnsMapping() throws IOException {
    var response = client
        .target(getTestUrl("/mapping"))
        .request().get();

    assertThat(response.getStatus()).isEqualTo(200);
    var fields = response.readEntity(String.class);
    var suggestionType = JsonPath.parse(fields).read("$.full-text.mappings.properties.contents.type");
    assertThat(suggestionType).isEqualTo("text");
  }

  @Test
  public void testFields_returnsCompletionSuggesterInput_whenTxt() throws IOException {
    var fileContents = getResourceAsBytes("file.txt");
    var response = postTestContents(fileContents, "text/plain");
    var fields = response.readEntity(String.class);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(fields).isEqualTo("{\"contents\":\"Scheepenen als in den hoofde gemelt\\n\"}");
  }

  @Test
  public void testFields_returns422UnprocessableEntity_whenPdf() throws IOException {
    var fileContents = getResourceAsBytes("file.pdf");
    var response = postTestContents(fileContents, "application/pdf");
    assertThat(response.getStatus()).isEqualTo(422);
    var fields = response.readEntity(String.class);
    assertThat(fields).contains("Unexpected mimetype: got [application/pdf] but should be one of [");
  }

  private Response postTestContents(byte[] bytes, String mimetype) {
    var contentDisposition = FormDataContentDisposition
        .name("file")
        .size(bytes.length)
        .build();

    var bodyPart = new FormDataBodyPart(contentDisposition, bytes, MediaType.valueOf(mimetype));
    var multiPart = new FormDataMultiPart()
        .bodyPart(bodyPart);

    var request = client
        .target(getTestUrl("/fields"))
        .request();

    var entity = entity(multiPart, multiPart.getMediaType());

    return request.post(entity);
  }

  private String getTestUrl(String endpoint) {
    var port = RULE.getLocalPort();
    var host = "http://localhost";
    return format("%s:%d/full-text%s", host, port, endpoint);
  }

}
