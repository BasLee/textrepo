package nl.knaw.huc.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import nl.knaw.huc.api.MetadataEntry;
import nl.knaw.huc.api.TextRepoFile;
import nl.knaw.huc.api.Version;

import javax.annotation.Nonnull;
import javax.ws.rs.NotFoundException;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class DocumentService {
  private final FileService fileService;
  private final MetadataService metadataService;
  private final VersionService versionService;
  private final Supplier<UUID> documentIdGenerator;

  public DocumentService(FileService fileService,
                         MetadataService metadataService,
                         VersionService versionService,
                         Supplier<UUID> documentIdGenerator) {
    this.fileService = fileService;
    this.metadataService = metadataService;
    this.versionService = versionService;
    this.documentIdGenerator = documentIdGenerator;
  }

  public Version addDocument(@Nonnull TextRepoFile file) {
    return versionService.insertNewVersion(documentIdGenerator.get(), file);
  }

  public Version getLatestVersion(@Nonnull UUID documentId) {
    return versionService
        .findLatestVersion(documentId)
        .orElseThrow(() -> new NotFoundException(format("No such document: %s", documentId)));
  }

  public TextRepoFile getLatestFile(UUID documentId) {
    var version = getLatestVersion(documentId);
    return fileService.getBySha224(version.getFileSha());
  }

  public Version replaceDocumentFile(@Nonnull UUID documentId, @Nonnull TextRepoFile file) {
    final var currentSha224 = file.getSha224();
    return versionService
        .findLatestVersion(documentId)
        .filter(v -> v.getFileSha().equals(currentSha224)) // already the current file for this document
        .orElseGet(() -> versionService.insertNewVersion(documentId, file));
  }

  public static class KeyValue {
    @JsonProperty
    public final String key;
    @JsonProperty
    public final String value;

    @ConstructorProperties({"key", "value"})
    public KeyValue(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }
}
