/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks.controller;

import org.opensearch.client.node.NodeClient;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.tasks.service.TaskService;

import java.io.IOException;
import java.util.List;

import static org.opensearch.rest.RestRequest.Method.DELETE;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.POST;
import static org.opensearch.rest.RestRequest.Method.PUT;

public class TaskController extends BaseRestHandler {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
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
                return null;
            case GET:
                return null;
            case PUT:
                return null;
            case DELETE:
                return null;
            default:
                return null;
        }
    }
}
