# mutation-stamp: sha256=1265855b6b81abc8ec3dfdb8cd4a655c13287f1e0a9669e5497f330578afff0f
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-26T20:33:37.970318Z","feature_name":"Place cloud","feature_path":"features/model/place-cloud.feature","background_hash":"635de7f37d2581ad41a2f9b87dfe12e9adff8d18ef00706872c8f5d02d60db6c","implementation_hash":"unknown","scenarios":[{"index":4,"name":"Second source requires re-arming the placement tool","scenario_hash":"f1595458929627708b0c6fdf6af109376136d57077397ac3815a2428df0b5a4d","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:33:37.970318Z"},{"index":0,"name":"Place source on diagram","scenario_hash":"3ceec3fcc40b52a2557c87aa4db2e5b7a92a6c9aa9ebd13ca7b806d4f346a329","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:33:20.339733Z"},{"index":1,"name":"Place sink on diagram","scenario_hash":"4d50781c83dff6f4184dd2f0f6894ca69b664889802a5cc5c9cf581202aa9b1f","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:33:20.339733Z"},{"index":3,"name":"Source placement disarms the tool","scenario_hash":"2aeca1cf065ae05e631f171aaec8c9c688e467267d3da13cc09654cf04946f6c","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:33:20.339733Z"}]}
# acceptance-mutation-manifest-end

Feature: Place cloud

Background:
  Given an empty diagram model

# place-cloud-01
Scenario Outline: Place source on diagram
  When I arm the source placement tool
  And I place a source at <x> <y>
  Then the diagram should contain source Source1
  And source Source1 should be at position 50 150

  Examples:
    | x  | y   |
    | 50 | 150 |

# place-cloud-02
Scenario Outline: Place sink on diagram
  When I arm the sink placement tool
  And I place a sink at <x> <y>
  Then the diagram should contain sink Sink1
  And sink Sink1 should be at position 400 150

  Examples:
    | x   | y   |
    | 400 | 150 |

# place-cloud-03
Scenario: Arming source without a canvas click places nothing
  When I arm the source placement tool
  Then the diagram source count should be 0

# place-cloud-04
Scenario Outline: Source placement disarms the tool
  When I arm the source placement tool
  And I place a source at <x> <y>
  Then source Source1 should be at position 50 150
  And the source placement tool should be disarmed

  Examples:
    | x  | y   |
    | 50 | 150 |

# place-cloud-05
Scenario Outline: Sink placement disarms the tool
  When I arm the sink placement tool
  And I place a sink at <x> <y>
  Then sink Sink1 should be at position 400 150
  And the sink placement tool should be disarmed

  Examples:
    | x   | y   |
    | 400 | 150 |

# place-cloud-06
Scenario Outline: Second source requires re-arming the placement tool
  When I arm the source placement tool
  And I place a source at <x1> <y1>
  And I arm the source placement tool
  And I place a source at <x2> <y2>
  Then the diagram should contain source Source1
  And source Source1 should be at position 50 150
  And the diagram should contain source Source2
  And source Source2 should be at position 450 150

  Examples:
    | x1 | y1  | x2  | y2  |
    | 50 | 150 | 450 | 150 |

# place-cloud-07
Scenario Outline: Second sink requires re-arming the placement tool
  When I arm the sink placement tool
  And I place a sink at <x1> <y1>
  And I arm the sink placement tool
  And I place a sink at <x2> <y2>
  Then the diagram should contain sink Sink1
  And sink Sink1 should be at position 400 150
  And the diagram should contain sink Sink2
  And sink Sink2 should be at position 550 150

  Examples:
    | x1  | y1  | x2  | y2  |
    | 400 | 150 | 550 | 150 |