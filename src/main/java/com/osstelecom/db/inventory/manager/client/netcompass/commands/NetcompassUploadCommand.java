package com.osstelecom.db.inventory.manager.client.netcompass.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.osstelecom.db.inventory.gson.converters.InstantConverter;
import com.osstelecom.db.inventory.manager.client.netcompass.configuration.NetcompassConfigurationManager;
import com.osstelecom.db.inventory.manager.client.netcompass.services.*;
import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;
import com.osstelecom.db.inventory.manager.http.client.configuration.NetcompassClientConfiguration;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

@Component
@CommandLine.Command(name = "upload", mixinStandardHelpOptions = true)
public class NetcompassUploadCommand  implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(NetcompassUploadCommand.class);

    @CommandLine.Option(names = {"-p", "--profile"})
    private String profile;

    @CommandLine.Option(names = {"-d", "--domain"}, required = true)
    private String domain;

    @CommandLine.Option(names = {"-pb", "--profile-backup"})
    private String profileBackup;

    @CommandLine.Option(names = {"-db", "--domain-backup"}, required = true)
    private String backupName;

    @CommandLine.Option(names = {"-r", "--resource"})
    private String resource;

    private NetcompassClientConfiguration netcompassClientConfiguration;

    private NetcompassAPIClient netcompassApiClient;

    private NetcompassClientConfiguration backupNetcompassClientConfiguration;

    private NetcompassAPIClient backupNetcompassAPIClient;

    @Override
    public void run() {
        if (backupName == null) {
            logger.info("[" + domain + "] BackupName not provided... Trying domain name");
            backupName = domain;
        }
        try {
            initNetcompassClient();

            if (!backupName.isEmpty()) {
                try {
                    logger.info("[" + domain + "] Renaming all resources from backup to new domain");
                    logger.info(profileBackup);
                    this.backupNetcompassClientConfiguration = new NetcompassConfigurationManager().loadConfiguration(profileBackup);
                    this.backupNetcompassAPIClient = new NetcompassAPIClient(backupNetcompassClientConfiguration);
                    this.backupNetcompassAPIClient.initLoadSession(backupName);
                    renameResourcesDomain();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            logger.info("[" + domain + "] Initializing upload from backup " + backupName );
//            this.upload();
            this.netcompassApiClient.upload();

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
        this.netcompassApiClient.initLoadSession(sessionName);
    }

    private void renameResourcesDomain() throws Exception {
        Optional<Domain> optDomain = this.netcompassApiClient.getDomains().stream().filter(d -> d.getDomainName().equals(domain)).findFirst();
        if (optDomain.isEmpty())
            throw new Exception("Domain não existe");
        Domain newDomain = optDomain.get();

        SchemaService schemaService = new SchemaService(netcompassApiClient, backupNetcompassAPIClient);

        if (resource == null || resource.equals("resources")) {
            ResourceService resourceService = new ResourceService(netcompassApiClient, backupNetcompassAPIClient, schemaService);
            resourceService.renameLocalDomain(newDomain);
        }

        if (resource == null || resource.equals("connections")) {
            ConnectionsService connectionsService = new ConnectionsService(netcompassApiClient, backupNetcompassAPIClient, schemaService);
            connectionsService.renameLocalDomain(newDomain);
        }

        if (resource == null || resource.equals("circuits")) {
            CircuitsService circuitsService = new CircuitsService(netcompassApiClient, backupNetcompassAPIClient, schemaService);
            circuitsService.renameLocalDomain(newDomain);
        }

        if (resource == null || resource.equals("services")) {
            ServicesService servicesService = new ServicesService(netcompassApiClient, backupNetcompassAPIClient,schemaService);
            servicesService.renameLocalDomain(newDomain);
        }

        if (resource == null || resource.equals("icons")) {
//        IconsService iconsService = new IconsService(netcompassApiClient, backupNetcompassAPIClient);
//        iconsService.renameLocalDomain(newDomain);
        }

        if (resource == null || resource.equals("metrics")) {
//        MetricsService metricsService = new MetricsService(netcompassApiClient, backupNetcompassAPIClient);
//        metricsService.renameLocalDomain(newDomain);
        }
    }

    private void upload() {
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Instant.class, new InstantConverter())
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").setPrettyPrinting().create();

            PreparedStatement pst = this.netcompassApiClient.getLocalQueueManager().getDbConnection().prepareStatement("SELECT * FROM RESOURCES");
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                InputStream in = rs.getBinaryStream("RESOURCE");
                ObjectInputStream ois = new ObjectInputStream(in);
                ManagedResource resource = (ManagedResource) ois.readObject();
                System.out.println(gson.toJson(resource));
            }
        } catch (SQLException | IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}