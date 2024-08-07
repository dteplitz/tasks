/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks;

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
import org.opensearch.rest.RestRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.opensearch.rest.RestRequest.Method.DELETE;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.rest.RestRequest.Method.PUT;

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
                new Route(POST, "/_plugins/tasks"),
                new Route(GET, "/_plugins/tasks/{id}"),
                new Route(PUT, "/_plugins/tasks/{id}"),
                new Route(DELETE, "/_plugins/tasks/{id}"),
                new Route(GET, "/_plugins/tasks/search")
        );
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        log.info("---------Starting prepareRequest {} -- {}--------------",request.param("id"), request.method());
        String id = request.param("id");

        switch (request.method()) {
            case POST:
                return channel -> {
                    Tasks task = parseRequestBody(request);
                    log.info("---------Task: {} ------------",task);
                    CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> tasksService.createTask(task), executor);
                    log.info("Future created, waiting accept");
                    future.thenAccept(status -> {
                        try {
                            log.info("Try future status");
                            channel.sendResponse(new BytesRestResponse(status, String.valueOf(XContentType.JSON), toJson(task)));
                            log.info("Channel response sent");
                        } catch (IOException e) {
                            log.info("Error future status");
                            channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, String.valueOf(XContentType.JSON), e.getMessage()));
                            log.info("Error is {}",e.getMessage());
                        }
                    });
                    future.exceptionally(ex -> {
                        log.error("Error processing request", ex);
                        channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
                        return null;
                    });
                };
            case GET:
                return channel -> {
                    if (id != null) {
                        CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> {
                            Tasks task = tasksService.getTaskById(id);
                            if (task != null) {
                                try {
                                    channel.sendResponse(new BytesRestResponse(RestStatus.OK, String.valueOf(XContentType.JSON), toJson(task)));
                                } catch (IOException e) {
                                    channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, String.valueOf(XContentType.JSON), e.getMessage()));
                                }
                            } else {
                                try {
                                    channel.sendResponse(new BytesRestResponse(RestStatus.NOT_FOUND, String.valueOf(XContentType.JSON), toJson(null)));
                                } catch (IOException e) {
                                    channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, String.valueOf(XContentType.JSON), e.getMessage()));
                                }
                            }
                            return RestStatus.OK;
                        });
                    } else {
                        String query = request.param("query");
                        CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> {
                            List<Tasks> tasks = tasksService.searchTasks(query);
                            try {
                                channel.sendResponse(new BytesRestResponse(RestStatus.OK, String.valueOf(XContentType.JSON), toJson(tasks)));
                            } catch (IOException e) {
                                channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, String.valueOf(XContentType.JSON), e.getMessage()));
                            }
                            return RestStatus.OK;
                        });
                    }
                };
            case PUT:
                return channel -> {
                    Tasks task = parseRequestBody(request);
                    if (task == null) {
                        channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, "Invalid task data"));
                        return;
                    }
                    RestStatus status = tasksService.updateTask(task);
                    channel.sendResponse(new BytesRestResponse(status, String.valueOf(XContentType.JSON), toJson(task)));
                };
            case DELETE:
                return channel -> {
                    if (id == null) {
                        channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, "Task ID is missing"));
                        return;
                    }
                    RestStatus status = tasksService.deleteTask(id);
                    channel.sendResponse(new BytesRestResponse(status, String.valueOf(XContentType.JSON), toJson(null)));
                };
            default:
                return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, String.valueOf(XContentType.JSON), toJson(null)));
        }
    }

    private Tasks parseRequestBody(RestRequest request) throws IOException {
        if (request.hasContent() == false) {
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
        task.setAssignee((String) map.getOrDefault("assignee", null));
        task.setTags((List<String>) map.getOrDefault("tags", null));
        return task;
    }

    private String toJson(Object object) throws IOException {
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
            builder.field("assignee", task.getAssignee());
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
                builder.field("assignee", task.getAssignee());
                builder.field("tags", task.getTags());
                builder.endObject();
            }
            builder.endArray();
        }
        builder.endObject();
        return builder.toString();
    }
}
