# mutation-stamp: sha256=742034f7bb2a7f90ac3e1a368d698e875344cc4afdac6ef8c649ff3133e3a041
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-26T23:02:35.145658Z","feature_name":"Connect connector","feature_path":"features/model/connect-connector.feature","background_hash":"5808bfcb3b07c6dd5e284554e16457b05d031f3bbe7d173d432565da245bef4d","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Connect converter to flow with connector","scenario_hash":"71fd154a589dc1e7b26722a118de122aeffe1395d670172779ef8dccadb3981d","mutation_count":3,"result":{"Total":3,"Killed":3,"Survived":0,"Errors":0},"tested_at":"2026-06-26T22:13:33.862210Z"},{"index":1,"name":"Connect stock to converter with connector","scenario_hash":"2b0c839f802a6b39a363f342362798da1610ae37e7fc02995f5af93189585a11","mutation_count":3,"result":{"Total":3,"Killed":3,"Survived":0,"Errors":0},"tested_at":"2026-06-26T22:13:33.862210Z"},{"index":5,"name":"Completing connector disarms the tool","scenario_hash":"8bc3c4390518b4427ab238444f5ebf913a1dade93d8f245ea604557148a3950e","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T22:13:33.862210Z"}]}
# acceptance-mutation-manifest-end

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