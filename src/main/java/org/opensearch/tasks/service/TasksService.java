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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TasksService {
    private final TasksRepository tasksRepository;
    private static final Logger log = LogManager.getLogger(TasksService.class);

    private static final String DATE_PATTERN = "^(\\d{4})-(\\d{2})-(\\d{2})$";
    private static final Pattern pattern = Pattern.compile(DATE_PATTERN);

    public TasksService(TasksRepository tasksRepository) {
        this.tasksRepository = tasksRepository;
    }

    /**
     * Creates a new task if the task status and dates are valid.
     *
     * @param tasks The task to be created.
     * @return The created task with its ID set, or null if the task is invalid.
     */
    public Tasks createTask(Tasks tasks) {
        if (!TaskStatus.isValidStatus(tasks.getStatus())) {
            log.info("Invalid task status: {}", tasks.getStatus());
            return null;
        }
        if (!checkValidDates(tasks)) {
            log.info("Invalid task dates");
            return null;
        }
        log.info("Creating task: {}", tasks);
        IndexResponse result = tasksRepository.createTask(tasks);
        if (result != null && result.status() == RestStatus.CREATED) {
            tasks.setId(result.getId());
            log.info("Task created successfully: {}", tasks);
            return tasks;
        }
        log.info("Task creation failed");
        return null;
    }

    /**
     * Retrieves a task by its ID.
     *
     * @param id The ID of the task to retrieve.
     * @return The task if found, or null if not.
     */
    public Tasks getTaskById(String id) {
        log.info("Retrieving task by ID: {}", id);
        if (id == null) {
            log.info("Task ID is null");
            return null;
        }
        Tasks task = tasksRepository.getTaskById(id);
        log.info("Task retrieved: {}", task);
        return task;
    }

    /**
     * Deletes a task by its ID.
     *
     * @param id The ID of the task to delete.
     * @return The status of the delete operation.
     */
    public RestStatus deleteTask(String id) {
        log.info("Deleting task by ID: {}", id);
        if (id == null) {
            log.info("Task ID is null, cannot delete");
            return RestStatus.NOT_FOUND;
        }
        Tasks task = tasksRepository.getTaskById(id);
        if (task != null) {
            log.info("Task found, proceeding to delete: {}", task);
            RestStatus status = tasksRepository.deleteTask(id);
            log.info("Task deletion status: {}", status);
            return status;
        }
        log.info("Task not found, cannot delete");
        return RestStatus.NOT_FOUND;
    }

    /**
     * Searches for tasks based on the provided criteria.
     *
     * @param body The search criteria as a map.
     * @return A list of tasks matching the search criteria.
     */
    public List<Tasks> searchTasks(Map<String, Object> body) {
        log.info("Searching tasks with criteria: {}", body);
        List<Tasks> tasksList = tasksRepository.searchTasks(body);
        log.info("Tasks found: {}", tasksList);
        return filterTasksByContains(body, tasksList);
    }

    /**
     * Updates a task if it exists and the status and dates are valid.
     *
     * @param task The task to update.
     * @return The status of the update operation.
     */
    public RestStatus updateTask(Tasks task) {
        log.info("Updating task: {}", task);
        if (task.getId() == null) {
            log.info("Task ID is null, cannot update");
            return RestStatus.BAD_REQUEST;
        }
        if (!TaskStatus.isValidStatus(task.getStatus())) {
            log.info("Invalid task status: {}", task.getStatus());
            return RestStatus.BAD_REQUEST;
        }
        if (!checkValidDates(task)) {
            log.info("Invalid task dates");
            return RestStatus.BAD_REQUEST;
        }
        Tasks existingTask = tasksRepository.getTaskById(task.getId());
        if (existingTask == null) {
            log.info("Task not found, cannot update: {}", task.getId());
            return RestStatus.NOT_FOUND;
        }
        IndexResponse updateResponse = tasksRepository.updateTask(task);
        if (updateResponse == null) {
            log.info("Task update failed, task not found");
            return RestStatus.NOT_FOUND;
        }
        log.info("Task updated successfully with status: {}", updateResponse.status());
        return updateResponse.status();
    }

    /**
     * Partially updates a task by patching the provided fields.
     *
     * @param task The task with fields to patch.
     * @return The status of the patch operation.
     */
    public RestStatus patchTask(Tasks task) {
        log.info("Patching task: {}", task);
        if (task.getId() == null) {
            log.info("Task ID is null, cannot patch");
            return RestStatus.BAD_REQUEST;
        }
        if (task.getStatus() != null && !TaskStatus.isValidStatus(task.getStatus())) {
            log.info("Invalid task status: {}", task.getStatus());
            return RestStatus.BAD_REQUEST;
        }
        if (!checkValidDates(task)) {
            log.info("Invalid task dates");
            return RestStatus.BAD_REQUEST;
        }
        Tasks existingTask = tasksRepository.getTaskById(task.getId());
        if (existingTask == null) {
            log.info("Task not found, cannot patch: {}", task.getId());
            return RestStatus.NOT_FOUND;
        }
        updateTaskFields(existingTask, task);
        IndexResponse patchResponse = tasksRepository.updateTask(existingTask);
        if (patchResponse == null) {
            log.info("Task patch failed, task not found");
            return RestStatus.NOT_FOUND;
        }
        log.info("Task patched successfully with status: {}", patchResponse.status());
        return patchResponse.status();
    }

    /**
     * Validates the date fields of a task.
     *
     * @param tasks The task whose dates are to be validated.
     * @return True if all date fields are valid, otherwise false.
     */
    private boolean checkValidDates(Tasks tasks) {
        return isValidDate(tasks.getCompletionDate()) &&
                isValidDate(tasks.getCreationDate()) &&
                isValidDate(tasks.getPlannedDate());
    }

    /**
     * Filters tasks based on the 'contains' criteria in the search body.
     *
     * @param body      The search criteria.
     * @param tasksList The list of tasks to filter.
     * @return A list of tasks that match the 'contains' criteria.
     */
    private static List<Tasks> filterTasksByContains(Map<String, Object> body, List<Tasks> tasksList) {
        if (!body.containsKey("contains")) {
            return tasksList;
        }
        Map<String, Object> contains = (Map<String, Object>) body.get("contains");
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

    /**
     * Updates the fields of the existing task with the fields from the new task.
     *
     * @param existingTask The existing task to update.
     * @param newTask      The new task with fields to update.
     */
    private void updateTaskFields(Tasks existingTask, Tasks newTask) {
        if (newTask.getTitle() != null) {
            existingTask.setTitle(newTask.getTitle());
        }
        if (newTask.getDescription() != null) {
            existingTask.setDescription(newTask.getDescription());
        }
        if (newTask.getStatus() != null) {
            existingTask.setStatus(newTask.getStatus());
        }
        if (newTask.getAssignee() != null) {
            existingTask.setAssignee(newTask.getAssignee());
        }
        if (newTask.getCreationDate() != null) {
            existingTask.setCreationDate(newTask.getCreationDate());
        }
        if (newTask.getCompletionDate() != null) {
            existingTask.setCompletionDate(newTask.getCompletionDate());
        }
        if (newTask.getPlannedDate() != null) {
            existingTask.setPlannedDate(newTask.getPlannedDate());
        }
        if (newTask.getTags() != null && !newTask.getTags().isEmpty()) {
            existingTask.setTags(newTask.getTags());
        }
    }

    /**
     * Validates a date string against the required pattern.
     *
     * @param date The date string to validate.
     * @return True if the date is valid or null, otherwise false.
     */
    public static boolean isValidDate(String date) {
        if (date == null) {
            return true; // Allow null dates
        }
        Matcher matcher = pattern.matcher(date);
        return matcher.matches();
    }

    /**
     * Enum representing the possible task statuses.
     */
    public enum TaskStatus {
        PLANNED,
        EXECUTED_OK,
        EXECUTED_ERROR;

        /**
         * Checks if the given status is valid.
         *
         * @param status The status to check.
         * @return True if the status is valid, otherwise false.
         */
        public static boolean isValidStatus(String status) {
            for (TaskStatus ts : TaskStatus.values()) {
                if (ts.name().equalsIgnoreCase(status)) {
                    return true;
                }
            }
            return false;
        }
    }
}
