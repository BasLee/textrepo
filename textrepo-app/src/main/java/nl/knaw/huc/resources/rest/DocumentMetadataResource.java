package nl.knaw.huc.resources.rest;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import nl.knaw.huc.api.MetadataEntry;
import nl.knaw.huc.api.ResultDocumentMetadataEntry;
import nl.knaw.huc.exceptions.MethodNotAllowedException;
import nl.knaw.huc.service.document.metadata.DocumentMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Api(tags = {"documents", "metadata"})
@Path("/rest/documents/{docId}/metadata")
public class DocumentMetadataResource {

  private static final Logger log = LoggerFactory.getLogger(DocumentMetadataResource.class);
  private static final String POST_ERROR_MSG = "Not allowed to post metadata: use put instead";

  private final DocumentMetadataService documentMetadataService;

  public DocumentMetadataResource(
      DocumentMetadataService documentMetadataService
  ) {
    this.documentMetadataService = documentMetadataService;
  }

  @POST
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = POST_ERROR_MSG)
  @ApiResponses(value = {@ApiResponse(code = 405, message = POST_ERROR_MSG)})
  public Response post() {
    throw new MethodNotAllowedException(POST_ERROR_MSG);
  }

  @GET
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieve document metadata")
  public Map<String, String> get(
      @PathParam("docId") @NotNull @Valid UUID docId
  ) {
    log.debug("Get document metadata: docId={}", docId);
    var metadata = documentMetadataService.getByDocId(docId);
    log.debug("Got document metadata: {}", metadata);
    return metadata;
  }

  @PUT
  @Path("/{key}")
  @Timed
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Create or update document metadata entry")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
  public Response update(
      @PathParam("docId") @NotNull @Valid UUID docId,
      @PathParam("key") @NotBlank String key,
      String value
  ) {
    log.debug("Update metadata: docId={}, key={}, value={}", docId, key, value);
    var entry = new MetadataEntry(key, value);
    documentMetadataService.upsert(docId, entry);
    log.debug("Updated metadata: docId={}, entry={}", docId, entry);
    return Response.ok(new ResultDocumentMetadataEntry(docId, entry)).build();
  }

  @DELETE
  @Path("/{key}")
  @Timed
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Delete document metadata entry")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
  public Response delete(
      @PathParam("docId") @NotNull @Valid UUID docId,
      @PathParam("key") @NotBlank String key
  ) {
    log.debug("Delete metadata: docId={}, key={}", docId, key);
    documentMetadataService.delete(docId, key);
    log.debug("Deleted metadata");
    return Response.ok().build();
  }

}
