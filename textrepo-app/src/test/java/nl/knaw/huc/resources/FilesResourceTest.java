package nl.knaw.huc.resources;

import com.jayway.jsonpath.JsonPath;
import io.dropwizard.testing.junit.ResourceTestRule;
import nl.knaw.huc.api.TextRepoFile;
import nl.knaw.huc.service.index.FileIndexer;
import nl.knaw.huc.service.FileService;
import nl.knaw.huc.service.store.FileStorage;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.huc.resources.ResourceTestUtils.responsePart;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FilesResourceTest {
  private static final FileStorage FILE_STORAGE = mock(FileStorage.class);
  private static final FileIndexer FILE_INDEXER = mock(FileIndexer.class);

  private final static String sha224 = "55d4c44f5bc05762d8807f75f3f24b4095afa583ef70ac97eaf7afc6";
  private final static String content = "hello test";
  private final static TextRepoFile textRepoFile = new TextRepoFile(
    sha224,
    content.getBytes()
  );

  @ClassRule
  public static final ResourceTestRule resource = ResourceTestRule
    .builder()
    .addProvider(MultiPartFeature.class)
    .addResource(new FilesResource(new FileService(FILE_STORAGE, FILE_INDEXER)))
    .build();

  @Before
  public void setup() {
  }

  @After
  public void teardown() {
    reset(FILE_STORAGE);
    reset(FILE_INDEXER);
  }

  @Test
  public void testPostFile_returns201CreatedWithLocationHeader_whenFileUploaded() {
    var multiPart = new FormDataMultiPart()
      .field("file", content);

    final var request = resource
      .client()
      .register(MultiPartFeature.class)
      .target("/files")
      .request();

    final var entity = Entity.entity(multiPart, multiPart.getMediaType());
    var response = request.post(entity);

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getHeaderString("Location")).endsWith("files/" + sha224);
    var actualSha = responsePart(response, "$.sha224");
    assertThat(actualSha).isEqualTo(sha224);
  }

  @Test
  public void testPostFile_returnsStatus400BadRequest_whenFileIsMissing() {
    // No .field("file", content):
    var multiPart = new FormDataMultiPart()
      .field("filename", "just-a-filename.txt");

    final var request = resource
      .client()
      .register(MultiPartFeature.class)
      .target("/files")
      .request();

    var entity = Entity.entity(multiPart, multiPart.getMediaType());

    var response = request.post(entity);
    assertThat(response.getStatus()).isEqualTo(400);
    var message = JsonPath
      .parse(response.readEntity(String.class))
      .read("$.message");
    assertThat(message).isEqualTo("File is missing");
  }

  @Test
  public void testPostFile_addsFileToIndex() {
    var multiPart = new FormDataMultiPart()
      .field("file", content);

    final var request = resource
      .client()
      .register(MultiPartFeature.class)
      .target("/files")
      .request();

    var entity = Entity.entity(multiPart, multiPart.getMediaType());

    var response = request.post(entity);
    assertThat(response.getStatus()).isEqualTo(201);

    var argument = ArgumentCaptor.forClass(TextRepoFile.class);
    verify(FILE_INDEXER).indexFile(argument.capture());
    assertThat(argument.getValue().getContent()).isEqualTo(content.getBytes());
  }

  @Test
  public void testGetFileBySha224_returnsFileContents_whenFileExists() throws IOException {
    when(FILE_STORAGE.getBySha224(eq(sha224))).thenReturn(textRepoFile);

    var response = resource.client().target("/files/" + sha224).request().get();
    var inputStream = response.readEntity(InputStream.class);
    var actualContent = IOUtils.toString(inputStream, UTF_8);
    assertThat(actualContent).isEqualTo(content);
  }

  @Test
  public void testGetFileBySha224_returns400BadRequest_whenIllegalSha224() {
    var response = resource.client().target("/files/55d4c44f5bc05762d8807f75f3").request().get();
    assertThat(response.getStatus()).isEqualTo(400);

    var actualErrorMessage = responsePart(response, "$.message");
    assertThat(actualErrorMessage).contains("not a sha224");
    assertThat(actualErrorMessage).contains("55d4c44f5bc05762d8807f75f3");
  }

  @Test
  public void testGetFileBySha224_returns404NotFound_whenNoSuchSha224Exists() {
    when(FILE_STORAGE.getBySha224(any())).thenThrow(new NotFoundException("File not found"));

    var response = resource.client().target("/files/" + sha224).request().get();
    assertThat(response.getStatus()).isEqualTo(404);

    String actualErrorMessage = responsePart(response, "$.message");
    assertThat(actualErrorMessage).contains("not found");
  }

}
