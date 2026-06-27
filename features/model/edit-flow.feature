Feature: Edit flow

Background:
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2

# edit-flow-01
Scenario Outline: Rename flow
  When I set flow <name> name to <new_name>
  Then the diagram should contain flow <new_name>
  And flow <new_name> canvas name should be <new_name>
  And flow <new_name> rate should be 0

  Examples:
    | name  | new_name |
    | Flow1 | Drain    |

# edit-flow-02
Scenario Outline: Edit constant flow rate
  When I set flow <name> rate to <rate>
  Then flow <name> rate should be <rate>
  And flow <name> canvas rate should be <rate>

  Examples:
    | name  | rate |
    | Flow1 | 5    |

# edit-flow-03
Scenario: Reject duplicate flow name
  Given flow Flow2 runs from stock Stock2 to stock Stock1
  When I set flow Flow1 name to Flow2
  Then the flow edit should be rejected
  And the diagram should contain flow Flow1
  And the diagram should contain flow Flow2

# edit-flow-04
Scenario Outline: Reject non-numeric flow rate
  When I set flow <name> rate to <rate>
  Then the flow edit should be rejected
  And flow <name> rate should be 0

  Examples:
    | name  | rate |
    | Flow1 | abc  |

# edit-flow-05
Scenario: Renaming flow updates connector references
  Given converter Converter1 at 100 250
  When I arm the connector placement tool
  And I select converter Converter1 as the connector origin
  And I select flow Flow1 as the connector destination
  And I set flow Flow1 name to Drain
  Then the diagram should contain flow Drain
  And connector Connector1 should run from converter Converter1 to flow Drain