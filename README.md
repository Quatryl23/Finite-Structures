# Finite Structures Mod
- Data packs can define Structure Limits to limit the amount of structures of a specified type that generate.
- The structure locations are stored which makes /locate and Explorer Map generation faster for these structures
- Fixes [MC-177381](https://mojira.dev/MC-177381) to return the correct distances on /locate

# Structure Limit Syntax
Path in datapack: `data/<namespace>/worldgen/structure_limit`

File extension: `.json`
```
{
  "target": "#wonders:wonder",
  "count": 7,
  "priority": 0,
  "group_mode": "group",
  "override_mode": "reduce_count",
  "limit_mode": "nearest",
  "center": {
    "source": "spawn",
    "x_offset": 0,
    "z_offset": 0
  }
}
```
`target`: The structures to be limited
- A structure, structure tag or list of structures
- Required
- Examples: `"#minecraft:village"`, `"minecraft:mansion"`, `["minecraft:desert_pyramid","minecraft:jungle_pyramid"]`

`count`: The number of structures you want to be generated. The resulting count of the provider is clamped between 0 and 1024
- A [Number Provider](https://minecraft.wiki/w/Number_provider)
- Required
- Examples: `1`, `{"type":"uniform","min":1,"max":2}`, `{"type":"binomial","n":{"type":"uniform","min":13,"max":23},"p":0.5}`

`priority`: Controls how structure limits that target the same structures behave. Every structure can have exactly one structure limit targeting that structure.
If multiple structure limits target the same structure, the one with the highest `priority` is applied. If multiple limits have the same `priority`, consistency
can't be guaranteed
- Any integer, can be negative
- Optional, defaults to `0`
- Examples: `0`, `-2000`, `1000`
- Conventions: Use `0` if you target a structure in the same datapack; use `100` if you target structures in a datapack that does not have any limits by itself; use `100` to override a target with a priority of `0`, use `200` to override a target with a priority of `100`, ...;
use numbers in between if you aim to override one limit but be overridden by another  

`group_mode`: How the limit should be applied to a list of multiple structures. `separate` applies the limit to all structures separately. `group` creates a group of structures
with a shared limit that doesn't distinguish different structures in the group. A structure is removed from the group if a higher priority limit targets that structure. Use
`override_mode` to control the reaction to group modifications
- Either `"separate"` or `"group"`
- Optional, defaults to `"separate"`

`override_mode`: Only required if `group_mode` is `group`. What to do if a structure is removed from a group because it is targeted by a limit with higher priority.
`ignore` does nothing, `remove_group` removes the limit for the whole group and `reduce_count` reduces the resulting count proportionally to the reduced group size.
(A count of `24` becomes `8` if 4 of 6 structures are removed from the group)
- Either `"ignore"`, `"remove_group"` or `"reduce_count"`
- Optional, defaults to `"reduce_count"`

`limit_mode`: How to decide which structure positions are allowed to generate. `found_first` allows the first structures that are found (because a player has
either approached the generation position, found a map to the structure or located the structure using /locate) to be generated, `nearest` searches around a `center` position
and allows the nearest found structures to be generated. `found_first` is dependent on player actions and thus not consistent per world seed, `nearest` is consistent but slows
down world generation when first entering a newly created world, especially for a large `count`. 
- Either `"nearest"` or `"found_first"`
- Optional, defaults to `"nearest"`

`center`: Only required if `limit_mode` is `nearest`. Specifies the position to use as position for the search for nearest structures. Consists of a `source` that
defines the original position and `x_offset`/`z_offset` modifiers. For dimensions that have a `coordinate_scale` other than 1, the position is adjusted to correspond
to the `center` position in the Overworld. (For example, a `center` position of [x=800, z=800] becomes [x=100, z=100] in the Nether)
- Optional, defaults to `{"source":"spawn","x_offset":0,"z_offset"_0}`

`source`: What position to use as the base for the `center` positon. `spawn` uses the world spawn, `fixed` uses the position [x=0, z=0]
- Either `"spawn"` or `"fixed"`
- Optional, defaults to `"spawn"`

`x_offset`: The offset to apply to the `source` position in x-direction
- Any integer, can be negative
- Optional, defaults to `0`

`z_offset`: The offset to apply to the `source` position in z-direction
- Any integer, can be negative
- Optional, defaults to `0`
