Feature: Fetch Example XML

  Background:
    Given header 'Accept' is 'valid xml'

  Scenario: Fetch World
    And I GET the resource '/world'
    Then the status code should be 'OK'
    And I should receive XML response:
    """
    <?xml version='1.0' encoding='ISO-8859-1'?>
<Hello><message>Hello World</message></Hello>
    """

  Scenario: Fetch User
    And I GET the resource '/user'
    Then the status code should be 'OK'
    And I should receive XML response:
    """
    <?xml version='1.0' encoding='ISO-8859-1'?>
<Hello><message>Hello User</message></Hello>
    """

  Scenario: Fetch Application
    And I GET the resource '/application'
    Then the status code should be 'OK'
    And I should receive XML response:
    """
    <?xml version='1.0' encoding='ISO-8859-1'?>
<Hello><message>Hello Application</message></Hello>
    """
