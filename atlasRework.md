### Atlas Handler Consolidation

The atlas handler has had some of its logic modified to consolidate other sprites and change how to obtain a `TextureAtlasSprite`.

First, map decorations, paintings, and GUI sprites are now proper atlases with their own sheets: `Sheets#MAP_DECORATIONS_SHEET`, `PAINTINGS_SHEET`, and `GUI_SHEET` respectively.

Obtaining the `TextureAtlasSprite` from said sheets are now completely routed through the `MaterialSet`: a functional interface that takes in a `Material` (basically a sheet location and texture location), and returns the associated `TextureAtlasSprite`. The `MaterialSet` handles texture grabs for item models, block entity renderers, and entity renderers:

```java
// Here is an example material to grab the apple texture from the appropriate sheet
public static final Material APPLE = new Material(
    TextureAtlas.LOCATION_BLOCKS, // The sheet where the item textures are stored
    ResourceLocation.fromNamespaceAndPath("minecraft", "item/apple") // The texture name according to the sprite contents
);
// You can also do the same using Sheets.ITEMS_MAPPER.defaultNamespaceApply("apple")

// For some item model
public class ExampleUnbakedItemModel implements ItemModel.Unbaked {
    
    // ...

    @Override
    public ItemModel bake(ItemModel.BakingContext ctx) {
        TextureAtlasSprite appleTexture = ctx.materials().get(APPLE);
        // ...
    }
}

// For some special item model
public class ExampleUnbakedSpecialModel implements SpecialModelRenderer.Unbaked {
    
    // ...

    @Override
    @Nullable
    public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext ctx) {
        TextureAtlasSprite appleTexture = ctx.materials().get(APPLE);
        // ...
    }
}

// For some block entity renderer
public class ExampleBlockEntityRenderer implements BlockEntityRenderer<ExampleBlockEntity> {

    public ExampleBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        TextureAtlasSprite appleTexture = ctx.materials().get(APPLE);
        // ...
    }

    // ...
}


// For some entity renderer
public class ExampleEntityRenderer implements EntityRenderer<ExampleEntity, ExampleEntityState> {

    public ExampleEntityRenderer(EntityRendererProvider.Context ctx) {
        TextureAtlasSprite appleTexture = ctx.getMaterials().get(APPLE);
        // ...
    }

    // ...
}
```

- `assets/minecraft/shaders/core`
    - `blit_screen.json` -> `screenquad.json`, using no-format triangles instead of positioned quads
    - `position_color_lightmap.*` are removed
    - `position_color_tex_lightmap.*` are removed
- `com.mojang.blaze3d.vertex`
    - `CompactVectorArray` - An holder that smashes a list of float vectors into a single sequential array.
    - `MeshData$SortState#centroids` now returns a `CompactVectorArray`
    - `VertexSorting`
        - `byDistance` now takes in a `Vector3fc` instead of a `Vector3f`
        - `sort` now takes in a `CompactVectorArray` instead of a `Vector3f[]`
- `net.minecraft.client.Minecraft`
    - `getTextureAtlas` -> `AtlasManager#getAtlasOrThrow`, not one-to-one
    - `getPaintingTextures`, `getMapDecorationTextures`, `getGuiSprites` -> `getAtlasManager`, not one-to-one
- `net.minecraft.client.animation.Keyframe` now has an overload that takes in the `preTarget` and `postTarget` instead of one simple `target`, taking in a `Vector3fc` instead of a `Vector3f`
- `net.minecraft.client.entity`
    - `ClientAvatarEntity` - The client data of the avatar.
    - `ClientAvatarState` - The movement state of the avatar.
    - `ClientMannequin` - The client version of the `Mannequin` entity.
- `net.minecraft.client.gui`
    - `GuiGraphics`
        - `renderOutline` -> `submitOutline`
        - `renderDeferredTooltip` -> `renderDeferredElements`, not one-to-one
        - `submitBannerPatternRenderState` now takes in a `BannerFlagModel` instead of a `ModelPart`
    `GuiSpriteManager` class is removed
- `net.minecraft.client.gui.render.GuiRenderer` now takes in the `SubmitNodeCollector` and `FeatureRenderDispatcher`
    - `MIN_GUI_Z` is now public
- `net.minecraft.client.gui.render.pip`
    - `GuiBannerResultRenderer` now takes in a `MaterialSet`
    - `GuiSignRenderer` now takes in a `MaterialSet`
