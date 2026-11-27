package com.example.bioplatform.research.preparation;

import com.example.bioplatform.research.collection.common.dto.SourcePublicationRecord;
import com.example.bioplatform.research.collection.common.service.ResearchPublicationSource;
import com.example.bioplatform.research.collection.common.service.SourceMembership;
import com.example.bioplatform.research.collection.openlb.mapper.OpenLbRoutingMapper;
import com.example.bioplatform.research.integration.service.impl.ResearchPublicationSourceRegistry;
import com.example.bioplatform.research.preparation.mapper.PreparationMapper;
import com.example.bioplatform.research.preparation.model.PreparationRequest;
import com.example.bioplatform.research.preparation.service.PreparationService;
import com.example.bioplatform.research.preparation.service.PreparedMetadataParser;
import com.example.bioplatform.research.statistics.dto.ResearchSnapshotBuildRequest;
import com.example.bioplatform.research.statistics.mapper.ResearchSnapshotMapper;
import com.example.bioplatform.research.statistics.prepared.mapper.PreparedStatisticsMapper;
import com.example.bioplatform.research.statistics.prepared.service.PreparedSnapshotService;
import com.example.bioplatform.research.statistics.vo.ResearchSnapshotDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Real MySQL, isolated disposable schema only. Never reads or modifies business publication rows.
 */
