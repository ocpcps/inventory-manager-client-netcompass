package com.osstelecom.db.inventory.manager.client.netcompass.services;

import com.osstelecom.db.inventory.manager.client.netcompass.client.NetcompassBasicClient;
import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;
import com.osstelecom.db.inventory.manager.http.exception.LocalQueueException;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SchemaService extends BasicService {

    private final Logger logger = LoggerFactory.getLogger(ResourceService.class);

    private List<String> createdSchemasList = new ArrayList<>();

    public SchemaService(NetcompassAPIClient netcompassAPIClient) {
        super(netcompassAPIClient);
    }

    public SchemaService(NetcompassAPIClient netcompassAPIClient, NetcompassAPIClient backupNetcompassAPIClient) {
        super(netcompassAPIClient, backupNetcompassAPIClient);
    }

    public void backupSchema(ResourceSchemaModel resourceSchemaModel) {
        try{
            if (createdSchemasList.contains(resourceSchemaModel.getSchemaName())) {
                // Check if schema was already processed
                logger.debug(resourceSchemaModel.getSchemaName() + " was already processed. Skipping...");
                return;
            }
            if (resourceSchemaModel.getSchemaName().equals("default")) {
                logger.debug(resourceSchemaModel.getSchemaName() + " schema. Skipping...");
                return;
            }

            logger.debug("Adding " + resourceSchemaModel.getSchemaName());
            this.netcompassAPIClient.addResourceSchemaModel(resourceSchemaModel);
            createdSchemasList.add(resourceSchemaModel.getSchemaName());

            // Check fromSchema on cache map
            if (!createdSchemasList.contains(resourceSchemaModel.getFromSchema())) {
                if (resourceSchemaModel.getFromSchema().equals("default")) {
                    logger.debug(resourceSchemaModel.getFromSchema() + " schema. Skipping...");
                    return;
                }
                NetcompassBasicClient client = new NetcompassBasicClient("inventory/v1/schema/" + resourceSchemaModel.getFromSchema(), netcompassAPIClient);
                ResourceSchemaModel fromSchema = client.getSchema().getPayLoad();
                if (fromSchema != null) {
                    // Add client recursively
                    backupSchema(fromSchema);
                } else {
                    logger.warn(resourceSchemaModel.getFromSchema() + " was not found!!!!");
                }
            }
        } catch (LocalQueueException e) {
            throw new RuntimeException(e);
        }
    }

    public void renameDomain(ResourceSchemaModel resourceSchemaModel) {
        try{
            if (createdSchemasList.contains(resourceSchemaModel.getSchemaName())) {
                // Check if schema was already processed
                return;
            }
            if (resourceSchemaModel.getSchemaName().equals("default")) {
                logger.debug(resourceSchemaModel.getSchemaName() + " schema. Skipping...");
                return;
            }

            this.netcompassAPIClient.addResourceSchemaModel(resourceSchemaModel);
            createdSchemasList.add(resourceSchemaModel.getSchemaName());

            // Check fromSchema on cache map
            if (!createdSchemasList.contains(resourceSchemaModel.getFromSchema())) {
                if (resourceSchemaModel.getFromSchema().equals("default")) {
                    logger.debug(resourceSchemaModel.getFromSchema() + " schema. Skipping...");
                    return;
                }
                // Get fromSchema from old backup domain
                ResourceSchemaModel fromSchema = this.backupNetcompassAPIClient.getResourceSchemaModel(resourceSchemaModel.getFromSchema());
                if (fromSchema != null) {
                    // Add to cache and client recursively
                    renameDomain(fromSchema);
                }
            }
        } catch (LocalQueueException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}