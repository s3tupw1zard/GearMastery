# GearMastery core architecture

## Data flow

Gameplay listeners are intentionally thin. A listener builds a source-specific context and calls `ProgressionService`. The service resolves the item profile, fires `GearExperienceGainEvent`, persists XP through `GearItemRepository`, performs every required level-up, then emits one `GearLevelUpEvent` for each completed level.

The current vertical slice is `BlockBreakEvent` mining XP. It ignores cancelled events and, by default, Creative players. It awards XP only when the held item's profile enables `block_break` and the broken block is configured in `xp/blocks.yml`.

## Persistence and profiles

Every initialized item stores a schema version, a GearMastery UUID, profile ID, level, current XP, and lifetime XP in its own Paper Persistent Data Container. `GearItemRepository` is the only class that knows the keys. This makes two otherwise identical `ItemStack`s independent without a database.

Item progress is durable while profile behavior is live-configured. `profile-aliases` can remap retired profile IDs. An item whose profile cannot be resolved is intentionally left readable but receives no XP or stat processing until an administrator remaps it.

## Curves and stats

`LevelingCurve` has linear, quadratic, and exponential implementations. It returns the XP needed for the transition from the current level to the next level. The configured maximum level is independent from each stat cap.

`StatRule` supports additive scaling (an absolute result cap) and multiplicative scaling (a multiplier cap). `StatValueCalculator` is pure; `StatHandlerRegistry` is the extension point for handlers that later apply an evaluated value to Paper.

Native Paper item data components are the planned mechanism for item-native features such as `ATTRIBUTE_MODIFIERS`, `MAX_DAMAGE`, `TOOL`, and enchantment components. PDC remains the mechanism for GearMastery-owned state.

## Configuration and extension

Configurations are parsed into immutable snapshots and atomically replaced only after validation succeeds. Profiles support one parent, material lists, source enablement, stat rules, overrides, and aliases. Resolution is material override, then matching profile, then no profile.

To add an XP source, implement or register an `ExperienceSource`, listen to the relevant Paper event, build an `ExperienceContext`, and call `ProgressionService`. Do not put progression calculations in listeners. To add a stat, add a `StatType` and a focused `StatHandler`; gameplay effects belong in that handler rather than in profile parsing.

## Paper 26.2 limits and deliberate differences

This project targets Paper 26.2 and Java 25 only. It uses Paper APIs, not NMS, reflection, Mixins, client networking, or a datapack/JSON system. That deliberately differs from WeaponLeveling's mod-side NBT, Mixins, networking, and JSON definitions.

Attack attributes, armor attributes, and max durability can later be represented natively through current Paper components. Mining speed, fishing timing/luck, projectile tuning, hidden enchantment effect multipliers, and durability prevention require focused runtime/event handlers. They are modeled but not applied by this foundation, so it does not claim unsupported server-native behavior.
