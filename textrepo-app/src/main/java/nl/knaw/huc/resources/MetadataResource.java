package nl.knaw.huc.resources;

import com.codahale.metrics.annotation.Timed;
import nl.knaw.huc.service.DocumentService;
import nl.knaw.huc.service.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/documents/{uuid}/metadata")
public class MetadataResource {
  private final Logger logger = LoggerFactory.getLogger(MetadataResource.class);

  private final MetadataService metadataService;

  public MetadataResource(MetadataService metadataService) {
    this.metadataService = metadataService;
  }

  @POST
  @Timed
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response addMetadata(
      @PathParam("uuid") @Valid UUID documentId,
      List<DocumentService.KeyValue> metadata
  ) {
    logger.debug("addMetadata: uuid={}, metadata={}", documentId, metadata);
    metadataService.addMetadata(documentId, metadata);
    return Response.ok().build();
  }

  @GET
  @Timed
  @Produces(APPLICATION_JSON)
  public Response getMetadata(@PathParam("uuid") @Valid UUID documentId) {
    return Response.ok().entity(metadataService.getMetadata(documentId)).build();
  }

}
