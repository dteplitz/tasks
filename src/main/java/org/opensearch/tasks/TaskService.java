/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks;

import org.opensearch.core.rest.RestStatus;

import java.util.List;

public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public RestStatus createTask(Tasks tasks) {
        return taskRepository.createTask(tasks);
    }

    public Tasks getTaskById(String id) {
        return taskRepository.getTaskById(id);
    }

    public RestStatus deleteTask(String id) {
        return taskRepository.deleteTask(id);
    }

    public List<Tasks> searchTasks(String query) {
        return taskRepository.searchTasks(query);
    }

    public RestStatus updateTask(Tasks task) { taskRepository.updateTask(task); return RestStatus.OK; }
}
