# mutation-stamp: sha256=cbe656b280cf640e294ea751f89cb59723635ed1dfe5d83aec82f60d430146f9
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-30T14:59:25.434758Z","feature_name":"Stock thermometer","feature_path":"features/model/stock-thermometer.feature","background_hash":"4c8c970e459ede1586e22cc3c60bafe2567086a820d4b039615fd7372d58ffd4","implementation_hash":"unknown","scenarios":[{"index":7,"name":"Thermometer fill updates after simulation","scenario_hash":"53007043f40291e041fecd08db81554401b7aac1dfed493f8c91beb2a2d55063","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-06-30T14:59:25.434758Z"}]}
# acceptance-mutation-manifest-end

Feature: Stock thermometer

Background:
  Given a diagram model with stock Stock1 at 200 150

# stock-thermometer-01a
Scenario: Thermometer fill at zero on bounded scale
  When I set stock Stock1 minimum to 0
  And I set stock Stock1 maximum to 100
  And I set stock Stock1 initial value to 0
  Then stock Stock1 initial value should be 0
  And stock Stock1 canvas thermometer fill width should be 0
  And stock Stock1 canvas thermometer fill color should be light blue

# stock-thermometer-01b
Scenario: Thermometer fill at mid on bounded scale
  When I set stock Stock1 minimum to 0
  And I set stock Stock1 maximum to 100
  And I set stock Stock1 initial value to 50
  Then stock Stock1 initial value should be 50
  And stock Stock1 canvas thermometer fill width should be 36
  And stock Stock1 canvas thermometer fill color should be light blue

# stock-thermometer-01c
Scenario: Thermometer fill at max on bounded scale
  When I set stock Stock1 minimum to 0
  And I set stock Stock1 maximum to 100
  And I set stock Stock1 initial value to 100
  Then stock Stock1 initial value should be 100
  And stock Stock1 canvas thermometer fill width should be 72
  And stock Stock1 canvas thermometer fill color should be light blue

# stock-thermometer-02a
Scenario: Thermometer fill at zero on unbounded scale
  When I clear stock Stock1 maximum
  And I set stock Stock1 initial value to 0
  Then stock Stock1 initial value should be 0
  And stock Stock1 canvas thermometer fill width should be 0
  And stock Stock1 canvas thermometer fill color should be light blue

# stock-thermometer-02b
Scenario: Thermometer fill at mid on unbounded scale
  When I clear stock Stock1 maximum
  And I set stock Stock1 initial value to 25
  Then stock Stock1 initial value should be 25
  And stock Stock1 canvas thermometer fill width should be 18
  And stock Stock1 canvas thermometer fill color should be light blue

# stock-thermometer-03
Scenario: Stock icon layout places name above thermometer
  When I set stock Stock1 minimum to 0
  And I set stock Stock1 maximum to 100
  Then stock Stock1 canvas name should be at top
  And stock Stock1 canvas thermometer should be below name
  And stock Stock1 canvas minimum should be 0
  And stock Stock1 canvas maximum should be 100

# stock-thermometer-04
Scenario: Thermometer track is a horizontal rectangle
  When I set stock Stock1 minimum to 0
  And I set stock Stock1 maximum to 100
  Then stock Stock1 canvas thermometer track width should be 72
  And stock Stock1 canvas thermometer track height should be 8

# stock-thermometer-05
Scenario Outline: Thermometer fill updates after simulation
  Given a diagram model with stock Stock1 at 100 100
  And source Source1 at 50 100
  And flow Flow1 runs from source Source1 to stock Stock1
  When I set stock Stock1 minimum to 0
  And I set stock Stock1 maximum to 100
  And I set stock Stock1 initial value to 0
  And I set flow Flow1 rate to 10
  And I run the simulation for <steps> steps
  Then stock Stock1 canvas thermometer fill width should be <fill_width>
  And stock Stock1 canvas thermometer fill color should be light blue

  Examples:
    | steps | fill_width |
    | 1     | 1          |
    | 5     | 4          |