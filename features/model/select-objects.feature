# mutation-stamp: sha256=3f327bbba8756327355305ddebbfe31902998e7a7a8e70552f51bb04352e2bb6
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-27T15:28:45.061074Z","feature_name":"Select objects","feature_path":"features/model/select-objects.feature","background_hash":"cb87b75886b11f0d16a0b473329b5f45738246fc49979a6f28d49866e872c997","implementation_hash":"unknown","scenarios":[]}
# acceptance-mutation-manifest-end

Feature: Select objects

Background:
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And converter Converter1 at 100 250

# select-objects-01
Scenario: Click selects stock
  When I click select stock Stock1
  Then stock Stock1 should be selected

# select-objects-01b
Scenario: Click selects converter
  When I click select converter Converter1
  Then converter Converter1 should be selected

# select-objects-02
Scenario: Clicking a selected object deselects it
  When I click select stock Stock1
  And I click select stock Stock1
  Then stock Stock1 should not be selected
  And the selection count should be 0

# select-objects-03
Scenario: Plain click replaces the current selection
  When I click select stock Stock1
  And I click select stock Stock2
  Then stock Stock1 should not be selected
  And stock Stock2 should be selected

# select-objects-04
Scenario: Shift click adds to the selection
  When I click select stock Stock1
  And I shift click select stock Stock2
  Then stock Stock1 should be selected
  And stock Stock2 should be selected
  And the selection count should be 2

# select-objects-05
Scenario: Shift clicking a selected object removes it from the selection
  When I click select stock Stock1
  And I shift click select stock Stock2
  And I shift click select stock Stock1
  Then stock Stock1 should not be selected
  And stock Stock2 should be selected
  And the selection count should be 1

# select-objects-06
Scenario: Marquee select chooses intersecting objects
  When I marquee select from 50 50 to 200 200
  Then stock Stock1 should be selected
  And stock Stock2 should not be selected
  And converter Converter1 should not be selected
  And the selection count should be 1

# select-objects-07
Scenario: Escape clears the selection
  When I click select stock Stock1
  And I clear the selection
  Then nothing should be selected

# select-objects-08
Scenario: Selection is disabled while a placement tool is armed
  When I arm the flow placement tool
  And I click select stock Stock1
  Then nothing should be selected