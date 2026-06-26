# mutation-stamp: sha256=950c7679eb743e537ebc9f1a47fc21d80cda51ee83b2c03199ad1071ed161445
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-26T20:31:38.481011Z","feature_name":"Shell application","feature_path":"features/shell/application.feature","background_hash":"6a881712f550f9c1872c40dd8d8048136add8a7362a1cfd554280dbdc8f31927","implementation_hash":"unknown","scenarios":[{"index":1,"name":"Showing the about dialog","scenario_hash":"36effe48cccf8e1c69212b3be5c5a30fd044b25081eeed9a4ba4d57f96eca79b","mutation_count":1,"result":{"Total":1,"Killed":1,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:19:38.288817Z"},{"index":0,"name":"Default shell is visible with titled window","scenario_hash":"089cc5c0b6a202ff6768cd17a9fe95e9bf9844e83865e90a38ea50b00a064a3d","mutation_count":1,"result":{"Total":1,"Killed":1,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:17:35.650878Z"}]}
# acceptance-mutation-manifest-end

Feature: Shell application

Background:
  Given a default shell application

# shell-app-01
Scenario Outline: Default shell is visible with titled window
  Then the shell window title should be <title>
  And the shell should be showing

  Examples:
    | title  |
    | Stella |

# shell-app-02
Scenario Outline: Showing the about dialog
  When I show the about dialog
  Then the about dialog should be visible
  And the about dialog text should include <app_name>

  Examples:
    | app_name |
    | Stella   |

# shell-app-03
Scenario: Quitting the shell application
  When I quit the shell application
  Then the shell should not be showing