- `net.minecraft.client.gui.render.state.TiledBlitRenderState` - A render state for building a sprite using tiling, usually for tile or nine slice textures.
- `net.minecraft.client.gui.render.state.pip.GuiBannerResultRenderState` now takes in a `BannerFlagModel` instead of a `ModelPart`
- `net.minecraft.client.model`
    - `AbstractPiglinModel#createArmorMeshSet` - Creates the model meshes for each of the humanoid armor slots.
    - `ArmedModel` now has a generic of the `EntityRenderState`
        - `translateToHand` now takes in the entity render state
    - `ArmorStandArmorModel#createBodyLayer` -> `createArmorLayerSet`, not one-to-one
    - `BellModel$State` - Represents the state of the backing object.
    - `BookModel$State` - Represents the state of the backing object.
    - `BreezeModel`
        - `createBodyLayer` -> `createBaseMesh`, now private
            - Replaced by `createBodyLayer`, `createWindLayer`, `createEyesLayer`
    - `CopperGolemModel` - A model for the copper golem entity.
    - `CopperGolemStatueModel` - A model for the coper golem statue.
    - `CreakingModel`
        - `NO_PARTS`, `getHeadModelParts` are removed
        - `createEyesLayer` - Creates the eyes of the model.
    - `EntityModel#setupAnim` -> `Model#setupAnim`
    - `GuardianParticleModel` - The particle spawned from a guardian.
    - `HeadedModel#translateToHead` - Transforms the pose stack to the head's position and rotation.
    - `HumanoidArmorModel` -> `HumanoidModel#createArmorMeshSet`, not one-to-one
    - `HumanoidModel#copyPropertiesTo` is removed
    - `Model` now takes in a generic representing the render state
    - `PlayerCapeModel` now extends `PlayerModel`
    - `PlayerEarsModel` now extends `PlayerModel`
    - `PlayerModel` render state has been broadened to `AvatarRenderState`
        - Static fields are now `protected`
        - `createArmorMeshSet` - Creates the model meshes for each of the humanoid armor slots.
    - `SkullModelBase$State` - Represents the state of the backing object.
    - `SpinAttackEffectModel` generic has been broadened to `AvatarRenderState`
    - `VillagerLikeModel` now takes in a generic for the render state
        - `hatVisible` is removed
            - Replaced by `VillagerModel#createNoHatModel`
        - `translateToArms` now takes in the render state
    - `WardenModel`
        - `createTendrilsLayer`, `createHeartLayer`, `createBioluminescentLayer`, `createPulsatingSpotsLayer` - Creates the layers used by the warden's `RenderLayer`s.
        - `getTendrilsLayerModelParts`, `getHeartLayerModelParts`, `getBioluminescentLayerModelParts`, `getPulsatingSpotsLayerModelParts` are removed
    - `ZombieVillagerModel`
        - `createArmorLayer` -> `createArmorLayerSet`, not one-to-one
        - `createNoHatLayer` - Creates the model without the hat layer.
- `net.minecraft.client.model.geom.ModelPart`
    - `copyFrom` is removed
    - `$Polygon#normal` is now a `Vector3fc` instead of a `Vector3f`
    - `$Vertex`
        - `pos` -> `x`, `y`, `z`
        - `worldX`, `worldY`, `worldZ` - Returns the coordinates scaled down by a factor of 16.
- `net.minecraft.client.model.geom.builders.PartDefinition`
    - `clearRecursively` - Clears all children parts and its sub-children.
    - `retainPartsAndChildren` - Retains the specified parts from its root and any sub-children.
    - `retainExactParts` - Retains only the top level part, clearing out all others and sub-children.
