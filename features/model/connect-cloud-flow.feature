Feature: Connect cloud flow

Background:
  Given a diagram model with stock Stock1 at 200 150
  And source Source1 at 50 150
  And sink Sink1 at 400 150

# connect-cloud-01
Scenario Outline: Connect source to stock with flow
  When I arm the flow placement tool
  And I select source <from> as the flow source
  And I select stock <to> as the flow destination
  Then the diagram should contain flow <flow>
  And flow <flow> should run from source <from> to stock <to>
  And flow <flow> rate should be <rate>

  Examples:
    | from    | to     | flow  | rate |
    | Source1 | Stock1 | Flow1 | 0    |

# connect-cloud-02
Scenario Outline: Connect stock to sink with flow
  When I arm the flow placement tool
  And I select stock <from> as the flow source
  And I select sink <to> as the flow destination
  Then the diagram should contain flow <flow>
  And flow <flow> should run from stock <from> to sink <to>
  And flow <flow> rate should be <rate>

  Examples:
    | from   | to    | flow  | rate |
    | Stock1 | Sink1 | Flow1 | 0    |

# connect-cloud-03
Scenario: Reject sink as flow source
  When I arm the flow placement tool
  And I select sink Sink1 as the flow source
  Then the diagram flow count should be 0
  And the flow placement tool should be armed

# connect-cloud-04
Scenario: Reject source as flow destination
  When I arm the flow placement tool
  And I select stock Stock1 as the flow source
  And I select source Source1 as the flow destination
  Then the diagram flow count should be 0

# connect-cloud-05
Scenario Outline: Completing cloud flow disarms the flow tool
  When I arm the flow placement tool
  And I select source <from> as the flow source
  And I select stock <to> as the flow destination
  Then the flow placement tool should be disarmed

  Examples:
    | from    | to     |
    | Source1 | Stock1 |