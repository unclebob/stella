Feature: Shell control panel

Background:
  Given a default shell application

# shell-control-01
Scenario: Control panel is visible with Step button
  Then the control panel should be visible
  And the Step button should be visible
  And the simulation time display should show 0

# shell-control-02
Scenario: Control panel ignores armed stock placement
  Given an empty diagram model
  When I arm the stock placement tool
  And I click in the control panel
  Then the diagram stock count should be 0
  And the stock placement tool should be armed

# shell-control-03
Scenario: Control panel ignores stock drag
  Given a diagram model with stock Stock1 at 100 100
  When I drag stock Stock1 within the control panel
  Then stock Stock1 should be at position 100 100

# shell-control-04
Scenario Outline: Step button advances simulation time display
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I click Step <clicks> times
  Then the simulation time display should show <time>

  Examples:
    | clicks | time |
    | 1      | 0.1  |
    | 3      | 0.3  |