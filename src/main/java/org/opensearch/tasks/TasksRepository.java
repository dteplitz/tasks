/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Requests;
import org.opensearch.common.action.ActionFuture;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TasksRepository {
    private final Client client;
    private final Gson gson = new Gson();
    private static final Logger log = LogManager.getLogger(TasksRepository.class);
    private static final String INDEX = "tasks";

    public TasksRepository(Client client) {
        this.client = client;
        createIndex();
    }

    private void createIndex() {
        try {
            log.info("Creating index");
            if (!client.admin().indices().prepareExists(INDEX).get().isExists()) {
                CreateIndexRequest request = new CreateIndexRequest(INDEX);
                CreateIndexResponse createIndexResponse = client.admin().indices().create(request).actionGet();
                log.info("Index created ok - {}", createIndexResponse);
                /*client.admin()
                        .indices()
                        .create(request, ActionListener.wrap(response -> listener.onResponse(response.isAcknowledged()), exception -> {
                            if (exception instanceof ResourceAlreadyExistsException
                                    || exception.getCause() instanceof ResourceAlreadyExistsException) {
                                listener.onResponse(true);
                            } else {
                                listener.onFailure(exception);
                            }
                        }));*/
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public RestStatus createTask(Tasks tasks) {
        try {
            log.info("Creating task {}", tasks);
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("title", tasks.getTitle());
            taskMap.put("description", tasks.getDescription());
            taskMap.put("status", tasks.getStatus());
            taskMap.put("creationDate", tasks.getCreationDate());
            taskMap.put("completionDate", tasks.getCompletionDate());
            taskMap.put("assignee", tasks.getAssignee());
            taskMap.put("tags", tasks.getTags());
            log.info("Task mapped {}", taskMap);
            IndexRequest indexRequest = Requests.indexRequest(INDEX)
                    .id(tasks.getId())
                    .source(taskMap, XContentType.JSON);
            log.info("Creating indexRequest {}", indexRequest);
            RestStatus result = client.index(indexRequest).actionGet().status();
            log.info("Result creating task {}", result);
            return result;
        } catch (Exception e) {
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
        List<Tasks> tasksList = new ArrayList<>();
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX);
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.queryStringQuery(query));
            searchRequest.source(sourceBuilder);

            ActionFuture<SearchResponse> future = client.search(searchRequest);
            SearchResponse searchResponse = future.actionGet();
            for (SearchHit hit : searchResponse.getHits()) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                tasksList.add(convertMapToTask(sourceAsMap));
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return tasksList;
    }

    private Tasks convertMapToTask(Map<String, Object> sourceAsMap) {
        Tasks tasks = new Tasks();
        tasks.setId((String) sourceAsMap.getOrDefault("id", null));
        tasks.setTitle((String) sourceAsMap.getOrDefault("title", null));
        tasks.setDescription((String) sourceAsMap.getOrDefault("description", null));
        tasks.setStatus((String) sourceAsMap.getOrDefault("status", null));
        tasks.setCreationDate((String) sourceAsMap.getOrDefault("creationDate", null));
        tasks.setCompletionDate((String) sourceAsMap.getOrDefault("completionDate", null));
        tasks.setAssignee((String) sourceAsMap.getOrDefault("assignee", null));
        tasks.setTags((List<String>) sourceAsMap.getOrDefault("tags", null));
        return tasks;
    }
}
