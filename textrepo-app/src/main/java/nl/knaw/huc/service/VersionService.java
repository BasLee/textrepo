package nl.knaw.huc.service;

import nl.knaw.huc.api.TextRepoContents;
import nl.knaw.huc.api.Version;

import javax.annotation.Nonnull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VersionService {

  Optional<Version> findLatestVersion(@Nonnull UUID fileId);

  Version insertNewVersion(@Nonnull UUID fileId, @Nonnull TextRepoContents contents, @Nonnull String filename,
                           @Nonnull LocalDateTime time);

  List<Version> getVersions(UUID fileId);
}
