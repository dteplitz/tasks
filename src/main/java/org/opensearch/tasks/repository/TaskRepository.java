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
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.tasks.model.Task;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TaskRepository {
    private final Client client;
    private final Gson gson = new Gson();

    private static final String INDEX = "tasks";

    public TaskRepository(Client client) {
        this.client = client;
        createIndex();
    }

    private void createIndex() {
        try {
            if (!client.admin().indices().prepareExists(INDEX).get().isExists()) {
                CreateIndexRequest request = new CreateIndexRequest(INDEX);
                CreateIndexResponse createIndexResponse = client.admin().indices().create(request).actionGet();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RestStatus createTask(Task task){
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("title", task.getTitle());
                builder.field("description", task.getDescription());
                builder.field("status", task.getStatus());
                builder.field("creationDate", task.getCreationDate());
                builder.field("completionDate", task.getCompletionDate());
                builder.field("assignee", task.getAssignee());
                builder.field("tags", task.getTags());
            }
            builder.endObject();

            return client.index(Requests.indexRequest(INDEX).id(task.getId()).source(builder)).actionGet().status();
        } catch (IOException e) {
            e.printStackTrace();
            return RestStatus.INTERNAL_SERVER_ERROR;
        }
    }

    public Task getTaskById(String id) {
        try {
            Map<String, Object> sourceAsMap = client.get(Requests.getRequest(INDEX).id(id)).actionGet().getSourceAsMap();
            return convertMapToTask(sourceAsMap);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public RestStatus updateTask(Task task) {
        return createTask(task);  // Re-indexing the task
    }

    public RestStatus deleteTask(String id) {
        return client.delete(Requests.deleteRequest(INDEX).id(id)).actionGet().status();
    }

    public List<Task> searchTasks(String query) {
        // Implement search logic based on query and return a list of tasks
        return null;
    }

    private Task convertMapToTask(Map<String, Object> sourceAsMap) {
        Task task = new Task();
        task.setId((String) sourceAsMap.get("id"));
        task.setTitle((String) sourceAsMap.get("title"));
        task.setDescription((String) sourceAsMap.get("description"));
        task.setStatus((String) sourceAsMap.get("status"));
        task.setCreationDate((Date) sourceAsMap.get("creationDate"));
        task.setCompletionDate((Date) sourceAsMap.get("completionDate"));
        task.setAssignee((String) sourceAsMap.get("assignee"));
        task.setTags((List<String>) sourceAsMap.get("tags"));
        return task;
    }
}
