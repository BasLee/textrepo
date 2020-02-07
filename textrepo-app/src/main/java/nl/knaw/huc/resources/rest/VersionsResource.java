package nl.knaw.huc.resources.rest;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import nl.knaw.huc.api.FormVersion;
import nl.knaw.huc.api.ResultVersion;
import nl.knaw.huc.service.VersionService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static nl.knaw.huc.core.Contents.fromContent;
import static nl.knaw.huc.resources.ResourceUtils.readContent;

@Api(tags = {"versions"})
@Path("/rest/versions")
public class VersionsResource {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final String PUT_ERROR_MSG = "Not allowed to update a version: create a new version using POST";

  private final VersionService versionService;
  private int maxPayloadSize;

  public VersionsResource(VersionService versionService, int maxPayloadSize) {
    this.versionService = versionService;
    this.maxPayloadSize = maxPayloadSize;
  }

  @POST
  @Timed
  @Consumes(MULTIPART_FORM_DATA)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Create version")
  @ApiResponses(value = {@ApiResponse(code = 200, response = ResultVersion.class, message = "OK")})
  public Response post(
      @Valid @FormDataParam("uuid") UUID fileId,
      @FormDataParam("contents") InputStream inputStream,
      @FormDataParam("contents") FormDataContentDisposition fileDetail
  ) {
    var filename = fileDetail.getFileName();
    logger.debug("post version: fileId={}, filename={}", fileId, filename);
    var contents = fromContent(readContent(inputStream, maxPayloadSize));
    var version = versionService.createNewVersion(fileId, contents, now());
    return Response.ok(new ResultVersion(version)).build();
  }

  @GET
  @Path("/{id}")
  @Timed
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieve version")
  @ApiResponses(value = {@ApiResponse(code = 200, response = ResultVersion.class, message = "OK")})
  public Response get(
      @PathParam("id") @NotNull @Valid UUID id
  ) {
    logger.debug("get version: id={}", id);
    var version = versionService.get(id);
    return Response.ok(new ResultVersion(version)).build();
  }

  @PUT
  @Path("/{id}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = PUT_ERROR_MSG)
  @ApiResponses(value = {@ApiResponse(code = 405, message = PUT_ERROR_MSG)})
  public Response put(
      @PathParam("id") @Valid UUID id,
      @Valid FormVersion form
  ) {
    throw new NotAllowedException(PUT_ERROR_MSG);
  }

  @DELETE
  @Path("/{id}")
  @ApiOperation(value = "Delete version")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK")})
  public Response delete(
      @PathParam("id") @Valid UUID id
  ) {
    logger.debug("delete version: id={}", id);
    versionService.delete(id);
    return Response.ok().build();
  }

}
