package com.osstelecom.db.inventory.manager.client.netcompass.services;

import com.osstelecom.db.inventory.manager.client.netcompass.client.NetcompassBasicClient;
import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;
import com.osstelecom.db.inventory.manager.http.exception.LocalQueueException;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ConnectionsService extends BasicService {

    private final Logger logger = LoggerFactory.getLogger(ConnectionsService.class);

    private final SchemaService schemaService;

    public ConnectionsService(NetcompassAPIClient netcompassAPIClient, SchemaService schemaService) {
        super(netcompassAPIClient);
        this.schemaService = schemaService;
    }

    public ConnectionsService(NetcompassAPIClient netcompassAPIClient, NetcompassAPIClient backupNetcompassAPIClient, SchemaService schemaService) {
        super(netcompassAPIClient, backupNetcompassAPIClient);
        this.schemaService = schemaService;
    }

    public void backupConnections(String domain) {
        NetcompassBasicClient client = new NetcompassBasicClient("inventory/v1/" + domain + "/filter/", netcompassAPIClient,
                List.of("connections"), "doc.nodeAddress like '%%'");

        while(client.hasNext()) {
            client.next();
            List<ResourceConnection> connections = client.getConnections();
            if (connections != null && !connections.isEmpty()) {
                for (ResourceConnection connection : connections) {
                    System.out.println("Connection Name: " + connection.getNodeAddress());
                    try {
                        netcompassAPIClient.addResourceConnection(connection);
                        schemaService.backupSchema(connection.getSchemaModel());
                    } catch (LocalQueueException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            } else {
                System.out.println("No connection found!");
            }
        }
    }

    public void renameLocalDomain(Domain newDomain) throws SQLException {
        // TODO Rename o DOMAIN de dentro do ManagedResource resource = (ManagedResource)ois.readObject();
        try {
            logger.info("Renaming resources...");

            PreparedStatement pst = backupNetcompassAPIClient.getLocalQueueManager()
                    .getDbConnection()
                    .prepareStatement("SELECT * FROM CONNECTIONS");
            ResultSet rs = pst.executeQuery();
            Integer count = 0;

            while(rs.next()) {
                InputStream in = rs.getBinaryStream("RESOURCE");
                ObjectInputStream ois = new ObjectInputStream(in);

                ResourceConnection connection = (ResourceConnection) ois.readObject();

                // Find NEW references
                ManagedResource fromResource = this.netcompassAPIClient.getManagedResource(connection.getFrom().getNodeAddress(), newDomain.getDomainName(),
                        connection.getFrom().getClassName(), connection.getFrom().getAttributeSchemaName());
                ManagedResource toResource = this.netcompassAPIClient.getManagedResource(connection.getTo().getNodeAddress(), newDomain.getDomainName(),
                        connection.getTo().getClassName(), connection.getTo().getAttributeSchemaName());
                if (fromResource == null || toResource == null) {
                    logger.warn("Invalid connection resource key. Could not find reference: FROM [" + connection.getFrom().getKey() + "] TO [" + connection.getTo().getKey() + "]" );
                    continue;
                }

                ResourceConnection newConnection = this.netcompassAPIClient.getResourceConnection(fromResource, toResource,
                        newDomain.getDomainName(), connection.getClassName(), connection.getAttributeSchemaName());

                newConnection.setIsConsumer(connection.getIsConsumer());
                //TODO mudar o consumer metric para o novo ID criado
//                newResource.setConsumerMetric();
                newConnection.setDescription(connection.getDescription());
                newConnection.setName(connection.getName());
                newConnection.setCategory(connection.getCategory());
                newConnection.setAdminStatus(connection.getAdminStatus());
                newConnection.setBusinessStatus(connection.getBusinessStatus());
                newConnection.setOperationalStatus(connection.getOperationalStatus());
                newConnection.setAttributes(connection.getAttributes());
                newConnection.setResourceType(connection.getResourceType());
                //TODO mudar o StructureID para o novo ID criado
//                newResource.setStructureId(resource.getStructureId());

                this.netcompassAPIClient.addResourceConnection(newConnection);
                schemaService.renameDomain(connection.getSchemaModel());
                count = count + 1;
                this.logger.info("Flushed Resources Structure:{}", count);
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
