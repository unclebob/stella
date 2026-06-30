Feature: Simulation run

Background:
  Given a default shell application

# simulation-run-01
Scenario: Control panel shows speed slider and Run button
  Then the control panel should be visible
  And the Step button should be visible
  And the speed slider should be visible
  And the simulation tick delay display should show 1
  And the Run button should show Run

# simulation-run-02
Scenario Outline: Set simulation tick delay on speed slider
  When I set simulation tick delay to <delay>
  Then the simulation tick delay display should show <delay>

  Examples:
    | delay |
    | 1     |
    | 0.5   |
    | 0     |

# simulation-run-03
Scenario: Run toggles to Stop while simulation is running
  When I click Run
  Then simulation run should be active
  And the Run button should show Stop
  When I click Stop
  Then simulation run should be stopped
  And the Run button should show Run

# simulation-run-04
Scenario Outline: Running simulation advances time on timer ticks
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I set simulation tick delay to 0
  And I click Run
  And <ticks> simulation run ticks elapse
  Then the simulation time display should show <time>
  And simulation run should be active

  Examples:
    | ticks | time |
    | 1     | 0.1  |
    | 3     | 0.3  |

# simulation-run-05
Scenario: Stop halts automatic stepping
  Given a diagram model with stock Stock1 at 100 100
  When I set simulation tick delay to 0
  And I click Run
  And 2 simulation run ticks elapse
  When I click Stop
  And 3 simulation run ticks elapse
  Then the simulation time display should show 0.2
  And simulation run should be stopped

# simulation-run-06
Scenario: Step still advances time while run is stopped
  When I click Step 1 times
  Then the simulation time display should show 0.1