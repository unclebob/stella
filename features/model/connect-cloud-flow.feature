# mutation-stamp: sha256=c37132b5b79b0a5a08e6cd9051365163b541922ca59458e0e4176df92015e0a9
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-26T20:31:45.976895Z","feature_name":"Connect cloud flow","feature_path":"features/model/connect-cloud-flow.feature","background_hash":"4cb483c07ad44ac6174b8afd517c0d0d4141bb3062259e93675934cfb8ccdcc6","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Connect source to stock with flow","scenario_hash":"b0ca02dde4f60cfd56a1e277a33563affce158a20037f039d115e28a74ae0873","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:31:45.976895Z"},{"index":1,"name":"Connect stock to sink with flow","scenario_hash":"53db6cf427e7613f19f197f8d423457325ab3f5092bdd8407b2dca6f7d5d8b02","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:31:45.976895Z"},{"index":4,"name":"Completing cloud flow disarms the flow tool","scenario_hash":"0fc9b4acef1730d6c86a095b96876afe88586622ab4c7d44b60e18222108fcaf","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:31:45.976895Z"}]}
# acceptance-mutation-manifest-end

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