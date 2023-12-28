package com.osstelecom.db.inventory.manager.client.netcompass.commands;

import com.osstelecom.db.inventory.manager.client.netcompass.configuration.NetcompassConfigurationManager;
import com.osstelecom.db.inventory.manager.client.netcompass.services.*;
import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;
import com.osstelecom.db.inventory.manager.http.client.configuration.NetcompassClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.FileNotFoundException;
import java.sql.SQLException;

@Component
@CommandLine.Command(name = "backup", mixinStandardHelpOptions = true)
public class NetcompassBackupCommand implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(NetcompassUploadCommand.class);

    @CommandLine.Option(names = {"-d", "--domain"}, required = true)
    private String domain;

    @CommandLine.Option(names = {"-p", "--profile"})
    private String profile;

//    @CommandLine.Option(names = {"-f", "--filter"}, required = true)
//    private String filter;

//    @CommandLine.Option(names = {"-bn", "--backup-name"}, required = true)
//    private String backupName;

    @CommandLine.Option(names = {"-t", "--threads"})
    private Integer threads = 4;

    @CommandLine.Option(names = {"-r", "--resource"})
    private String resource;

    private NetcompassClientConfiguration netcompassClientConfiguration;

    private NetcompassAPIClient netcompassApiClient;

    @Override
    public void run() {
        logger.info("[" + domain + "] Initializing backup... Using " + threads + " threads.");
        try {
            logger.info(" ****  initNetcompassClient()  ***** ");
            initNetcompassClient();

            SchemaService schemaService = new SchemaService(netcompassApiClient);

            if (resource == null || resource.equals("resources")) {
                ResourceService resourceService = new ResourceService(netcompassApiClient, schemaService);
                resourceService.backupResources(domain);
            }

            if (resource == null || resource.equals("connections")) {
                ConnectionsService connectionsService = new ConnectionsService(netcompassApiClient, schemaService);
                connectionsService.backupConnections(domain);
            }

            if (resource == null || resource.equals("circuits")) {
                CircuitsService circuitsService = new CircuitsService(netcompassApiClient, schemaService);
                circuitsService.backupCircuits(domain);
            }

            if (resource == null || resource.equals("services")) {
                ServicesService servicesService = new ServicesService(netcompassApiClient, schemaService);
                servicesService.backupServices(domain);
            }

            if (resource == null || resource.equals("icons")) {
                IconsService iconsService = new IconsService(netcompassApiClient);
                iconsService.backupIcons(domain);
            }

            if (resource == null || resource.equals("metrics")) {
                MetricsService metricsService = new MetricsService(netcompassApiClient);
                metricsService.backupMetrics(domain);
            }

        } catch (SQLException | FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void initNetcompassClient() throws SQLException, FileNotFoundException {
//        logger.debug("Trying to Create Netcompass Client");
        this.netcompassClientConfiguration = new NetcompassConfigurationManager().loadConfiguration(profile);
        this.netcompassApiClient = new NetcompassAPIClient(this.netcompassClientConfiguration);

        if (profile == null) {
            profile = "";
        } else {
            profile = profile + "/";
        }
        String sessionName = profile + this.domain;
        this.netcompassApiClient.initLoadSession(sessionName, this.threads);
    }

}