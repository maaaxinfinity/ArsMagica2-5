package net.tclproject.mysteriumlib.asm.fixes;

import am2.affinity.AffinityHelper;
import am2.api.spell.ItemSpellBase;
import am2.armor.BoundArmor;
import am2.buffs.BuffList;
import am2.items.ItemSpellStaff;
import am2.items.SpellBase;
import am2.spell.SkillManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.Fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MysteriumPatchesFixesMagicka{

	public static List<int[]> providingRedstone = new ArrayList<int[]>();
	static int staffSlotTo = -1, staffSlotColumnTo = -1, staffSlotFrom = -1, staffSlotColumnFrom = -1;
	static int armorSlotTo = -1, armorSlotColumnTo = -1;
	static int spellSlotFrom = -1, spellSlotColumnFrom = -1;
	static boolean craftingStaffsPossible = false, craftingSpellsPossible = false, craftingArmorPossible = false;

	@Fix(returnSetting = EnumReturnSetting.ALWAYS)
	@SideOnly(Side.CLIENT)
	public static boolean isInvisibleToPlayer(Entity e, EntityPlayer p_98034_1_){
		return p_98034_1_.isPotionActive(BuffList.trueSight.id) ? false : e.isInvisible();
	}

	@Fix(returnSetting = EnumReturnSetting.ON_TRUE, booleanAlwaysReturned = true)
	public static boolean isBlockIndirectlyGettingPowered(World world, int x, int y, int z)
	{
		int theID = world.provider.dimensionId;
		boolean toReturn = false;
		int counter = 0;
		for (int[] redstoneProvider : providingRedstone) {
			if (redstoneProvider[0] == theID && redstoneProvider[1] == x && redstoneProvider[2] == y && redstoneProvider[3] == z) {
				toReturn = true;
				break;
			}
			counter++;
		}

		if (toReturn) {
			int newValue = providingRedstone.get(counter)[4] - 1;
			if (newValue <= 0) {
				providingRedstone.remove(counter);
			} else{
				providingRedstone.add(new int[]{theID, x, y, z, newValue});
				providingRedstone.remove(counter);
			}

			world.getBlock(x, y, z).onNeighborBlockChange(world, x, y, z, Blocks.stonebrick);
			return true;
		}
		return false;
	}

	@Fix(returnSetting = EnumReturnSetting.ON_TRUE, anotherMethodReturned = "isInsideWater")
	public static boolean isInsideOfMaterial(Entity e, Material p_70055_1_)
	{
		if (e instanceof EntityPlayer && p_70055_1_.isLiquid()) {
			if (AffinityHelper.isNotInWaterActually.contains((EntityPlayer)e)) {
				return true;
			}
		}
		return false;
	}

	@Fix(returnSetting = EnumReturnSetting.ON_TRUE, anotherMethodReturned = "isInsideWater")
	public static boolean isInWater(Entity e) {
		if (e instanceof EntityPlayer) {
			if (AffinityHelper.isNotInWaterActually.contains((EntityPlayer)e)) {
				return true;
			}
		}
		return false;
	}

	@Fix(returnSetting = EnumReturnSetting.ON_TRUE)
	public static boolean velocityToAddToEntity(BlockLiquid block, World p_149640_1_, int p_149640_2_, int p_149640_3_, int p_149640_4_, Entity e, Vec3 p_149640_6_) {
		if (e instanceof EntityPlayer) {
			if (AffinityHelper.isNotInWaterActually.contains((EntityPlayer)e)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isInsideWater(Entity e, Material p_70055_1_) {
		return false;
	}

	public static boolean isInsideWater(Entity e) {
		return false;
	}

	@Fix(returnSetting = EnumReturnSetting.ON_TRUE, anotherMethodReturned = "findMatchingRecipeResult")
	public static boolean findMatchingRecipe(CraftingManager cm, InventoryCrafting p_82787_1_, World p_82787_2_) {
		craftingStaffsPossible = false;
		craftingSpellsPossible = false;
		craftingArmorPossible = false;
		int craftingCompsStaffs = 0, craftingCompsSpells = 0, craftingCompsArmor = 0;
		for (int i = 0; i<3; i++) {
			for (int j = 0; j<3; j++) {
				if (p_82787_1_.getStackInRowAndColumn(i,j) != null) {
					if (p_82787_1_.getStackInRowAndColumn(i,j).getItem() instanceof ItemSpellStaff) {
						if (!((ItemSpellStaff)p_82787_1_.getStackInRowAndColumn(i,j).getItem()).isMagiTechStaff()) {
							craftingCompsStaffs++;
							if (staffSlotTo == -1) {
								staffSlotTo = i;
								staffSlotColumnTo = j;
							} else {
								staffSlotFrom = i;
								staffSlotColumnFrom = j;
							}
						}
					} else if (p_82787_1_.getStackInRowAndColumn(i,j).getItem() instanceof SpellBase) {
						craftingCompsSpells++;
						spellSlotFrom = i;
						spellSlotColumnFrom = j;
					} else if (p_82787_1_.getStackInRowAndColumn(i,j).getItem() instanceof BoundArmor) {
						craftingCompsArmor++;
						armorSlotTo = i;
						armorSlotColumnTo = j;
					}
				}
			}
		}

		if (craftingCompsSpells == 1 && craftingCompsStaffs == 1) {
			craftingSpellsPossible = true;
		} else if (craftingCompsStaffs == 2) {
			craftingStaffsPossible = true;
		} else if (craftingCompsSpells == 1 && craftingCompsArmor == 1) {
			craftingArmorPossible = true;
		}

		if (craftingStaffsPossible || craftingSpellsPossible || craftingArmorPossible) {
			return true;
		} else {
			staffSlotTo = -1;
			staffSlotColumnTo = -1;
			spellSlotFrom = -1;
			spellSlotColumnFrom = -1;
			staffSlotFrom = -1;
			staffSlotColumnFrom = -1;
			armorSlotColumnTo = -1;
			armorSlotTo = -1;
		}
		return false;
	}

	public static ItemStack findMatchingRecipeResult(CraftingManager cm, InventoryCrafting p_82787_1_, World p_82787_2_) {

		if (craftingStaffsPossible){
			ItemStack result = ItemSpellStaff.copyChargeFrom(
					p_82787_1_.getStackInRowAndColumn(staffSlotTo, staffSlotColumnTo).copy(),
					p_82787_1_.getStackInRowAndColumn(staffSlotFrom, staffSlotColumnFrom));
			staffSlotTo = -1;
			staffSlotFrom = -1;
			staffSlotColumnTo = -1;
			staffSlotColumnFrom = -1;
			return result;
		} else if (craftingSpellsPossible) {
			ItemStack result = ItemSpellStaff.setSpellScroll(
					p_82787_1_.getStackInRowAndColumn(staffSlotTo, staffSlotColumnTo).copy(),
					p_82787_1_.getStackInRowAndColumn(spellSlotFrom, spellSlotColumnFrom));
			staffSlotTo = -1;
			staffSlotColumnTo = -1;
			spellSlotFrom = -1;
			spellSlotColumnFrom = -1;
			return result;
		} else { // if crafting armor possible
			ItemStack result = BoundArmor.setSpell(
					p_82787_1_.getStackInRowAndColumn(armorSlotTo, armorSlotColumnTo).copy(),
					p_82787_1_.getStackInRowAndColumn(spellSlotFrom, spellSlotColumnFrom));
			armorSlotTo = -1;
			armorSlotColumnTo = -1;
			spellSlotFrom = -1;
			spellSlotColumnFrom = -1;
			return result;
		}
	}

}
