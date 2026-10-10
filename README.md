# Finite Structures Mod
- Data packs can define Structure Limits to limit the amount of structures of a specified type that generate.
- The structure locations are stored which makes /locate and Explorer Map generation faster for these structures
- Fixes [MC-177381](https://mojira.dev/MC-177381) to return the correct distances on /locate

## Structure Limit Syntax
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
- A [Number Provider](https://minecraft.wiki/w/Number_provider) (integer until 1.20.1)*
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
defines the original position and `x_offset`/`z_offset` modifiers. If the offset is not constant, the `center` position will be evaluated once and apply to every dimension
and every affected structure. For dimensions that have a `coordinate_scale` other than 1, the position is adjusted to correspond
to the `center` position in the Overworld. (For example, a `center` position of [x=800, z=800] becomes [x=100, z=100] in the Nether)
- Optional, defaults to `{"source":"spawn","x_offset":0,"z_offset"0}`

`source`: What position to use as the base for the `center` positon. `spawn` uses the world spawn, `fixed` uses the position [x=0, z=0]
- Either `"spawn"` or `"fixed"`
- Optional, defaults to `"spawn"`

`x_offset`: The offset to apply to the `source` position in x-direction
- A [Number Provider](https://minecraft.wiki/w/Number_provider) (integer until 1.20.1)*
- Optional, defaults to `0`
- Examples: `0`, `-12000`, `{"type":"uniform","min":-30000000,"max":30000000}`, `{"type":"binomial","n":{"type":"uniform","min":10000,"max":2000000},"p":0.99}`

`z_offset`: The offset to apply to the `source` position in z-direction
- A [Number Provider](https://minecraft.wiki/w/Number_provider) (integer until 1.20.1)*
- Optional, defaults to `0`
- Examples: `0`, `-12000`, `{"type":"uniform","min":-30000000,"max":30000000}`, `{"type":"binomial","n":{"type":"uniform","min":10000,"max":2000000},"p":0.99}`

## Examples
```
{
  "target": "#wonders:wonder",
  "count": 1
}
```
All structures from the `wonders:wonder` tag will generate once, with consistent positions per seed.

```
{
  "target": "#minecraft:ruined_portal",
  "priority": 100,
  "group_mode": "group",
  "count": 5
}
```
In every dimension, 5 structures from the `#minecraft:ruined_portal` tag will generate. In the overworld, this means a total of
five ruined portals from different biomes will generate, not guaranteeing any biome variant to spawn in every world. In the nether,
`minecraft:ruined_portal_nether` will generate five times as it is the only entry in the tag that can generate in the nether. The
limit will override limits with a priority below 100.

```
{
  "target": ["minecraft:desert_pyramid", "minecraft:ruined_portal_desert"],
  "group_mode": "group",
  "count": 25
}
```
The combined count of generated structures of the types `minecraft:desert_pyramid` and `minecraft:ruined_portal_desert` will be 25.
If the limit above was to be loaded in the same world, this limit would be overridden for `minecraft:ruined_portal_desert` since the
`#minecraft:ruined_portal` tag contains `minecraft:ruined_portal_desert`. As no `override_mode` is specified, the default `reduce_count`
would cause the count to be multiplied by 1/2, resulting in 13 structures of type `minecraft:desert_pyramid` generating.

```
{
  "target": "minecraft:mansion",
  "limit_mode": "found_first",
  "count": {
    "type": "uniform",
    "min": -1,
    "max": 41
  }
}
```
A random number of Woodland Mansions (`minecraft:mansion`) between 0 and 41 would generate, with each count between 0 and 41 being equally likely except for 0.
Since -1 is clamped to 0, the outcome of 0 mansions generating is twice as likely. The positions of the mansions that generate are not consistent with the seed,
but instead the first 0-41 mansions that are found by players.
```
{
  "target": "#minecraft:village",
  "count": 120,
  "group_mode": "group",
  "override_mode": "remove_group",
  "center": {
    "source": "fixed",
    "x_offset": {
      "type": "uniform",
      "min": -1000,
      "max": 1000
    },
    "z_offset": {
      "type": "uniform",
      "min": -1000,
      "max": 1000
    }
  }
}
```
A random number between -1000 and 1000 is picked for both x and z coordinate to get a position, e.g. [x=-451, z=294]. The 120 villages nearest to that position
will generate. If any village has another limit with a higher `priority` applied to it, however, the limit to villages will not apply (`remove_group`) and all
villages not affected by any limit will generate normally.

__\* Note: Versions 1.20.1 and below do not support [Number Providers](https://minecraft.wiki/w/Number_provider). All Number
provider fields are replaced with integers for these versions.__