- `net.minecraft.client.particle`
    - `AttackSweepParticle` now extends `SingleQuadParticle`
    - `BaseAshSmokeParticle` now extends `SingleQuadParticle` and is `abstract`
    - `BlockMarker` now extends `SingleQuadParticle`
    - `BreakingItemParticle` now extends `SingleQuadParticle`
        - The constructor takes in a `TextureAtlasSprite` instead of the `ItemStackRenderState`
        - `$ItemParticleProvider#calculateState` -> `getSprite`, not one-to-one
    - `BubbleColumnUpParticle` now extends `SingleQuadParticle`
    - `BubbleParticle` now extends `SingleQuadParticle`
    - `BubblePopParticle` now extends `SingleQuadParticle`
    - `CampfireSmokeParticle` now extends `SingleQuadParticle`
    - `CritParticle` now extends `SingleQuadParticle`
    - `DragonBreathParticle` now extends `SingleQuadParticle`
        - `$Provider` generic now uses a `PowerParticleOption`
    - `DripParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
        - `create*Particle` methods -> `$*Provider` classes
    - `DustParticleBase` now extends `SingleQuadParticle`
    - `ElderGuardianParticleGroup` - The particle group responsible for setting up and submitting the elder guardian particle.
    - `ExplodeParticle` now extends `SingleQuadParticle`
    - `FallingDustParticle` now extends `SingleQuadParticle`
    - `FallingLeavesParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite` instead of a `SpriteSet`
    - `FireflyParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `FireworkParticles`
        - `$FlashProvider` generic now uses `ColorParticleOption`
        - `$OverlayParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `FlameParticle` now takes in a `TextureAtlasSprite`
    - `FlyStraightTowardsParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `FlyTowardsPositionParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `GlowParticle` now extends `SingleQuadParticle`
    - `GustParticle` now extends `SingleQuadParticle`
    - `HeartParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `HugeExplosionParticle` now extends `SingleQuadParticle`
    - `ItemPickupParticle` now takes in the `EntityRenderState` instead of the `EntityRenderDispatcher`
        - Fields are now all `protected` aside from the target entity
    - `ItemPickupParticleGroup` - The particle group responsible for setting up and submitting the item pickup particle.
    - `LavaParticle` now extends `SingleQuadParticle`
    - `MobAppearanceParticle` -> `ElderGuardianParticle`
    - `NoRenderParticleGroup` - The particle group that does nothing.
    - `NoteParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `Particle`
        - `rCol`, `gCol`, `bCol`, `alpha` -> `SingleQuadParticle#rCol`, `gCol`, `bCol`, `alpha`
        - `roll`, `oRoll` -> `SingleQuadParticle#roll`, `oRoll`
        - `setColor`, `setAlpha` -> `SingleQuadParticle#setColor`, `setAlpha`
        - `render`, `renderCustom` -> `ParticleGroupRenderState#submit`, not one-to-one
        - `getRenderType` -> `getGroup`
            - The original purpose of this method has been moved to `SingleQuadParticle#getLayer`
        - `getParticleGroup` -> `getParticleLimit`, not one-to-one
    - `ParticleEngine` no longer implements `PreparableReloadListener`
        - The constructor now takes in the `ParticleResources` instead of the `TextureManager`
        - `close` is removed
        - `updateCount` is now `protected`
        - `render` -> `extract`, not one-to-one
        - `destroy` -> `ClientLevel#addDestroyBlockEffect`
        - `crack` -> `ClientLevel#addBreakingBlockEffect`
        - `clearParticles` is now `public`
        - `$MutableSpriteSet` -> `ParticleResources$MutableSpriteSet`
        - `$SpriteParticleRegistration` -> `ParticleResources$SpriteParticleRegistration`
    - `ParticleGroup` - A holder of particles for a specific `ParticleRenderType`, responsible for ticking and extracting the general render state.
    - `ParticleProvider`
        - `createParticle` now takes in the `RandomSource`
        - `$Sprite#createParticle` now takes in the `RandomSource` and returns a `SingleQuadParticle` instead of a `TextureSheetParticle`
    - `ParticleRenderType` no longer takes in the `RenderType`
        - This record has been repurposed to represent a key for the particle groups
        - `TERRAIN_SHEET` -> `SingleQuadParticle$Layer#TERRAIN`
        - `PARTICLE_SHEET_OPAQUE` -> `SingleQuadParticle$Layer#OPAQUE`
        - `PARTICLE_SHEET_TRANSLUCENT` -> `SingleQuadParticle$Layer#TRANSLUCENT`
        - `CUSTOM` is replaced by a particle group that is not for `ParticleRenderType#SINGLE_QUADS`
    - `ParticleResources` - Loads the particle providers, any necessary descriptions, and computes them into their desired sprite set.
    - `PlayerCloudParticle` now extends `SingleQuadParticle`
    - `PortalParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `QuadParticleGroup` - The particle group responsible for setting up and submitting single quad particles.
    - `ReversePortalParticle` now takes in a `TextureAtlasSprite`
    - `RisingParticle` now extends `SingleQuadParticle`
    - `SculkChargeParticle` now extends `SingleQuadParticle`
    - `SculkChargePopParticle` now extends `SingleQuadParticle`
    - `SculkChargePopParticle` now extends `SingleQuadParticle`
    - `ShriekParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `SimpleAnimatedParticle` now extends `SingleQuadParticle` and is `abstract`
    - `SingleQuadParticle` now takes in a `TextureAtlasSprite`
        - `sprite` - The texture of the particle.
        - `render` -> `extract`, not one-to-one, now taking in the `QuadParticleRenderState` instead of a `VertexConsumer`
        - `renderRotatedQuad` -> `extractRotatedQuad`, not one-to-one, now taking in the `QuadParticleRenderState` instead of a `VertexConsumer`
        - `getU0`, `getU1`, `getV0`, `getV1` are no longer abstract
        - `getLayer` - Sets the render layer of the single quad.
        - `$Layer` - The layer the single quad should render in.
    - `SnowflakeParticle` now extends `SingleQuadParticle`
    - `SpellParticle` now extends `SingleQuadParticle`
        - `InstantProvider` generic now uses `SpellParticleOption`
    - `SplashParticle` now takes in a `TextureAtlasSprite`
    - `SpriteSet#first` - Returns the first texture in the sprite set.
    - `SuspendedParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `SuspendedTownParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `TerrainParticle` now extends `SingleQuadParticle`
    - `TextureSheetParticle` class is removed, use `SingleQuadParticle` instead
    - `TrailParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `TrialSpawnerDetectionParticle` now extends `SingleQuadParticle`
    - `VibrationSignalParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `WakeParticle` now extends `SingleQuadParticle`
    - `WaterCurrentDownParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
    - `WaterDropParticle` now extends `SingleQuadParticle` and takes in a `TextureAtlasSprite`
- `net.minecraft.client.player.AbstractClientPlayer` fields are now stored within `ClientAvatarState`
    - `elytraRot*` -> `*Cloak`
    - `clientLevel` is removed
    - `getDeltaMovementLerped` -> `addWalkedDistance`, not one-to-one
    - `updateBob` - Updates the bobbing motion of the camera.
- `net.minecraft.client.renderer`
    - `EndFlashState` - The render state of the end flashes.
    - `GameRenderer` now takes in the `BlockRenderDispatcher`
        - `getSubmitNodeStorage` - Gets the node submission for feature-like objects.
        - `getFeatureRenderDispatcher` - Gets the dispatcher for rendering feature-like objects.
        - `getLevelRenderState` - Gets the render state of dynamic features in a level.
    - `ItemInHandRenderer`
        - `renderItem` now takes in a `SubmitNodeCollector` instead of a `MultiBufferSource`
        - `renderHandsWithItems` now takes in a `SubmitNodeCollector` instead of a `MultiBufferSource$BufferSource`
    - `LevelRenderer` now takes in the `LevelRenderState` and `FeatureRenderDispatcher`
        - `getSectionRenderDispatcher` is now nullable
        - `tickParticles` is removed
        - `addParticle` is removed
    - `MapRenderer` now takes in an `AtlasManager` instead of a `MapDecorationTextureManager`
        - `render` now takes in a `SubmitNodeCollector` instead of a `MultiBufferSource`
    - `OrderedSubmitNodeCollector` - A submission handler for holding elements to be drawn in a given order to the screen whenever the features are dispatched.
    - `OutlineBufferSource` no longer takes in any parameters
        - `setColor` now takes in a single integer
    - `ParticleGroupRenderState` - The render state for a group of particles.
    - `ParticlesRenderState` - The render state for all particles.
    - `QuadParticleRenderState` - The render group state for all single quad particles.
    - `RenderPipelines`
        - `GUI_TEXT` - The pipeline for text in a gui.
        - `GUI_TEXT_INTENSITY` - The pipeline for text intensity when not colored in a gui.
    - `RenderStateShard#TRANSLUCENT_TARGET`, `PARTICLES_TARGET` are removed
    - `RenderType`
        - `pipeline` - The `RenderPipeline` the type uses.
        - `opaqueParticle`, `translucentParticle` are removed
        - `sunriseSunset`, `celestial` are removed
    - `ScreenEffectRenderer` now takes in a `MaterialSet`
        - `renderScreenEffect` now takes in a `SubmitNodeCollector`
    - `ShapeRenderer`
        - `renderLineBox` now takes in a `PoseStack$Pose` instead of a `PoseStack`
        - `renderFace` now takes in a `Matrix4f` instead of a `PoseStack`
    - `Sheets`
        - `GUI_SHEET`, `MAP_DECORATIONS_SHEET`, `PAINTINGS_SHEET` - Atlas textures.
        - `BLOCK_ENTITIES_MAPPER` - A mapper for block textures onto block entities.
        - `*COPPER*` - Materials for copper chests.
        - `chooseMaterial` now takes in a `ChestRenderState$ChestMaterialType` instead of a `BlockEntity` and `boolean`
    - `SkyRenderer`
        - `END_SKY_LOCATION` is now private
        - `renderSunMoonAndStars` no longer takes in the buffer source
        - `renderEndFlash` no longer takes in the buffer source
        - `renderSunriseAndSunset` no longer takes in the buffer source
    - `SpecialBlockModelRenderer`
        - `vanilla` now takes in a `SpecialModelRenderer$BakingContext` instead of an `EntityModelSet`
        - `renderByBlock` now takes in a `SubmitNodeCollector` instead of a `MultiBufferSource`, and an outline color
    - `SubmitNodeCollection` - An implementation of `OrderedSubmitNodeCollector` that holds the submitted features in separate lists.
    - `SubmitNodeCollector` - An `OrderedSubmitNodeCollector` that provides a method to change the current order that an element will be rendered in.
    - `SubmitNodeStorage` - A storage of collections held by some order.
    - `SkyRenderer`
        - `renderEndFlash` - Renders the end flashes.
        - `initTextures` - Gets the texture for the used elements.
        - `extractRenderState` - Extracts the `SkyRenderState` from the current level.
    - `WeatherEffectRenderer`
        - `render` now takes in the `WeatherRenderState` instead of an `int`, `float`, and `Level`
            - Those fields have moved to `extractRenderState`
        - `extractRenderState` - Extracts the `WeatherRenderState` from the current level.
        - `$ColumnInstance` is now public
    - `WorldBorderRenderer`
        - `render` now takes in the `WorldBorderRenderState` instead of the `WorldBorder`
        - `extract` - Extracts the `WorldBorderRenderState` from the current world border.
