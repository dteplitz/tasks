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

import static org.opensearch.rest.RestRequest.Method.DELETE;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.PATCH;
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
    //todo move files to folders insted everything in the same folder
    //todo specifications of apis

    @Override
    public String getName() {
        return "task-plugin";
    }

    @Override
    public List<Route> routes() {
        return List.of(
                new Route(GET, "/_plugins/tasks/{id}"),
                new Route(POST, "/_plugins/tasks/status/{status}"),
                new Route(POST, "/_plugins/tasks/assignee/{assignee}"),
                new Route(POST, "/_plugins/tasks/status/{status}/assignee/{assignee}"),
                new Route(POST, "/_plugins/tasks/search"),
                new Route(POST, "/_plugins/tasks"),
                new Route(PUT, "/_plugins/tasks/{id}"),
                new Route(PATCH, "/_plugins/tasks/{id}"),
                new Route(DELETE, "/_plugins/tasks/{id}")
        );
    }
/*
These users would like to be able to know:

- The tasks they have already done.
- The tasks they have left to do.
- When they have completed a task, or when they plan to execute it.

They would also like to be able to search for tasks, for example by the text they contain,
or by a list of tags. Each task can be in different states, such as planned, successfully
executed, or executed with error.

- Read the `Task` items.
- Create new `Task` items and persist them (in an OpenSearch index).
- Set `Task` items as completed.
- Delete `Task` items.
- Search `Task` items.
 */


    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        log.info("---------Starting prepareRequest {} -- {} -- {}--------------",request.param("id"), request.method(), request.params());
        switch (request.method()) {
            case POST:
                return channel -> {
                    postRequestHandle(request, channel);
                };
            case GET:
                log.info("---------Starting GET ------------");
                return channel -> {
                    getRequestHandle(request, channel);

                /*        log.info("---------Getting task with params------------");
                        Map<String,Object> body = request.contentParser().mapOrdered();
                        log.info("---------Params: {} ------------",body.toString());
                        CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> {
                            log.info("---------Searching tasks with params ------------");
                            List<Tasks> tasks = tasksService.searchTasks(body);
                            log.info("---------Tasks found {} ------------",tasks);
                            try {
                                log.info("--------- Returning tasks response ------------");
                                channel.sendResponse(new BytesRestResponse(RestStatus.OK, String.valueOf(XContentType.JSON), toJson(tasks)));
                                log.info("--------- Tasks response sent ------------");
                            } catch (IOException e) {
                                log.info("---------Send response failed with error {}------------",e.getMessage());
                                channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, String.valueOf(XContentType.JSON), e.getMessage()));
                                log.info("---------Send response failed. Returning Internal server error------------");
                            }
                            return RestStatus.OK;
                        });
                    }*/
                };
            case PUT:
                //test manuelly this
                return channel -> {
                    putRequestHandle(request, channel);
                };
            case DELETE:
                //test manuelly this
                return channel -> {
                    deleteRequestHandle(channel, request);
                };
            //todo add patch method
            default:
                return channel -> defaultRequestHandle(channel);
        }
    }

    private void defaultRequestHandle(RestChannel channel) throws IOException {
        channel.sendResponse(new BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, String.valueOf(XContentType.JSON), toJson(null)));
    }

    private void deleteRequestHandle(RestChannel channel, RestRequest request) throws IOException {
        //todo add validation of param id
        String id = request.param("id");
        if (id == null) {
            channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, "Task ID is missing"));
            return;
        }
        CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> tasksService.deleteTask(id), executor);
        log.info("Future created, waiting accept");
        future.thenAccept(status -> {
            log.info("Try future status");
            channel.sendResponse(new BytesRestResponse(status, String.valueOf(XContentType.JSON), id));
            log.info("Channel response sent");
        });
        future.exceptionally(ex -> {
            log.error("Error processing request", ex);
            channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
            return null;
        });
    }

    private void putRequestHandle(RestRequest request, RestChannel channel) throws IOException {
        Tasks task = parseRequestBody(request);
        if (task == null) {
            channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, "Invalid task data"));
            return;
        }
        //todo add validation of param id in service
        CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> tasksService.updateTask(task), executor);
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
    }

    private void getRequestHandle(RestRequest request, RestChannel channel) {
        log.info("---------Channel started ------------");
        //todo add validation of param id if exist
        String id = request.param("id");
        log.info("---------Getting task for id: {} ------------",id);
        CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> {
            log.info("---------Starting future get ------------");
            Tasks task = tasksService.getTaskById(id);
            log.info("---------Task is {} ------------",task);
            if (task != null) {
                try {
                    log.info("---------Task is not null. Returning task ------------");
                    channel.sendResponse(new BytesRestResponse(RestStatus.OK, String.valueOf(XContentType.JSON), toJson(task)));
                    log.info("---------Task returned ------------");
                } catch (IOException e) {
                    log.info("---------Send response failed with error {}------------",e.getMessage());
                    channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, String.valueOf(XContentType.JSON), e.getMessage()));
                    log.info("---------Send response failed. Returning Internal server error------------");
                }
            } else {
                try {
                    log.info("---------Task is null. Returning not found ------------");
                    channel.sendResponse(new BytesRestResponse(RestStatus.NOT_FOUND, String.valueOf(XContentType.JSON), toJson(null)));
                    log.info("---------Not found returned ------------");
                } catch (IOException e) {
                    log.info("---------Send response failed with error {}------------",e.getMessage());
                    channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, String.valueOf(XContentType.JSON), e.getMessage()));
                    log.info("---------Send response failed. Returning Internal server error------------");
                }
            }
            return RestStatus.OK;
        });
    }

    private void postRequestHandle(RestRequest request, RestChannel channel) throws IOException {
        //check for status and asignee
        //      get buy statusAsignee
        //check if search
        //      search with params
        //create
        Map<String,Object> body = request.contentParser().mapOrdered();
        log.info("---------Body of Post: {} ------------",body.toString());
        if(body.containsKey("title")){
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
        }
        else{
            log.info("---------Getting tasks with params------------");
            CompletableFuture<RestStatus> future = CompletableFuture.supplyAsync(() -> {
                log.info("---------Searching tasks with params ------------");
                List<Tasks> tasks = tasksService.searchTasks(body);
                log.info("---------Tasks found {} ------------",tasks);
                try {
                    log.info("--------- Returning tasks response ------------");
                    channel.sendResponse(new BytesRestResponse(RestStatus.OK, String.valueOf(XContentType.JSON), toJson(tasks)));
                    log.info("--------- Tasks response sent ------------");
                } catch (IOException e) {
                    log.info("---------Send response failed with error {}------------",e.getMessage());
                    channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, String.valueOf(XContentType.JSON), e.getMessage()));
                    log.info("---------Send response failed. Returning Internal server error------------");
                }
                return RestStatus.OK;
            });
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
