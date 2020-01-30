package nl.knaw.huc.service;

import nl.knaw.huc.api.MetadataEntry;
import nl.knaw.huc.core.Document;
import nl.knaw.huc.core.TextrepoFile;
import nl.knaw.huc.db.DocumentFilesDao;
import nl.knaw.huc.db.DocumentsDao;
import nl.knaw.huc.db.MetadataDao;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;
import org.postgresql.util.PSQLException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static java.lang.String.format;

public class JdbiDocumentService implements DocumentService {
  private final Jdbi jdbi;

  public JdbiDocumentService(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Document get(UUID id) {
    return documents().get(id);
  }

  private DocumentsDao documents() {
    return jdbi.onDemand(DocumentsDao.class);
  }
}