- `net.minecraft.client.renderer.block`
    - `BlockRenderDispatcher` now takes in a `MaterialSet`
    - `MovingBlockRenderState` - A render state for a moving block that implements `BlockAndTintGetter`.
    - `LiquidBlockRenderer#setupSprites` now take in the `BlockModelShaper` and `MaterialSet`
- `net.minecraft.client.renderer.blockentity`
    - Most methods here that take in the `MultiBufferSource` have been replaced by a `SubmitNodeCollector`, and a `ModelFeatureRenderer$CrumblingOverlay` if the method is not used for item rendering
    - Most methods change their name from `render*` to `submit*`, with the main submit method now using a `BlockEntityRenderState`
    - All `BlockEntityRenderer`s now have a `BlockEntityRenderState` generic
    - `AbstractEndPortalRenderer` - A block entity renderer for the end portal.
    - `AbstractSignRenderer`
        - `getSignModel` now returns a `Model$Simple`
        - `renderSign` -> `submitSign`, now takes in a `Model$Simple` and no longer takes in a tint color
    - `BannerRenderer` has an overload that takes in a `SpecialModelRenderer$BakingContext`
        - The `EntityModelSet` constructor now takes in the `MaterialSet`
        - `renderPatterns` -> `submitPatterns` now takes in the `MaterialSet` and `ModelFeatureRenderer$CrumblingOverlay`, the `ModelPart` has been replaced with the `Model` and its render state, a `boolean` for whether to use the entity glint, and an outline color
            - The overload with two additional `boolean`s has been removed
        - `renderSpecial` -> `submitSpecial`, now takes in the outline color
    - `BeaconRenderer#renderBeaconBeam` -> `submitBeaconBeam`, no longer takes in the game time `long`
    - `BedRenderer` has an overload that takes in a `SpecialModelRenderer$BakingContext`
        - The `EntityModelSet` constructor now takes in the `MaterialSet`
        - `renderSpecial` -> `submitSpecial`, now takes in the outline color
    - `BlockEntityRenderDispatcher` now takes in the `MaterialSet` and `PlayerSkinRenderCache`
        - `render` -> `submit`, now takes in the `BlockEntityRenderState` instead of a `BlockEntity`, no longer takes in the partial tick `float`, and takes in the `CameraRenderState`
        - `getRenderer` now has an overload that can get the renderer from its `BlockEntityRenderState`
        - `tryExtractRenderState` - Gets the `BlockEntityRenderState` from its `BlockEntity`
        - `level`, `camera`, `cameraHitResult` is removed
        - `prepare` now only takes in the `Camera`
        - `setLevel` is removed
    - `BlockEntityRenderer` now has another generic `S` representing the `BlockEntityRenderState`
        - `render` -> `submit`, taking in the `BlockEntityRenderState`, the `PoseStack`, the `SubmitNodeCollector`, and the `CameraRenderState`
        - `createRenderState` - Creates the render state object.
        - `extractRenderState` - Extracts the render state from the block entity.
    - `BlockEntityRendererProvider$Context` is now a record, taking in a `MaterialSet` and `PlayerSkinRenderCache`
        - It now has another generic `S` representing the `BlockEntityRenderState`
    - `CopperGolemStatueBlockRenderer` - A block entity renderer for the copper golem statue.
    - `DecoratedPotRenderer` has an overload that takes in a `SpecialModelRenderer$BakingContext`
        - The `EntityModelSet` constructor now takes in the `MaterialSet`
        - `render` overload -> `submit`, now takes in the outline color
    - `HangingSignRenderer`
        - `createSignModel` now returns a `Model$Simple`
        - `renderInHand` now takes in a `MaterialSet`
    - `ShelfRenderer` - A block entity renderer for a shelf.
    - `ShulkerBoxRenderer` has an overload that takes in a `SpecialModelRenderer$BakingContext`
        - The `EntityModelSet` constructor now takes in the `MaterialSet`
        - `render` overload -> `submit`, now takes in the outline color
    - `SignRenderer`
        - `createSignModel` now returns a `Model$Simple`
        - `renderInHand` now takes in a `MaterialSet`
    - `SkullBlockRenderer#submitSkull` - Submits the skull model to the collector.
    - `SpawnerRenderer#renderEntityInSpawner` -> `submitEntityInSpawner`, now takes in the `CameraRenderState`
    - `TestInstanceREnderer` now takes in the `BlockEntityRendererProvider$Context`
