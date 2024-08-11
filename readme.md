# OpenSearch Tasks Plugin

## Overview
The **Tasks Plugin for OpenSearch** is crafted to manage task-related information efficiently, enabling users to create, update, retrieve, and delete tasks. This plugin provides a REST API that interfaces with OpenSearch indices to persist and retrieve task data. It is particularly useful for security professionals using Wazuh to track tasks related to security processes and standards.

## Architecture
The plugin adopts a layered architecture based on the **MVC (Model-View-Controller)** pattern, tailored for an OpenSearch plugin:

- **Controller Layer:** Manages HTTP request handling.
- **Service Layer:** Contains business logic.
- **Repository Layer:** Manages interactions with the OpenSearch index.
- **Model Layer:** Defines the data structures used in the application.

Each layer is decoupled to ensure a clear separation of concerns, enhancing code maintainability and testability.

## Key Components
- **TasksController:** Handles HTTP requests and responses, routing them to appropriate service methods.
- **TasksService:** Contains the business logic for managing tasks, including creating, updating, retrieving, and deleting tasks.
- **TasksRepository:** Manages CRUD operations with the OpenSearch index.
- **Tasks:** Defines the task entity model.

## Features
- **Create Task:** Users can create a new task with attributes like title, description, status, creation date, completion date, planned date, assignee, and tags. All dates must be in the yyyy-MM-dd format. The status must be one of the following: PLANNED, EXECUTED_OK, EXECUTED_ERROR.
- **Retrieve Task:** Users can retrieve a task by its ID.
- **Update Task:** Users can update the attributes of an existing task. The same validation rules for dates and status apply as when creating a task.
- **Patch Task:** Users can patch the attributes of an existing task. The same validation rules for dates and status apply as when creating a task.
- **Delete Task:** Users can delete a task by its ID.
- **Search Tasks:** Provides functionality to search for tasks based on the following optional parameters:

    ```json
    {
      "creationDateTo": "2023-12-31",
      "creationDateFrom": "2023-01-01",
      "completionDateFrom": "",
      "completionDateTo": "",
      "plannedDateFrom": "",
      "plannedDateTo": "",
      "contains": {
        "tags": ["exampleTag1", "exampleTag2"],
        "title": "Sample Title",
        "description": "Sample Description",
        "status": "PLANNED",
        "assignee": "JohnDoe"
      },
      "equals": {
        "title": "Exact Match Title",
        "description": "Exact Match Description",
        "status": "EXECUTED_OK",
        "assignee": "JaneDoe"
      }
    }
    ```

  ### Explanation of the JSON Search:
    - **Date Fields:** The date fields (`creationDateFrom`, `creationDateTo`, `completionDateFrom`, `completionDateTo`, `plannedDateFrom`, `plannedDateTo`) are compared based on equality or greater/lower values. For example, `creationDateFrom` would search for tasks created on or after the specified date.
    - **Contains:** The `contains` section searches for tasks where the specified fields contain the provided values. For example, `title: "Sample Title"` would search for tasks with titles that include "Sample Title".
    - **Equals:** The `equals` section searches for tasks where the specified fields exactly match the provided values. For example, `status: "EXECUTED_OK"` would only return tasks where the status is exactly "EXECUTED_OK".
    - **Tags:** Tags are included in the `contains` section. This means that it searches for tasks that contain the exact specified tag(s). For example, `tags: ["exampleTag1"]` will search for tasks that have "exampleTag1" as a tag.



## API Endpoints
**POST /tasks:** Create a new task.
- **Request Body:** JSON object with task attributes. Example:
```json
{
"title": "Sample Task",
"description": "This is a description of the sample task.",
"status": "PLANNED",
"creationDate": "2024-01-01",
"completionDate": "2024-02-02",
"plannedDate": "2024-02-01",
"assignee": "JohnDoe",
"tags": ["exampleTag1", "exampleTag2", "exampleTag3"]
}
```
- **Response:** Status code `201 (Created)` if successful. In the body is the object with its ID.
- **GET /tasks/{id}:** Retrieve a task by ID.
    - **Response:** JSON object with task details and status code `200 (OK)` if found.
- **PUT /tasks:** Update a task.
    - **Request Body:** JSON object with updated task attributes and ID.
    - **Response:** Status code `200 (OK)` if successful.
- **DELETE /tasks/{id}:** Delete a task by ID.
    - **Response:** Status code `200 (OK)` if successful.
- **POST /tasks/search:** Search for tasks based on parameters.
    - **Request Body:** JSON object with search criteria.
    - **Response:** JSON array of tasks matching the search criteria and status code `200 (OK)` if found.

## Asynchronous Handling
To ensure non-blocking operations, especially on transport threads, all request handling is performed asynchronously using `CompletableFuture.supplyAsync(...)`. This approach maintains the performance and stability of the plugin by avoiding blocking operations on critical threads.

## Testing
Unit tests for the repository layer ensure the correctness of the implementation.

## Running the Plugin
1. **Build the Plugin:** Use Gradle to build the plugin.
   ```bash
   ./gradlew clean build
2. **Run the Plugin:** Use Gradle to run the plugin.
   ```bash
   ./gradlew run
3. **URL Configuration: The plugin is configured to run on localhost:9200/_plugins/..**

