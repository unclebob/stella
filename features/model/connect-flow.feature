# mutation-stamp: sha256=c71e52b95f13c6b50f96f9a50cac27059b1f1086ad9eb7878cacb4e8ca8e98fc
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-26T20:33:43.992834Z","feature_name":"Connect flow","feature_path":"features/model/connect-flow.feature","background_hash":"000c135b9cc93370cc3c350f602528b0ca49d9548108f78d8be293c661e067f0","implementation_hash":"unknown","scenarios":[{"index":1,"name":"Second flow requires re-arming the placement tool","scenario_hash":"19ae27c91f9b62cfc5cc95b17fb1cccfef5d89fa137dedba167363cc61ef9c9d","mutation_count":3,"result":{"Total":3,"Killed":3,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:33:43.992834Z"},{"index":0,"name":"Connect two stocks with a flow","scenario_hash":"32fbe0e11f4b41da491386f3659aca6b6e0ddbec992a309fa8c37842aabff54e","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:31:51.859909Z"},{"index":3,"name":"Completing a connection disarms the flow tool","scenario_hash":"8931dad63f586cbfc413ac6ee83e9ba49dde0570719da66f55edd13c63db8ec7","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:31:51.859909Z"}]}
# acceptance-mutation-manifest-end

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
  Given flow Flow1 runs from stock Stock1 to stock Stock2
  When I arm the flow placement tool
  And I select stock <to> as the flow source
  And I select stock <from> as the flow destination
  Then the diagram should contain flow Flow1
  And the diagram should contain flow <flow2>
  And flow <flow2> should run from stock <to> to stock <from>

  Examples:
    | from   | to     | flow2 |
    | Stock1 | Stock2 | Flow2 |

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