- `net.minecraft.client.renderer.blockentity.state`
    - `BannerRenderState` - The render state for the banner block entity.
    - `BeaconRenderState` - The render state for the beacon block entity.
    - `BedRenderState` - The render state for the bed block entity.
    - `BellRenderState` - The render state for the bell block entity.
    - `BlockEntityRenderState` - The base render state for all block entities.
    - `BlockEntityWithBoundingBoxRenderState` - The render state for a block entity with a custom bounding box.
    - `BrushableBlockRenderState` - The render state for a brushable block entity.
    - `CampfireRenderState` - The render state for the campfire block entity.
    - `ChestRenderState` - The render state for the chest block entity.
    - `CondiutRenderState` - The render state for the conduit block entity.
    - `CopperGolemStatueRenderState` - The render state for the copper golem block entity.
    - `DecoratedPotRenderState` - The render state for the decorated pot block entity.
    - `EnchantTableRenderState` - The render state for the enchantment table block entity.
    - `EndGatewayRenderState` - The render state for the end gateway block entity.
    - `EndPortalRenderState` - The render state for the end portal block entity.
    - `LecternRenderState` - The render state for the lectern block entity.
    - `PistonHeadRenderState` - The render state for the piston head block entity.
    - `ShelfRenderState` - The render state for the shelf block entity.
    - `ShulkerBoxRenderState` - The render state for the shulker box block entity.
    - `SignRenderState` - The render state for the sign block entity.
    - `SkullBlockRenderState` - The render state for the skull block entity.
    - `SpawnerRenderState` - The render state for the spawner block entity.
    - `TestInstanceRenderState` - The render state for the test instance block entity.
    - `VaultRenderState` - The render state for the vault block entity.
