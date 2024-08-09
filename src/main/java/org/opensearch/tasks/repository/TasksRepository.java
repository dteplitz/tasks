/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks.repository;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
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
import org.opensearch.tasks.model.Tasks;

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
    }

    public IndexResponse createTask(Tasks tasks) {
        try {
            log.info("Creating task {}", tasks);
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("title", tasks.getTitle());
            taskMap.put("description", tasks.getDescription());
            taskMap.put("status", tasks.getStatus());
            taskMap.put("creationDate", tasks.getCreationDate());
            taskMap.put("completionDate", tasks.getCompletionDate());
            taskMap.put("plannedDate", tasks.getPlannedDate());
            taskMap.put("assignee", tasks.getAssignee());
            taskMap.put("tags", tasks.getTags());
            log.info("Task mapped {}", taskMap);
            IndexRequest indexRequest = Requests.indexRequest(INDEX)
                    .source(taskMap, XContentType.JSON);
            log.info("Creating indexRequest {}", indexRequest);
            IndexResponse result = client.index(indexRequest).actionGet();
            log.info("Result creating task {}", result);
            return result;
        } catch (Exception e) {
            log.info("Exception creating task {}", e.getMessage());
            return null;
        }
    }

    public Tasks getTaskById(String id) {
        try {
            log.info("Getting task by id {}", id);
            GetResponse getResponse = client.get(Requests.getRequest(INDEX).id(id)).actionGet();
            if (getResponse.getId() != null) {
                Tasks task = convertMapToTask(getResponse.getSourceAsMap(), getResponse.getId());
                log.info("Task found {}", task);
                return task;
            } else {
                log.info("Task not found for id {}", id);
                return null;
            }
        } catch (Exception e) {
            log.info("Exception getting task {}", e.getMessage());
            return null;
        }
    }

    public IndexResponse updateTask(Tasks tasks) {
        log.info("Updating task {}", tasks);
        IndexResponse updateTaskIndex = createTask(tasks);
        log.info("Updating task result {}", updateTaskIndex.status());
        return updateTaskIndex;
    }

    public RestStatus deleteTask(String id) {
        log.info("Deleting task by id {}", id);
        RestStatus status = client.delete(Requests.deleteRequest(INDEX).id(id)).actionGet().status();
        log.info("Delete task result {}", status);
        return status;
    }

    public List<Tasks> searchTasks(Map<String, Object> body) {
        log.info("Search tasks. Building query");
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        log.info("Adding filters to query");
        addDateFilters(body, boolQuery);
        addEqualsFilters(body, boolQuery);
        log.info("Filters added");

        return executeQuery(boolQuery);
    }

    private List<Tasks> executeQuery(BoolQueryBuilder boolQuery) {

        log.info("---------Creating request------------");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(boolQuery);
        SearchRequest searchRequest = new SearchRequest("tasks"); // Use your index name here
        searchRequest.source(sourceBuilder);
        log.info("---------Request created------------");

        log.info("---------Executing search------------");
        List<Tasks> result = executeSearch(searchRequest);
        log.info("---------Tasks found {}------------", result);

        return result;
    }

    private static void addEqualsFilters(Map<String, Object> body, BoolQueryBuilder boolQuery) {
        //todo create Constants for these variables
        if (body.containsKey("equals")) {
            Map<String, Object> equals = (Map<String, Object>) body.get("equals");
            if (equals.containsKey("statusIs")) {
                boolQuery.must(QueryBuilders.matchQuery("status", equals.get("statusIs")));
                log.info("Status filter added {}", equals.get("statusIs"));
            }
            if (equals.containsKey("assigneeIs")) {
                boolQuery.must(QueryBuilders.matchQuery("assignee", equals.get("assigneeIs")));
                log.info("Asignee filter added {}", equals.get("assigneeIs"));
            }
        }
    }

    private static void addDateFilters(Map<String, Object> body, BoolQueryBuilder boolQuery) {
        //todo create Constants for these variables
        log.info("Adding date filters");
        if (body.containsKey("creationDateFrom")) {
            boolQuery.must(QueryBuilders.rangeQuery("creationDate").gte(body.get("creationDateFrom")));
            log.info("CreationDate filter added {}", body.get("creationDateFrom"));
        }
        if (body.containsKey("creationDateTo")) {
            boolQuery.must(QueryBuilders.rangeQuery("creationDate").lte(body.get("creationDateTo")));
            log.info("CreationDateTo filter added {}", body.get("creationDateTo"));
        }
        if (body.containsKey("completionDateFrom")) {
            boolQuery.must(QueryBuilders.rangeQuery("completionDate").gte(body.get("completionDateFrom")));
            log.info("CreationDate filter added {}", body.get("completionDateFrom"));
        }
        if (body.containsKey("completionDateTo")) {
            boolQuery.must(QueryBuilders.rangeQuery("completionDate").lte(body.get("completionDateTo")));
            log.info("CreationDateTo filter added {}", body.get("completionDateTo"));
        }
        if (body.containsKey("plannedDateFrom")) {
            boolQuery.must(QueryBuilders.rangeQuery("plannedDate").gte(body.get("plannedDateFrom")));
            log.info("CreationDate filter added {}", body.get("plannedDateFrom"));
        }
        if (body.containsKey("plannedDateTo")) {
            boolQuery.must(QueryBuilders.rangeQuery("plannedDate").lte(body.get("plannedDateTo")));
            log.info("CreationDateTo filter added {}", body.get("plannedDateTo"));
        }
    }

    private Tasks convertMapToTask(Map<String, Object> sourceAsMap, String id) {
        log.info("Converting to task {} - {}", id, sourceAsMap);
        Tasks tasks = new Tasks();
        tasks.setId(id);
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
                Tasks task = convertMapToTask(hit.getSourceAsMap(), hit.getId());
                tasksList.add(task);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return tasksList;
    }
}
