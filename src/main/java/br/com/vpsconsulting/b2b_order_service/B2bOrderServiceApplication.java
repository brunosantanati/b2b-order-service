package br.com.vpsconsulting.b2b_order_service;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootApplication
public class B2bOrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(B2bOrderServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner testMongoConnection(MongoTemplate mongoTemplate) {
		return args -> {
			try {
				// Cria um documento de teste para inicializar o banco orders_db fisicamente
				Document testDoc = new Document("init", true);
				mongoTemplate.save(testDoc, "ping_test");

				System.out.println("\n=================================================");
				System.out.println(" SUCCESS: Conexão com o MongoDB estabelecida!");
				System.out.println(" Banco de Dados Ativo: " + mongoTemplate.getDb().getName());
				System.out.println("=================================================\n");
			} catch (Exception e) {
				System.err.println("\n=================================================");
				System.err.println(" ERROR: Falha ao conectar ao MongoDB!");
				System.err.println(" Mensagem: " + e.getMessage());
				System.err.println("=================================================\n");
			}
		};
	}
}