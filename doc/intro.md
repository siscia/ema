# Introduction to ema

Ema is an interface who handle REST data.

## Workflow

The user define what is database looks like in a YAML file.

The file is then corverted into a clojure map. See ema/possible.clj

The map after conversion will be of the the type `EntryMap`

The entry-map is used as argument by the function `generate-resource-definition`  to create a list of resource-definition, of type `ResourceDefinition`

From this point all the work is left to the custom layer.

The list of resource-definition is passed trought generate-asts whose result is passed trought generate-handler.

## Types

No yet implemented because I am not able to, however there are some types definition that can help undrstand the overall structure.

The types definition can be found in the `ema.types` namespace.
