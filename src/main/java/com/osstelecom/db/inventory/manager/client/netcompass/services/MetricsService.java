package com.osstelecom.db.inventory.manager.client.netcompass.services;

import com.osstelecom.db.inventory.manager.client.netcompass.client.NetcompassBasicClient;
import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;
import com.osstelecom.db.inventory.manager.resources.ConsumableMetric;

import java.sql.SQLException;
import java.util.List;

public class MetricsService extends BasicService {

    public MetricsService(NetcompassAPIClient netcompassAPIClient) {
        super(netcompassAPIClient);
    }

    public MetricsService(NetcompassAPIClient netcompassAPIClient, NetcompassAPIClient backupNetcompassAPIClient) {
        super(netcompassAPIClient, backupNetcompassAPIClient);
    }

    public void backupMetrics(String domain) {
        NetcompassBasicClient client = new NetcompassBasicClient("inventory/v1/" + domain + "/consumableMetric/filter/",
                netcompassAPIClient,
                List.of("metrics"),
                "");

        while(client.hasNext()) {
            client.next();
            List<ConsumableMetric> metrics = client.getMetrics();
            if (metrics != null && !metrics.isEmpty()) {
                for (ConsumableMetric metric : metrics) {
                    System.out.println("Metric Name: " + metric.getMetricDescription());
                    /*
                    try {
                        // adicionar o Metrics no banco de dados
                    }  catch (LocalQueueException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    */
                }
            } else {
                System.out.println("No metric found!");
            }
        }
    }

    public void renameLocalDomain(NetcompassAPIClient backupNetcompassAPIClient, String newDomain) throws SQLException {

    }
}
