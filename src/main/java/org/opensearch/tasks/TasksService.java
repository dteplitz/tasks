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
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<Tasks> searchTasks(Map<String, Object> body) {
        log.info("---------Searching tasks------------");
        List<Tasks> tasksList = tasksRepository.searchTasks(body);
        log.info("---------Tasks found {}------------",tasksList);
        log.info("---------Filtering tasks found with params------------");
        List<Tasks> tasksResponse = tasksList.stream()
                .filter(task -> body.containsKey("tagContains") ? task.getTags().contains(body.get("tagContains").toString()) : true)
                .filter(task -> body.containsKey("titleContains") ? task.getTitle().contains(body.get("titleContains").toString()) : true)
                .filter(task -> body.containsKey("descriptionContains") ? task.getDescription().contains(body.get("descriptionContains").toString()) : true)
                .collect(Collectors.toList());
        log.info("---------Tasks filtered {}------------",tasksResponse);
        return tasksResponse;
    }

    public RestStatus updateTask(Tasks task) { tasksRepository.updateTask(task); return RestStatus.OK; }
}
