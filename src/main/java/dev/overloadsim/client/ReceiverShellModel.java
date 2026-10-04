package dev.overloadsim.client;

import dev.overloadsim.laser.ReceiverShellVisuals;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Suppresses only the formed machine's owned frame/glass cells; loose parts keep their baked model. */
public final class ReceiverShellModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Boolean> HIDDEN = new ModelProperty<>();
    public ReceiverShellModel(BakedModel original) { super(original); }
    @Override public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData existing) {
        return originalModel.getModelData(level, pos, state, existing).derive()
                .with(HIDDEN, ReceiverShellVisuals.hidden(level, pos)).build();
    }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random, ModelData data, RenderType type) {
        return Boolean.TRUE.equals(data.get(HIDDEN)) ? List.of() : originalModel.getQuads(state, face, random, data, type);
    }
}
