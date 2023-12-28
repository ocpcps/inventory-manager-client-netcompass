package com.osstelecom.db.inventory.manager.client.netcompass.services;

import com.osstelecom.db.inventory.manager.client.netcompass.client.NetcompassBasicClient;
import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;
import com.osstelecom.db.inventory.manager.http.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.http.exception.LocalQueueException;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.model.ResourceSchemaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ResourceService extends BasicService {

    private final Logger logger = LoggerFactory.getLogger(ResourceService.class);

    private final SchemaService schemaService;

    public ResourceService(NetcompassAPIClient netcompassAPIClient, SchemaService schemaService) {
        super(netcompassAPIClient);
        this.schemaService = schemaService;
    }

    public ResourceService(NetcompassAPIClient netcompassAPIClient, NetcompassAPIClient backupNetcompassAPIClient, SchemaService schemaService) {
        super(netcompassAPIClient, backupNetcompassAPIClient);
        this.schemaService = schemaService;
    }

    public void backupResources(String domain) {
        NetcompassBasicClient client = new NetcompassBasicClient("inventory/v1/" + domain + "/filter/", netcompassAPIClient,
                List.of("nodes"), "doc.nodeAddress like '%%'");

        while (client.hasNext()) {
            client.next();
            List<ManagedResource> resources = client.getResources();
            if (resources != null && !resources.isEmpty()) {
                for (ManagedResource resource : resources) {
                    try {
                        if (resource.getStructureId() != null && resource.getStructureId().isEmpty()) {
                            // Ajusta o structureId caso venha vazio
                            resource.setStructureId(null);
                        }
                        netcompassAPIClient.addManagedResource(resource);

                        ResourceSchemaModel schema = resource.getSchemaModel();
                        if (schema != null) {
                            schemaService.backupSchema(schema);
                        }
                    } catch (LocalQueueException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            } else {
                System.out.println("No resources found!");
            }
        }
    }

    public void renameLocalDomain(Domain newDomain) {
        renameResourcesWithoutStructure(newDomain);
        renameResourcesWithStructure(newDomain);
    }

    private void renameResourcesWithoutStructure(Domain newDomain) {
        try {
            logger.info("Renaming resources without structure...");
            PreparedStatement pst = this.backupNetcompassAPIClient.getLocalQueueManager()
                    .getDbConnection()
                    .prepareStatement("SELECT * FROM RESOURCES WHERE STRUCTURE_ID IS NULL");
            ResultSet rs = pst.executeQuery();
            Integer count = 0;

            while (rs.next()) {
                InputStream in = rs.getBinaryStream("RESOURCE");
                ObjectInputStream ois = new ObjectInputStream(in);
                ManagedResource resource = (ManagedResource) ois.readObject();
                ManagedResource newResource = createNewResource(resource, newDomain);

                this.netcompassAPIClient.addManagedResource(newResource);
                schemaService.renameDomain(resource.getSchemaModel());

                count = count + 1;
                this.logger.info("Flushed Resources Structure:{}", count);
            }
            rs.close();
            pst.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void renameResourcesWithStructure(Domain newDomain) {
        try {
            logger.info("Renaming resources with structure...");
            PreparedStatement pst = this.backupNetcompassAPIClient.getLocalQueueManager()
                    .getDbConnection()
                    .prepareStatement("SELECT * FROM RESOURCES WHERE STRUCTURE_ID IS NOT NULL");
            ResultSet rs = pst.executeQuery();
            Integer count = 0;

            while (rs.next()) {
                InputStream in = rs.getBinaryStream("RESOURCE");
                ObjectInputStream ois = new ObjectInputStream(in);
                ManagedResource resource = (ManagedResource) ois.readObject();
                ManagedResource newResource = createNewResource(resource, newDomain);

                // Busca structure ID, resourceID é só String
                ManagedResource oldStructureResource = this.backupNetcompassAPIClient.getManagedResource(resource.getStructureId());
                if (oldStructureResource != null) {
                    ManagedResource newStructureResource = this.netcompassAPIClient.getManagedResource(oldStructureResource.getNodeAddress(), newDomain.getDomainName(), oldStructureResource.getClassName(), oldStructureResource.getAttributeSchemaName());
                    newResource.setStructureId(newStructureResource.getKey());
                } else {
                    logger.warn("Invalid ResourceKey [" + resource.getStructureId() + "] reference in the StructureID. Couldn't set reference for " + newResource.getKey());
                }

                this.netcompassAPIClient.addManagedResource(newResource);
                schemaService.renameDomain(newResource.getSchemaModel());
                count = count + 1;
                this.logger.info("Flushed Resources Structure:{}", count);
            }
            rs.close();
            pst.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ManagedResource createNewResource(ManagedResource resource, Domain domain) throws SQLException, InvalidRequestException, IOException {
        ManagedResource newResource = this.netcompassAPIClient.getManagedResource(resource.getNodeAddress(), domain.getDomainName(), resource.getClassName(), resource.getAttributeSchemaName());
        newResource.setIsConsumer(resource.getIsConsumer());
        //TODO mudar o consumer metric para o novo ID criado
//                newResource.setConsumerMetric();
        newResource.setDescription(resource.getDescription());
        newResource.setName(resource.getName());
        newResource.setCategory(resource.getCategory());
        newResource.setAdminStatus(resource.getAdminStatus());
        newResource.setBusinessStatus(resource.getBusinessStatus());
        newResource.setOperationalStatus(resource.getOperationalStatus());
        newResource.setAttributes(resource.getAttributes());
        newResource.setResourceType(resource.getResourceType());
        return newResource;
    }
}
