/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks;

import org.apache.lucene.search.TotalHits;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.common.action.ActionFuture;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.tasks.model.Tasks;
import org.opensearch.tasks.repository.TasksRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TasksRepositoryTests extends LuceneTestCase {

    @Mock
    private Client client;
    @Mock
    private SearchResponse searchResponse;
    @Mock
    private ActionFuture<SearchResponse> searchResponseActionFuture;


    @Mock
    private ActionFuture<GetResponse> actionFuture;
    @Mock
    private ActionFuture<IndexResponse> actionFutureIndex;
    @Mock
    private GetResponse getResponse;

    private TasksRepository tasksRepository;
    @Mock
    private ActionFuture<DeleteResponse> deleteActionFuture;

    @Mock
    private DeleteResponse deleteResponse;
    @Mock
    private IndexResponse indexResponse;

    @BeforeEach
    public void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
        // Initialize the repository
        tasksRepository = new TasksRepository(client);
    }

    private static final String TASK_ID = "1";
    private static final String TITLE = "Sample Task";
    private static final String DESCRIPTION = "This is a sample task";
    private static final String STATUS = "open";
    private static final String ASSIGNEE = "user1";
    private static final String PLANNED_DATE = "2024-01-01";
    private static final List<String> TAGS = Arrays.asList("tag1", "tag2");

    private static final String CREATION_DATE_FROM = "creationDateFrom";
    private static final String CREATION_DATE_TO = "creationDateTo";
    private static final String COMPLETION_DATE_FROM = "completionDateFrom";
    private static final String COMPLETION_DATE_TO = "completionDateTo";
    private static final String PLANNED_DATE_FROM = "plannedDateFrom";
    private static final String PLANNED_DATE_TO = "plannedDateTo";

    // Helper method to create a task map
    private Map<String, Object> createTaskMap(String taskId, String title, String description,
                                              String status, String assignee,
                                              String plannedDate, List<String> tags) {
        Map<String, Object> sourceAsMap = new HashMap<>();
        sourceAsMap.put("id", taskId);
        sourceAsMap.put("title", title);
        sourceAsMap.put("description", description);
        sourceAsMap.put("status", status);
        sourceAsMap.put("creationDate", null);
        sourceAsMap.put("completionDate", null);
        sourceAsMap.put("plannedDate", plannedDate);
        sourceAsMap.put("assignee", assignee);
        sourceAsMap.put("tags", tags);
        return sourceAsMap;
    }

    // Helper method to create a sample task
    private Tasks createSampleTask() {
        Tasks task = new Tasks();
        task.setTitle(TITLE);
        task.setDescription(DESCRIPTION);
        task.setStatus(STATUS);
        task.setCreationDate(null);
        task.setCompletionDate(null);
        task.setPlannedDate(PLANNED_DATE);
        task.setAssignee(ASSIGNEE);
        task.setTags(TAGS);
        return task;
    }

    @Test
    public void givenValidTaskId_whenGettingTask_shouldReturnTask() {
        Map<String, Object> sourceAsMap = createTaskMap(TASK_ID, TITLE, DESCRIPTION, STATUS, ASSIGNEE, PLANNED_DATE, TAGS);

        when(client.get(any())).thenReturn(actionFuture);
        when(actionFuture.actionGet()).thenReturn(getResponse);
        when(getResponse.getSourceAsMap()).thenReturn(sourceAsMap);
        when(getResponse.getId()).thenReturn(TASK_ID);

        // Call the method
        Tasks task = tasksRepository.getTaskById(TASK_ID);

        // Verify and assert
        assertNotNull(task);
        assertEquals(TASK_ID, task.getId());
        assertEquals(TITLE, task.getTitle());
        assertEquals(DESCRIPTION, task.getDescription());
        assertEquals(STATUS, task.getStatus());
        assertEquals(ASSIGNEE, task.getAssignee());
        assertEquals(PLANNED_DATE, task.getPlannedDate());
        assertEquals(TAGS, task.getTags());
    }

    @Test
    public void givenInvalidTaskId_whenGettingTask_shouldReturnNull() {
        String invalidTaskId = "non-existent-id";

        when(client.get(any())).thenReturn(actionFuture);
        when(actionFuture.actionGet()).thenReturn(getResponse);
        when(getResponse.getId()).thenReturn(null);

        // Call the method
        Tasks task = tasksRepository.getTaskById(invalidTaskId);

        // Verify and assert
        assertNull(task);
    }

    @Test
    public void givenNullTaskId_whenGettingTask_shouldReturnNull() {
        // Call the method
        Tasks task = tasksRepository.getTaskById(null);

        // Verify and assert
        assertNull(task);
    }

    @Test
    public void givenExceptionWhileGettingTask_whenGettingTask_shouldReturnNull() {
        when(client.get(any())).thenThrow(new RuntimeException("Simulated exception"));

        // Call the method
        Tasks task = tasksRepository.getTaskById(TASK_ID);

        // Verify and assert
        assertNull(task);
    }

    @Test
    public void givenTaskWithNullFields_whenGettingTask_shouldReturnTaskWithNullValues() {
        Map<String, Object> sourceAsMap = createTaskMap(TASK_ID, null, null, null, null, null, null);

        when(client.get(any())).thenReturn(actionFuture);
        when(actionFuture.actionGet()).thenReturn(getResponse);
        when(getResponse.getSourceAsMap()).thenReturn(sourceAsMap);
        when(getResponse.getId()).thenReturn(TASK_ID);

        // Call the method
        Tasks task = tasksRepository.getTaskById(TASK_ID);

        // Verify and assert
        assertNotNull(task);
        assertEquals(TASK_ID, task.getId());
        assertNull(task.getTitle());
        assertNull(task.getDescription());
        assertNull(task.getStatus());
        assertNull(task.getAssignee());
        assertNull(task.getPlannedDate());
        assertNull(task.getTags());
    }

    @Test
    public void givenValidTask_whenCreatingTask_shouldCreateTaskSuccessfully() {
        Tasks task = createSampleTask();

        // Mock the index response
        when(client.index(any(IndexRequest.class))).thenReturn(actionFutureIndex);
        when(actionFutureIndex.actionGet()).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.CREATED);

        // Call the method
        IndexResponse response = tasksRepository.createTask(task);

        // Verify and assert
        assertNotNull(response);
        assertEquals(RestStatus.CREATED, response.status());
    }

    @Test
    public void givenNullTask_whenCreatingTask_shouldReturnNull() {
        // Call the method with null task
        IndexResponse response = tasksRepository.createTask(null);

        // Verify and assert
        assertNull(response);
    }

    @Test
    public void givenTaskWithNullFields_whenCreatingTask_shouldCreateTaskSuccessfully() {
        Tasks task = new Tasks(); // All fields are null

        // Mock the index response
        when(client.index(any(IndexRequest.class))).thenReturn(actionFutureIndex);
        when(actionFutureIndex.actionGet()).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.CREATED);

        // Call the method
        IndexResponse response = tasksRepository.createTask(task);

        // Verify and assert
        assertNotNull(response);
        assertEquals(RestStatus.CREATED, response.status());
    }

    @Test
    public void givenExceptionWhileCreatingTask_whenCreatingTask_shouldReturnNull() {
        Tasks task = createSampleTask();

        when(client.index(any(IndexRequest.class))).thenThrow(new RuntimeException("Simulated exception"));

        // Call the method
        IndexResponse response = tasksRepository.createTask(task);

        // Verify and assert
        assertNull(response);
    }

    @Test
    public void givenValidTask_whenUpdatingTask_shouldUpdateTaskSuccessfully() {
        Tasks task = new Tasks();
        task.setId(TASK_ID);
        task.setTitle("Updated Task");
        task.setDescription("This is an updated task");
        task.setStatus("in-progress");
        task.setCreationDate("2024-07-31T03:08:32.299Z");
        task.setCompletionDate(null);
        task.setPlannedDate(PLANNED_DATE);
        task.setAssignee("user2");
        task.setTags(Arrays.asList("tag3", "tag4"));

        // Mock the index response
        when(client.index(any(IndexRequest.class))).thenReturn(actionFutureIndex);
        when(actionFutureIndex.actionGet()).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.OK);

        // Call the method
        IndexResponse response = tasksRepository.updateTask(task);

        // Verify and assert
        assertNotNull(response);
        assertEquals(RestStatus.OK, response.status());
    }

    @Test
    public void givenNullTask_whenUpdatingTask_shouldReturnNull() {
        // Call the method with null task
        IndexResponse response = tasksRepository.updateTask(null);

        // Verify and assert
        assertNull(response);
    }

    @Test
    public void givenTaskWithNullFields_whenUpdatingTask_shouldUpdateTaskSuccessfully() {
        Tasks task = new Tasks();
        task.setId(TASK_ID);  // Only ID is set; other fields are null

        // Mock the index response
        when(client.index(any(IndexRequest.class))).thenReturn(actionFutureIndex);
        when(actionFutureIndex.actionGet()).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.OK);

        // Call the method
        IndexResponse response = tasksRepository.updateTask(task);

        // Verify and assert
        assertNotNull(response);
        assertEquals(RestStatus.OK, response.status());
    }

    @Test
    public void givenValidTask_whenExceptionOccursWhileUpdatingTask_shouldReturnNull() {
        Tasks task = createSampleTask();
        task.setId(TASK_ID);

        when(client.index(any(IndexRequest.class))).thenThrow(new RuntimeException("Simulated exception"));

        // Call the method
        IndexResponse response = tasksRepository.updateTask(task);

        // Verify and assert
        assertNull(response);
    }

    @Test
    public void givenTaskWithInvalidId_whenUpdatingTask_shouldReturnNotFoundStatus() {
        Tasks task = createSampleTask();
        task.setId("invalid-id");

        when(client.index(any(IndexRequest.class))).thenReturn(actionFutureIndex);
        when(actionFutureIndex.actionGet()).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.NOT_FOUND);

        // Call the method
        IndexResponse response = tasksRepository.updateTask(task);

        // Verify and assert
        assertNotNull(response);
        assertEquals(RestStatus.NOT_FOUND, response.status());
    }

    @Test
    public void givenValidTaskId_whenDeletingTask_shouldReturnOkStatus() {
        // Mock the delete response
        when(client.delete(any(DeleteRequest.class))).thenReturn(deleteActionFuture);
        when(deleteActionFuture.actionGet()).thenReturn(deleteResponse);
        when(deleteResponse.status()).thenReturn(RestStatus.OK);

        // Call the method
        RestStatus status = tasksRepository.deleteTask(TASK_ID);

        // Verify and assert
        assertNotNull(status);
        assertEquals(RestStatus.OK, status);
    }

    @Test
    public void givenInvalidTaskId_whenDeletingTask_shouldReturnNotFoundStatus() {
        // Mock the delete response
        when(client.delete(any(DeleteRequest.class))).thenReturn(deleteActionFuture);
        when(deleteActionFuture.actionGet()).thenReturn(deleteResponse);
        when(deleteResponse.status()).thenReturn(RestStatus.NOT_FOUND);

        // Call the method
        RestStatus status = tasksRepository.deleteTask("invalid-id");

        // Verify and assert
        assertNotNull(status);
        assertEquals(RestStatus.NOT_FOUND, status);
    }

    @Test
    public void givenNullTaskId_whenDeletingTask_shouldReturnBadRequest() {
        // Call the method with null ID
        RestStatus status = tasksRepository.deleteTask(null);

        // Verify and assert
        assertNotNull(status);
        assertEquals(RestStatus.BAD_REQUEST, status);
    }

    @Test
    public void givenValidTaskId_whenExceptionOccursWhileDeletingTask_shouldReturnBadRequest() {
        when(client.delete(any(DeleteRequest.class))).thenThrow(new RuntimeException("Simulated exception"));

        // Call the method
        RestStatus status = tasksRepository.deleteTask(TASK_ID);

        // Verify and assert
        assertNotNull(status);
        assertEquals(RestStatus.BAD_REQUEST, status);
    }

    @Test
    void givenDateFilters_whenSearchingTasks_shouldReturnMatchingTasks() throws Exception {
        // Prepare test data
        Map<String, Object> body = new HashMap<>();
        body.put(CREATION_DATE_FROM, "2023-01-01");
        body.put(CREATION_DATE_TO, "2023-12-31");

        // Build the JSON body
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            builder.field(entry.getKey(), entry.getValue());
        }
        builder.endObject();
        BytesReference bytes = BytesReference.bytes(builder);

        // Mock the search hit
        SearchHit searchHit = new SearchHit(1);
        searchHit.sourceRef(bytes);

        // Mock the search hits
        TotalHits totalHits = new TotalHits(1, TotalHits.Relation.EQUAL_TO);
        SearchHits searchHits = new SearchHits(new SearchHit[]{searchHit}, totalHits, 1.0f);

        // Mock the client search response
        when(client.search(any(SearchRequest.class))).thenReturn(searchResponseActionFuture);
        when(searchResponseActionFuture.actionGet()).thenReturn(searchResponse);
        when(searchResponse.getHits()).thenReturn(searchHits);

        // Execute the search
        List<Tasks> result = tasksRepository.searchTasks(body);

        // Verify the interactions and assertions
        verify(client, times(1)).search(any(SearchRequest.class));
        assertEquals(1, result.size());
    }

    @Test
    void givenEmptyDateFilters_whenSearchingTasks_shouldReturnAllTasks() throws Exception {
        // Prepare empty filter data
        Map<String, Object> body = new HashMap<>();

        // Create an empty array for SearchHit
        SearchHit[] emptyHitsArray = new SearchHit[0];

        // Mock the search hits as an empty result
        SearchHits searchHits = new SearchHits(emptyHitsArray, new TotalHits(0, TotalHits.Relation.EQUAL_TO), 1.0f);


        // Mock the client search response
        when(client.search(any(SearchRequest.class))).thenReturn(searchResponseActionFuture);
        when(searchResponseActionFuture.actionGet()).thenReturn(searchResponse);
        when(searchResponse.getHits()).thenReturn(searchHits);

        // Execute the search
        List<Tasks> result = tasksRepository.searchTasks(body);

        // Verify the interactions and assertions
        verify(client, times(1)).search(any(SearchRequest.class));
        assertEquals(0, result.size());
    }

    @Test
    void givenInvalidDateFilters_whenSearchingTasks_shouldReturnEmptyResult() throws Exception {
        // Prepare invalid date filters
        Map<String, Object> body = new HashMap<>();
        body.put(CREATION_DATE_FROM, "invalid-date");

// Create an empty array for SearchHit
        SearchHit[] emptyHitsArray = new SearchHit[0];

        // Mock the search hits as an empty result
        SearchHits searchHits = new SearchHits(emptyHitsArray, new TotalHits(0, TotalHits.Relation.EQUAL_TO), 1.0f);

        // Mock the client search response
        when(client.search(any(SearchRequest.class))).thenReturn(searchResponseActionFuture);
        when(searchResponseActionFuture.actionGet()).thenReturn(searchResponse);
        when(searchResponse.getHits()).thenReturn(searchHits);

        // Execute the search
        List<Tasks> result = tasksRepository.searchTasks(body);

        // Verify the interactions and assertions
        verify(client, times(1)).search(any(SearchRequest.class));
        assertEquals(0, result.size());
    }


}