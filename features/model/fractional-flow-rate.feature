# mutation-stamp: sha256=36d163bc8228c2a2f4fa6c6cd020b0f8be66fa0a16bd5f68a7d401307cf50c81
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-30T15:44:22.790737Z","feature_name":"Fractional flow rate","feature_path":"features/model/fractional-flow-rate.feature","background_hash":"ba597956270d81fe363acacd37c96ac101b54cfa8ce1e3162680b710b921e415","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Fractional source flow accumulates stock value","scenario_hash":"ce2c96131948804e8b5af8882f28884ad465fd9cdb64c9f9e78b832a898f1a6b","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-06-30T15:44:22.790737Z"},{"index":1,"name":"Fractional stock flow transfers between stocks","scenario_hash":"f19bc95c21523ddfcdbfa613f4937ab440f9c8899862bca367dcb532f4508f2c","mutation_count":5,"result":{"Total":5,"Killed":5,"Survived":0,"Errors":0},"tested_at":"2026-06-30T15:44:22.790737Z"}]}
# acceptance-mutation-manifest-end

Feature: Fractional flow rate

Background:
  Given a diagram model with stock Stock1 at 200 150
  And source Source1 at 50 150
  And flow Flow1 runs from source Source1 to stock Stock1
  When I set stock Stock1 initial value to 0

# fractional-flow-rate-01
Scenario Outline: Fractional source flow accumulates stock value
  When I set flow Flow1 rate to <rate>
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And simulation time should be <time>

  Examples:
    | rate | steps | stock1 | time |
    | 0.1  | 10    | 0.1    | 1    |
    | 0.1  | 20    | 0.2    | 2    |

# fractional-flow-rate-02
Scenario Outline: Fractional stock flow transfers between stocks
  Given stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I set stock Stock1 initial value to 1
  And I set stock Stock2 initial value to 0
  And I set flow Flow1 rate to <rate>
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And stock Stock2 value should be <stock2>
  And simulation time should be <time>

  Examples:
    | rate | steps | stock1 | stock2 | time |
    | 0.2  | 5     | 0.9    | 0.1    | 0.5  |