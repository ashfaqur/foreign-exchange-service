package com.crewmeister.cmcodingchallenge.config;

import com.crewmeister.cmcodingchallenge.sync.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupSyncConfig {

    private static final Logger LOG = LoggerFactory.getLogger(StartupSyncConfig.class);

    @Bean
    ApplicationRunner startupSyncRunner(SyncService syncService) {
        return args -> {
            LOG.info("Running startup request to Bank to sync db ");
            try {
                syncService.syncLastDays();
                LOG.debug("Startup sync finished.");
            } catch (Exception e) {
                LOG.error("Startup sync failed", e);
            }
        };
    }
}
