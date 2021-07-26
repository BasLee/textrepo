package nl.knaw.huc.service.index;

import nl.knaw.huc.core.TextRepoFile;
import nl.knaw.huc.service.index.config.IndexerConfiguration;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Indexer {

  /**
   * @return String result message
   */
  Optional<String> index(
      @Nonnull TextRepoFile file,
      @Nonnull String latestVersionContents
  );

  /**
   * @return String result message
   */
  Optional<String> delete(
      @Nonnull UUID fileId
  );

  IndexerConfiguration getConfig();

  /**
   * List of mimetypes supported by indexer
   * When not present, indexer is assumed to support al mimetypes
   */
  Optional<List<String>> getMimetypes();
}