class PreparedPipelineMySqlTest {
    @Test
    void countsDrilldownVersionsUpdatesAndResumableRollback() throws Exception {
        String url = System.getenv("PREPARED_TEST_MYSQL_URL");
        assumeTrue(url != null, "Opt-in isolated MySQL test");
        String user = System.getenv("DB_USERNAME"), password = System.getenv("DB_PASSWORD");
        String schema = "codex_prepared_test_" + UUID.randomUUID().toString().replace("-", "");
        assertTrue(schema.matches("codex_prepared_test_[a-f0-9]{32}"));
        Path root = Path.of("").toAbsolutePath();
        if (!Files.exists(root.resolve("database/task2-preparation-20261126.sql"))) root = root.getParent();
        String ddl = Files.readString(root.resolve("database/task2-preparation-20261126.sql"));
        String query = url.contains("?") ? url.substring(url.indexOf('?')) : "";
        String testUrl = url.replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + schema + query);
        try (var admin = DriverManager.getConnection(url, user, password); var adminSql = admin.createStatement()) {
            adminSql.execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4");
            PreparationService preparation = null;
            PreparedSnapshotService snapshots = null;
            com.zaxxer.hikari.HikariDataSource ds = null;
            try {
                ds = new com.zaxxer.hikari.HikariDataSource();
                ds.setJdbcUrl(testUrl);
                ds.setUsername(user);
                ds.setPassword(password);
                ds.setMaximumPoolSize(3);
                ds.setMinimumIdle(1);
                ds.setPoolName(schema);
                ds.setConnectionTimeout(5000);
                try (var c = ds.getConnection(); var st = c.createStatement()) {
                    org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c, new org.springframework.core.io.support.EncodedResource(new org.springframework.core.io.ByteArrayResource(ddl.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "UTF-8"));
                    st.execute("CREATE TABLE research_stat_snapshot(id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,version_no BIGINT NOT NULL UNIQUE,snapshot_name VARCHAR(100),status VARCHAR(20),source_codes VARCHAR(500),source_count INT,include_related BOOL,include_resources BOOL,publication_count INT DEFAULT 0,resource_count BIGINT DEFAULT 0,year_coverage_count INT DEFAULT 0,topic_coverage_count INT DEFAULT 0,author_coverage_count INT DEFAULT 0,institution_coverage_count INT DEFAULT 0,fund_coverage_count INT DEFAULT 0,error_message VARCHAR(2000),started_time DATETIME,completed_time DATETIME)");
                    st.execute("CREATE TABLE research_stat_dimension(id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,snapshot_id BIGINT UNSIGNED NOT NULL,scope_code VARCHAR(100),dimension_type VARCHAR(30),dimension_key VARCHAR(700),dimension_hash BINARY(32) GENERATED ALWAYS AS (UNHEX(SHA2(CONCAT(scope_code,':',dimension_type,':',dimension_key),256))) STORED,display_name VARCHAR(1000),author_name VARCHAR(500),institution_name VARCHAR(1000),stat_year SMALLINT NOT NULL DEFAULT 0,publication_count INT,UNIQUE(snapshot_id,dimension_hash),FOREIGN KEY(snapshot_id) REFERENCES research_stat_snapshot(id) ON DELETE CASCADE)");
                    st.execute("CREATE TABLE research_topic_category(id BIGINT PRIMARY KEY,topic_code VARCHAR(40),topic_name VARCHAR(100),status VARCHAR(20),sort_order INT)");
                    st.execute("CREATE TABLE research_database_topic(database_code VARCHAR(40),topic_id BIGINT,verification_status VARCHAR(30))");
                    st.execute("CREATE TABLE research_fund_dictionary(grant_id VARCHAR(100),full_name VARCHAR(500),status VARCHAR(20))");
                    st.execute("INSERT INTO research_topic_category VALUES(1,'SEQUENCE','Sequence','ENABLED',1),(2,'TOOL','Tool','ENABLED',2)");
                    st.execute("INSERT INTO research_database_topic VALUES('A',1,'CONFIRMED'),('B',1,'CONFIRMED')");
                    st.execute("INSERT INTO research_fund_dictionary VALUES('G1','Full Funder','ENABLED')");
                }
                Configuration cfg = new Configuration();
                cfg.setMapUnderscoreToCamelCase(true);
                cfg.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
                for (Class<?> type : List.of(PreparationMapper.class, PreparedStatisticsMapper.class, ResearchSnapshotMapper.class))
                    cfg.addMapper(type);
                for (String name : List.of("PreparationMapper", "PreparedStatisticsMapper", "ResearchSnapshotMapper"))
                    try (var in = getClass().getResourceAsStream("/mapper/research/" + name + ".xml")) {
                        new XMLMapperBuilder(in, cfg, name, cfg.getSqlFragments()).parse();
                    }
                var session = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(cfg));
                var pm = org.mockito.Mockito.spy(session.getMapper(PreparationMapper.class));
                var sm = session.getMapper(PreparedStatisticsMapper.class);
                var legacy = session.getMapper(ResearchSnapshotMapper.class);
                var a = new Source("A");
                var b = new Source("B");
                var c = new Source("C");
                a.rows.add(row(1, "10001", true));
                a.rows.add(row(2, "10002", true));
                // A's relation points at B's native paper: ownership must stay relative to its own source.
                a.rows.add(row(3, "10003", false));
                b.rows.add(row(1, "10001", true));
                b.rows.add(row(2, "10003", true));
                c.rows.add(row(1, "10004", true));
                a.rows.get(0).setAuthorsJson("[{\"firstName\":\"Alice\",\"lastName\":\"Li\",\"institutionName\":\"University One; University Two\"},{\"fullName\":\"Bob\",\"institutionName\":\"University One\"},{\"fullName\":\"Unknown\"}]");
                a.rows.get(0).setGrantsJson("[{\"grantId\":\"G1\"},{\"agency\":\"Full Funder\"},{\"grantId\":\"missing\"}]");
                a.rows.get(0).setKeywordList("[\"Genetics\",\"Genetics\",\"Sequencing\"]");
                OpenLbRoutingMapper routes = (OpenLbRoutingMapper) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{OpenLbRoutingMapper.class}, (proxy, method, args) -> List.of());
                var registry = new ResearchPublicationSourceRegistry(List.of(a, b, c), routes, null);
                var json = new ObjectMapper();
                var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
                preparation = new PreparationService(pm, registry, legacy, routes, new PreparedMetadataParser(json), new SourceMembership(json), json, tx);
                preparation.start(new PreparationRequest(null, 1), false);
                awaitPreparation(pm, "SUCCESS");
                assertEquals(6, pm.latestJob().getRowsChanged());
                assertEquals(6, pm.latestJob().getRowsScanned());
                assertTrue((Boolean) preparation.status().get("canBuild"));
                snapshots = new PreparedSnapshotService(preparation, sm, legacy, registry, tx, json);
                var request = new ResearchSnapshotBuildRequest();
                request.setName("fixture");
                request.setIncludeRelated(false);
                request.setIncludeResources(false);
                snapshots.startBuild(request);
                var first = awaitSnapshot(snapshots);
                long firstId = first.getId();
                assertEquals(4, first.getPublicationCount());
                assertEquals(2, snapshots.databases(firstId).stream().filter(d -> d.code().equals("A")).findFirst().orElseThrow().publicationCount());
                var total = snapshots.result(firstId, 50).dimensions();
                var one = total.get("INSTITUTION").stream().filter(d -> d.getDisplayName().equals("University One")).findFirst().orElseThrow();
                assertEquals(1, one.getPublicationCount());
                assertEquals(1, snapshots.dimensionPublications(firstId, one.getId(), 1, 10).total());
                assertTrue(total.get("TOPIC").stream().anyMatch(d -> d.getDimensionKey().equals("TOOL") && d.getPublicationCount() == 0));
                assertEquals(1, total.get("FUND").size());
                assertEquals("Full Funder", total.get("FUND").get(0).getDisplayName());
                assertEquals(1, total.get("FUND").get(0).getPublicationCount());
                var author = total.get("AUTHOR").stream().filter(d -> d.getDisplayName().equals("Alice Li · University Two")).findFirst().orElseThrow();
                var paper = snapshots.dimensionPublications(firstId, author.getId(), 1, 10).items().get(0);
                assertEquals("10001", paper.getPmid());
                assertNotNull(paper.getSourceUrl());
                assertTrue(paper.getSourceRecordKeys().contains("A"));
                for (var year : total.get("YEAR"))
                    assertEquals(year.getPublicationCount().longValue(), snapshots.dimensionPublications(firstId, year.getId(), 1, 10).total());
                // Stable content must not create another full member version on every refresh.
                preparation.start(null, false);
                awaitPreparation(pm, "SUCCESS");
                assertEquals(0, pm.latestJob().getRowsChanged());
                // Replace old members on update; old snapshots still read their immutable prepared revision.
                a.rows.get(0).setAuthorsJson("[{\"fullName\":\"Replacement\",\"institutionName\":\"University Three\"}]");
                preparation.start(new PreparationRequest(List.of("A"), 1), false);
                awaitPreparation(pm, "SUCCESS");
                assertEquals(1, pm.latestJob().getRowsChanged());
                snapshots.startBuild(request);
                var second = awaitSnapshot(snapshots);
                assertEquals(4, second.getPublicationCount());
                assertTrue(snapshots.result(second.getId(), 50).dimensions().get("INSTITUTION").stream().noneMatch(d -> d.getDisplayName().equals("University Two")));
                assertEquals(1, snapshots.dimensionPublications(firstId, author.getId(), 1, 10).total());
                // A bad second batch rolls back fully; repair it and resume from the first committed cursor.
                a.rows.get(1).setAuthorsJson("[{broken");
                preparation.start(new PreparationRequest(List.of("A"), 1), false);
                awaitPreparation(pm, "FAILED");
                assertEquals(1, pm.latestJob().getAfterId());
                assertEquals(1, pm.latestJob().getRowsScanned());
                assertFalse((Boolean) preparation.status().get("canBuild"));
                var testedSnapshots = snapshots;
                assertThrows(RuntimeException.class, () -> testedSnapshots.startBuild(request));
                a.rows.get(1).setAuthorsJson("[]");
                preparation.start(null, false);
                awaitPreparation(pm, "SUCCESS");
                assertEquals(3, pm.latestJob().getRowsScanned());
                // Non-primary relations may be included explicitly without duplicating their global PMID.
                request.setIncludeRelated(true);
                snapshots.startBuild(request);
                var related = awaitSnapshot(snapshots);
                assertEquals(4, related.getPublicationCount());
                assertEquals(3, snapshots.databases(related.getId()).stream().filter(d -> d.code().equals("A")).findFirst().orElseThrow().publicationCount());
                // DOI-only vs PMID, unresolved DOI collision, and resource identifiers.
                var doiOnly = row(4, null, true);
                doiOnly.setDoi("HTTPS://doi.org/10.1/shared");
                doiOnly.setSourceRecordKey("DOI:10.1/shared");
                a.rows.add(doiOnly);
                b.rows.get(0).setDoi("10.1/shared");
                var resource = row(5, null, true);
                resource.setSourceRecordKey("RESOURCE:CRA1");
                resource.setRecordKind("RESOURCE");
                resource.setResourceAccession("CRA1");
                resource.setResourceUrl("https://example.org/CRA1");
                a.rows.add(resource);
                preparation.start(null, false);
                awaitPreparation(pm, "SUCCESS");
                request.setIncludeRelated(false);
                snapshots.startBuild(request);
                assertEquals(4, awaitSnapshot(snapshots).getPublicationCount());
                request.setIncludeResources(true);
                snapshots.startBuild(request);
                var mixed = awaitSnapshot(snapshots);
                assertEquals(5, mixed.getPublicationCount());
                assertEquals(1, mixed.getResourceCount());
                // Explicit stop preserves a committed cursor, including when a source read is already in progress.
                a.pause = true;
                preparation.start(new PreparationRequest(List.of("A"), 1), false);
                assertTrue(a.paused.await(10, java.util.concurrent.TimeUnit.SECONDS));
                preparation.stop();
                a.release.countDown();
                awaitPreparation(pm, "STOPPED");
                assertTrue(pm.latestJob().getAfterId() > 0);
                assertTrue(pm.latestJob().getAfterId() < 5);
                preparation.start(null, false);
                awaitPreparation(pm, "SUCCESS");
                assertEquals(5, pm.latestJob().getRowsScanned());
                // A conflicting DOI cannot merge two different PMIDs; the unresolved DOI-only paper stays separate.
                c.rows.get(0).setDoi("10.1/shared");
                preparation.start(null, false);
                awaitPreparation(pm, "SUCCESS");
                request.setIncludeResources(false);
                snapshots.startBuild(request);
                assertEquals(5, awaitSnapshot(snapshots).getPublicationCount());
                // Discard a failed revision after its first changed document was committed.
                a.rows.get(0).setTitle("unpublished partial change");
                a.rows.get(1).setAuthorsJson("[{broken");
                preparation.start(new PreparationRequest(List.of("A"), 1), false);
                awaitPreparation(pm, "FAILED");
                preparation.discard();
                assertEquals("CANCELLED", pm.latestJob().getStatus());
                try (var connection = ds.getConnection(); var statement = connection.createStatement(); var result = statement.executeQuery("SELECT title FROM research_prepared_document WHERE source_code='A' AND pmid='10001' AND valid_until IS NULL")) {
                    assertTrue(result.next());
                    assertEquals("Paper 10001", result.getString(1));
                }
                // Ordered incremental windows: only changed raw documents are parsed after a full baseline.
                a.rows.get(0).setTitle("Paper 10001");
                a.rows.get(1).setAuthorsJson("[]");
                var baseline = pm.successfulCursor("A").getUntilTime();
                for (var source : List.of(a, b, c)) {
                    source.delta = true;
                    for (var row : source.rows) row.setSourceUpdatedTime(baseline.minusDays(1));
                }
                var fakeClock = new java.util.concurrent.atomic.AtomicReference<>(baseline.plusDays(1));
                org.mockito.Mockito.doAnswer(invocation -> fakeClock.get()).when(pm).databaseTime();
                a.rows.get(0).setTitle("incremental A");
                a.rows.get(0).setSourceUpdatedTime(baseline.plusSeconds(10));
                b.rows.get(0).setTitle("incremental B");
                b.rows.get(0).setSourceUpdatedTime(baseline.plusSeconds(10));
                var added = row(6, "10005", true);
                added.setSourceUpdatedTime(baseline.plusSeconds(10));
                a.rows.add(added);
                preparation.incremental(new PreparationRequest(null, 1));
                awaitPreparation(pm, "SUCCESS");
                assertEquals(3, pm.latestJob().getRowsScanned());
                assertEquals(3, pm.latestJob().getRowsChanged());
                assertTrue(pm.sourceCursors(pm.latestJob().getId()).stream().allMatch(cursor -> cursor.getScanMode().equals("INCREMENTAL")));
                assertEquals(0, pm.sourceCursor(pm.latestJob().getId(), "C").getAfterId());
                try (var connection = ds.getConnection(); var statement = connection.createStatement(); var result = statement.executeQuery("SELECT COUNT(*) FROM research_prepared_document WHERE valid_until IS NULL")) {
                    result.next();
                    assertEquals(9, result.getLong(1));
                }
                // Same timestamp, smaller IDs, business-key changes, and resource retirement must all be handled.
                var stableWatermark = pm.successfulCursor("A");
                a.rows.get(1).setSourceRecordKey("DOI:10.test/changed-key");
                a.rows.get(1).setSourceUpdatedTime(baseline.plusDays(1).plusSeconds(10));
                resource.setRecordActive(false);
                resource.setSourceUpdatedTime(baseline.plusDays(1).plusSeconds(10));
                added.setAuthorsJson("[{broken");
                added.setSourceUpdatedTime(baseline.plusDays(1).plusSeconds(10));
                fakeClock.set(baseline.plusDays(2));
                preparation.incremental(new PreparationRequest(null, 1));
                awaitPreparation(pm, "FAILED");
                assertEquals(2, pm.latestJob().getRowsScanned());
                assertEquals(5, pm.latestJob().getAfterId());
                assertEquals(baseline.plusDays(1).plusSeconds(10), pm.latestJob().getAfterUpdateTime());
                assertEquals(stableWatermark.getJobId(), pm.successfulCursor("A").getJobId());
                added.setAuthorsJson("[]");
                preparation.start(null, false);
                awaitPreparation(pm, "SUCCESS");
                assertEquals(3, pm.latestJob().getRowsScanned());
                try (var connection = ds.getConnection(); var statement = connection.createStatement()) {
                    try (var result = statement.executeQuery("SELECT COUNT(*) FROM research_prepared_document WHERE source_code='A' AND source_row_id=2 AND valid_until IS NULL")) {
                        result.next();
                        assertEquals(1, result.getInt(1));
                    }
                    try (var result = statement.executeQuery("SELECT source_key FROM research_prepared_document WHERE source_code='A' AND source_row_id=2 AND valid_until IS NULL")) {
                        assertTrue(result.next());
                        assertEquals("DOI:10.test/changed-key", result.getString(1));
                    }
                    try (var result = statement.executeQuery("SELECT COUNT(*) FROM research_prepared_document WHERE source_code='A' AND record_kind='RESOURCE' AND valid_until IS NULL")) {
                        result.next();
                        assertEquals(0, result.getLong(1));
                    }
                }
                fakeClock.set(baseline.plusDays(3));
                preparation.incremental(null);
                awaitPreparation(pm, "SUCCESS");
                assertEquals(0, pm.latestJob().getRowsScanned());
                // A row exactly at the previous exclusive cutoff belongs to the next window, even for a smaller ID.
                a.rows.get(0).setTitle("boundary");
                a.rows.get(0).setSourceUpdatedTime(baseline.plusDays(3));
                fakeClock.set(baseline.plusDays(4));
                preparation.incremental(null);
                awaitPreparation(pm, "SUCCESS");
                assertEquals(1, pm.latestJob().getRowsScanned());
                var beforeCancelled = pm.successfulCursor("A").getJobId();
                a.rows.get(0).setTitle("unpublished delta");
                a.rows.get(0).setSourceUpdatedTime(baseline.plusDays(4).plusSeconds(10));
                a.rows.get(1).setAuthorsJson("[{broken");
                a.rows.get(1).setSourceUpdatedTime(baseline.plusDays(4).plusSeconds(10));
                fakeClock.set(baseline.plusDays(5));
                preparation.incremental(new PreparationRequest(null, 1));
                awaitPreparation(pm, "FAILED");
                preparation.discard();
                assertEquals(beforeCancelled, pm.successfulCursor("A").getJobId());
                try (var connection = ds.getConnection(); var statement = connection.createStatement(); var result = statement.executeQuery("SELECT title FROM research_prepared_document WHERE source_code='A' AND source_row_id=1 AND valid_until IS NULL")) {
                    assertTrue(result.next());
                    assertEquals("boundary", result.getString(1));
                }
                a.rows.get(1).setAuthorsJson("[]");
                fakeClock.set(baseline.plusDays(6));
                preparation.incremental(null);
                awaitPreparation(pm, "SUCCESS");
                assertEquals(2, pm.latestJob().getRowsScanned());
                // EXPLAIN the actual parameterized SQL, not a hand-written substitute.
                var q = new com.example.bioplatform.research.statistics.prepared.model.PreparedScope();
                q.setRevision(pm.readyRevision());
                q.setSources(List.of("A", "B", "C"));
                q.setType("AUTHOR");
                var params = Map.of("q", q, "table", PreparedMetadataParser.table("AUTHOR"));
                var bound = cfg.getMappedStatement(PreparedStatisticsMapper.class.getName() + ".rank").getBoundSql(params);
                try (var connection = ds.getConnection(); var statement = connection.prepareStatement("EXPLAIN " + bound.getSql())) {
                    var handler = new org.apache.ibatis.scripting.defaults.DefaultParameterHandler(cfg.getMappedStatement(PreparedStatisticsMapper.class.getName() + ".rank"), params, bound);
                    handler.setParameters(statement);
                    try (var explain = statement.executeQuery()) {
                        assertTrue(explain.next());
                    }
                }
            } finally {
                if (snapshots != null) snapshots.shutdown();
                if (preparation != null) preparation.shutdown();
                if (ds != null) ds.close();
                adminSql.execute("DROP DATABASE " + schema);
            }
        }
    }

    static SourcePublicationRecord row(long id, String pmid, boolean primary) {
        var r = new SourcePublicationRecord();
        r.setSourceRowId(id);
        r.setSourceRecordKey("PMID:" + pmid);
        r.setPmid(pmid);
        r.setTitle("Paper " + pmid);
        r.setPubYear(2026);
        r.setPrimaryFlag(primary);
        r.setAuthorsJson("[]");
        r.setSourceUrl("https://example.org/paper/" + id);
        return r;
    }

    static void awaitPreparation(PreparationMapper mapper, String expected) throws Exception {
        long end = System.nanoTime() + 30_000_000_000L;
        while ("RUNNING".equals(mapper.latestJob().getStatus()) && System.nanoTime() < end) Thread.sleep(20);
        assertEquals(expected, mapper.latestJob().getStatus(), mapper.latestJob().getErrorMessage());
        Thread.sleep(20);
    }

    static ResearchSnapshotDetail awaitSnapshot(PreparedSnapshotService service) throws Exception {
        long end = System.nanoTime() + 30_000_000_000L;
        while (service.status().running() && System.nanoTime() < end) Thread.sleep(20);
        var state = service.status();
        assertEquals("SUCCESS", state.current().getStatus(), state.current().getErrorMessage());
        return state.current();
    }

    static class Source implements ResearchPublicationSource {
        final String code;
        final List<SourcePublicationRecord> rows = new CopyOnWriteArrayList<>();

        Source(String code) {
            this.code = code;
        }

        volatile boolean delta;
        volatile boolean pause;
        final java.util.concurrent.CountDownLatch paused = new java.util.concurrent.CountDownLatch(1), release = new java.util.concurrent.CountDownLatch(1);

        public String sourceCode() {
            return code;
        }

        public List<SourcePublicationRecord> loadRecordsPage(boolean related, long after, int size) {
            if (pause && after > 0) {
                pause = false;
                paused.countDown();
                try {
                    if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                        throw new IllegalStateException("fixture pause timeout");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }
            return rows.stream().filter(r -> r.getSourceRowId() > after).sorted(Comparator.comparing(SourcePublicationRecord::getSourceRowId)).limit(size).toList();
        }

        public boolean supportsIncremental() {
            return delta;
        }

        public List<SourcePublicationRecord> loadChangedRecordsPage(boolean related, java.time.LocalDateTime time, long after, java.time.LocalDateTime until, int size) {
            return rows.stream().filter(r -> r.getSourceUpdatedTime().isBefore(until) && (r.getSourceUpdatedTime().isAfter(time) || r.getSourceUpdatedTime().equals(time) && r.getSourceRowId() > after)).sorted(Comparator.comparing(SourcePublicationRecord::getSourceUpdatedTime).thenComparing(SourcePublicationRecord::getSourceRowId)).limit(size).toList();
        }

        public List<SourcePublicationRecord> loadRecordsByIdentifiers(List<String> p, List<String> d) {
            return List.of();
        }

        public List<SourcePublicationRecord> loadRecordsBySourceKeys(List<String> keys) {
            return List.of();
        }

        public long countRecords(boolean related) {
            return rows.size();
        }

        public List<Integer> distinctYears(boolean related) {
            return List.of(2026);
        }
    }
}
