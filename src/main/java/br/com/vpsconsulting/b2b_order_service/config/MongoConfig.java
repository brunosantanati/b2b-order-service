package br.com.vpsconsulting.b2b_order_service.config;

import br.com.vpsconsulting.b2b_order_service.domain.Order;
import br.com.vpsconsulting.b2b_order_service.domain.Partner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory() {
        return new SimpleMongoClientDatabaseFactory(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTransactionManager(mongoDatabaseFactory);
    }

    /**
     * Garante a criação automática dos índices anotados com @Indexed e @CompoundIndex
     * imediatamente após a inicialização completa do contexto do Spring.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void initMongoIndexes(ContextRefreshedEvent event) {
        MongoTemplate mongoTemplate = event.getApplicationContext().getBean(MongoTemplate.class);
        MongoMappingContext mappingContext = event.getApplicationContext().getBean(MongoMappingContext.class);

        IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

        resolver.resolveIndexFor(Partner.class)
                .forEach(indexDefinition -> mongoTemplate.indexOps(Partner.class).createIndex(indexDefinition));

        resolver.resolveIndexFor(Order.class)
                .forEach(indexDefinition -> mongoTemplate.indexOps(Order.class).createIndex(indexDefinition));
    }
}