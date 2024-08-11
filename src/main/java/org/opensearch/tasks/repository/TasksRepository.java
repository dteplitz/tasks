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
    private static final Logger log = LogManager.getLogger(TasksRepository.class);
    private static final String INDEX = "tasks";

    public TasksRepository(Client client) {
        this.client = client;
        createIndex();
    }

    /**
     * Creates the index for storing tasks if it does not already exist.
     */
    private void createIndex() {
        try {
            log.info("Creating index: {}", INDEX);
            if (!client.admin().indices().prepareExists(INDEX).get().isExists()) {
                CreateIndexRequest request = new CreateIndexRequest(INDEX);
                CreateIndexResponse createIndexResponse = client.admin().indices().create(request).actionGet();
                log.info("Index created successfully: {}", createIndexResponse);
            }
        } catch (Exception e) {
            log.error("Error while creating index: {}", e.getMessage());
        }
    }

    /**
     * Creates a new task in the index.
     *
     * @param tasks The task to create.
     * @return The index response containing the result of the operation.
     */
    public IndexResponse createTask(Tasks tasks) {
        try {
            log.info("Creating task: {}", tasks);
            Map<String, Object> taskMap = convertTaskToMap(tasks);
            IndexRequest indexRequest = Requests.indexRequest(INDEX)
                    .source(taskMap, XContentType.JSON);
            IndexResponse result = client.index(indexRequest).actionGet();
            log.info("Task created with result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Exception while creating task: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a task by its ID.
     *
     * @param id The ID of the task to retrieve.
     * @return The retrieved task, or null if not found.
     */
    public Tasks getTaskById(String id) {
        try {
            log.info("Retrieving task by ID: {}", id);
            GetResponse getResponse = client.get(Requests.getRequest(INDEX).id(id)).actionGet();
            if (getResponse.getId() != null) {
                Tasks task = convertMapToTask(getResponse.getSourceAsMap(), getResponse.getId());
                log.info("Task retrieved: {}", task);
                return task;
            } else {
                log.warn("Task not found for ID: {}", id);
                return null;
            }
        } catch (Exception e) {
            log.error("Exception while retrieving task: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Updates an existing task in the index.
     *
     * @param tasks The task to update.
     * @return The index response containing the result of the operation.
     */
    public IndexResponse updateTask(Tasks tasks) {
        try {
            log.info("Updating task: {}", tasks);
            Map<String, Object> taskMap = convertTaskToMap(tasks);
            IndexRequest indexRequest = Requests.indexRequest(INDEX)
                    .id(tasks.getId())
                    .source(taskMap, XContentType.JSON);
            IndexResponse result = client.index(indexRequest).actionGet();
            log.info("Task updated with result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Exception while updating task: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Deletes a task by its ID.
     *
     * @param id The ID of the task to delete.
     * @return The result status of the delete operation.
     */
    public RestStatus deleteTask(String id) {
        try {
            log.info("Deleting task by ID: {}", id);
            RestStatus status = client.delete(Requests.deleteRequest(INDEX).id(id)).actionGet().status();
            log.info("Task delete result: {}", status);
            return status;
        } catch (Exception e) {
            log.error("Exception while deleting task: {}", e.getMessage());
            return RestStatus.BAD_REQUEST;
        }
    }

    /**
     * Searches for tasks based on the provided search criteria.
     *
     * @param body The search criteria as a map.
     * @return A list of tasks matching the search criteria.
     */
    public List<Tasks> searchTasks(Map<String, Object> body) {
        log.info("Building search query for tasks.");
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        addDateFilters(body, boolQuery);
        addEqualsFilters(body, boolQuery);

        return executeQuery(boolQuery);
    }

    /**
     * Executes the search query and returns the results.
     *
     * @param boolQuery The boolean query to execute.
     * @return A list of tasks matching the query.
     */
    private List<Tasks> executeQuery(BoolQueryBuilder boolQuery) {
        try {
            log.info("Executing search query.");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(boolQuery);
            SearchRequest searchRequest = new SearchRequest(INDEX).source(sourceBuilder);
            SearchResponse response = client.search(searchRequest).actionGet();

            List<Tasks> tasksList = new ArrayList<>();
            for (SearchHit hit : response.getHits()) {
                Tasks task = convertMapToTask(hit.getSourceAsMap(), hit.getId());
                tasksList.add(task);
            }

            log.info("Search completed with {} tasks found.", tasksList.size());
            return tasksList;
        } catch (Exception e) {
            log.error("Exception while executing search: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Converts a task object to a map for indexing.
     *
     * @param tasks The task to convert.
     * @return A map representing the task.
     */
    private Map<String, Object> convertTaskToMap(Tasks tasks) {
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("title", tasks.getTitle());
        taskMap.put("description", tasks.getDescription());
        taskMap.put("status", tasks.getStatus());
        taskMap.put("creationDate", tasks.getCreationDate());
        taskMap.put("completionDate", tasks.getCompletionDate());
        taskMap.put("plannedDate", tasks.getPlannedDate());
        taskMap.put("assignee", tasks.getAssignee());
        taskMap.put("securityStandards", tasks.getSecurityStandards());
        taskMap.put("tags", tasks.getTags());
        return taskMap;
    }

    /**
     * Converts a map to a task object.
     *
     * @param sourceAsMap The source map.
     * @param id          The ID of the task.
     * @return A task object.
     */
    private Tasks convertMapToTask(Map<String, Object> sourceAsMap, String id) {
        Tasks tasks = new Tasks();
        tasks.setId(id);
        tasks.setTitle((String) sourceAsMap.getOrDefault("title", null));
        tasks.setDescription((String) sourceAsMap.getOrDefault("description", null));
        tasks.setStatus((String) sourceAsMap.getOrDefault("status", null));
        tasks.setCreationDate((String) sourceAsMap.getOrDefault("creationDate", null));
        tasks.setCompletionDate((String) sourceAsMap.getOrDefault("completionDate", null));
        tasks.setPlannedDate((String) sourceAsMap.getOrDefault("plannedDate", null));
        tasks.setAssignee((String) sourceAsMap.getOrDefault("assignee", null));
        tasks.setSecurityStandards((String) sourceAsMap.getOrDefault("securityStandards", null));
        tasks.setTags((List<String>) sourceAsMap.getOrDefault("tags", null));
        return tasks;
    }

    /**
     * Adds equality filters to the search query.
     *
     * @param body      The search criteria.
     * @param boolQuery The query builder to add filters to.
     */
    private static void addEqualsFilters(Map<String, Object> body, BoolQueryBuilder boolQuery) {
        if (body.containsKey("equals")) {
            Map<String, Object> equals = (Map<String, Object>) body.get("equals");
            if (equals.containsKey("title")) {
                boolQuery.must(QueryBuilders.matchPhraseQuery("title.keyword", equals.get("title").toString()));
                log.info("Title filter added: {}", equals.get("title"));
            }
            if (equals.containsKey("description")) {
                boolQuery.must(QueryBuilders.matchPhraseQuery("description.keyword", equals.get("description").toString()));
                log.info("Description filter added: {}", equals.get("description"));
            }
            if (equals.containsKey("status")) {
                boolQuery.must(QueryBuilders.matchPhraseQuery("status.keyword", equals.get("status").toString()));
                log.info("Status filter added: {}", equals.get("status"));
            }
            if (equals.containsKey("assignee")) {
                boolQuery.must(QueryBuilders.matchPhraseQuery("assignee.keyword", equals.get("assignee").toString()));
                log.info("Assignee filter added: {}", equals.get("assignee"));
            }
            if (equals.containsKey("securityStandards")) {
                boolQuery.must(QueryBuilders.matchPhraseQuery("securityStandards.keyword", equals.get("securityStandards").toString()));
                log.info("SecurityStantards filter added: {}", equals.get("securityStandards"));
            }
        }
    }

    /**
     * Adds date filters to the search query.
     *
     * @param body      The search criteria.
     * @param boolQuery The query builder to add filters to.
     */
    private static void addDateFilters(Map<String, Object> body, BoolQueryBuilder boolQuery) {
        log.info("Adding date filters to the search query.");
        if (body.containsKey("creationDateFrom")) {
            boolQuery.must(QueryBuilders.rangeQuery("creationDate").gte(body.get("creationDateFrom")));
            log.info("CreationDateFrom filter added: {}", body.get("creationDateFrom"));
        }
        if (body.containsKey("creationDateTo")) {
            boolQuery.must(QueryBuilders.rangeQuery("creationDate").lte(body.get("creationDateTo")));
            log.info("CreationDateTo filter added: {}", body.get("creationDateTo"));
        }
        if (body.containsKey("completionDateFrom")) {
            boolQuery.must(QueryBuilders.rangeQuery("completionDate").gte(body.get("completionDateFrom")));
            log.info("CompletionDateFrom filter added: {}", body.get("completionDateFrom"));
        }
        if (body.containsKey("completionDateTo")) {
            boolQuery.must(QueryBuilders.rangeQuery("completionDate").lte(body.get("completionDateTo")));
            log.info("CompletionDateTo filter added: {}", body.get("completionDateTo"));
        }
        if (body.containsKey("plannedDateFrom")) {
            boolQuery.must(QueryBuilders.rangeQuery("plannedDate").gte(body.get("plannedDateFrom")));
            log.info("PlannedDateFrom filter added: {}", body.get("plannedDateFrom"));
        }
        if (body.containsKey("plannedDateTo")) {
            boolQuery.must(QueryBuilders.rangeQuery("plannedDate").lte(body.get("plannedDateTo")));
            log.info("PlannedDateTo filter added: {}", body.get("plannedDateTo"));
        }
    }
}
