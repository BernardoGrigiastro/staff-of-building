package draylar.staffofbuilding.item;

import draylar.staffofbuilding.StaffOfBuilding;
import draylar.staffofbuilding.api.SelectionCalculator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

public class BuilderStaffItem extends Item {

    private final int size;
    private final Item repairIngredient;

    public BuilderStaffItem(Settings settings, int size, Item repairIngredient) {
        super(settings);
        this.size = size;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.getItem().equals(repairIngredient);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(new TranslatableText("staffofbuilding.placement_range", size).formatted(Formatting.GRAY));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        Direction side = context.getSide();
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();
        Block block = state.getBlock();
        Item item = block.asItem();

        // check to make sure the block we're placing off has an item
        if(player != null && item != Items.AIR && context.getHand() == Hand.MAIN_HAND) {
            // get amount of required item in player inventory
            int count = player.inventory.count(item);

            // run placement logic if they have at least 1 of the item (or if they are a creative user)
            if(count > 0 || player.isCreative()) {
                if(!world.isClient) {
                    // potentially reset state to prevent dupe or similar  mechanics
                    if(StaffOfBuilding.RESET_LIST.contains(state.getBlock())) {
                        state = state.getBlock().getDefaultState();
                    }

                    // get number of blocks to place (min between max size and the count of items in inventory)
                    int maxChecks = Math.min(size, player.isCreative() ? size : count);
                    List<BlockPos> positions = SelectionCalculator.calculateSelection(world, pos, side, maxChecks);
                    int taken = 0;

                    // place blocks
                    for (BlockPos position : positions) {
                        if(world.getBlockState(position).isAir()) {
                            world.setBlockState(position, state);
                            taken++;
                        }
                    }

                    // take items from survival inventory
                    if(!player.isCreative()) {
                        player.inventory.remove(stack -> stack.getItem().equals(item), taken, player.inventory);
                    }

                    // damage item
                    if(context.getStack().isDamageable()) {
                        context.getStack().damage(taken, player, (livingEntity) -> {
                            livingEntity.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND);
                        });
                    }

                    if(taken > 0) {
                        world.playSound(null, player.getBlockPos(), state.getSoundGroup().getPlaceSound(), SoundCategory.PLAYERS, state.getSoundGroup().getVolume(), state.getSoundGroup().getPitch());
                    }
                }

                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.FAIL;
    }

    public int getMaxSize() {
        return size;
    }
}
