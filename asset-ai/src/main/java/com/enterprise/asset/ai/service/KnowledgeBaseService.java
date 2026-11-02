package com.enterprise.asset.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class KnowledgeBaseService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final VectorStore vectorStore;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${rag.knowledge.directory:knowledge/}")
    private String knowledgeDirectory;

    @Value("${rag.chunk.max-size:800}")
    private int chunkMaxSize;

    @Value("${rag.chunk.overlap-size:200}")
    private int chunkOverlapSize;

    @Value("${rag.knowledge.default-tenant:asset}")
    private String defaultTenant;

    @Value("${spring.ai.vectorstore.redis.prefix:asset:rag:vector:}")
    private String vectorPrefix;

    private static final String PROCESSED_FILES_KEY = "ai:rag:processed_files";
    private static final String PROCESSED_DOCS_KEY = "ai:rag:processed_docs";
    private static final String INDEX_VERSION_KEY = "ai:rag:index_version";
    private static final String INDEX_VERSION = "1";

    public KnowledgeBaseService(VectorStore vectorStore, RedisTemplate<String, Object> redisTemplate) {
        this.vectorStore = vectorStore;
        this.redisTemplate = redisTemplate;
    }

    @Bean
    public ApplicationRunner knowledgeBaseLoader() {
        return args -> {
            logger.info("Starting knowledge base loading...");
            long startTime = System.currentTimeMillis();

            try {
                loadKnowledgeBase();
                logger.info("Knowledge base loading completed in {}ms", System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                logger.error("Knowledge base loading failed", e);
            }
        };
    }

    public void loadKnowledgeBase() throws IOException {
        migrateIndexIfNeeded();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:" + knowledgeDirectory + "**/*.md");

        if (resources.length == 0) {
            logger.info("No markdown files found in classpath:{}", knowledgeDirectory);
            return;
        }

        for (Resource resource : resources) {
            String fileKey = resolveFileKey(resource);
            try {
                loadFile(resource, fileKey);
            } catch (Exception e) {
                logger.error("Failed to process knowledge file: {}", fileKey, e);
            }
        }
    }

    private void migrateIndexIfNeeded() {
        try {
            Object currentVersion = redisTemplate.opsForValue().get(INDEX_VERSION_KEY);
            if (INDEX_VERSION.equals(String.valueOf(currentVersion))) {
                return;
            }
            logger.info("Knowledge index version changed, rebuilding vector index...");
            deleteAllVectors();
            redisTemplate.delete(PROCESSED_FILES_KEY);
            redisTemplate.delete(PROCESSED_DOCS_KEY);
            redisTemplate.opsForValue().set(INDEX_VERSION_KEY, INDEX_VERSION);
        } catch (Exception e) {
            logger.warn("Knowledge index migration skipped: {}", e.getMessage());
        }
    }

    private void deleteAllVectors() {
        List<String> keys = new ArrayList<>();
        // SCAN 代替 KEYS，避免 key 数量大时阻塞 Redis
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(vectorPrefix + "*")
                .count(1000)
                .build())) {
            cursor.forEachRemaining(keys::add);
        }

        if (keys.isEmpty()) {
            return;
        }

        List<String> ids = keys.stream()
                .map(key -> key.substring(vectorPrefix.length()))
                .collect(Collectors.toList());
        vectorStore.delete(ids);
        logger.info("Deleted {} legacy vector entries", ids.size());
    }

    private void loadFile(Resource resource, String fileKey) throws IOException {
        String previousHash = getProcessedHash(fileKey);
        String contentHash = computeContentHash(resource);
        if (contentHash.equals(previousHash)) {
            logger.info("Skipping unchanged file: {}", fileKey);
            return;
        }

        logger.info("Processing file: {} (hash={})", fileKey, contentHash);

        List<String> previousDocIds = getProcessedDocIds(fileKey);
        List<Document> documents = processFile(resource, fileKey);

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
            logger.info("Added {} document chunks to vector store for {}", documents.size(), fileKey);
        }

        boolean deleted = deleteDocuments(previousDocIds, fileKey);
        if (previousDocIds.isEmpty() && previousHash != null) {
            logger.warn("No document id record found for updated file {}, old chunks may remain. "
                    + "Recreate the Redis index if duplicates appear.", fileKey);
        }

        if (deleted) {
            storeProcessedDocIds(fileKey, documents);
            markFileAsProcessed(fileKey, contentHash);
        } else {
            logger.warn("Deletion failed for {}, keeping previous ids for retry on next startup", fileKey);
        }
    }

    private List<Document> processFile(Resource resource, String fileKey) throws IOException {
        TextReader textReader = new TextReader(resource);
        List<Document> documents = textReader.get();

        if (documents.isEmpty()) {
            return new ArrayList<>();
        }

        Document originalDoc = documents.get(0);
        TextSplitter splitter = new TokenTextSplitter(chunkMaxSize, chunkOverlapSize, 5, 10000, true);
        List<Document> chunks = splitter.split(List.of(originalDoc));

        List<Document> enrichedChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            metadata.put("tenant", defaultTenant);
            metadata.put("source", fileKey);
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());

            enrichedChunks.add(new Document(
                    buildDocumentId(fileKey, i, chunk.getContent()),
                    chunk.getContent(),
                    metadata));
        }

        logger.info("Split file {} into {} chunks", fileKey, enrichedChunks.size());
        return enrichedChunks;
    }

    private boolean deleteDocuments(List<String> docIds, String fileKey) {
        if (docIds.isEmpty()) {
            return true;
        }
        try {
            var result = vectorStore.delete(docIds);
            boolean succeeded = result.isEmpty() || Boolean.TRUE.equals(result.get());
            if (succeeded) {
                logger.info("Deleted {} stale chunks for {}", docIds.size(), fileKey);
                return true;
            }
            logger.warn("Vector store reported failure deleting {} stale chunks for {}", docIds.size(), fileKey);
            return false;
        } catch (Exception e) {
            logger.warn("Failed to delete stale chunks for {}: {}", fileKey, e.getMessage());
            return false;
        }
    }

    private String computeContentHash(Resource resource) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    private String resolveFileKey(Resource resource) {
        try {
            String path = resource.getURL().getPath().replace('\\', '/');
            int index = path.lastIndexOf(knowledgeDirectory);
            if (index >= 0) {
                return path.substring(index + knowledgeDirectory.length());
            }
        } catch (IOException e) {
            logger.warn("Cannot resolve resource path for {}, fallback to filename", resource.getDescription());
        }
        String fileName = resource.getFilename();
        return fileName != null ? fileName : resource.getDescription();
    }

    private String buildDocumentId(String fileKey, int chunkIndex, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(fileKey.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '#');
            digest.update(Integer.toString(chunkIndex).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '#');
            digest.update(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String getProcessedHash(String fileKey) {
        try {
            Object value = redisTemplate.opsForHash().get(PROCESSED_FILES_KEY, fileKey);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            logger.warn("Error reading processed hash for {}: {}", fileKey, e.getMessage());
            return null;
        }
    }

    private void markFileAsProcessed(String fileKey, String contentHash) {
        try {
            redisTemplate.opsForHash().put(PROCESSED_FILES_KEY, fileKey, contentHash);
            logger.debug("Marked file as processed: {}", fileKey);
        } catch (Exception e) {
            logger.warn("Error marking file as processed: {}", e.getMessage());
        }
    }

    private List<String> getProcessedDocIds(String fileKey) {
        try {
            Object value = redisTemplate.opsForHash().get(PROCESSED_DOCS_KEY, fileKey);
            if (value == null) {
                return new ArrayList<>();
            }
            return Arrays.stream(value.toString().split(","))
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Error reading processed document ids for {}: {}", fileKey, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void storeProcessedDocIds(String fileKey, List<Document> documents) {
        try {
            List<String> ids = documents.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());
            redisTemplate.opsForHash().put(PROCESSED_DOCS_KEY, fileKey, String.join(",", ids));
        } catch (Exception e) {
            logger.warn("Error storing processed document ids for {}: {}", fileKey, e.getMessage());
        }
    }

    public void clearProcessedFiles() {
        redisTemplate.delete(PROCESSED_FILES_KEY);
        redisTemplate.delete(PROCESSED_DOCS_KEY);
        logger.info("Cleared all processed file records");
    }

    public int getProcessedFileCount() {
        return redisTemplate.opsForHash().size(PROCESSED_FILES_KEY).intValue();
    }
}