- `net.minecraft.client.renderer.culling.Frustum`
    - `offset` - Offsets the position.
    - `pointInFrustum` - Checks whether the provided coordinate is within the frustum.
- `net.minecraft.client.renderer.entity`
    - Most methods here that take in the `MultiBufferSource` and light coordinates integer have been replaced by `SubmitNodeCollector` and a render state param
    - Most methods change their name from `render*` to `submit*`
    - `AbstractBoatRenderer#renderTypeAdditions` -> `submitTypeAdditions`
    - `AbstractMinecartRenderer#renderMinecartContents` -> `submitMinecartContents`
    - `AbstractSkeletonRenderer` takes in a `ArmorModelSet` instead of a `ModelLayerLocation`
    - `AbstractZombieRenderer` takes in a `ArmorModelSet` instead of a model
    - `ArmorModelSet` - A holder that maps some object to each humanoid armor slot. Typically holds the layer definitions, which are then baked into their associated models.
    - `BreezeRenderer#enable` is removed
    - `CopperGolemRenderer` - The renderer for the copper golem entity.
    - `DisplayRenderer#renderInner` -> `submitInner`
    - `EnderDragonRenderer#renderCrystalBeams` -> `submitCrystalBeams`
    - `EntityRenderDispatcher` now takes in the `AtlasManager`
        - `prepare` no longer takes in the `Level`
        - `setRenderShadow`, `setRenderHitBoxes`, `shouldRenderHitBoxes` are removed
        - `extractEntity` - Creates the render state from the entity and the partial tick.
        - `render` -> `submit`, now takes in the `CameraRenderState`
        - `setLevel` -> `resetCamera`, not one-to-one
        - `getPlayerRenderer` - Gets the `AvatarRenderer` from the given client player.
        - `overrideCameraOrientation`, `distanceToSqr`, `cameraOrientation` are removed
    - `EntityRenderer`
        - `NAMETAG_SCALE` is now public
        - `render(S, PoseStack, MultiBufferSource, int)` -> `submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)`
        - `renderNameTag` -> `submitNameTag`, now takes in a `CameraRenderState`
        - `finalizeRenderState` - Extracts the information of the render state as a last step after `extractRenderState`, such as shadows.
    - `EntityRendererProvider$Context` now takes in the `PlayerSkinRenderCache` and `AtlasManager`
        - `getModelManager` is removed
        - `getMaterials` - Returns a mapper of material to atlas sprite.
        - `getPlayerSkinRenderCache` - Gets the render cache of player skins.
        - `getAtlas` - Returns the atlas for that location.
    - `EntityRenderers#createPlayerRenderers` now returns a map of `PlayerModelType`s to `AvatarRenderer`s
    - `ItemEntityRenderer`
        - `renderMultipleFromCount` -> `submitMultipleFromCount`
        - `renderMultipleFromCount(PoseStack, MultiBufferSource, int, ItemClusterRenderState, RandomSource)` -> `renderMultipleFromCount(PoseStack, SubmitNodeCollector, int, ItemClusterRenderState, RandomSource)`
    - `ItemRenderer` no longer takes in the `ItemModelResolver`
        - `getArmorFoilBuffer` -> `getFoilRenderTypes`, not one-to-one
        - `renderStatic` methods are removed
    - `MobRenderer#checkMagicName` - Returns whether the custom name matches the given string.
    - `PiglinRenderer` takes in a `ArmorModelSet` instead of a `ModelLayerLocation`
    - `TntMinecartRenderer#renderWhiteSolidBlock` -> `submitWhiteSolidBlock`, now takes in an outline color
    - `ZombieRenderer` takes in a `ArmorModelSet` instead of a `ModelLayerLocation`
    - `ZombifiedPiglinPiglinRenderer` takes in a `ArmorModelSet` instead of a `ModelLayerLocation`
