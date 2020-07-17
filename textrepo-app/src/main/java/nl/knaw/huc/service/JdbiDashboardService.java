package nl.knaw.huc.service;

import nl.knaw.huc.core.Document;
import nl.knaw.huc.core.DocumentsOverview;
import nl.knaw.huc.core.Page;
import nl.knaw.huc.core.PageParams;
import nl.knaw.huc.db.DashboardDao;
import nl.knaw.huc.db.DashboardDao.KeyCount;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JdbiDashboardService implements DashboardService {
  private static final Logger log = LoggerFactory.getLogger(JdbiDashboardService.class);
  private final Jdbi jdbi;

  public JdbiDashboardService(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public int countOrphans() {
    return dashboard().countOrphans();
  }

  @Override
  public DocumentsOverview getDocumentsOverview() {
    return dashboard().getDocumentsOverview();
  }

  @Override
  public Page<Document> findOrphans(PageParams pageParams) {
    return new Page<>(dashboard().findOrphans(pageParams), countOrphans(), pageParams);
  }

  @Override
  public List<KeyCount> documentCountsByMetadataKey() {
    return dashboard().documentCountsByMetadataKey();
  }

  @Override
  public List<DashboardDao.ValueCount> documentCountsByMetadataValue(String key) {
    return dashboard().documentCountsByMetadataValue(key);
  }

  private DashboardDao dashboard() {
    return jdbi.onDemand(DashboardDao.class);
  }
}
