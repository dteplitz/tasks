/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks;

import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.tasks.model.Tasks;
import org.opensearch.tasks.repository.TasksRepository;
import org.opensearch.tasks.service.TasksService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TasksServiceTests extends LuceneTestCase {

    @Mock
    private TasksRepository tasksRepository;

    @Mock
    private IndexResponse indexResponse;

    private TasksService tasksService;

    @BeforeEach
    public void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
        // Initialize the service
        tasksService = new TasksService(tasksRepository);
    }

    @Test
    void givenValidTask_whenCreatingTask_shouldReturnCreatedTask() {
        // Arrange
        Tasks task = new Tasks();
        task.setStatus("PLANNED");
        task.setCreationDate("2024-01-01");
        task.setCompletionDate("2024-12-31");

        when(tasksRepository.createTask(any(Tasks.class))).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.CREATED);
        when(indexResponse.getId()).thenReturn("1");

        // Act
        Tasks createdTask = tasksService.createTask(task);

        // Assert
        assertNotNull(createdTask);
        assertEquals("1", createdTask.getId());
        verify(tasksRepository, times(1)).createTask(task);
    }

    @Test
    void givenInvalidTaskStatus_whenCreatingTask_shouldReturnNull() {
        // Arrange
        Tasks task = new Tasks();
        task.setStatus("invalid-status");

        // Act
        Tasks createdTask = tasksService.createTask(task);

        // Assert
        assertNull(createdTask);
        verify(tasksRepository, never()).createTask(any(Tasks.class));
    }

    @Test
    void givenInvalidTaskDates_whenCreatingTask_shouldReturnNull() {
        // Arrange
        Tasks task = new Tasks();
        task.setStatus("PLANNED");
        task.setCreationDate("2024-01-01");
        task.setCompletionDate("2023-12-31"); // Invalid because completion date is before creation date

        // Act
        Tasks createdTask = tasksService.createTask(task);

        // Assert
        assertNull(createdTask);
    }

    @Test
    void givenInvalidDateFormat_whenCreatingTask_shouldReturnNull() {
        // Arrange
        Tasks task = new Tasks();
        task.setStatus("PLANNED");
        task.setCreationDate("2024-01-01");
        task.setCompletionDate("invalid-date"); // Invalid date format

        // Act
        Tasks createdTask = tasksService.createTask(task);

        // Assert
        assertNull(createdTask);
        verify(tasksRepository, never()).createTask(any(Tasks.class));
    }

    @Test
    void givenValidTask_whenRepositoryFailsToCreateTask_shouldReturnNull() {
        // Arrange
        Tasks task = new Tasks();
        task.setStatus("EXECUTED_OK");
        task.setCreationDate("2024-01-01");
        task.setCompletionDate("2024-12-31");

        when(tasksRepository.createTask(any(Tasks.class))).thenReturn(null); // Simulating null result from repository

        // Act
        Tasks createdTask = tasksService.createTask(task);

        // Assert
        assertNull(createdTask);
        verify(tasksRepository, times(1)).createTask(task);
    }

    @Test
    void givenNullTaskDates_whenCreatingTask_shouldReturnCreatedTask() {
        // Arrange
        Tasks task = new Tasks();
        task.setStatus("PLANNED");
        task.setCreationDate(null); // Null dates should be considered valid
        task.setCompletionDate(null);
        task.setPlannedDate(null);

        when(tasksRepository.createTask(any(Tasks.class))).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.CREATED);
        when(indexResponse.getId()).thenReturn("1");

        // Act
        Tasks createdTask = tasksService.createTask(task);

        // Assert
        assertNotNull(createdTask);
        assertEquals("1", createdTask.getId());
        verify(tasksRepository, times(1)).createTask(task);
    }

    @Test
    void givenValidId_whenGettingTaskById_shouldReturnTask() {
        // Arrange
        String taskId = "1";
        Tasks expectedTask = new Tasks();
        expectedTask.setId(taskId);

        when(tasksRepository.getTaskById(taskId)).thenReturn(expectedTask);

        // Act
        Tasks actualTask = tasksService.getTaskById(taskId);

        // Assert
        assertNotNull(actualTask);
        assertEquals(expectedTask, actualTask);
        verify(tasksRepository, times(1)).getTaskById(taskId);
    }

    @Test
    void givenNullId_whenGettingTaskById_shouldReturnNull() {
        // Act
        Tasks actualTask = tasksService.getTaskById(null);

        // Assert
        assertNull(actualTask);
        verify(tasksRepository, never()).getTaskById(anyString());
    }

    @Test
    void givenInvalidId_whenGettingTaskById_shouldReturnNull() {
        // Arrange
        String invalidTaskId = "non-existent-id";

        when(tasksRepository.getTaskById(invalidTaskId)).thenReturn(null);

        // Act
        Tasks actualTask = tasksService.getTaskById(invalidTaskId);

        // Assert
        assertNull(actualTask);
        verify(tasksRepository, times(1)).getTaskById(invalidTaskId);
    }

    @Test
    void givenValidId_whenDeletingTask_shouldReturnOkStatus() {
        // Arrange
        String taskId = "1";
        Tasks task = new Tasks();
        task.setId(taskId);

        when(tasksRepository.getTaskById(taskId)).thenReturn(task);
        when(tasksRepository.deleteTask(taskId)).thenReturn(RestStatus.OK);

        // Act
        RestStatus status = tasksService.deleteTask(taskId);

        // Assert
        assertEquals(RestStatus.OK, status);
        verify(tasksRepository, times(1)).getTaskById(taskId);
        verify(tasksRepository, times(1)).deleteTask(taskId);
    }

    @Test
    void givenNullId_whenDeletingTask_shouldReturnNotFoundStatus() {
        // Act
        RestStatus status = tasksService.deleteTask(null);

        // Assert
        assertEquals(RestStatus.NOT_FOUND, status);
        verify(tasksRepository, never()).getTaskById(anyString());
        verify(tasksRepository, never()).deleteTask(anyString());
    }

    @Test
    void givenValidId_whenTaskNotFound_shouldReturnNotFoundStatus() {
        // Arrange
        String taskId = "1";

        when(tasksRepository.getTaskById(taskId)).thenReturn(null);

        // Act
        RestStatus status = tasksService.deleteTask(taskId);

        // Assert
        assertEquals(RestStatus.NOT_FOUND, status);
        verify(tasksRepository, times(1)).getTaskById(taskId);
        verify(tasksRepository, never()).deleteTask(taskId);
    }

    @Test
    void givenValidId_whenRepositoryFailsToDeleteTask_shouldReturnInternalServerErrorStatus() {
        // Arrange
        String taskId = "1";
        Tasks task = new Tasks();
        task.setId(taskId);

        when(tasksRepository.getTaskById(taskId)).thenReturn(task);
        when(tasksRepository.deleteTask(taskId)).thenReturn(RestStatus.INTERNAL_SERVER_ERROR);

        // Act
        RestStatus status = tasksService.deleteTask(taskId);

        // Assert
        assertEquals(RestStatus.INTERNAL_SERVER_ERROR, status);
        verify(tasksRepository, times(1)).getTaskById(taskId);
        verify(tasksRepository, times(1)).deleteTask(taskId);
    }

    @Test
    void givenNoFilters_whenSearchingTasks_shouldReturnAllTasks() {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        List<Tasks> tasksList = createSampleTasksList();

        when(tasksRepository.searchTasks(body)).thenReturn(tasksList);

        // Act
        List<Tasks> result = tasksService.searchTasks(body);

        // Assert
        assertEquals(tasksList.size(), result.size());
        assertEquals(tasksList, result);
        verify(tasksRepository, times(1)).searchTasks(body);
    }

    @Test
    void givenContainsFilters_whenSearchingTasks_shouldReturnFilteredTasks() {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> containsFilters = new HashMap<>();
        containsFilters.put("title", "Task 1");
        body.put("contains", containsFilters);

        List<Tasks> tasksList = createSampleTasksList();
        when(tasksRepository.searchTasks(body)).thenReturn(tasksList);

        // Act
        List<Tasks> result = tasksService.searchTasks(body);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
        verify(tasksRepository, times(1)).searchTasks(body);
    }

    @Test
    void givenInvalidContainsFilters_whenSearchingTasks_shouldReturnEmptyList() {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> containsFilters = new HashMap<>();
        containsFilters.put("title", "Non-Existent Task");
        body.put("contains", containsFilters);

        List<Tasks> tasksList = createSampleTasksList();
        when(tasksRepository.searchTasks(body)).thenReturn(tasksList);

        // Act
        List<Tasks> result = tasksService.searchTasks(body);

        // Assert
        assertTrue(result.isEmpty());
        verify(tasksRepository, times(1)).searchTasks(body);
    }

    @Test
    void givenContainsTagFilters_whenSearchingTasks_shouldReturnFilteredTasks() {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> containsFilters = new HashMap<>();
        containsFilters.put("tags", Arrays.asList("tag1", "tag2"));
        body.put("contains", containsFilters);

        List<Tasks> tasksList = createSampleTasksList();
        when(tasksRepository.searchTasks(body)).thenReturn(tasksList);

        // Act
        List<Tasks> result = tasksService.searchTasks(body);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
        verify(tasksRepository, times(1)).searchTasks(body);
    }

    // Helper method to create a sample list of tasks
    private List<Tasks> createSampleTasksList() {
        Tasks task1 = new Tasks();
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setStatus("open");
        task1.setAssignee("user1");
        task1.setTags(Arrays.asList("tag1", "tag2"));

        Tasks task2 = new Tasks();
        task2.setTitle("Task 2");
        task2.setDescription("Description 2");
        task2.setStatus("closed");
        task2.setAssignee("user2");
        task2.setTags(Collections.singletonList("tag3"));

        return Arrays.asList(task1, task2);
    }

    @Test
    void givenNullId_whenPatchingTask_shouldReturnBadRequest() {
        // Arrange
        Tasks task = new Tasks();  // Task with null ID

        // Act
        RestStatus status = tasksService.patchTask(task);

        // Assert
        assertEquals(RestStatus.BAD_REQUEST, status);
        verify(tasksRepository, never()).getTaskById(anyString());
        verify(tasksRepository, never()).updateTask(any(Tasks.class));
    }

    @Test
    void givenInvalidStatus_whenPatchingTask_shouldReturnBadRequest() {
        // Arrange
        Tasks task = new Tasks();
        task.setId("1");
        task.setStatus("invalid-status");

        // Act
        RestStatus status = tasksService.patchTask(task);

        // Assert
        assertEquals(RestStatus.BAD_REQUEST, status);
        verify(tasksRepository, never()).getTaskById(anyString());
        verify(tasksRepository, never()).updateTask(any(Tasks.class));
    }

    @Test
    void givenInvalidDates_whenPatchingTask_shouldReturnBadRequest() {
        // Arrange
        Tasks task = new Tasks();
        task.setId("1");
        task.setCreationDate("invalid-date"); // Invalid date format
        task.setCompletionDate("2023-12-31"); // This date is logically valid but invalid format should trigger the checkValidTime method

        // Act
        RestStatus status = tasksService.patchTask(task);

        // Assert
        assertEquals(RestStatus.BAD_REQUEST, status);
        verify(tasksRepository, never()).getTaskById(anyString());
        verify(tasksRepository, never()).updateTask(any(Tasks.class));
    }

    @Test
    void givenNonExistentTask_whenPatchingTask_shouldReturnNotFound() {
        // Arrange
        Tasks task = new Tasks();
        task.setId("1");

        when(tasksRepository.getTaskById("1")).thenReturn(null);

        // Act
        RestStatus status = tasksService.patchTask(task);

        // Assert
        assertEquals(RestStatus.NOT_FOUND, status);
        verify(tasksRepository, times(1)).getTaskById("1");
        verify(tasksRepository, never()).updateTask(any(Tasks.class));
    }

    @Test
    void givenValidTask_whenRepositoryFailsToPatchTask_shouldReturnNotFound() {
        // Arrange
        Tasks task = new Tasks();
        task.setId("1");
        Tasks existingTask = new Tasks();
        existingTask.setId("1");

        when(tasksRepository.getTaskById("1")).thenReturn(existingTask);
        when(tasksRepository.updateTask(any(Tasks.class))).thenReturn(null);  // Simulating failure

        // Act
        RestStatus status = tasksService.patchTask(task);

        // Assert
        assertEquals(RestStatus.NOT_FOUND, status);
        verify(tasksRepository, times(1)).getTaskById("1");
        verify(tasksRepository, times(1)).updateTask(any(Tasks.class));
    }

    @Test
    void givenValidTask_whenPatchingTask_shouldReturnOkStatus() {
        // Arrange
        Tasks task = new Tasks();
        task.setId("1");
        task.setTitle("Updated Title");
        Tasks existingTask = new Tasks();
        existingTask.setId("1");

        when(tasksRepository.getTaskById("1")).thenReturn(existingTask);
        when(tasksRepository.updateTask(any(Tasks.class))).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.OK);

        // Act
        RestStatus status = tasksService.patchTask(task);

        // Assert
        assertEquals(RestStatus.OK, status);
        verify(tasksRepository, times(1)).getTaskById("1");
        verify(tasksRepository, times(1)).updateTask(any(Tasks.class));
    }
}