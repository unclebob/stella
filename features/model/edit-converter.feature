# mutation-stamp: sha256=c3f00a638632cb88dee5a3a8c6badc599a82787091e47fe95ce39981c34703fb
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-27T15:18:56.222363Z","feature_name":"Edit converter","feature_path":"features/model/edit-converter.feature","background_hash":"32c473a256e6af6144383abd9749321b33f8b08bf9f60111f88add4cdf127f60","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Rename converter","scenario_hash":"d3beea5fdc3b0c56edb87c684210b79fc67b52c93def7749dbd77f0ed2a57e97","mutation_count":1,"result":{"Total":1,"Killed":1,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:18:56.222363Z"},{"index":1,"name":"Set connector formula from converter editor","scenario_hash":"554ffdfc94f44220fe8ec5743d479b55337823dc22f30541bcef0313a149b281","mutation_count":1,"result":{"Total":1,"Killed":1,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:18:56.222363Z"}]}
# acceptance-mutation-manifest-end

Feature: Edit converter

Background:
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  And converter Converter1 at 100 250
  And connector Connector1 runs from converter Converter1 to flow Flow1

# edit-converter-01
Scenario Outline: Rename converter
  When I set converter Converter1 name to <new_name>
  Then the diagram should contain converter Growth
  And converter Growth canvas name should be Growth
  And connector Connector1 should run from converter Growth to flow Flow1

  Examples:
    | new_name |
    | Growth   |

# edit-converter-02
Scenario Outline: Set connector formula from converter editor
  When I set converter Converter1 formula to <formula>
  Then connector Connector1 formula should be Stock1 * 0.1
  And connector Connector1 canvas formula should be Stock1 * 0.1

  Examples:
    | formula      |
    | Stock1 * 0.1 |

# edit-converter-03
Scenario: Reject duplicate converter name
  Given converter Converter2 at 300 250
  When I set converter Converter1 name to Converter2
  Then the converter edit should be rejected
  And the diagram should contain converter Converter1
  And the diagram should contain converter Converter2

# edit-converter-04
Scenario: Reject formula edit without converter to flow connector
  Given converter Converter2 at 300 250
  When I set converter Converter2 formula to Stock1 * 0.2
  Then the converter edit should be rejected
  And connector Connector1 should have no formula