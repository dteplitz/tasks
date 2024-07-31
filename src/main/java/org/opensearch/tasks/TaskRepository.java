/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks;

import com.google.gson.Gson;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;

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
            //e.printStackTrace();
        }
    }

    public RestStatus createTask(Tasks tasks){
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("title", tasks.getTitle());
                builder.field("description", tasks.getDescription());
                builder.field("status", tasks.getStatus());
                builder.field("creationDate", tasks.getCreationDate());
                builder.field("completionDate", tasks.getCompletionDate());
                builder.field("assignee", tasks.getAssignee());
                builder.field("tags", tasks.getTags());
            }
            builder.endObject();

            return client.index(Requests.indexRequest(INDEX).id(tasks.getId()).source(builder)).actionGet().status();
        } catch (IOException e) {
            //e.printStackTrace();
            return RestStatus.INTERNAL_SERVER_ERROR;
        }
    }

    public Tasks getTaskById(String id) {
        try {
            Map<String, Object> sourceAsMap = client.get(Requests.getRequest(INDEX).id(id)).actionGet().getSourceAsMap();
            return convertMapToTask(sourceAsMap);
        } catch (Exception e) {
            //e.printStackTrace();
            return null;
        }
    }

    public RestStatus updateTask(Tasks tasks) {
        return createTask(tasks);  // Re-indexing the task
    }

    public RestStatus deleteTask(String id) {
        return client.delete(Requests.deleteRequest(INDEX).id(id)).actionGet().status();
    }

    public List<Tasks> searchTasks(String query) {
        // Implement search logic based on query and return a list of tasks
        return null;
    }

    private Tasks convertMapToTask(Map<String, Object> sourceAsMap) {
        Tasks tasks = new Tasks();
        tasks.setId((String) sourceAsMap.get("id"));
        tasks.setTitle((String) sourceAsMap.get("title"));
        tasks.setDescription((String) sourceAsMap.get("description"));
        tasks.setStatus((String) sourceAsMap.get("status"));
        tasks.setCreationDate((Date) sourceAsMap.get("creationDate"));
        tasks.setCompletionDate((Date) sourceAsMap.get("completionDate"));
        tasks.setAssignee((String) sourceAsMap.get("assignee"));
        tasks.setTags((List<String>) sourceAsMap.get("tags"));
        return tasks;
    }
}
