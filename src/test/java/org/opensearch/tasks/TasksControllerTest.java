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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensearch.client.node.NodeClient;

class TasksControllerTest extends LuceneTestCase {
    @Mock
    private TasksService tasksService;

    @Mock
    private NodeClient client;

    private TasksController tasksController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        tasksController = new TasksController(tasksService);
    }


}