- `net.minecraft.client.renderer.entity.layers`
    - `ArrowLayer` now deals with `AvatarRenderState` instead of `PlayerRenderState`
    - `BeeStingerLayer` now deals with `AvatarRenderState` instead of `PlayerRenderState`
    - `BlockDecorationLayer` - A layer that handles a block model transformed by an entity.
    - `BreezeWindLayer` now takes in the `EntityModelSet` instead of the `EntityRendererProvider$Context`
    - `CapeLayer` now deals with `AvatarRenderState` instead of `PlayerRenderState`
    - `CustomHeadLayer` now takes in the `PlayerSkinRenderCache`
    - `Deadmau5EarsLayer` now deals with `AvatarRenderState` instead of `PlayerRenderState`
    - `EquipmentLayerRenderer#renderLayers` now takes in the render state, `SubmitNodeCollector`, outline color, and an initial order instead of a `MultiBufferSource`
    - `HumanoidArmorLayer` now takes in `ArmorModelSet`s instead of models
        - `setPartVisibility` is removed
    - `ItemInHandLayer#renderArmWithItem` -> `submitArmWithItem`
    - `LivingEntityEmissiveLayer` now takes in a function for the texture instead of a `ResourceLocation` and a model instead of the `$DrawSelector`
        - `$DrawSelector` is removed
    - `ParrotOnShoulderLayer` now deals with `AvatarRenderState` instead of `PlayerRenderState`
    - `PlayerItemInHandLayer` now deals with `AvatarRenderState` instead of `PlayerRenderState`
    - `RenderLayer`
        - `renderColoredCutoutModel`, `coloredCutoutModelCopyLayerRender` now takes in a `Model` instead of an `EntityModel`, a `SubmitNodeCollector` instead of a `MultiBufferSource`, and an integer representing the order layer for rendering
        - `render` -> `submit`, taking in a `SubmitNodeCollector` instead of a `MultiBufferSource`
    - `SimpleEquipmentLayer` now takes in an order integer
    - `SpinAttackEffectLayer` now deals with `AvatarRenderState` instead of `PlayerRenderState`
    - `StuckInBodyLayer` now has an additional generic for the render state, also taking in the render state in the constructor
        - `numStuck` now takes in an `AvatarRenderState` instead of the `PlayerRenderState`
    - `VillagerProfessionLayer` now takes in two models
- `net.minecraft.client.renderer.entity.player.PlayerRenderer` -> `AvatarRenderer`
    - `render*Hand` now takes in a `SubmitNodeCollector` instead of a `MultiBufferSource`
- `net.minecraft.client.renderer.entity.state`
    - `CopperGolemRenderState` - The render state for the copper golem entity.
    - `DisplayEntityRenderState#cameraYRot`, `cameraXRot` - The rotation of the camera.
    - `EntityRenderState`
        - `NO_OUTLINE` - A constant that represents the color for no outline.
        - `outlineColor` - The outline color of the entity.
        - `lightCoords` - The packed light coordinates used to light the entity.
        - `shadowPieces`, `$ShadowPiece` - Represents the relative coordinates of the shadow(s) the entity is casting.
    - `FallingBlockRenderState` fields and implementations have all been moved to `MovingBlockRenderState`
    - `LivingEntityRenderState`
        - `appearsGlowing` -> `EntityRenderState#appearsGlowing`, now a method
        - `customName` is removed
    - `PaintingRenderState#lightCoords` -> `lightCoordsPerBlock`
    - `PlayerRenderState` -> `AvatarRenderState`
        - `useItemRemainingTicks`, `swinging` are removed
        - `showDeadMouseEars` -> `showExtraEars`
    - `SheepRenderState`
        - `id` is removed
        - `isJebSheep` is now a field instead of a method
    - `WitherSkullRenderState#xRot`, `yRot` -> `modeState`, not one-to-one
- `net.minecraft.client.renderer.feature`
    - `BlockFeatureRenderer` - Renders the submitted blocks, block models, or falling blocks.
    - `CustomFeatureRenderer` - Renders the submitted geometry via a passed function.
    - `FeatureRenderDispatcher` - Dispatches all features to render from the submitted node collector objects.
    - `FlameFeatureRenderer` - Renders the submitted entity on fire animation.
    - `HitboxFeatureRenderer` - Renders the submitted entity hitbox.
    - `ItemFeatureRenderer` - Renders the submitted items.
    - `LeashFeatureRenderer` - Renders the submitted leash attached to entities.
    - `ModelFeatureRenderer` - Renders the submitted `Model`s.
    - `ModelPartFeatureRenderer` - Renders the submitted `ModelPart`s.
    - `NameTagFeatureRenderer` - Renders the submitted name tags.
    - `ParticleFeatureRenderer` - Renders the submitted particles.
    - `ShadowFeatureRenderer` - Render the submitted entity shadow.
    - `TextFeatureRenderer` - Renders the submitted text.
- `net.minecraft.client.renderer.item`
    - `ItemModel$BakingContext` now takes in a `MaterialSet` and `PlayerSkinRenderCache`, and implements `SpecialModelRenderer$BakingContext`
    - `ItemStackRenderState#render` -> `submit`, taking in a `SubmitNodeCollector` instead of a `MultiBufferSource` and an outline color
