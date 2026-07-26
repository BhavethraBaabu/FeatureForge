package com.featureforge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Day 1 scope: wire up the MongoDB connection and repository scanning only.
 * Document models (flag definitions, evaluation events, audit trail) are
 * added Day 2+ once the relational schema (users/orgs/projects) is settled.
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.featureforge.repository.mongo")
@EnableMongoAuditing
public class MongoConfig {
}
