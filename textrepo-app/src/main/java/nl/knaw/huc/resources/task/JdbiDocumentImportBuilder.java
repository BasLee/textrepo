package nl.knaw.huc.resources.task;

import nl.knaw.huc.core.Contents;
import nl.knaw.huc.core.Version;
import nl.knaw.huc.db.TypeDao;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.NotFoundException;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static nl.knaw.huc.resources.ResourceUtils.readContent;

class JdbiDocumentImportBuilder implements TaskBuilder {
  private final Jdbi jdbi;
  private final Supplier<UUID> documentIdGenerator;
  private final Supplier<UUID> fileIdGenerator;

  private String externalId;
  private String typeName;
  private String filename;
  private InputStream inputStream;

  public JdbiDocumentImportBuilder(Jdbi jdbi, Supplier<UUID> idGenerator) {
    this.jdbi = jdbi;
    this.documentIdGenerator = idGenerator;
    this.fileIdGenerator = idGenerator;
  }

  @Override
  public TaskBuilder forExternalId(String externalId) {
    this.externalId = externalId;
    return this;
  }

  @Override
  public TaskBuilder withType(String typeName) {
    this.typeName = typeName;
    return this;
  }

  @Override
  public TaskBuilder forFilename(String name) {
    this.filename = name;
    return this;
  }

  @Override
  public TaskBuilder withContents(InputStream inputStream) {
    this.inputStream = inputStream;
    return this;
  }

  @Override
  public Task build() {
    final var typeId = getTypeId();
    final var contents = getContents();

    return new JdbiImportDocumentTask(externalId, typeId, filename, contents);
  }

  private short getTypeId() {
    return types().find(typeName).orElseThrow(typeNotFound(typeName));
  }

  private Contents getContents() {
    return Contents.fromContent(readContent(inputStream));
  }

  private TypeDao types() {
    return jdbi.onDemand(TypeDao.class);
  }

  private Supplier<NotFoundException> typeNotFound(String name) {
    return () -> new NotFoundException(String.format("No type found with name: %s", name));
  }

  private class JdbiImportDocumentTask implements Task {
    private final String externalId;
    private final Function<String, Version> task;

    public JdbiImportDocumentTask(String externalId, short typeId, String filename, Contents contents) {
      this.externalId = externalId;
      this.task = new GetOrCreateDocument(jdbi, documentIdGenerator)
          .andThen(new GetOrCreateFile(jdbi, fileIdGenerator, typeId))
          .andThen(new UpdateFilename(jdbi, filename))
          .andThen(new GetOrCreateVersion(jdbi, contents));
    }

    @Override
    public void run() {
      task.apply(externalId);
    }
  }
}
