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
import org.opensearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.opensearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.AdminClient;
import org.opensearch.client.Client;
import org.opensearch.client.IndicesAdminClient;
import org.opensearch.common.action.ActionFuture;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.tasks.model.Tasks;
import org.opensearch.tasks.repository.TasksRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opensearch.action.update.UpdateHelper.ContextFields.INDEX;

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

    @Test
    public void testGetTaskById_returnTask() {
        String taskId = "1";

        // Mock the response map
        Map<String, Object> sourceAsMap = new HashMap<>();
        sourceAsMap.put("id", taskId);
        sourceAsMap.put("title", "Sample Task");
        sourceAsMap.put("description", "This is a sample task");
        sourceAsMap.put("status", "open");
        sourceAsMap.put("creationDate", null);
        sourceAsMap.put("completionDate", null);
        sourceAsMap.put("plannedDate", "2024-01-01");
        sourceAsMap.put("assignee", "user1");
        sourceAsMap.put("tags", Arrays.asList("tag1", "tag2"));

        when(client.get(any())).thenReturn(actionFuture);
        when(actionFuture.actionGet()).thenReturn(getResponse);
        when(getResponse.getSourceAsMap()).thenReturn(sourceAsMap);
        when(getResponse.getId()).thenReturn(taskId);

        // Call the method
        Tasks task = tasksRepository.getTaskById(taskId);

        // Verify and assert
        assertNotNull(task);
        //assertEquals(taskId, task.getId());
        assertEquals("Sample Task", task.getTitle());
        assertEquals("This is a sample task", task.getDescription());
        assertEquals("open", task.getStatus());
        assertEquals("user1", task.getAssignee());
        assertEquals("user1", task.getAssignee());
        assertEquals("2024-01-01", task.getPlannedDate());
        assertEquals(Arrays.asList("tag1", "tag2"), task.getTags());
    }

    @Test
    public void testGetTaskById_returnNull() {
        String taskId = "1";

        // Mock the response map
        Map<String, Object> sourceAsMap = new HashMap<>();
        sourceAsMap.put("id", taskId);
        sourceAsMap.put("title", "Sample Task");
        sourceAsMap.put("description", "This is a sample task");
        sourceAsMap.put("status", "open");
        sourceAsMap.put("creationDate", null);
        sourceAsMap.put("completionDate", null);
        sourceAsMap.put("assignee", "user1");
        sourceAsMap.put("tags", Arrays.asList("tag1", "tag2"));

        when(client.get(any())).thenReturn(actionFuture);
        when(actionFuture.actionGet()).thenReturn(getResponse);
        when(getResponse.getSourceAsMap()).thenReturn(null);
        when(getResponse.getId()).thenReturn(null);

        // Call the method
        Tasks task = tasksRepository.getTaskById(taskId);

        // Verify and assert
        assertNull(task);

    }

    @Test
    public void testCreateTask() throws Exception {
        // Create a sample task
        Tasks task = new Tasks();
        task.setTitle("Sample Task");
        task.setDescription("This is a sample task");
        task.setStatus("open");
        task.setCreationDate(null);
        task.setCompletionDate(null);
        task.setAssignee("user1");
        task.setTags(Arrays.asList("tag1", "tag2"));

        // Mock the index response
        when(client.index(any(IndexRequest.class))).thenReturn(actionFutureIndex);
        when(actionFutureIndex.actionGet()).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.CREATED);

        // Call the method
        IndexResponse response = tasksRepository.createTask(task);

        // Verify and assert
        assertEquals(RestStatus.CREATED, response.status());
    }

    @Test
    public void testUpdateTask_returnOk() {
        // Create a sample task
        Tasks task = new Tasks();
        task.setId("1");
        task.setTitle("Updated Task");
        task.setDescription("This is an updated task");
        task.setStatus("in-progress");
        task.setCreationDate("2024-07-31T03:08:32.299Z");
        task.setCompletionDate(null);
        task.setAssignee("user2");
        task.setTags(Arrays.asList("tag3", "tag4"));

        // Mock the index response
        when(client.index(any(IndexRequest.class))).thenReturn(actionFutureIndex);
        when(actionFutureIndex.actionGet()).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.OK);

        // Call the method
        IndexResponse indexResponse = tasksRepository.updateTask(task);

        // Verify and assert
        assertEquals(RestStatus.OK, indexResponse.status());
    }

    @Test
    public void testUpdateTask_returnError() {
        // Create a sample task
        Tasks task = new Tasks();
        task.setId("1");
        task.setTitle("Updated Task");
        task.setDescription("This is an updated task");
        task.setStatus("in-progress");
        task.setCreationDate("2024-07-31T03:08:32.299Z");
        task.setCompletionDate(null);
        task.setAssignee("user2");
        task.setTags(Arrays.asList("tag3", "tag4"));

        // Mock the index response
        when(client.index(any(IndexRequest.class))).thenReturn(actionFutureIndex);
        when(actionFutureIndex.actionGet()).thenReturn(indexResponse);
        when(indexResponse.status()).thenReturn(RestStatus.INTERNAL_SERVER_ERROR);

        // Call the method
        IndexResponse indexResponse = tasksRepository.updateTask(task);

        // Verify and assert
        assertEquals(RestStatus.INTERNAL_SERVER_ERROR, indexResponse.status());
    }

    @Test
    public void testDeleteTask() {
        // Mock the delete response
        when(client.delete(any(DeleteRequest.class))).thenReturn(deleteActionFuture);
        when(deleteActionFuture.actionGet()).thenReturn(deleteResponse);
        when(deleteResponse.status()).thenReturn(RestStatus.OK);

        // Call the method
        RestStatus status = tasksRepository.deleteTask("1");

        // Verify and assert
        assertEquals(RestStatus.OK, status);
    }

    @Test
    void testSearchTasks_withDateFilters() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("creationDateFrom", "2023-01-01");
        body.put("creationDateTo", "2023-12-31");

        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            builder.field(entry.getKey(), entry.getValue());
        }
        builder.endObject();
        BytesReference bytes = BytesReference.bytes(builder);

        SearchHit searchHit = new SearchHit(1);
        searchHit.sourceRef(bytes);

        TotalHits totalHits = new TotalHits(1, TotalHits.Relation.EQUAL_TO);
        SearchHits searchHits = new SearchHits(new SearchHit[]{searchHit}, totalHits, 1.0f);


        when(client.search(any(SearchRequest.class))).thenReturn(searchResponseActionFuture);
        when(searchResponseActionFuture.actionGet()).thenReturn(searchResponse);
        when(searchResponse.getHits()).thenReturn(searchHits);

        List<Tasks> result = tasksRepository.searchTasks(body);

        verify(client, times(1)).search(any(SearchRequest.class));
        assertEquals(1, result.size());
    }
    /*@Test
    void testSearchTasks_withEqualsFilters() throws Exception {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> equals = new HashMap<>();
        equals.put("statusIs", "completed");
        equals.put("assigneeIs", "user1");
        body.put("equals", equals);

        SearchHit[] hits = new SearchHit[]{searchHit};
        SearchHits searchHits = new SearchHits(hits, new TotalHits(1,TotalHits.Relation.EQUAL_TO), 1.0f);

        when(client.search(any(SearchRequest.class))).thenReturn(searchResponseActionFuture);
        when(searchResponse.getHits()).thenReturn(searchHits);
        when(searchHit.getSourceAsMap()).thenReturn(new HashMap<>());
        when(searchHit.getId()).thenReturn("1");

        List<Tasks> result = tasksRepository.searchTasks(body);

        verify(client, times(1)).search(any(SearchRequest.class));
        assertEquals(1, result.size());
    }

    @Test
    void testSearchTasks_withNoFilters() throws Exception {
        Map<String, Object> body = new HashMap<>();

        SearchHit[] hits = new SearchHit[]{searchHit};
        SearchHits searchHits = new SearchHits(hits, new TotalHits(1,TotalHits.Relation.EQUAL_TO), 1.0f);

        when(client.search(any(SearchRequest.class))).thenReturn(searchResponseActionFuture);
        when(searchResponse.getHits()).thenReturn(searchHits);
        when(searchHit.getSourceAsMap()).thenReturn(new HashMap<>());
        when(searchHit.getId()).thenReturn("1");

        List<Tasks> result = tasksRepository.searchTasks(body);

        verify(client, times(1)).search(any(SearchRequest.class));
        assertEquals(1, result.size());
    }*/


    //todo create tests for other methods and basic errors
}