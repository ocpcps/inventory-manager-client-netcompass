package com.osstelecom.db.inventory.manager.client.netcompass.services;

import com.osstelecom.db.inventory.manager.client.netcompass.client.NetcompassBasicClient;
import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;
import com.osstelecom.db.inventory.manager.http.exception.LocalQueueException;
import com.osstelecom.db.inventory.manager.resources.Domain;
import com.osstelecom.db.inventory.manager.resources.ServiceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class ServicesService extends BasicService {

    private static final Logger logger = LoggerFactory.getLogger(ServicesService.class);

    private final SchemaService schemaService;

    public ServicesService(NetcompassAPIClient netcompassAPIClient, SchemaService schemaService) {
        super(netcompassAPIClient);
        this.schemaService = schemaService;

    }
    public ServicesService(NetcompassAPIClient netcompassAPIClient, NetcompassAPIClient backupNetcompassAPIClient, SchemaService schemaService) {
        super(netcompassAPIClient, backupNetcompassAPIClient);
        this.schemaService = schemaService;
    }

    public void backupServices(String domain) {
        NetcompassBasicClient client = new NetcompassBasicClient("inventory/v1/" + domain + "/service/filter/",
                netcompassAPIClient,
                List.of("services"),
                "doc.nodeAddress like '%%'");

        while(client.hasNext()) {
            client.next();
            List<ServiceResource> services = client.getServices();

            if (services != null && !services.isEmpty()) {
                for (ServiceResource service : services) {
                    System.out.println("Service Name: " + service.getName());

                    try {
                        logger.info("Sending service object for addition: {}", service.getKey());
                        netcompassAPIClient.addServiceResource(service);
                    }  catch (LocalQueueException e) {
                        logger.error("Error adding service to the local database: " + e.getMessage(), e);
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }

                }
            } else {
                System.out.println("No service found!");
            }
        }
    }

    public void renameLocalDomain(Domain newDomain) throws Exception {
        // TODO Rename o DOMAIN de dentro do ManagedResource resource = (ManagedResource)ois.readObject();
        if (this.backupNetcompassAPIClient == null) {
            throw new Exception("Invalid Netcompass Backup API Client");
        }
        try {
            logger.info("Renaming services...");
            PreparedStatement pst = this.backupNetcompassAPIClient.getLocalQueueManager()
                    .getDbConnection()
                    .prepareStatement("SELECT * FROM SERVICES");
            ResultSet rs = pst.executeQuery();
            Integer count = 0;

            while(rs.next()) {
                InputStream in = rs.getBinaryStream("RESOURCE");
                ObjectInputStream ois = new ObjectInputStream(in);
                ServiceResource service = (ServiceResource) ois.readObject();

                // Find NEW references
//                ManagedResource aPoint = this.netcompassAPIClient.getManagedResource(circuit.getaPoint().getNodeAddress(), newDomain.getDomainName(),
//                        circuit.getaPoint().getClassName(), circuit.getaPoint().getAttributeSchemaName());
//                ManagedResource zPoint = this.netcompassAPIClient.getManagedResource(circuit.getzPoint().getNodeAddress(), newDomain.getDomainName(),
//                        circuit.getzPoint().getClassName(), circuit.getzPoint().getAttributeSchemaName());
//                if (aPoint == null || zPoint == null) {
//                    logger.warn("Invalid circuit resource key. Could not find reference: FROM [" + circuit.getaPoint().getKey() + "] TO [" + circuit.getzPoint().getKey()+ "]" );
//                    continue;
//                }

//                CircuitResource newService = this.netcompassAPIClient.getServiceResource(service.getNodeAddress(), service.getres.getName(),
//                        newDomain.getDomainName(), aPoint, zPoint, circuit.getClassName(), circuit.getAttributeSchemaName());
//
//                newService.setIsConsumer(service.getIsConsumer());
//                //TODO mudar o consumer metric para o novo ID criado
////                newResource.setConsumerMetric();
//                newService.setDescription(service.getDescription());
//                newService.setName(service.getName());
//                newService.setCategory(service.getCategory());
//                newService.setAdminStatus(service.getAdminStatus());
//                newService.setBusinessStatus(service.getBusinessStatus());
//                newService.setOperationalStatus(service.getOperationalStatus());
//                newService.setAttributes(service.getAttributes());
//                newService.setResourceType(service.getResourceType());
//
//                this.netcompassAPIClient.addCircuitResource(newCircuit);
//
//                renameCircuitPaths(circuit, newCircuit);
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
