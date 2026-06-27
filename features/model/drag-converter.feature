# mutation-stamp: sha256=7ba8e0e01d51ec2c1ae189c4c9e1f2bb56fe8e6cee5902ddd8aaec890c00922b
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-27T15:22:32.085759Z","feature_name":"Drag converter","feature_path":"features/model/drag-converter.feature","background_hash":"fd69d82c9748fd80eceff8252817a91f8218541ed07572010ad6a454fc491800","implementation_hash":"unknown","scenarios":[{"index":0,"name":"Move converter to new position","scenario_hash":"11964618c689ff18063a6731ad591b5040b1248f3da2d5803a0d06439ebe4f6b","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:22:32.085759Z"},{"index":3,"name":"Move one converter without moving the other","scenario_hash":"fcb95d81dfbd8f34aa4298aaf3a7040dc2dc99336a97793a27c328c37380a209","mutation_count":3,"result":{"Total":3,"Killed":3,"Survived":0,"Errors":0},"tested_at":"2026-06-27T15:22:32.085759Z"}]}
# acceptance-mutation-manifest-end

Feature: Drag converter

Background:
  Given converter Converter1 at 100 250

# drag-converter-01
Scenario Outline: Move converter to new position
  When I move converter Converter1 to <x> <y>
  Then converter Converter1 should be at position 220 300
  And converter Converter1 canvas position should be 220 300

  Examples:
    | x   | y   |
    | 220 | 300 |

# drag-converter-02
Scenario: Moving converter does not create another converter
  When I move converter Converter1 to 150 200
  Then the diagram converter count should be 1
  And the diagram should contain converter Converter1

# drag-converter-03
Scenario: Moving converter preserves connector endpoints
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  And connector Connector1 runs from converter Converter1 to flow Flow1
  When I move converter Converter1 to 50 180
  Then converter Converter1 should be at position 50 180
  And connector Connector1 should run from converter Converter1 to flow Flow1

# drag-converter-04
Scenario Outline: Move one converter without moving the other
  Given converter Converter2 at 300 250
  When I move converter <name> to <x> <y>
  Then converter Converter1 should be at position 120 90
  And converter Converter2 should be at position 300 250

  Examples:
    | name       | x   | y   |
    | Converter1 | 120 | 90  |