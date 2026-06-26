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