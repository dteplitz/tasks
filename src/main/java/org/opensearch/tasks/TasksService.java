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
import org.opensearch.core.rest.RestStatus;

import java.util.List;

public class TasksService {
    private final TasksRepository tasksRepository;
    private static final Logger log = LogManager.getLogger(TasksService.class);
    public TasksService(TasksRepository tasksRepository) {
        this.tasksRepository = tasksRepository;
    }

    public RestStatus createTask(Tasks tasks) {
        log.info("Creating task {}", tasks);
        RestStatus result = tasksRepository.createTask(tasks);
        log.info("Task creating result {}", result);
        return result;
    }

    public Tasks getTaskById(String id) {
        return tasksRepository.getTaskById(id);
    }

    public RestStatus deleteTask(String id) {
        return tasksRepository.deleteTask(id);
    }

    public List<Tasks> searchTasks(String query) {
        return tasksRepository.searchTasks(query);
    }

    public RestStatus updateTask(Tasks task) { tasksRepository.updateTask(task); return RestStatus.OK; }
}
