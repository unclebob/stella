# mutation-stamp: sha256=f2f08932e9d896e2bafa079a6a3db4896ac5a26e5349534b494da0cd5b48ddd3
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-27T15:14:56.041171Z","feature_name":"Edit flow","feature_path":"features/model/edit-flow.feature","background_hash":"0d8b717afe0ca03b971a16c9b91252a42d4f05b60d55cd58cd2b6c8b1681257d","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Rename flow","scenario_hash":"e6e05edd1fb041a3444670e3aea514b51b151f314e6c274099ff5e38ab39a1a5","mutation_count":1,"result":{"Total":1,"Killed":1,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:14:56.041171Z"},{"index":1,"name":"Edit constant flow rate","scenario_hash":"12379ca48597de3b687b2deb93cdbc40cd9a3b117625da86ee355bc9756a2b5a","mutation_count":1,"result":{"Total":1,"Killed":1,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:14:56.041171Z"}]}
# acceptance-mutation-manifest-end

Feature: Edit flow

Background:
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2

# edit-flow-01
Scenario Outline: Rename flow
  When I set flow Flow1 name to <new_name>
  Then the diagram should contain flow Drain
  And flow Drain canvas name should be Drain
  And flow Drain rate should be 0

  Examples:
    | new_name |
    | Drain    |

# edit-flow-02
Scenario Outline: Edit constant flow rate
  When I set flow Flow1 rate to <rate>
  Then flow Flow1 rate should be 5
  And flow Flow1 canvas rate should be 5

  Examples:
    | rate |
    | 5    |

# edit-flow-03
Scenario: Reject duplicate flow name
  Given flow Flow2 runs from stock Stock2 to stock Stock1
  When I set flow Flow1 name to Flow2
  Then the flow edit should be rejected
  And the diagram should contain flow Flow1
  And the diagram should contain flow Flow2

# edit-flow-04
Scenario: Reject non-numeric flow rate
  When I set flow Flow1 rate to abc
  Then the flow edit should be rejected
  And flow Flow1 rate should be 0

# edit-flow-05
Scenario: Renaming flow updates connector references
  Given converter Converter1 at 100 250
  When I arm the connector placement tool
  And I select converter Converter1 as the connector origin
  And I select flow Flow1 as the connector destination
  And I set flow Flow1 name to Drain
  Then the diagram should contain flow Drain
  And connector Connector1 should run from converter Converter1 to flow Drain