- `net.minecraft.client.renderer.special`
    - Most methods here that take in the `MultiBufferSource` have been replaced by `SubmitNodeCollector`
    - `ChestSpecialRenderer` now takes in a `MaterialSet`
        - `*COPPER*` - Textures for the copper chest.
    - `ConduitSpecialRenderer` now takes in a `MaterialSet`
    - `CopperGolemStatueSpecialRenderer` - A special renderer for the copper golem statue as an item.
    - `HangingSignSpecialRenderer` now takes in a `MaterialSet` and a `Model$Simple` instead of a `Model`
    - `NoDataSpecialModelRenderer#render` -> `submit`, now takes in an outline color
    - `PlayerHeadSpecialRenderer` now takes in the `PlayerSkinRenderCache`
    - `ShieldSpecialRenderer` now takes in a `MaterialSet`
    - `SpecialModelRenderer`
        - `render` -> `submit`, now takes in an outline color
        - `$BakingContext` - The context used to bake a special item model.
        - `$Unbaked#bake` now takes in a `SpecialModelRenderer$BakingContext` instead of an `EntityModelSet`
    - `SpecialModelRenderers#createBlockRenderers` now takes in a `SpecialModelRenderer$BakingContext` instead of an `EntityModelSet`
    - `StandingSignSpecialRenderer` now takes in a `MaterialSet` and a `Model$Simple` instead of a `Model`
- `net.minecraft.client.renderer.state`
    - `BlockBreakingRenderState` - The render state for how far the current block has been broken.
    - `BlockOutlineRenderState` - The render state for the outline of a block via its `VoxelShape`s.
    - `CameraRenderState` - The render state of the camera.
    - `LevelRenderState` - The render state of the dynamic features in a level.
    - `ParticleGroupRenderState` - The render state for a group of particles.
    - `ParticlesRenderState` - The render state for all particles.
    - `QuadParticleRenderState` - The render group state for all single quad particles.
    - `SkyRenderState` - The render state of the sky, including the moon and stars.
    - `WeatherRenderState` - The render state of the current weather.
    - `WorldBorderRenderState` - The render state of the world border.
- `net.minecraft.client.renderer.texture`
    - `SkinTextureDownloader` is now an instance class rather than a static method holder, taking in a `Proxy`, `TextureManager`, and the main thread `Executor`
        - Most methods that were previously static are now instance methods
    - `SpriteContents` now takes in an optional `AnimationMetadataSection` and `MetadataSectionType$WithValue` list instead of a `ResourceMetadata`
        - `metadata` -> `getAdditionalMetadata`, not one-to-one
    - `SpriteLoader`
        - `DEFAULT_METADATA_SECTIONS` is removed
        - `stitch` is now private
        - `runSpriteSuppliers` is now private
        - `loadAndStitch(ResourceManager, ResourceLocation, int, Executor)` isn removed
        - `loadAndStitch` now takes a set of `MetadataSectionType`s instead of a collection
        - `$Preparations`
            - `waitForUpload` is removed
            - `getSprite` - Returns the atlas sprite for a given resource location.
    - `TextureAtlasSprite#isAnimated` -> `SpriteContents#isAnimated`
- `net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader#create` now takes a set of `MetadataSectionType`s instead of a collection
- `net.minecraft.client.resources`
    - `MapDecorationTextureManager` class is removed
    - `PaintingTextureManager` class is removed
    - `PlayerSkin$Model` -> `PlayerModelType`, not one-to-one
        - The constructor not takes in the legacy service name
        - `byName` -> `byLegacyServicesName`
    - `SkinManager` now takes in `Services` instead of a `MinecraftSessionService`, and a `SkinTextureDownloader`
        - `lookupInsecure` -> `createLookup`, now taking in a boolean of whether to check for insecure skins
        - `getOrLoad` -> `get`
    - `TextureAtlasHolder` class is removed
- `net.minecraft.client.resources.model`
    - `AtlasIds` -> `net.minecraft.data.AtlasIds`
    - `AtlasSet` -> `AtlasManager`, not one-to-one
        - `forEach` - Iterates through each of the atlas sheets.
    - `Material`
        - `sprite` -> `MaterialSet#get`
        - `buffer` now takes in a `MaterialSet`
    - `MaterialSet` - A map of material to its atlas sprite.
    - `ModelBakery` now takes in a `MaterialSet` and `PlayerSkinRenderCache`
    - `ModelManager` is no longer `AutoCloseable`
        - The constructor takes in the `PlayerSkinRenderCache`, `AtlasManager` instead of the `TextureManager`, and the max mipmap levels integer
        - `getAtlas` -> `MaterialSet#get`
        - `updateMaxMipLevel` -> `AtlasManager#updateMaxMipLevel`
- `net.minecraft.core.particles`
    - `ParticleGroup` -> `ParticleLimit`
    - `ParticleTypes`
        - `DRAGON_BREATH` now uses a `PowerParticleOption`
        - `EFFECT` now uses a `SpellParticleOption`
        - `FLASH` now uses a `ColorParticleOption`
        - `INSTANT_EFFECT` now uses a `SpellParticleOption`
    - `PowerParticleOption` - A particle option for the dragon's breath.
    - `SpellParticleOption` - A particle option for potion effects.