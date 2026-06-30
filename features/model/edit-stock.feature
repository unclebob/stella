# mutation-stamp: sha256=0db3f918852850c261c1f08b38a031d1ad31ebcd1774e18223e699782395dcbc
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-27T15:10:40.967818Z","feature_name":"Edit stock","feature_path":"features/model/edit-stock.feature","background_hash":"4c8c970e459ede1586e22cc3c60bafe2567086a820d4b039615fd7372d58ffd4","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Rename stock","scenario_hash":"649f5374fe8936effea8818a85315f16d71937b0bb8226deab10f4855283df05","mutation_count":1,"result":{"Total":1,"Killed":1,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:10:40.967818Z"},{"index":1,"name":"Edit stock initial value","scenario_hash":"5ff4d4f1e1a700643ace129c1e150a3d39d9cdc58e0229ecf7865490066409be","mutation_count":1,"result":{"Total":1,"Killed":1,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:10:40.967818Z"},{"index":2,"name":"Set stock value bounds","scenario_hash":"3702ef324370a912e575ebc2c5f7af9bd58053675b22f03473306433c51a9507","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:10:40.967818Z"}]}
# acceptance-mutation-manifest-end

Feature: Edit stock

Background:
  Given a diagram model with stock Stock1 at 200 150

# edit-stock-01
Scenario Outline: Rename stock
  When I set stock Stock1 name to <new_name>
  Then the diagram should contain stock Cats
  And stock Cats canvas name should be Cats
  And stock Cats initial value should be 0

  Examples:
    | new_name |
    | Cats     |

# edit-stock-02
Scenario Outline: Edit stock initial value
  When I set stock Stock1 initial value to <value>
  Then stock Stock1 initial value should be 25

  Examples:
    | value |
    | 25    |

# edit-stock-03
Scenario Outline: Set stock value bounds
  When I set stock Stock1 minimum to <min>
  And I set stock Stock1 maximum to <max>
  Then stock Stock1 minimum should be 0
  And stock Stock1 maximum should be 100
  And stock Stock1 canvas minimum should be 0
  And stock Stock1 canvas maximum should be 100

  Examples:
    | min | max |
    | 0   | 100 |

# edit-stock-04
Scenario: Clear stock maximum bound
  When I set stock Stock1 maximum to 50
  And I clear stock Stock1 maximum
  Then stock Stock1 should have no maximum
  And stock Stock1 should display no maximum on canvas

# edit-stock-05
Scenario: Reject duplicate stock name
  Given a diagram model with stock Stock2 at 300 200
  When I set stock Stock1 name to Stock2
  Then the stock edit should be rejected
  And the diagram should contain stock Stock1
  And the diagram should contain stock Stock2

# edit-stock-06
Scenario: Reject initial value below minimum
  When I set stock Stock1 minimum to 11
  And I set stock Stock1 initial value to 10
  Then the stock edit should be rejected
  And stock Stock1 minimum should be 11
  And stock Stock1 initial value should be 0

# edit-stock-07
Scenario: Reject initial value above maximum
  When I set stock Stock1 maximum to 39
  And I set stock Stock1 initial value to 40
  Then the stock edit should be rejected
  And stock Stock1 maximum should be 39
  And stock Stock1 initial value should be 0

# edit-stock-08
Scenario: Edit stock current value
  When I set stock Stock1 current value to 25
  Then stock Stock1 current value should be 25

# edit-stock-09
Scenario: Current value below minimum clamps to minimum
  When I set stock Stock1 minimum to 10
  And I set stock Stock1 current value to 5
  Then stock Stock1 current value should be 10

# edit-stock-10
Scenario: Current value above maximum clamps to maximum
  When I set stock Stock1 maximum to 40
  And I set stock Stock1 current value to 50
  Then stock Stock1 current value should be 40
