# Upstream report drafts / 上游反馈草稿

复制下面任意一段提交到对应项目的 issue 区即可（建议附上你的 `spark` 报告文件）。

---

## 1) For Aeronautics: Interstellar Expansion (`vsie`)

**Title:** Holding a Create blueprint that contains vsie blocks drops the client to ~1 FPS

**Environment**

- Minecraft 1.21.1, NeoForge 21.1.248
- Create 6.0.10 (Catnip / `net.createmod.catnip.levelWrappers.SchematicLevel` used for the blueprint preview)
- Aeronautics: Interstellar Expansion 1.0.1 (`AeroIE-1.0.1.jar`, modId `vsie`)
- Profile taken with spark 1.10.124

**Steps to reproduce**

1. Build a Create blueprint that contains any vsie block (control seat, turret, thruster, screen, fuel tank, …).
2. Hold that blueprint in hand (so Create shows its ghost preview).
3. The client drops from ~86 FPS to **~1 FPS** while the preview is active. Removing the vsie blocks from the blueprint restores full performance.

**What spark shows (hot chain on the Render thread)**

```
com.simibubi.create.content.schematics.client.SchematicHandler.render
  -> com.simibubi.create.content.schematics.client.SchematicRenderer.render
     -> com.simibubi.create.foundation.render.BlockEntityRenderHelper.renderBlockEntities
        -> com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer.render   <-- called every frame
           -> com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer.sampledModelLight

net.createmod.catnip.levelWrappers.SchematicChunkSource.getChunk
  -> net.minecraft.world.level.chunk.LevelChunk.<init> (chunk construction for the preview)
```

Additionally, the control seat's link bookkeeping keeps running inside the preview's fake level:

```
[Render thread] com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity:
  [VSIE-LINK-TRACE] phase=READ_APPLY context=minecraft:overworld/SchematicLevel@BlockPos{...}
[Server thread] ... phase=READ_SKIPPED_PARTIAL ... / phase=WRITE ...
```

(`AbstractControlSeatBlockEntity#read/write` and `SeatRegistry#registerOrUpdateControlSeat` are reached from the preview world too.)

**Suggestion**

Skip block-entity rendering and the linked-peripheral bookkeeping when the level is a schematic preview / virtual world (e.g. `SchematicLevel`, `VirtualRenderWorld`), or make `AlwaysRenderGeoBlockRenderer` check for such levels. That would fix the frame drop for anyone who uses Create blueprints with this mod.

---

## 2) For ByePregen (author: MoePus)

**Title:** `replaceMissingSections` runs inside Create's schematic preview chunk construction

**Summary**

While profiling the issue above, ByePregen's hook shows up inside Create's blueprint preview chunk creation on the render thread:

```
net.createmod.catnip.levelWrappers.SchematicChunkSource.getChunk
  -> net.minecraft.world.level.chunk.LevelChunk.<init> / ChunkAccess.<init>
     -> net.minecraft.world.level.chunk.ChunkAccess.redirect$bdf000$byepregen$replaceMissingSections
     -> net.minecraft.world.level.chunk.PalettedContainer.count
     -> net.minecraft.world.level.chunk.LevelChunkSection.recalcBlockCounts
```

Suggestion: skip the redirect for fake/virtual levels (level class name containing `Schematic` or `VirtualRenderWorld`), since those chunks are re-created constantly while a blueprint is held.
