package nl.knaw.huc.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import nl.knaw.huc.api.ResultVersion;
import nl.knaw.huc.service.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Api(tags = {"files", "versions"})
@Path("/files/{uuid}/versions")
public class VersionsResource {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private VersionService versionService;

  public VersionsResource(VersionService versionService) {
    this.versionService = versionService;
  }

  @GET
  @Timed
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Get all versions of a file")
  @ApiResponses(value = {
    @ApiResponse(code = 200, responseContainer = "Map", response = ResultVersion.class, message = "OK")})
  public Response getVersions(
      @PathParam("uuid") @Valid UUID fileId
  ) {
    logger.debug("getVersions: fileId={}", fileId);
    var results = versionService
        .getVersions(fileId)
        .stream()
        .map(ResultVersion::new);
    return Response.ok().entity(results).build();
  }

}
