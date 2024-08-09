/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.tasks.model.Tasks;
import org.opensearch.tasks.repository.TasksRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TasksService {
    private final TasksRepository tasksRepository;
    private static final Logger log = LogManager.getLogger(TasksService.class);

    public TasksService(TasksRepository tasksRepository) {
        this.tasksRepository = tasksRepository;
    }

    public Tasks createTask(Tasks tasks) {
        log.info("Creating task {}", tasks);
        IndexResponse result = tasksRepository.createTask(tasks);
        if (result.status() == RestStatus.CREATED) {
            tasks.setId(result.getId());
            log.info("Task creating result {}", tasks);
            return tasks;
        }
        return null;

    }

    public Tasks getTaskById(String id) {
        log.info("Getting task by id {}", id);
        if (id == null) {
            log.info("Id was null, returning null");
            return null;
        }
        Tasks tasks = tasksRepository.getTaskById(id);
        log.info("Task retrieved {}", tasks);
        return tasks;
    }

    public RestStatus deleteTask(String id) {
        log.info("Deleting task by id {}", id);
        if (id == null) {
            log.info("Id was null, returning NOT_FOUND");
            return RestStatus.NOT_FOUND;
        }
        log.info("Searching task to delete");
        Tasks task = tasksRepository.getTaskById(id);
        if (task != null) {
            log.info("Task found, deleting {}", task);
            RestStatus status = tasksRepository.deleteTask(id);
            log.info("Delete task process with status {}", status);
            return status;
        }
        log.info("Task to delete not found");
        return RestStatus.NOT_FOUND;
    }

    public List<Tasks> searchTasks(Map<String, Object> body) {
        log.info("Searching tasks");
        List<Tasks> tasksList = tasksRepository.searchTasks(body);
        log.info("Tasks found {}", tasksList);
        log.info("Filtering tasks found with search parameters");
        List<Tasks> tasksResponse = filterTasksByContains(body, tasksList);
        log.info("Tasks filtered {}", tasksResponse);
        return tasksResponse;
    }

    public RestStatus updateTask(Tasks task) {
        log.info("Updating task {}", task);
        if (task.getId() == null) {
            log.info("Id cannot be null to update {}", task.getId());
            return RestStatus.BAD_REQUEST;
        }
        Tasks taskToUpdate = tasksRepository.getTaskById(task.getId());
        if (taskToUpdate == null) {
            log.info("Task not found to update {}", task.getId());
            return RestStatus.NOT_FOUND;
        }
        IndexResponse taskUpdated = tasksRepository.updateTask(task);
        log.info("Update task process with status {}", taskUpdated);
        return taskUpdated.status();
    }

    private static List<Tasks> filterTasksByContains(Map<String, Object> body, List<Tasks> tasksList) {
        if (!body.containsKey("contains")) {
            return tasksList;
        }
        Map<String, Object> contains = (Map<String, Object>) body.get("contains");
        @SuppressWarnings("unchecked")
        List<String> tagsToFilter = (List<String>) contains.get("tags");
        return tasksList.stream()
                .filter(task ->
                        (!contains.containsKey("title") || task.getTitle().contains(contains.get("title").toString())) &&
                                (!contains.containsKey("description") || task.getDescription().contains(contains.get("description").toString())) &&
                                (!contains.containsKey("status") || task.getStatus().contains(contains.get("status").toString())) &&
                                (!contains.containsKey("assignee") || task.getAssignee().contains(contains.get("assignee").toString())) &&
                                (tagsToFilter == null || task.getTags().stream().allMatch(tagsToFilter::contains))
                ).collect(Collectors.toList());
    }


}
