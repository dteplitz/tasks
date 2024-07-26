/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks.repository;

import com.google.gson.Gson;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.tasks.model.Task;

import java.io.IOException;

public class TaskRepository {
    private final Client client;
    private final Gson gson = new Gson();

    private static final String INDEX = "tasks";

    public TaskRepository(Client client) {
        this.client = client;
        createIndexIfNotExists();
    }

    private void createIndexIfNotExists() {
        boolean indexExists = client.admin().indices().exists(new IndicesExistsRequest(INDEX)).actionGet().isExists();
        if (!indexExists) {
            client.admin().indices().create(new CreateIndexRequest(INDEX)).actionGet();
        }
    }

    public void createTask(Task task) throws IOException {
        IndexRequest request = new IndexRequest(INDEX)
        .id(task.getId())
        .source(gson.toJson(task), XContentType.JSON);
        client.index(request).actionGet();
    }

    public Task getTask(String id) throws IOException {
        GetResponse response = client.prepareGet(INDEX, id).get();
        return gson.fromJson(response.getSourceAsString(), Task.class);
    }

    public void deleteTask(String id) {
        DeleteRequest request = new DeleteRequest(INDEX, id);
        client.delete(request).actionGet();
    }

    public SearchResponse searchTasks(String query) {
        SearchRequest searchRequest = new SearchRequest(INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.queryStringQuery(query));
        searchRequest.source(sourceBuilder);
        return client.search(searchRequest).actionGet();
    }
}
