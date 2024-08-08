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
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;

import java.util.ArrayList;
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
                //todo me hicieron comentario de crear indice con formato de una
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

    public List<Tasks> searchTasks(Map<String, Object> body) {
        log.info("---------Search tasks. Building query------------");
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        log.info("---------Adding filters to query------------");
        // Add "is" filters to the query
        if (body.containsKey("statusIs")) {
            boolQuery.must(QueryBuilders.matchQuery("status", body.get("statusIs")));
            log.info("---------Status filter added {}------------",body.get("statusIs"));
        }
        if (body.containsKey("assigneeIs")) {
            boolQuery.must(QueryBuilders.matchQuery("assignee", body.get("assigneeIs")));
            log.info("---------Asignee filter added {}------------",body.get("assigneeIs"));
        }
        if (body.containsKey("creationDateIs")) {
            boolQuery.must(QueryBuilders.matchQuery("creationDate", body.get("creationDateIs")));
            log.info("---------CreationDate filter added {}------------",body.get("creationDateIs"));
        }
        if (body.containsKey("completionDateIs")) {
            boolQuery.must(QueryBuilders.matchQuery("completionDate", body.get("completionDateIs")));
            log.info("---------CompletionDate filter added {}------------",body.get("completionDateIs"));
        }
        log.info("---------Filters added------------");
        log.info("---------Creating request------------");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(boolQuery);
        //todo create constants for tasks index and params abled and everything
        SearchRequest searchRequest = new SearchRequest("tasks"); // Use your index name here
        searchRequest.source(sourceBuilder);
        log.info("---------Request created------------");
        log.info("---------Executing search------------");
        List<Tasks> result = executeSearch(searchRequest);
        log.info("---------Tasks found {}------------",result);
        return result;
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
    private List<Tasks> executeSearch(SearchRequest searchRequest) {
        List<Tasks> tasksList = new ArrayList<>();
        try {
            SearchResponse response = client.search(searchRequest).actionGet();
            for (SearchHit hit : response.getHits()) {
                Tasks task = convertMapToTask(hit.getSourceAsMap());
                tasksList.add(task);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return tasksList;
    }
}
