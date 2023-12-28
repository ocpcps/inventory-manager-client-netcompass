package com.osstelecom.db.inventory.manager.client.netcompass.services;

import com.osstelecom.db.inventory.manager.client.netcompass.client.NetcompassBasicClient;
import com.osstelecom.db.inventory.manager.dto.CircuitPathDTO;
import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;
import com.osstelecom.db.inventory.manager.http.exception.InvalidRequestException;
import com.osstelecom.db.inventory.manager.http.exception.LocalQueueException;
import com.osstelecom.db.inventory.manager.resources.CircuitResource;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ManagedResource;
import com.osstelecom.db.inventory.manager.resources.ResourceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CircuitsService extends BasicService {

    private final Logger logger = LoggerFactory.getLogger(CircuitsService.class);

    private final SchemaService schemaService;

    public CircuitsService(NetcompassAPIClient netcompassAPIClient, SchemaService schemaService) {
        super(netcompassAPIClient);
        this.schemaService = schemaService;
    }

    public CircuitsService(NetcompassAPIClient netcompassAPIClient, NetcompassAPIClient backupNetcompassAPIClient, SchemaService schemaService) {
        super(netcompassAPIClient, backupNetcompassAPIClient);
        this.schemaService = schemaService;
    }

    public void backupCircuits(String domain) {
        NetcompassBasicClient client = new NetcompassBasicClient(
                "inventory/v1/" + domain + "/circuit/filter/",
                netcompassAPIClient,
                List.of("circuits"),
                "doc.nodeAddress like '%%'");

        while(client.hasNext()) {
            client.next();
            List<CircuitResource> circuits = client.getCircuits();

            if (circuits != null && !circuits.isEmpty()) {
                for (CircuitResource circuit : circuits) {
                    System.out.println("Circuit Name: " + circuit.getName());
                    try {
                        netcompassAPIClient.addCircuitResource(circuit);
                        schemaService.backupSchema(circuit.getSchemaModel());
                        logger.info("Circuit added to the local database: " + circuit.getName());
                        processCircuitPaths(circuit);
                    }  catch (LocalQueueException e) {
                        logger.error("Error adding circuit to the local database: " + e.getMessage(), e);
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            } else {
                System.out.println("No circuit found!");
            }
        }
    }

    private void processCircuitPaths(CircuitResource circuit) throws LocalQueueException {
        if (circuit.getCircuitPath() != null && !circuit.getCircuitPath().isEmpty()) {
            List<ResourceConnection> paths = new ArrayList<>();
            try {
                for (String circuitPath : circuit.getCircuitPath()) {
                    logger.info("Processing CircuitPath: " + circuitPath);
                    if (circuitPath != null) {
                        // Get all connections from circuitPath
                        String circuitPathKey = circuitPath.substring(circuitPath.indexOf("/") + 1);
                        ResourceConnection connection = this.netcompassAPIClient.getResourceConnection(circuitPathKey);
                        if (connection == null) {
                            logger.error("Connection not found");
                            return;
                        }
                        paths.add(connection);
                    } else {
                        logger.info("No CircuitPath found!");
                    }
                }
                CircuitPathDTO circuitPath = this.netcompassAPIClient.getCircuitPath(circuit, paths);
                this.netcompassAPIClient.addCircuitPath(circuitPath);
            } catch (SQLException | InvalidRequestException | IOException e) {
                e.printStackTrace();
                logger.error("Error while getting circuit connection");
            }
        }
    }

    public void renameLocalDomain(Domain newDomain) throws SQLException {
        // TODO Rename o DOMAIN de dentro do ManagedResource resource = (ManagedResource)ois.readObject();
        try {
            logger.info("Renaming resources...");
            PreparedStatement pst = backupNetcompassAPIClient.getLocalQueueManager()
                    .getDbConnection()
                    .prepareStatement("SELECT * FROM CIRCUITS");
            ResultSet rs = pst.executeQuery();
            Integer count = 0;

            while(rs.next()) {
                InputStream in = rs.getBinaryStream("RESOURCE");
                ObjectInputStream ois = new ObjectInputStream(in);
                CircuitResource circuit = (CircuitResource) ois.readObject();

                // Find NEW references
                ManagedResource aPoint = this.netcompassAPIClient.getManagedResource(circuit.getaPoint().getNodeAddress(), newDomain.getDomainName(),
                        circuit.getaPoint().getClassName(), circuit.getaPoint().getAttributeSchemaName());
                ManagedResource zPoint = this.netcompassAPIClient.getManagedResource(circuit.getzPoint().getNodeAddress(), newDomain.getDomainName(),
                        circuit.getzPoint().getClassName(), circuit.getzPoint().getAttributeSchemaName());
                if (aPoint == null || zPoint == null) {
                    logger.warn("Invalid circuit resource key. Could not find reference: FROM [" + circuit.getaPoint().getKey() + "] TO [" + circuit.getzPoint().getKey()+ "]" );
                    continue;
                }

                CircuitResource newCircuit = this.netcompassAPIClient.getCircuitResource(circuit.getNodeAddress(), circuit.getName(),
                        newDomain.getDomainName(), aPoint, zPoint, circuit.getClassName(), circuit.getAttributeSchemaName());

                newCircuit.setIsConsumer(circuit.getIsConsumer());
                //TODO mudar o consumer metric para o novo ID criado
//                newResource.setConsumerMetric();
                newCircuit.setDescription(circuit.getDescription());
                newCircuit.setName(circuit.getName());
                newCircuit.setCategory(circuit.getCategory());
                newCircuit.setAdminStatus(circuit.getAdminStatus());
                newCircuit.setBusinessStatus(circuit.getBusinessStatus());
                newCircuit.setOperationalStatus(circuit.getOperationalStatus());
                newCircuit.setAttributes(circuit.getAttributes());
                newCircuit.setResourceType(circuit.getResourceType());

                this.netcompassAPIClient.addCircuitResource(newCircuit);
                renameCircuitPaths(circuit, newCircuit);

                schemaService.renameDomain(circuit.getSchemaModel());
                count = count + 1;
                this.logger.info("Flushed Resources Structure:{}", count);
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void renameCircuitPaths(CircuitResource oldCircuit, CircuitResource circuit) throws SQLException, InvalidRequestException, IOException, LocalQueueException {
        List<String> oldPaths = oldCircuit.getCircuitPath();
        List<ResourceConnection> paths = new ArrayList<>();
        for (String path : oldPaths) {
            String circuitPathKey = path.substring(path.indexOf("/") + 1);

            ResourceConnection oldConnection = this.netcompassAPIClient.getResourceConnection(circuitPathKey);
            if (oldConnection == null) {
                logger.error("Connection not found");
                return;
            }
            String domain = circuit.getDomainName();

            // Find NEW connection references
            ManagedResource fromResource = this.netcompassAPIClient.getManagedResource(oldConnection.getFrom().getNodeAddress(), domain,
                    oldConnection.getFrom().getClassName(), oldConnection.getFrom().getAttributeSchemaName());
            ManagedResource toResource = this.netcompassAPIClient.getManagedResource(oldConnection.getTo().getNodeAddress(), domain,
                    oldConnection.getTo().getClassName(), oldConnection.getTo().getAttributeSchemaName());
            if (fromResource == null || toResource == null) {
                logger.warn("Invalid connection resource key. Could not find reference: FROM [" + oldConnection.getFrom().getKey() + "] TO [" + oldConnection.getTo().getKey() + "]" );
                continue;
            }

            ResourceConnection newConnection = this.netcompassAPIClient.getResourceConnection(fromResource, toResource,
                    domain, oldConnection.getClassName(), oldConnection.getAttributeSchemaName());

            paths.add(newConnection);
        }

        CircuitPathDTO circuitPath = this.netcompassAPIClient.getCircuitPath(circuit, paths);
        this.netcompassAPIClient.addCircuitPath(circuitPath);
    }
}
