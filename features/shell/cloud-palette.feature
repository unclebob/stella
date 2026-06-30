# mutation-stamp: sha256=ff6948c3234a3fe33428308a93a10ac8859e59d9a1dab36adcbc8a641380b2a5
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-30T15:06:37.041166Z","feature_name":"Cloud palette","feature_path":"features/shell/cloud-palette.feature","background_hash":"56c9d47754e84119720f557ae0782e9e686c08336a7cf99c7de102b9a85a9622","implementation_hash":"unknown","scenarios":[{"index":2,"name":"Placing a source resets the palette","scenario_hash":"a8a9d94d096c3945f0e3538e90372998ce85ef7f1231df5c3ab17192bf9bfa82","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-30T15:06:37.041166Z"},{"index":3,"name":"Placing a sink resets the palette","scenario_hash":"2fea95a83760aeae14286d6c6bbd034dccd8704d394f670107b3a46cad98c661","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-30T15:06:37.041166Z"}]}
# acceptance-mutation-manifest-end

Feature: Cloud palette

Background:
  Given a default shell application
  And an empty diagram model

# cloud-palette-01
Scenario: Arming highlights the source palette tool
  When I arm the source placement tool
  Then the palette tool Source should be active
  And the palette tool Sink should be inactive

# cloud-palette-02
Scenario: Arming highlights the sink palette tool
  When I arm the sink placement tool
  Then the palette tool Sink should be active
  And the palette tool Source should be inactive

# cloud-palette-03
Scenario Outline: Placing a source resets the palette
  When I arm the source placement tool
  And I place a source at <x> <y>
  Then the diagram should contain source Source1
  And source Source1 should be at position 50 150
  And the source placement tool should be disarmed
  And no palette tool should be active

  Examples:
    | x  | y   |
    | 50 | 150 |

# cloud-palette-04
Scenario Outline: Placing a sink resets the palette
  When I arm the sink placement tool
  And I place a sink at <x> <y>
  Then the diagram should contain sink Sink1
  And sink Sink1 should be at position 400 150
  And the sink placement tool should be disarmed
  And no palette tool should be active

  Examples:
    | x   | y   |
    | 400 | 150 |

# cloud-palette-05
Scenario: Switching palette tools moves cloud highlight
  When I arm the source placement tool
  And I arm the sink placement tool
  Then the palette tool Sink should be active
  And the palette tool Source should be inactive