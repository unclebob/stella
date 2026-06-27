# mutation-stamp: sha256=924380c300bf6ad02046c164da4c5505230bfa46b88eb9dcb34c9cd643995fcb
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-27T15:28:49.872025Z","feature_name":"Delete selection","feature_path":"features/model/delete-selection.feature","background_hash":"000c135b9cc93370cc3c350f602528b0ca49d9548108f78d8be293c661e067f0","implementation_hash":"unknown","scenarios":[]}
# acceptance-mutation-manifest-end

Feature: Delete selection

Background:
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200

# delete-selection-01
Scenario: Delete selected stock
  When I click select stock Stock1
  And I delete the selection
  Then the diagram should not contain stock Stock1
  And the diagram stock count should be 1
  And the diagram should contain stock Stock2

# delete-selection-02
Scenario: Deleting stock removes attached flows
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I click select stock Stock1
  And I delete the selection
  Then the diagram should not contain stock Stock1
  And the diagram should not contain flow Flow1
  And the diagram should contain stock Stock2

# delete-selection-03
Scenario: Deleting flow removes connectors on that flow
  And flow Flow1 runs from stock Stock1 to stock Stock2
  And converter Converter1 at 100 250
  And connector Connector1 runs from converter Converter1 to flow Flow1
  When I click select flow Flow1
  And I delete the selection
  Then the diagram should not contain flow Flow1
  And the diagram should not contain connector Connector1
  And the diagram should contain stock Stock1
  And the diagram should contain converter Converter1

# delete-selection-04
Scenario: Deleting converter removes its connectors
  And converter Converter1 at 100 250
  And connector Connector1 runs from stock Stock1 to converter Converter1
  When I click select converter Converter1
  And I delete the selection
  Then the diagram should not contain converter Converter1
  And the diagram should not contain connector Connector1
  And the diagram should contain stock Stock1

# delete-selection-05
Scenario: Delete multiple selected objects
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I click select stock Stock1
  And I shift click select stock Stock2
  And I delete the selection
  Then the diagram stock count should be 0
  And the diagram should not contain flow Flow1

# delete-selection-06
Scenario: Delete with empty selection does nothing
  When I delete the selection
  Then the diagram stock count should be 2
  And the diagram should contain stock Stock1
  And the diagram should contain stock Stock2