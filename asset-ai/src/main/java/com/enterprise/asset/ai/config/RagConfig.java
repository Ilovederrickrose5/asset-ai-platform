package com.enterprise.asset.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.ai.vectorstore.RedisVectorStore.RedisVectorStoreConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RagConfig {

    private static final Logger logger = LoggerFactory.getLogger(RagConfig.class);

    @Value("${spring.ai.vectorstore.redis.index:asset_knowledge_index}")
    private String indexName;

    @Value("${spring.ai.vectorstore.redis.prefix:rag:vector:}")
    private String prefix;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Bean
    @ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
    public JedisPooled jedisPooled() {
        // 与 spring.data.redis.* 保持一致，密码/database 必须透传，否则向量库会连错实例
        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .password(StringUtils.hasText(redisPassword) ? redisPassword : null)
                .database(redisDatabase)
                .build();
        logger.info("Creating JedisPooled connection to {}:{}, database={}",
                redisHost, redisPort, redisDatabase);
        return new JedisPooled(new HostAndPort(redisHost, redisPort), clientConfig);
    }

    @Bean
    @ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
    public VectorStore vectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
        logger.info("Creating RedisVectorStore, index={}, prefix={}", indexName, prefix);

        RedisVectorStoreConfig config = RedisVectorStoreConfig.builder()
                .withIndexName(indexName)
                .withPrefix(prefix)
                .build();

        return new RedisVectorStore(config, embeddingModel, jedisPooled, true);
    }
}
