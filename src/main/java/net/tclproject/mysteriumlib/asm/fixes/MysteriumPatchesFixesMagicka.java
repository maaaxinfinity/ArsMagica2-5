package net.tclproject.mysteriumlib.asm.fixes;

import am2.api.spell.ItemSpellBase;
import am2.items.ItemSpellStaff;
import am2.items.SpellBase;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.world.World;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.Fix;

public class MysteriumPatchesFixesMagicka {

	static int staffSlotTo = -1, staffSlotColumnTo = -1, staffSlotFrom = -1, staffSlotColumnFrom = -1;
	static int spellSlotFrom = -1, spellSlotColumnFrom = -1;
	static boolean craftingStaffsPossible = false, craftingSpellsPossible = false;

	@Fix(returnSetting = EnumReturnSetting.ON_TRUE, anotherMethodReturned = "findMatchingRecipeResult")
	public static boolean findMatchingRecipe(CraftingManager cm, InventoryCrafting p_82787_1_, World p_82787_2_) {
		craftingStaffsPossible = false;
		craftingSpellsPossible = false;
		int craftingCompsStaffs = 0, craftingCompsSpells = 0;
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
					}
					if (p_82787_1_.getStackInRowAndColumn(i,j).getItem() instanceof SpellBase) {
						craftingCompsSpells++;
						spellSlotFrom = i;
						spellSlotColumnFrom = j;
					}
				}
			}
		}

		if (craftingCompsSpells == 1 && craftingCompsStaffs == 1) {
			craftingSpellsPossible = true;
		}

		if (craftingCompsStaffs == 2) {
			craftingStaffsPossible = true;
		}

		if (craftingStaffsPossible || craftingSpellsPossible) {
			return true;
		} else {
			staffSlotTo = -1;
			staffSlotColumnTo = -1;
			spellSlotFrom = -1;
			spellSlotColumnFrom = -1;
			staffSlotFrom = -1;
			staffSlotColumnFrom = -1;
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
		} else { // if craftingSpells possible
			ItemStack result = ItemSpellStaff.setSpellScroll(
					p_82787_1_.getStackInRowAndColumn(staffSlotTo, staffSlotColumnTo).copy(),
					p_82787_1_.getStackInRowAndColumn(spellSlotFrom, spellSlotColumnFrom));
			staffSlotTo = -1;
			staffSlotColumnTo = -1;
			spellSlotFrom = -1;
			spellSlotColumnFrom = -1;
			return result;
		}

	}

}
