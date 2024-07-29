/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks.service;

import org.opensearch.core.rest.RestStatus;
import org.opensearch.tasks.model.Task;
import org.opensearch.tasks.repository.TaskRepository;

import java.util.List;

public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public RestStatus createTask(Task task) {
        return taskRepository.createTask(task);
    }

    public Task getTaskById(String id) {
        return taskRepository.getTaskById(id);
    }

    public RestStatus deleteTask(String id) {
        return taskRepository.deleteTask(id);
    }

    public List<Task> searchTasks(String query) {
        return taskRepository.searchTasks(query);
    }
}
