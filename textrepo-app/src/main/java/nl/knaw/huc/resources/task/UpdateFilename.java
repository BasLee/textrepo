package nl.knaw.huc.resources.task;

import nl.knaw.huc.api.MetadataEntry;
import nl.knaw.huc.core.TextrepoFile;
import nl.knaw.huc.db.MetadataDao;
import org.jdbi.v3.core.Jdbi;

import java.util.function.Function;

class UpdateFilename implements Function<TextrepoFile, TextrepoFile> {
  private final Jdbi jdbi;
  private final String filename;

  UpdateFilename(Jdbi jdbi, String filename) {
    this.jdbi = jdbi;
    this.filename = filename;
  }

  @Override
  public TextrepoFile apply(TextrepoFile file) {
    metadata().updateFileMetadata(file.getId(), new MetadataEntry("filename", filename));
    return file;
  }

  private MetadataDao metadata() {
    return jdbi.onDemand(MetadataDao.class);
  }
}
