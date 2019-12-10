package nl.knaw.huc.service;

import nl.knaw.huc.db.DocumentFilesDao;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.NotFoundException;
import java.util.UUID;
import java.util.function.Supplier;

public class JdbiDocumentService implements DocumentService {
  private final Jdbi jdbi;
  private final Supplier<UUID> idGenerator;

  public JdbiDocumentService(Jdbi jdbi, Supplier<UUID> idGenerator) {
    this.jdbi = jdbi;
    this.idGenerator = idGenerator;
  }

  public UUID createDocument(UUID fileId) {
    final var docId = idGenerator.get();
    documentFiles().insert(docId, fileId);
    return docId;
  }

  public UUID findFileForType(UUID docId, String fileType) {
    return documentFiles().findFile(docId, fileType)
                          .orElseThrow(() -> new NotFoundException(
                              String.format("No %s file found for document %s", fileType, docId)));
  }

  private DocumentFilesDao documentFiles() {
    return jdbi.onDemand(DocumentFilesDao.class);
  }
}
