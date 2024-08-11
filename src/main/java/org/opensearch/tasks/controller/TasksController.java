/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.util.NamedThreadFactory;
import org.opensearch.client.node.NodeClient;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestRequest;
import org.opensearch.tasks.model.Tasks;
import org.opensearch.tasks.service.TasksService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.opensearch.rest.RestRequest.Method.*;

public class TasksController extends BaseRestHandler {

    private final TasksService tasksService;
    private final ExecutorService executor;
    private static final Logger log = LogManager.getLogger(TasksController.class);

    public TasksController(TasksService tasksService) {
        this.tasksService = tasksService;
        this.executor = Executors.newFixedThreadPool(10, new NamedThreadFactory("TasksControllerThread"));
    }

    @Override
    public String getName() {
        return "task-plugin";
    }

    @Override
    public List<Route> routes() {
        return List.of(
                new Route(GET, "/_plugins/tasks/{id}"),
                new Route(POST, "/_plugins/tasks/search"),
                new Route(POST, "/_plugins/tasks"),
                new Route(PUT, "/_plugins/tasks"),
                new Route(PATCH, "/_plugins/tasks"),
                new Route(DELETE, "/_plugins/tasks/{id}")
        );
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        log.info("Preparing request - Method: {}, ID: {}", request.method(), request.param("id"));
        switch (request.method()) {
            case POST:
                return channel -> handlePostRequest(request, channel);
            case GET:
                return channel -> handleGetRequest(request, channel);
            case PUT:
                return channel -> handlePutRequest(request, channel);
            case DELETE:
                return channel -> handleDeleteRequest(request, channel);
            case PATCH:
                return channel -> handlePatchRequest(request, channel);
            default:
                return this::handleDefaultRequest;
            //return channel -> defaultRequestHandle(channel);
        }
    }

