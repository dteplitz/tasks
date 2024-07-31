/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks;

import org.opensearch.client.node.NodeClient;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestRequest;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.opensearch.rest.RestRequest.Method.DELETE;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.rest.RestRequest.Method.PUT;

public class TasksController extends BaseRestHandler {
    private final TasksService tasksService;

    public TasksController(TasksService tasksService) {
        this.tasksService = tasksService;
    }

    @Override
    public String getName() {
        return "task-plugin";
    }

    @Override
    public List<Route> routes() {
        return List.of(
                new Route(POST, "/tasks"),
                new Route(GET, "/tasks/{id}"),
                new Route(PUT, "/tasks/{id}"),
                new Route(DELETE, "/tasks/{id}"),
                new Route(GET, "/tasks/search")
        );
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String id = request.param("id");
        switch (request.method()) {
            case POST:
                return channel -> {
                    Tasks task = parseRequestBody(request);
                    RestStatus status = tasksService.createTask(task);
                    channel.sendResponse(new BytesRestResponse(status, String.valueOf(XContentType.JSON), toJson(task)));
                };
            case GET:
                return channel -> {
                    if (id != null) {
                        Tasks task = tasksService.getTaskById(id);
                        if (task != null) {
                            channel.sendResponse(new BytesRestResponse(RestStatus.OK, String.valueOf(XContentType.JSON), toJson(task)));
                        } else {
                            channel.sendResponse(new BytesRestResponse(RestStatus.NOT_FOUND,"",toJson(null)));
                        }
                    } else {
                        String query = request.param("query");
                        List<Tasks> tasks = tasksService.searchTasks(query);
                        channel.sendResponse(new BytesRestResponse(RestStatus.OK, String.valueOf(XContentType.JSON), toJson(tasks)));
                    }
                };
            case PUT:
                return channel -> {
                    Tasks task = parseRequestBody(request);
                    RestStatus status = tasksService.updateTask(task);
                    channel.sendResponse(new BytesRestResponse(status, String.valueOf(XContentType.JSON), toJson(task)));
                };
            case DELETE:
                return channel -> {
                    RestStatus status = tasksService.deleteTask(id);
                    channel.sendResponse(new BytesRestResponse(status, String.valueOf(XContentType.JSON), toJson(null)));
                };
            default:
                return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, String.valueOf(XContentType.JSON), toJson(null)));
        }
    }

    private Tasks parseRequestBody(RestRequest request) throws IOException {
        return request.contentParser().mapOrdered().entrySet().stream()
                .collect(Tasks::new, (task, entry) -> {
                    switch (entry.getKey()) {
                        case "id":
                            task.setId(entry.getValue().toString());
                            break;
                        case "title":
                            task.setTitle(entry.getValue().toString());
                            break;
                        case "description":
                            task.setDescription(entry.getValue().toString());
                            break;
                        case "status":
                            task.setStatus(entry.getValue().toString());
                            break;
                        case "creationDate":
                            task.setCreationDate(entry.getValue().toString());
                            break;
                        case "completionDate":
                            task.setCompletionDate(entry.getValue().toString());
                            break;
                        case "assignee":
                            task.setAssignee(entry.getValue().toString());
                            break;
                        case "tags":
                            task.setTags((List<String>) entry.getValue());
                            break;
                    }
                }, (task1, task2) -> {
                });
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
