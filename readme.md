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
- **Create Task:** Users can create a new task with attributes like title, description, status, creation date, completion date, assignee, and tags.
- **Retrieve Task:** Users can retrieve a task by its ID or search for tasks using query parameters.
- **Update Task:** Users can update the attributes of an existing task.
- **Delete Task:** Users can delete a task by its ID.
- **Search Tasks:** Provides functionality to search for tasks based on query parameters.

## API Endpoints
- **POST /tasks:** Create a new task.
    - **Request Body:** JSON object with task attributes.
    - **Response:** Status code `201 (Created)` if successful.
- **GET /tasks/{id}:** Retrieve a task by ID.
    - **Response:** JSON object with task details and status code `200 (OK)` if found.
- **PUT /tasks/{id}:** Update a task by ID.
    - **Request Body:** JSON object with updated task attributes.
    - **Response:** Status code `200 (OK)` if successful.
- **DELETE /tasks/{id}:** Delete a task by ID.
    - **Response:** Status code `200 (OK)` if successful.
- **GET /tasks/search:** Search for tasks based on query parameters.
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