    private void handlePatchRequest(RestRequest request, RestChannel channel) throws IOException {
        log.info("Processing PATCH request");
        Tasks task = parseRequestBody(request);
        if (task == null || task.getId() == null) {
            channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, "Invalid task data"));
            return;
        }
        CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> tasksService.patchTask(task), executor);
        future.thenAccept(status -> channel.sendResponse(new BytesRestResponse(status, XContentType.JSON.mediaType(), "")))
                .exceptionally(ex -> handleException(channel, ex));
        log.info("PATCH request processed");
    }

    private void handleDeleteRequest(RestRequest request, RestChannel channel) throws IOException {
        log.info("Processing DELETE request");
        String id = request.param("id");
        if (id == null) {
            channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, "Task ID is missing"));
            return;
        }
        CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> tasksService.deleteTask(id), executor);
        future.thenAccept(status -> channel.sendResponse(new BytesRestResponse(status, XContentType.JSON.mediaType(), id)))
                .exceptionally(ex -> handleException(channel, ex));
        log.info("DELETE request processed");
    }

    private void handlePutRequest(RestRequest request, RestChannel channel) throws IOException {
        log.info("Processing PUT request");
        Tasks task = parseRequestBody(request);
        if (task == null || task.getId() == null) {
            channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, "Invalid task data"));
            return;
        }
        CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> tasksService.updateTask(task), executor);
        future.thenAccept(status -> {
            if (status == RestStatus.CREATED) {
                channel.sendResponse(new BytesRestResponse(status, XContentType.JSON.mediaType(), toJson(task)));
            } else {
                channel.sendResponse(new BytesRestResponse(status, XContentType.JSON.mediaType(), ""));
            }
        }).exceptionally(ex -> handleException(channel, ex));
        log.info("PUT request processed");
    }

    private void handleGetRequest(RestRequest request, RestChannel channel) {
        log.info("Processing GET request");
        String id = request.param("id");
        if (id == null) {
            channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, "Task ID is missing"));
            return;
        }
        CompletableFuture.runAsync(() -> {
            Tasks task = tasksService.getTaskById(id);
            if (task != null) {
                channel.sendResponse(new BytesRestResponse(RestStatus.OK, XContentType.JSON.mediaType(), toJson(task)));
            } else {
                channel.sendResponse(new BytesRestResponse(RestStatus.NOT_FOUND, XContentType.JSON.mediaType(), toJson(null)));
            }
        }, executor).exceptionally(ex -> handleException(channel, ex));
        log.info("GET request processed");
    }

    private void handlePostRequest(RestRequest request, RestChannel channel) throws IOException {
        log.info("Processing POST request");
        Map<String, Object> body = request.contentParser().mapOrdered();
        if (!request.path().contains("search")) {
            log.info("Creating task");
            Tasks task = parseRequestBody(request);
            CompletableFuture<Tasks> future = CompletableFuture.supplyAsync(() -> tasksService.createTask(task), executor);
            handleFutureCreateTask(channel, future);
        } else {
            log.info("Searching tasks");
            CompletableFuture.runAsync(() -> searchTasks(channel, body), executor)
                    .exceptionally(ex -> handleException(channel, ex));
        }
        log.info("POST request processed");
    }

    private void handleFutureCreateTask(RestChannel channel, CompletableFuture<Tasks> future) {
        future.thenAccept(taskResult -> {
            if (taskResult != null) {
                channel.sendResponse(new BytesRestResponse(RestStatus.CREATED, XContentType.JSON.mediaType(), toJson(taskResult)));
            } else {
                channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, XContentType.JSON.mediaType(), ""));
            }
        }).exceptionally(ex -> handleException(channel, ex));
    }

    private RestStatus searchTasks(RestChannel channel, Map<String, Object> body) {
        List<Tasks> tasks = tasksService.searchTasks(body);
        channel.sendResponse(new BytesRestResponse(RestStatus.OK, XContentType.JSON.mediaType(), toJson(tasks)));
        return RestStatus.OK;
    }

    private Void handleException(RestChannel channel, Throwable ex) {
        log.error("Error processing request", ex);
        channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
        return null;
    }

    private void handleDefaultRequest(RestChannel channel) {
        log.info("Processing default request");
        channel.sendResponse(new BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, XContentType.JSON.mediaType(), toJson(null)));
        log.info("Default request processed");
    }

    private Tasks parseRequestBody(RestRequest request) throws IOException {
        if (!request.hasContent()) {
            return null;
        }
        Map<String, Object> map = request.contentParser().mapOrdered();
        Tasks task = new Tasks();
        task.setId((String) map.getOrDefault("id", null));
        task.setTitle((String) map.getOrDefault("title", null));
        task.setDescription((String) map.getOrDefault("description", null));
        task.setStatus((String) map.getOrDefault("status", null));
        task.setCreationDate((String) map.getOrDefault("creationDate", null));
        task.setCompletionDate((String) map.getOrDefault("completionDate", null));
        task.setPlannedDate((String) map.getOrDefault("plannedDate", null));
        task.setAssignee((String) map.getOrDefault("assignee", null));
        task.setSecurityStandards((String) map.getOrDefault("securityStandards", null));
        task.setTags((List<String>) map.getOrDefault("tags", null));
        return task;
    }

    private String toJson(Object object) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            if (object instanceof Tasks) {
                Tasks task = (Tasks) object;
                builder.field("id", task.getId());
                builder.field("title", task.getTitle());
                builder.field("description", task.getDescription());
                builder.field("status", task.getStatus());
                builder.field("creationDate", task.getCreationDate());
                builder.field("completionDate", task.getCompletionDate());
                builder.field("plannedDate", task.getPlannedDate());
                builder.field("assignee", task.getAssignee());
                builder.field("securityStandards", task.getSecurityStandards());
                builder.field("tags", task.getTags());
            } else if (object instanceof List) {
                List<Tasks> tasks = (List<Tasks>) object;
                builder.startArray("tasks");
                for (Tasks task : tasks) {
                    builder.startObject();
                    builder.field("id", task.getId());
                    builder.field("title", task.getTitle());
                    builder.field("description", task.getDescription());
                    builder.field("status", task.getStatus());
                    builder.field("creationDate", task.getCreationDate());
                    builder.field("completionDate", task.getCompletionDate());
                    builder.field("plannedDate", task.getPlannedDate());
                    builder.field("assignee", task.getAssignee());
                    builder.field("securityStandards", task.getSecurityStandards());
                    builder.field("tags", task.getTags());
                    builder.endObject();
                }
                builder.endArray();
            }
            builder.endObject();
            return builder.toString();
        } catch (IOException e) {
            log.error("Error converting object to JSON", e);
            return "";
        }
    }
}
