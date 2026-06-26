Feature: Connect flow

Background:
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200

# connect-flow-01
Scenario Outline: Connect two stocks with a flow
  When I arm the flow placement tool
  And I select stock <from> as the flow source
  And I select stock <to> as the flow destination
  Then the diagram should contain flow <flow>
  And flow <flow> should run from stock <from> to stock <to>
  And flow <flow> rate should be <rate>

  Examples:
    | from   | to     | flow  | rate |
    | Stock1 | Stock2 | Flow1 | 0    |

# connect-flow-02
Scenario Outline: Second flow requires re-arming the placement tool
  Given flow <flow1> runs from stock <from> to stock <to>
  When I arm the flow placement tool
  And I select stock <to> as the flow source
  And I select stock <from> as the flow destination
  Then the diagram should contain flow <flow2>
  And flow <flow2> should run from stock <to> to stock <from>

  Examples:
    | from   | to     | flow1 | flow2 |
    | Stock1 | Stock2 | Flow1 | Flow2 |

# connect-flow-03
Scenario: Arming without selecting stocks connects nothing
  When I arm the flow placement tool
  Then the diagram flow count should be 0

# connect-flow-04
Scenario Outline: Completing a connection disarms the flow tool
  When I arm the flow placement tool
  And I select stock <from> as the flow source
  And I select stock <to> as the flow destination
  Then the flow placement tool should be disarmed

  Examples:
    | from   | to     |
    | Stock1 | Stock2 |