# mutation-stamp: sha256=a8773c695a24aafd1ea095fc00949cc5d10b5366794c91a6a1f7e96a7f6a6194
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-30T13:37:56.171266Z","feature_name":"Shell control panel","feature_path":"features/shell/control-panel.feature","background_hash":"6a881712f550f9c1872c40dd8d8048136add8a7362a1cfd554280dbdc8f31927","implementation_hash":"unknown","scenarios":[{"index":3,"name":"Step button advances simulation time display","scenario_hash":"7efc397f18dd5a6e4abe1ef9a13346b443cc5ce8849d5ca16d71c7d3f19b0c12","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-06-30T13:37:56.171266Z"}]}
# acceptance-mutation-manifest-end

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