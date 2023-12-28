package com.osstelecom.db.inventory.manager.client.netcompass.services;

import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;

public abstract class BasicService {

    protected final NetcompassAPIClient netcompassAPIClient;

    protected final NetcompassAPIClient backupNetcompassAPIClient;

    public BasicService(NetcompassAPIClient netcompassAPIClient) {
        this.netcompassAPIClient = netcompassAPIClient;
        this.backupNetcompassAPIClient = null;
    }

    public BasicService(NetcompassAPIClient netcompassAPIClient, NetcompassAPIClient backupNetcompassAPIClient) {
        this.netcompassAPIClient = netcompassAPIClient;
        this.backupNetcompassAPIClient = backupNetcompassAPIClient;
    }
}