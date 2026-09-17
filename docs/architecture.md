# GearMastery core architecture

## Data flow

Gameplay listeners are intentionally thin. A listener builds a source-specific context and calls `ProgressionService`. The service resolves the item profile, fires `GearExperienceGainEvent`, then persists XP through `GearItemRepository`, performs every required level-up, and emits one `GearLevelUpEvent` for each completed level. A cancelled or non-positive gain does not initialize or otherwise modify an uninitialized item.

The current vertical slice is `BlockBreakEvent` mining XP. It ignores cancelled events and, by default, Creative players. It awards XP only when the held item's profile enables `block_break` and the broken block is configured in `xp/blocks.yml`.

## Persistence and profiles

Every initialized item stores a schema version, a GearMastery UUID, profile ID, level, current XP, and lifetime XP in its own Paper Persistent Data Container. `GearItemRepository` is the only class that knows the keys. This makes two otherwise identical `ItemStack`s independent without a database.

Item progress is durable while profile behavior is live-configured. `profile-aliases` can remap retired profile IDs. An item whose profile cannot be resolved is intentionally left readable but receives no XP or stat processing until an administrator remaps it.

## Curves and stats

`LevelingCurve` has linear, quadratic, and exponential implementations. It returns the XP needed for the transition from the current level to the next level. The configured maximum level is independent from each stat cap.

`StatRule` supports additive scaling (an absolute result cap) and multiplicative scaling (a multiplier cap). `StatValueCalculator` and `StatDeltaCalculator` are pure: an item modifier is always `scaled baseline - baseline`, never a scale of a previously applied GearMastery value.

`StatApplicationService` is called when an item is initialized, after the final state of an XP transaction, after a level set, and on selected lazy accesses. It dispatches to registered handlers and records an applied level, stat schema, and configuration generation in item PDC. A reload increments the generation; it does not scan player inventories or containers.

The first native application captures only the data GearMastery needs: numeric item-attribute baselines, max damage, and tool default/rule speeds. This avoids serializing complete item stacks. Existing non-GearMastery attribute entries are preserved. GearMastery removes and replaces only key-based modifiers in its own namespace, so repeated application is idempotent.

Durability is implemented through `MAX_DAMAGE` and `DAMAGE`. When maximum damage changes, GearMastery preserves the item's relative remaining durability using nearest-integer rounding. Vanilla damage, Unbreaking, Mending, and repairs continue to update `DAMAGE` normally.

Mining speed is implemented with `TOOL`. The handler rebuilds the component from its effective rules, retaining block registry sets, `correctForDrops`, `damagePerBlock`, and creative behavior, while scaling only captured non-null speeds and the default speed.

Native Paper item data components implement `ATTRIBUTE_MODIFIERS`, `MAX_DAMAGE`, and `TOOL`. Attack damage, attack speed, attack knockback, armor, armor toughness, and knockback resistance use key-based `ADD_NUMBER` modifiers with `MAINHAND` or the correct armor `EquipmentSlotGroup`. PDC remains the mechanism for GearMastery-owned state.

## Configuration and extension

Configurations are parsed into immutable snapshots and atomically replaced only after validation succeeds. YAML syntax failures, unknown fully resolved profile curves, non-positive or overflowing reachable curve transitions, and ambiguous inherited material ownership reject a reload while retaining the active snapshot. Profiles support one parent, material lists, source enablement, stat rules, overrides, and aliases. A material override intentionally selects an owner; otherwise each material must resolve to exactly one profile.

To add an XP source, implement or register an `ExperienceSource`, listen to the relevant Paper event, build an `ExperienceContext`, and call `ProgressionService`. Do not put progression calculations in listeners. To add a stat, add a `StatType` and a focused `StatHandler`; gameplay effects belong in that handler rather than in profile parsing.

## Paper 26.2 limits and deliberate differences

This project targets Paper 26.2 and Java 25 only. It uses Paper APIs, not NMS, reflection, Mixins, client networking, or a datapack/JSON system. That deliberately differs from WeaponLeveling's mod-side NBT, Mixins, networking, and JSON definitions.

Projectile damage, fishing luck/speed, hidden enchantment effect multipliers, and durability prevention remain deferred. The current clone policy is unchanged: copied item stacks may carry the same GearMastery UUID until a dedicated anti-dupe branch defines a policy.
