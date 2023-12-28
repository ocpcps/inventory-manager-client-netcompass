package com.osstelecom.db.inventory.manager.client.netcompass.services;

import com.osstelecom.db.inventory.manager.client.netcompass.client.NetcompassBasicClient;
import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;
import com.osstelecom.db.inventory.manager.resources.model.IconModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class IconsService extends BasicService {

    private final Logger logger = LoggerFactory.getLogger(IconsService.class);

    public IconsService(NetcompassAPIClient netcompassAPIClient) {
        super(netcompassAPIClient);
    }

    public IconsService(NetcompassAPIClient netcompassAPIClient, NetcompassAPIClient backupNetcompassAPIClient) {
        super(netcompassAPIClient, backupNetcompassAPIClient);
    }

    public void backupIcons(String domain) {
        NetcompassBasicClient client = new NetcompassBasicClient("inventory/v1/" + domain + "/icon/filter/",
                netcompassAPIClient,
                List.of("icons"),
                "");

        while(client.hasNext()) {
            client.next();
            List<IconModel> icons = client.getIcons();
            if (icons != null && !icons.isEmpty()) {
                for (IconModel icon : icons) {
                    System.out.println("Icon Schema Name: " + icon.getSchemaName());
                    /*
                    try {
                        // adicionar o Icons no banco de dados
                    }  catch (LocalQueueException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    */
                }
            } else {
                System.out.println("No connection found!");
            }
        }
    }

    public void renameLocalDomain(NetcompassAPIClient backupNetcompassAPIClient, String newDomain) throws SQLException {
        // TODO
    }
}
