/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks.service;

import org.opensearch.action.search.SearchResponse;
import org.opensearch.tasks.model.Task;
import org.opensearch.tasks.repository.TaskRepository;

import java.io.IOException;
import java.util.Optional;

public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void createTask(Task task) throws IOException {
        taskRepository.createTask(task);
    }

    public Optional<Task> getTask(String id) throws IOException {
        return Optional.ofNullable(taskRepository.getTask(id));
    }

    public void deleteTask(String id) {
        taskRepository.deleteTask(id);
    }

    public SearchResponse searchTasks(String query) {
        return taskRepository.searchTasks(query);
    }
}
