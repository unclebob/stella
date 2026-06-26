Feature: Connect connector

Background:
  Given a diagram model with stock Stock1 at 200 150
  And stock Stock2 at 350 150
  And flow Flow1 runs from stock Stock1 to stock Stock2
  And converter Converter1 at 100 250

# connect-connector-01
Scenario Outline: Connect converter to flow with connector
  When I arm the connector placement tool
  And I select converter <from> as the connector origin
  And I select flow <to> as the connector destination
  Then the diagram should contain connector <connector>
  And connector <connector> should run from converter <from> to flow <to>

  Examples:
    | from       | to    | connector  |
    | Converter1 | Flow1 | Connector1 |

# connect-connector-02
Scenario Outline: Connect stock to converter with connector
  When I arm the connector placement tool
  And I select stock <from> as the connector origin
  And I select converter <to> as the connector destination
  Then the diagram should contain connector <connector>
  And connector <connector> should run from stock <from> to converter <to>

  Examples:
    | from   | to         | connector  |
    | Stock1 | Converter1 | Connector1 |

# connect-connector-03
Scenario: Reject flow as connector origin
  When I arm the connector placement tool
  And I select flow Flow1 as the connector origin
  Then the diagram connector count should be 0
  And the connector placement tool should be armed

# connect-connector-04
Scenario: Reject source as connector endpoint
  Given source Source1 at 50 150
  When I arm the connector placement tool
  And I select converter Converter1 as the connector origin
  And I select source Source1 as the connector destination
  Then the diagram connector count should be 0

# connect-connector-05
Scenario: Arming connector without selecting elements connects nothing
  When I arm the connector placement tool
  Then the diagram connector count should be 0

# connect-connector-06
Scenario Outline: Completing connector disarms the tool
  When I arm the connector placement tool
  And I select converter <from> as the connector origin
  And I select flow <to> as the connector destination
  Then the connector placement tool should be disarmed

  Examples:
    | from       | to    |
    | Converter1 | Flow1 |