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
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.Client;
import org.opensearch.common.action.ActionFuture;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.tasks.model.Tasks;
import org.opensearch.tasks.repository.TasksRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TasksRepositoryTests extends LuceneTestCase {

    @Mock
    private Client client;

    @Mock
    private ActionFuture<GetResponse> actionFuture;
    @Mock
    private ActionFuture<SearchResponse> searchActionFuture;

    @Mock
    private SearchResponse searchResponse;
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
        MockitoAnnotations.openMocks(this);
        tasksRepository = new TasksRepository(client);
    }

    @Test
    public void testGetTaskById() {
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
        when(getResponse.getSourceAsMap()).thenReturn(sourceAsMap);

        // Call the method
        Tasks task = tasksRepository.getTaskById(taskId);

        // Verify and assert
        assertNotNull(task);
        assertEquals(taskId, task.getId());
        assertEquals("Sample Task", task.getTitle());
        assertEquals("This is a sample task", task.getDescription());
        assertEquals("open", task.getStatus());
        assertEquals("user1", task.getAssignee());
        assertEquals(Arrays.asList("tag1", "tag2"), task.getTags());
    }

    @Test
    public void testCreateTask() {
        // Create a sample task
        Tasks task = new Tasks();
        task.setId("1");
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
        RestStatus status = tasksRepository.createTask(task);

        // Verify and assert
        assertEquals(RestStatus.CREATED, status);
    }

    @Test
    public void testUpdateTask() {
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
        RestStatus status = tasksRepository.updateTask(task);

        // Verify and assert
        assertEquals(RestStatus.OK, status);
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

    /*@Test
    public void testSearchTasks() throws IOException {
        String query = "sample";

        // Create a sample task as a map
        Map<String, Object> sourceAsMap = new HashMap<>();
        sourceAsMap.put("id", "1");
        sourceAsMap.put("title", "Sample Task");
        sourceAsMap.put("description", "This is a sample task");
        sourceAsMap.put("status", "open");
        sourceAsMap.put("creationDate", (new Date()).toString());
        sourceAsMap.put("completionDate", null);
        sourceAsMap.put("assignee", "user1");
        sourceAsMap.put("tags", Arrays.asList("tag1", "tag2"));

        // Use XContentBuilder to create the source for the SearchHit
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        for (Map.Entry<String, Object> entry : sourceAsMap.entrySet()) {
            builder.field(entry.getKey(), entry.getValue());
        }
        builder.endObject();
        BytesReference bytes = BytesReference.bytes(builder);

        SearchHit searchHit = new SearchHit(1);
        searchHit.sourceRef(bytes);

        TotalHits totalHits = new TotalHits(1, TotalHits.Relation.EQUAL_TO);
        SearchHits searchHits = new SearchHits(new SearchHit[]{searchHit}, totalHits, 1.0f);
        when(searchResponse.getHits()).thenReturn(searchHits);

        // Mock the search response
        when(client.search(any(SearchRequest.class))).thenReturn(searchActionFuture);
        when(searchActionFuture.actionGet()).thenReturn(searchResponse);

        // Call the method
        List<Tasks> tasksList = tasksRepository.searchTasks(query);

        // Verify and assert
        assertEquals(1, tasksList.size());
        Tasks task = tasksList.get(0);
        assertEquals("1", task.getId());
        assertEquals("Sample Task", task.getTitle());
        assertEquals("This is a sample task", task.getDescription());
        assertEquals("open", task.getStatus());
        assertEquals("user1", task.getAssignee());
        assertEquals(Arrays.asList("tag1", "tag2"), task.getTags());
    }*/
}