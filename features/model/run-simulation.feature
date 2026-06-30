# mutation-stamp: sha256=1331a4f6081a6cb2fc920a1b5aee616ebca5d732c47540c566b208fa239bff08
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-30T13:37:46.982255Z","feature_name":"Run simulation","feature_path":"features/model/run-simulation.feature","background_hash":"74234e98afe7498fb5daf1f36ac2d78acc339464f950703b8c019892f982b90b","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Stock flow transfers value between stocks","scenario_hash":"f0c0c95069fdb9592289bff7f9342ddff3250627320508b4b08a2f4ae4facecb","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-06-30T13:37:46.982255Z"},{"index":1,"name":"Source flow increases stock value","scenario_hash":"e9a42e1a6f7587ec63bcd500d09fd1da78c2f7550d2d703986c966e4ed6e834d","mutation_count":6,"result":{"Total":6,"Killed":6,"Survived":0,"Errors":0},"tested_at":"2026-06-30T13:37:46.982255Z"},{"index":2,"name":"Sink flow decreases stock value","scenario_hash":"06badf35ea60fbed07ab6ab2545d98aa0a9821f9c24fe46c427c2e3f389e5b07","mutation_count":6,"result":{"Total":6,"Killed":6,"Survived":0,"Errors":0},"tested_at":"2026-06-30T13:37:46.982255Z"},{"index":3,"name":"Source and sink flows net on stock value","scenario_hash":"85904f0ecf412e266e7c49ab652598ed704458f00d6f639ee858a65ebc84a815","mutation_count":6,"result":{"Total":6,"Killed":6,"Survived":0,"Errors":0},"tested_at":"2026-06-30T13:37:46.982255Z"},{"index":4,"name":"Zero rate flow leaves stock values unchanged","scenario_hash":"e81cfb483a5df03d686149fe626ac515bef868cddf3edb690c8e88196f4e4b41","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-30T13:37:46.982255Z"}]}
# acceptance-mutation-manifest-end

Feature: Run simulation

# run-simulation-01
Scenario Outline: Stock flow transfers value between stocks
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I set stock Stock1 initial value to 100
  And I set stock Stock2 initial value to 0
  And I set flow Flow1 rate to 10
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And stock Stock2 value should be <stock2>
  And simulation time should be <time>

  Examples:
    | steps | stock1 | stock2 | time |
    | 1     | 99     | 1      | 0.1  |
    | 5     | 95     | 5      | 0.5  |

# run-simulation-02
Scenario Outline: Source flow increases stock value
  Given a diagram model with stock Stock1 at 200 150
  And source Source1 at 50 150
  And flow Flow1 runs from source Source1 to stock Stock1
  When I set stock Stock1 initial value to 0
  And I set flow Flow1 rate to 10
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And simulation time should be <time>

  Examples:
    | steps | stock1 | time |
    | 1     | 1      | 0.1  |
    | 5     | 5      | 0.5  |

# run-simulation-03
Scenario Outline: Sink flow decreases stock value
  Given a diagram model with stock Stock1 at 200 150
  And sink Sink1 at 400 150
  And flow Flow1 runs from stock Stock1 to sink Sink1
  When I set stock Stock1 initial value to 100
  And I set flow Flow1 rate to 10
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And simulation time should be <time>

  Examples:
    | steps | stock1 | time |
    | 1     | 99     | 0.1  |
    | 5     | 95     | 0.5  |

# run-simulation-04
Scenario Outline: Source and sink flows net on stock value
  Given a diagram model with stock Stock1 at 200 150
  And source Source1 at 50 150
  And sink Sink1 at 400 150
  And flow Flow1 runs from source Source1 to stock Stock1
  And flow Flow2 runs from stock Stock1 to sink Sink1
  When I set stock Stock1 initial value to 0
  And I set flow Flow1 rate to 10
  And I set flow Flow2 rate to 5
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And simulation time should be <time>

  Examples:
    | steps | stock1 | time |
    | 1     | 0.5    | 0.1  |
    | 2     | 1      | 0.2  |

# run-simulation-05
Scenario Outline: Zero rate flow leaves stock values unchanged
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I set stock Stock1 initial value to 50
  And I set stock Stock2 initial value to 20
  And I set flow Flow1 rate to 0
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be 50
  And stock Stock2 value should be 20
  And simulation time should be <time>

  Examples:
    | steps | time |
    | 3     | 0.3  |

# run-simulation-06
Scenario: Stock value equals initial value before simulation runs
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  When I set stock Stock1 initial value to 42
  And I set stock Stock2 initial value to 7
  Then stock Stock1 value should be 42
  And stock Stock2 value should be 7
  And simulation time should be 0