# mutation-stamp: sha256=6cf0bb94e86c92d07c8dc340ccac8609a79d1378082a652f9aee5f803decf45f
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-27T15:22:26.921050Z","feature_name":"Drag stock","feature_path":"features/model/drag-stock.feature","background_hash":"2f575443317bca9d2ca4ace77041b4b6649aa740cc16b59e6bbfd9dc34025c47","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Move stock to new position","scenario_hash":"bfe3c32cb828f8952ab1ef892c396206dc27d2724ab92bb98c1d141862934564","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:22:26.921050Z"},{"index":3,"name":"Move one stock without moving the other","scenario_hash":"e8a1bbde7654a33347a9bf13501e33a9db57758aad2012eb9e90927b8861e21c","mutation_count":3,"result":{"Total":3,"Killed":3,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:22:26.921050Z"}]}
# acceptance-mutation-manifest-end

Feature: Drag stock

Background:
  Given a diagram model with stock Stock1 at 100 100

# drag-stock-01
Scenario Outline: Move stock to new position
  When I move stock Stock1 to <x> <y>
  Then stock Stock1 should be at position 250 180
  And stock Stock1 canvas position should be 250 180

  Examples:
    | x   | y   |
    | 250 | 180 |

# drag-stock-02
Scenario: Moving stock does not create another stock
  When I move stock Stock1 to 200 150
  Then the diagram stock count should be 1
  And the diagram should contain stock Stock1

# drag-stock-03
Scenario: Moving stock preserves flow endpoints
  Given a diagram model with stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I move stock Stock1 to 50 80
  Then stock Stock1 should be at position 50 80
  And flow Flow1 should run from stock Stock1 to stock Stock2

# drag-stock-04
Scenario Outline: Move one stock without moving the other
  Given a diagram model with stock Stock2 at 300 200
  When I move stock <name> to <x> <y>
  Then stock Stock1 should be at position 120 90
  And stock Stock2 should be at position 300 200

  Examples:
    | name   | x   | y   |
    | Stock1 | 120 | 90  |