package nl.knaw.huc.service;

import nl.knaw.huc.api.MetadataEntry;
import nl.knaw.huc.core.Contents;
import nl.knaw.huc.core.Version;
import nl.knaw.huc.db.FilesDao;
import org.jdbi.v3.core.Jdbi;

import javax.annotation.Nonnull;
import javax.ws.rs.NotFoundException;
import java.util.UUID;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;

public class JdbiFileContentsService implements FileContentsService {

  private final ContentsService contentsService;
  private final VersionService versionService;
  private FileMetadataService fileMetadataService;
  private Jdbi jdbi;

  public JdbiFileContentsService(
      Jdbi jdbi, ContentsService contentsService,
      VersionService versionService,
      FileMetadataService fileMetadataService
  ) {
    this.contentsService = contentsService;
    this.versionService = versionService;
    this.fileMetadataService = fileMetadataService;
    this.jdbi = jdbi;
  }

  @Override
  public Contents getLatestFileContents(UUID fileId) {

    var version = versionService
        .findLatestVersion(fileId)
        .orElseThrow(() -> new NotFoundException(format("No such file: %s", fileId)));

    return contentsService.getBySha(version.getContentsSha());
  }

  @Override
  public Version replaceFileContents(
      @Nonnull UUID fileId,
      @Nonnull Contents contents,
      @Nonnull String filename
  ) {
    final var currentSha224 = contents.getSha224();
    var version = versionService
        .findLatestVersion(fileId)
        .filter(v -> v.getContentsSha().equals(currentSha224)) // already the current file for this file
        .orElseGet(() -> versionService.createNewVersion(fileId, contents));

    fileMetadataService.upsert(fileId, new MetadataEntry("filename", filename));

    return version;
  }

  private FilesDao files() {
    return jdbi.onDemand(FilesDao.class);
  }

}
