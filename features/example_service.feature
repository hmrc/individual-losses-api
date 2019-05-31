Feature: Fetch Example

  Background:
    Given header 'Accept' is 'valid json'

  Scenario: Fetch World
    And I GET the resource '/world'
    Then the status code should be 'OK'
    And I should receive JSON response:
    """
    {
      "message": "Hello World"
    }
    """


  Scenario: Fetch User
    And I GET the resource '/user'
    Then the status code should be 'OK'
    And I should receive JSON response:
    """
    {
      "message": "Hello User"
    }
    """

  Scenario: Fetch Application
    And I GET the resource '/application'
    Then the status code should be 'OK'
    And I should receive JSON response:
    """
    {
      "message": "Hello Application"
    }
    """
