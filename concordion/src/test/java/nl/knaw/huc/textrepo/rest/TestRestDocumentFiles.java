package nl.knaw.huc.textrepo.rest;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.huc.textrepo.AbstractConcordionTest;
import nl.knaw.huc.textrepo.util.RestUtils;
import org.concordion.api.extension.Extensions;
import org.concordion.api.option.ConcordionOptions;
import org.concordion.ext.EmbedExtension;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static nl.knaw.huc.textrepo.util.TestUtils.asPrettyJson;
import static nl.knaw.huc.textrepo.util.TestUtils.replaceUrlParams;

@Extensions(EmbedExtension.class)
@ConcordionOptions(declareNamespaces = {"ext", "urn:concordion-extensions:2010"})
public class TestRestDocumentFiles extends AbstractConcordionTest {

  private String docId;
  private String textFileId;
  private String fooFileId;

  public void createDocumentWithTwoFiles() {
    this.docId = RestUtils.createDocument();
    this.textFileId = RestUtils.createFile(docId, textTypeId);
    this.fooFileId = RestUtils.createFile(docId, fooTypeId);
  }

  public String getDocId() {
    return docId;
  }

  public String getTextFileId() {
    return textFileId;
  }

  public String getFooFileId() {
    return fooFileId;
  }

  public static class RetrieveResult {
    public int status;
    public String body;
    public int count;
    public String type1;
    public String type2;
  }

  public RetrieveResult retrieve(Object endpoint, Object id) {
    final var response = client
        .target(replaceUrlParams(endpoint, id))
        .request()
        .get();

    var result = new RetrieveResult();
    result.status = response.getStatus();
    var body = response.readEntity(String.class);
    result.body = asPrettyJson(body);
    var json = JsonPath.parse(body);
    result.count = json.read("$.length()");
    int typeId1 = json.read("$[0].typeId");
    int typeId2 = json.read("$[1].typeId");
    result.type1 = typeId1 == textTypeId ? "text" : format("[%d] != text type id [%d]", typeId1, textTypeId);
    result.type2 = typeId2 == fooTypeId ? "foo" : format("[%d] != foo type id [%d]", typeId2, fooTypeId);
    return result;
  }

}
