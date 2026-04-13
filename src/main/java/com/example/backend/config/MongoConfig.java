package com.example.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

// Se activa solo cuando spring.data.mongodb.uri está definido en el .env.
// Para habilitar: agregar MONGODB_URI al .env y quitar las exclusiones de
// MongoAutoConfiguration en application.yaml.
@Configuration
@ConditionalOnProperty(name = "spring.data.mongodb.uri")
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.example.backend.repositories.mongo")
public class MongoConfig {
}
