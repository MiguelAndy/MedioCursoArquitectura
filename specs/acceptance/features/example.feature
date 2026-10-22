Feature: Item Management
  As an authorized system client
  I want to create and retrieve items
  So that I can manage domain resources reliably

  Scenario: Successfully creating an item
    Given the system is running and healthy
    When I submit a POST request to "/api/v1/items" with the following payload:
      """
      {
        "name": "Microservice Architecture Book",
        "description": "Essential reading for distributed systems"
      }
      """
    Then the response status code should be 201
    And the response body should contain a valid UUID "id"
    And the response body "status" should be "ACTIVE"

  Scenario: Rejecting item creation with missing required name
    Given the system is running and healthy
    When I submit a POST request to "/api/v1/items" with the following payload:
      """
      {
        "description": "No name provided"
      }
      """
    Then the response status code should be 400
    And the response body "code" should be "VALIDATION_ERROR"
