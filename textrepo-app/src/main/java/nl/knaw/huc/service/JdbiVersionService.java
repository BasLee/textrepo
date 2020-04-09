package nl.knaw.huc.service;

import nl.knaw.huc.core.Contents;
import nl.knaw.huc.core.Page;
import nl.knaw.huc.core.PageParams;
import nl.knaw.huc.core.TextrepoFile;
import nl.knaw.huc.core.Version;
import nl.knaw.huc.db.ContentsDao;
import nl.knaw.huc.db.FilesDao;
import nl.knaw.huc.db.VersionsDao;
import nl.knaw.huc.service.index.MappedIndexer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static java.lang.String.format;
import static nl.knaw.huc.service.PsqlExceptionService.Constraint.VERSIONS_CONTENTS_SHA;
import static nl.knaw.huc.service.PsqlExceptionService.violatesConstraint;

public class JdbiVersionService implements VersionService {

  private final Jdbi jdbi;
  private final ContentsService contentsService;
  private List<MappedIndexer> indexers;
  private Supplier<UUID> uuidGenerator;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public JdbiVersionService(
      Jdbi jdbi,
      ContentsService contentsService,
      List<MappedIndexer> indexers,
      Supplier<UUID> uuidGenerator
  ) {
    this.jdbi = jdbi;
    this.contentsService = contentsService;
    this.indexers = indexers;
    this.uuidGenerator = uuidGenerator;
  }

  @Override
  public Optional<Version> findLatestVersion(@Nonnull UUID fileId) {
    return versions().findLatestByFileId(fileId);
  }

  @Override
  public Version createNewVersion(
      @Nonnull UUID fileId,
      @Nonnull Contents contents
  ) {
    var file = files()
        .find(fileId)
        .orElseThrow(() -> new NotFoundException(format("Could not create new version: file %s not found", fileId)));
    return createNewVersion(file, contents);
  }

  private Version createNewVersion(
      @Nonnull TextrepoFile file,
      @Nonnull Contents contents
  ) {
    contentsService.addContents(contents);
    var latestVersionContents = contents.asUtf8String();
    indexers.forEach(indexer -> indexer.index(file, latestVersionContents));

    var id = uuidGenerator.get();
    var newVersion = new Version(id, file.getId(), contents.getSha224());
    return versions().insert(newVersion);
  }

  @Override
  public Page<Version> getAll(UUID fileId, PageParams pageParams, LocalDateTime createdAfter) {
    var items = versions().findByFileId(fileId, pageParams, createdAfter);
    var total = versions().countByFileId(fileId, createdAfter);
    return new Page<>(items, total, pageParams);
  }

  @Override
  public Version get(UUID id) {
    return versions()
        .find(id)
        .orElseThrow(() -> new NotFoundException(format("Version %s could not be found", id)));
  }

  @Override
  public void delete(UUID id) {
    var version = versions().find(id);
    versions().delete(id);
    version.ifPresent(v -> tryDeletingContents(id, v.getContentsSha()));
  }

  /**
   * Deletes contents when its sha is not linked to any version
   */
  private void tryDeletingContents(@Nonnull UUID id, String sha) {
    try {
      contents().delete(sha);
      logger.info("Deleted contents of version {} by sha {}", id, sha);
    } catch (JdbiException ex) {
      if (violatesConstraint(ex, VERSIONS_CONTENTS_SHA)) {
        logger.info("Not deleting contents of version {} because sha {} is still in use", id, sha);
      } else {
        throw ex;
      }
    }
  }

  private FilesDao files() {
    return jdbi.onDemand(FilesDao.class);
  }

  private VersionsDao versions() {
    return jdbi.onDemand(VersionsDao.class);
  }

  private ContentsDao contents() {
    return jdbi.onDemand(ContentsDao.class);
  }

}
