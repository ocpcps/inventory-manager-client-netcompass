package com.osstelecom.db.inventory.manager.client.netcompass.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.osstelecom.db.inventory.gson.converters.InstantConverter;
import com.osstelecom.db.inventory.manager.dto.FilterDTO;
import com.osstelecom.db.inventory.manager.http.client.NetcompassAPIClient;
import com.osstelecom.db.inventory.manager.request.FilterRequest;
import com.osstelecom.db.inventory.manager.resources.*;
import com.osstelecom.db.inventory.manager.resources.model.IconModel;
import com.osstelecom.db.inventory.manager.response.FilterResponse;
import com.osstelecom.db.inventory.manager.response.ResourceSchemaResponse;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class NetcompassBasicClient {

    private final String url;

    private final NetcompassAPIClient netcompassAPIClient;

    private FilterDTO filterDTO;

    private Gson gson;

    private Boolean hasNext = true;

    private FilterResponse currentResponse;

    public NetcompassBasicClient(String url, NetcompassAPIClient netcompassAPIClient) {
        this.url = url;
        this.netcompassAPIClient = netcompassAPIClient;

        if (this.gson == null) {
            this.gson = new GsonBuilder()
                    .registerTypeAdapter(Instant.class, new InstantConverter())
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").setPrettyPrinting().create();
        }
    }

    public NetcompassBasicClient(String url, NetcompassAPIClient netcompassAPIClient, List<String> objects, String aqlFilter) {
        this.url = url;
        this.netcompassAPIClient = netcompassAPIClient;
        this.setFilter(objects, aqlFilter);

        if (this.gson == null) {
            this.gson = new GsonBuilder()
                    .registerTypeAdapter(Instant.class, new InstantConverter())
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").setPrettyPrinting().create();
        }
    }

    public void setFilter(List<String> objects, String aqlFilter) {
        this.filterDTO = new FilterDTO();
        this.filterDTO.setLimit(5L);
        this.filterDTO.setOffSet(0L);
        this.filterDTO.setAqlFilter(aqlFilter);
        this.filterDTO.setObjects(objects);
    }

    public FilterResponse next() {
        if (currentResponse != null) {
            System.out.println("FETCHING NEXT PAGE");
            filterDTO.setOffSet(currentResponse.getPayLoad().getOffSet() + filterDTO.getLimit());
        }
        FilterRequest req = new FilterRequest();
        req.setPayLoad(this.filterDTO);

        //  Cria uma solicitação HTTP para obter recursos do dominio especificado.
        //  Configura os parâmetros do filtro, cria um objeto 'FilterRequest' com base nele constrói a solicitação HTTP Post.
        RequestBody body = RequestBody.create(netcompassAPIClient.toJson(req), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(netcompassAPIClient.getConfiguration().getNetcompassApiUrl() + url)
                .post(body).build();

        //  Envia a solicitação HTTP e processa a resposta.
        //  A resposta é analisada usando Gson e os recursos obtidos são armazenados.
        //  Resposta no formato JSON e a converte para um objeto 'FilterResponse' (Java)
        try (Response r = netcompassAPIClient.getOauthManager().getAuthHttpClient().newCall(request).execute()) {
            if (r.code() != 200) {
                System.out.println("Error: " + r.code());
                hasNext = false;
                return null;
            }

            this.currentResponse = gson.fromJson(r.body().string(), FilterResponse.class);
            hasNext = this.currentResponse.getSize() > this.currentResponse.getPayLoad().getOffSet() + this.currentResponse.getPayLoad().getLimit();

            return this.currentResponse;
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            hasNext = false;
            return null;
        }
    }

    public Boolean hasNext() {
        return this.hasNext;
    }

    public FilterResponse getResponse() {
        if (this.currentResponse == null) {
            this.currentResponse = this.next();
        }
        return this.currentResponse;
    }

    public List<ManagedResource> getResources() {
        FilterResponse filterResponse = getResponse();
        if (filterResponse == null || filterResponse.getPayLoad() == null || filterResponse.getPayLoad().getNodes() == null) {
            System.out.println("No resources found.");
            return Collections.emptyList();
        }

        return filterResponse.getPayLoad().getNodes();
    }

    public List<ResourceConnection> getConnections() {
        FilterResponse filterResponse = getResponse();
        if (filterResponse == null || filterResponse.getPayLoad() == null || filterResponse.getPayLoad().getConnections() == null) {
            System.out.println("No connections found.");
            return Collections.emptyList();
        }

        return this.currentResponse.getPayLoad().getConnections();
    }

    public List<CircuitResource> getCircuits() {
        FilterResponse filterResponse = getResponse();
        if (filterResponse == null || filterResponse.getPayLoad() == null || filterResponse.getPayLoad().getCircuits() == null) {
            System.out.println("No circuits found.");
            return Collections.emptyList();
        }

        return filterResponse.getPayLoad().getCircuits();
    }

    public List<ServiceResource> getServices() {
        FilterResponse filterResponse = getResponse();
        if (filterResponse == null || filterResponse.getPayLoad() == null || filterResponse.getPayLoad().getServices() == null) {
            System.out.println("No services found.");
            return Collections.emptyList();
        }

        return filterResponse.getPayLoad().getServices();
    }

    public List<IconModel> getIcons() {
        FilterResponse filterResponse = getResponse();
        if (filterResponse == null || filterResponse.getPayLoad() == null || filterResponse.getPayLoad().getIcons() == null) {
            System.out.println("No icons found.");
            return Collections.emptyList();
        }

        return filterResponse.getPayLoad().getIcons();
    }

    public List<ConsumableMetric> getMetrics() {
        FilterResponse filterResponse = getResponse();
        if (filterResponse == null || filterResponse.getPayLoad() == null || filterResponse.getPayLoad().getMetrics() == null) {
            System.out.println("No metrics found.");
            return Collections.emptyList();
        }

        return filterResponse.getPayLoad().getMetrics();
    }

    public ResourceSchemaResponse getSchema() {
        Request request = new Request.Builder()
                .url(netcompassAPIClient.getConfiguration().getNetcompassApiUrl() + url)
                .get().build();

        //  Envia a solicitação HTTP e processa a resposta.
        //  A resposta é analisada usando Gson e os recursos obtidos são armazenados.
        //  Resposta no formato JSON e a converte para um objeto 'FilterResponse' (Java)
        try (Response r = netcompassAPIClient.getOauthManager().getAuthHttpClient().newCall(request).execute()) {
            if (r.code() != 200) {
                System.out.println("Error: " + r.code());
                hasNext = false;
                return null;
            }
            return gson.fromJson(r.body().string(), ResourceSchemaResponse.class);
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
