# mutation-stamp: sha256=31cc0b45d3f5dac0bcd200e4b2d73e0f2231fe327da85a523b99f71badba97d3
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-26T20:33:14.360138Z","feature_name":"Place stock","feature_path":"features/model/place-stock.feature","background_hash":"635de7f37d2581ad41a2f9b87dfe12e9adff8d18ef00706872c8f5d02d60db6c","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Place first stock on empty diagram","scenario_hash":"7aef52d8a2388e2b68a0ee21e20d4f2f6216746eddaee01ccd2f9b167fc9df90","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:33:14.360138Z"},{"index":1,"name":"Second stock requires re-arming the placement tool","scenario_hash":"f1641a5e35ceed8a394e6cc1e1990327757d5d1e6e74aa28675601eb9561d8a7","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:33:14.360138Z"},{"index":3,"name":"Placement disarms the stock tool","scenario_hash":"5ac865e1790ac85adec8b02cdd7cf32bc6cff39c9f0c22fae1bc8b9d63353644","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:33:14.360138Z"}]}
# acceptance-mutation-manifest-end

Feature: Place stock

Background:
  Given an empty diagram model

# place-stock-01
Scenario Outline: Place first stock on empty diagram
  When I arm the stock placement tool
  And I place a stock at <x> <y>
  Then the diagram should contain stock Stock1
  And stock Stock1 should be at position 200 150
  And stock Stock1 initial value should be 0

  Examples:
    | x   | y   |
    | 200 | 150 |

# place-stock-02
Scenario Outline: Second stock requires re-arming the placement tool
  When I arm the stock placement tool
  And I place a stock at <x1> <y1>
  And I arm the stock placement tool
  And I place a stock at <x2> <y2>
  Then the diagram should contain stock Stock1
  And stock Stock1 should be at position 100 100
  And the diagram should contain stock Stock2
  And stock Stock2 should be at position 300 200
  And stock Stock2 initial value should be 0

  Examples:
    | x1  | y1  | x2  | y2  |
    | 100 | 100 | 300 | 200 |

# place-stock-03
Scenario: Arming without a canvas click places no stock
  When I arm the stock placement tool
  Then the diagram stock count should be 0

# place-stock-04
Scenario Outline: Placement disarms the stock tool
  When I arm the stock placement tool
  And I place a stock at <x> <y>
  Then stock Stock1 should be at position 200 150
  And the stock placement tool should be disarmed

  Examples:
    | x   | y   |
    | 200 | 150 |