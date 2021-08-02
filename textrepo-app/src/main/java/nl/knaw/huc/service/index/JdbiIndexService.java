package nl.knaw.huc.service.index;

import nl.knaw.huc.core.TextRepoFile;
import nl.knaw.huc.db.ContentsDao;
import nl.knaw.huc.db.FilesDao;
import nl.knaw.huc.db.VersionsDao;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.elasticsearch.client.RequestOptions.DEFAULT;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * Handle index mutations of all configured indices
 */
public class JdbiIndexService implements IndexService {

  private final List<Indexer> indexers;
  private final List<TextRepoElasticClient> indexClients;
  private final Jdbi jdbi;
  private static final Logger log = LoggerFactory.getLogger(IndexerWithMapping.class);

  public JdbiIndexService(
      List<Indexer> indexers,
      List<TextRepoElasticClient> indexClients,
      Jdbi jdbi
  ) {
    this.indexers = indexers;
    this.indexClients = indexClients;
    this.jdbi = jdbi;
  }

  @Override
  public void index(@Nonnull UUID fileId) {
    var found = jdbi.onDemand(FilesDao.class).find(fileId);
    found.ifPresentOrElse(
        (file) -> {
          var latestContents = getLatestVersionContents(file);
          indexers.forEach(indexer -> indexer.index(file, latestContents));
        },
        () -> {
          throw new NotFoundException(format("Could not find file by id %s", fileId));
        });
  }

  @Override
  public void index(@Nonnull TextRepoFile file) {
    var latestContents = getLatestVersionContents(file);
    indexers.forEach(indexer -> indexer.index(file, latestContents));
  }

  @Override
  public void index(@Nonnull TextRepoFile file, String contents) {
    indexers.forEach(i -> i.index(file, contents));
  }

  @Override
  public void delete(UUID fileId) {
    indexClients.forEach(indexClient -> deleteFromIndex(fileId, indexClient));
  }

  @Override
  public List<UUID> getAllIds() {
    var result = new HashSet<UUID>();
    indexClients.forEach(indexClient -> result.addAll(getAllIdsFromIndex(indexClient)));
    return result.stream().toList();
  }

  /**
   * Retrieve all doc IDs from index using ES scroll api
   */
  private List<UUID> getAllIdsFromIndex(TextRepoElasticClient indexClient) {
    var indexName = indexClient.getConfig().index;
    try {
      final var scroll = new Scroll(TimeValue.timeValueMinutes(1L));
      var searchRequest = new SearchRequest(indexName);
      searchRequest.scroll(scroll);
      var searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.query(matchAllQuery());
      searchRequest.source(searchSourceBuilder);

      var client = indexClient.getClient();
      var searchResponse = client.search(searchRequest, DEFAULT);
      var scrollId = searchResponse.getScrollId();
      var result = getIds(searchResponse);
      var hasHits = result.size() > 0;

      while (hasHits) {
        var scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(scroll);
        searchResponse = client.scroll(scrollRequest, DEFAULT);
        scrollId = searchResponse.getScrollId();
        var newHits = getIds(searchResponse);
        hasHits = newHits.size() > 0;
        result.addAll(newHits);
      }

      log.debug("Found {} files in index {}", result.size(), indexName);
      var clearScrollRequest = new ClearScrollRequest();
      clearScrollRequest.addScrollId(scrollId);
      client.clearScroll(clearScrollRequest, DEFAULT);
      return result;
    } catch (IOException ex) {
      throw new WebApplicationException(format("Could not retrieve IDs from index %s", indexName), ex);
    }
  }

  private List<UUID> getIds(SearchResponse searchResponse) {
    return Arrays
        .stream(searchResponse.getHits().getHits())
        .map(hit -> UUID.fromString(hit.getId()))
        .collect(toList());
  }

  private String getLatestVersionContents(TextRepoFile file) {
    var latestVersion = jdbi
        .onDemand(VersionsDao.class)
        .findLatestByFileId(file.getId());
    String latestContents;
    if (latestVersion.isEmpty()) {
      latestContents = "";
    } else {
      latestContents = jdbi
          .onDemand(ContentsDao.class)
          .findBySha224(latestVersion.get().getContentsSha())
          .orElseThrow(() -> new IllegalStateException(""))
          .asUtf8String();
    }
    return latestContents;
  }

  private void deleteFromIndex(@Nonnull UUID fileId, TextRepoElasticClient indexClient) {
    var indexName = indexClient.getConfig().index;
    log.info(format("Deleting file %s from index %s", fileId, indexName));
    DeleteResponse response;
    var deleteRequest = new DeleteRequest();
    deleteRequest.index(indexName);
    deleteRequest.id(fileId.toString());
    try {
      response = indexClient.getClient().delete(deleteRequest, DEFAULT);
    } catch (Exception ex) {
      throw new WebApplicationException(format("Could not delete file %s in index %s", fileId, indexName), ex);
    }
    var status = response.status().getStatus();
    final String msg;
    if (status == 200) {
      msg = format("Successfully deleted file %s from index %s", fileId, indexName);
    } else if (status == 404) {
      msg = format("File %s not found in index %s", fileId, indexName);
    } else {
      throw new WebApplicationException(format("Could not delete file %s from index %s", fileId, indexName));
    }
    log.info(msg);
  }

}
