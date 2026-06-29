# mutation-stamp: sha256=7a9725d39951c91686526232f5e88e44a3f1f82c234e109ca32d25252ce98568
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-26T23:02:34.577596Z","feature_name":"Place converter","feature_path":"features/model/place-converter.feature","background_hash":"635de7f37d2581ad41a2f9b87dfe12e9adff8d18ef00706872c8f5d02d60db6c","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Place converter on diagram","scenario_hash":"56bcdcf0ef77e70fa6782f021b1076e456e1b95ddae4944518ca48ce6b0e5250","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T22:12:56.474927Z"},{"index":2,"name":"Converter placement disarms the tool","scenario_hash":"333b55f3774adab893576b8074853830dde0cf0c48f13d604ec4e11858c23611","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T22:12:56.474927Z"},{"index":3,"name":"Second converter requires re-arming the placement tool","scenario_hash":"40f2f0d1a75dd355812897b823b9955e777df3b34c3702f010699a0bc6640120","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-06-26T22:12:56.474927Z"}]}
# acceptance-mutation-manifest-end

Feature: Place converter

Background:
  Given an empty diagram model

# place-converter-01
Scenario Outline: Place converter on diagram
  When I arm the converter placement tool
  And I place a converter at <x> <y>
  Then the diagram should contain converter Converter1
  And converter Converter1 should be at position 100 250
  And converter Converter1 value should be 0

  Examples:
    | x   | y   |
    | 100 | 250 |

# place-converter-02
Scenario: Arming converter without a canvas click places nothing
  When I arm the converter placement tool
  Then the diagram converter count should be 0

# place-converter-03
Scenario Outline: Converter placement keeps the tool armed
  When I arm the converter placement tool
  And I place a converter at <x> <y>
  Then converter Converter1 should be at position 100 250
  And the converter placement tool should be armed

  Examples:
    | x   | y   |
    | 100 | 250 |

# place-converter-04
Scenario Outline: Second converter without re-arming the placement tool
  When I arm the converter placement tool
  And I place a converter at <x1> <y1>
  And I place a converter at <x2> <y2>
  Then the diagram should contain converter Converter2
  And converter Converter1 should be at position 100 250
  And converter Converter2 should be at position 300 250

  Examples:
    | x1  | y1  | x2  | y2  |
    | 100 | 250 | 300 | 250 |