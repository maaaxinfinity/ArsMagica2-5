package am2.armor;

import am2.AMCore;
import am2.api.items.armor.ArmorTextureEvent;
import am2.api.items.armor.IArmorImbuement;
import am2.api.items.armor.ImbuementApplicationTypes;
import am2.items.ItemsCommonProxy;
import am2.particles.AMParticle;
import am2.particles.ParticleFadeOut;
import am2.playerextensions.ExtendedProperties;
import am2.proxy.gui.ModelLibrary;
import am2.texture.ResourceManager;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class ArmorEventHandler{

	@SubscribeEvent
	public void onEntityLiving(LivingUpdateEvent event){
		if (!(event.entityLiving instanceof EntityPlayer))
			return;

		doInfusions(ImbuementApplicationTypes.ON_TICK, event, (EntityPlayer)event.entityLiving);

		EntityPlayer player = (EntityPlayer)event.entityLiving;
		if (player.inventory.armorInventory[0] != null && player.inventory.armorInventory[0].getItem() == ItemsCommonProxy.archmageBoots){
			ModifiableAttributeInstance instance = (ModifiableAttributeInstance)player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.movementSpeed);

			ArrayList<AttributeModifier> toRemove = new ArrayList<AttributeModifier>();

			Collection c = instance.func_111122_c();
			ArrayList arraylist = new ArrayList(c);
			Iterator iterator = arraylist.iterator();

			while (iterator.hasNext()){
				AttributeModifier attributemodifier = (AttributeModifier)iterator.next();
				if (attributemodifier.getOperation() == 2 && attributemodifier.getAmount() < 0.0f){
					toRemove.add(attributemodifier);
				}
			}

			for (AttributeModifier modifier : toRemove){
				instance.removeModifier(modifier);
			}

			float speedInAirLast = ReflectionHelper.getPrivateValue(EntityPlayer.class, player, "speedInAir", "field_71102_ce");
			if (speedInAirLast < 0.1F){
				ReflectionHelper.setPrivateValue(EntityPlayer.class, player, 0.1F, "speedInAir", "field_71102_ce");
				setSpeedInAir.put(player, speedInAirLast);
			}

			if (player.worldObj.isRemote) {
				if (Minecraft.getMinecraft().gameSettings.keyBindJump.getIsKeyPressed()) {
					player.motionY += 0.135;
				}
			}
		} else {
			if (setSpeedInAir.get(player) != null) {
				ReflectionHelper.setPrivateValue(EntityPlayer.class, player, setSpeedInAir.get(player), "speedInAir", "field_71102_ce");
			}
		}
	}

	private Map<EntityPlayer, Float> setSpeedInAir = new HashMap<EntityPlayer, Float>();

	@SubscribeEvent
	public void onEntityHurt(LivingHurtEvent event){
		if (!(event.entityLiving instanceof EntityPlayer))
			return;

		doInfusions(ImbuementApplicationTypes.ON_HIT, event, (EntityPlayer)event.entityLiving);

		if (event.entityLiving instanceof EntityPlayer)
			doXPInfusion((EntityPlayer)event.entityLiving, 0.01f, Math.max(0.05f, Math.min(event.ammount, 5)));
	}

	@SubscribeEvent
	public void onEntityJump(LivingJumpEvent event){
		if (!(event.entityLiving instanceof EntityPlayer))
			return;

		doInfusions(ImbuementApplicationTypes.ON_JUMP, event, (EntityPlayer)event.entityLiving);
	}

	@SubscribeEvent
	public void onMiningSpeed(BreakSpeed event){
		doInfusions(ImbuementApplicationTypes.ON_MINING_SPEED, event, (EntityPlayer)event.entityPlayer);
	}

	@SubscribeEvent
	public void onEntityDeath(LivingDeathEvent event){
		if (event.source.getSourceOfDamage() instanceof EntityPlayer)
			doXPInfusion((EntityPlayer)event.source.getSourceOfDamage(), 1, Math.min(20, event.entityLiving.getMaxHealth()));

		if (!(event.entityLiving instanceof EntityPlayer))
			return;

		doInfusions(ImbuementApplicationTypes.ON_DEATH, event, (EntityPlayer)event.entityLiving);
	}

	private void doInfusions(ImbuementApplicationTypes type, Event event, EntityPlayer player){
		ExtendedProperties props = ExtendedProperties.For(player);

		for (int i = 0; i < 4; ++i){
			IArmorImbuement[] infusions = ArmorHelper.getInfusionsOnArmor(player, i);
			int cd = props.armorProcCooldowns[i];
			for (IArmorImbuement inf : infusions){
				if (inf == null)
					continue;
				if (inf.getApplicationTypes().contains(type)){
					if (cd == 0 || inf.canApplyOnCooldown()){
						if (inf.applyEffect(player, player.worldObj, player.getCurrentArmor(i), type, event)){
							if (inf.getCooldown() > 0){
								if (props.armorProcCooldowns[i] < inf.getCooldown()){
									props.armorProcCooldowns[i] = inf.getCooldown();
									if (player instanceof EntityPlayerMP)
										AMCore.proxy.blackoutArmorPiece((EntityPlayerMP)player, i, inf.getCooldown());
								}
							}
						}
					}
				}
			}
		}
	}

	private void doXPInfusion(EntityPlayer player, float xpMin, float xpMax){
		float amt = (float)((player.worldObj.rand.nextFloat() * xpMin + (xpMax - xpMin)) * AMCore.config.getArmorXPInfusionFactor());
		ArmorHelper.addXPToArmor(amt, player);
	}

	@SubscribeEvent
	public void onArmorTexture(ArmorTextureEvent event){
		if (event.renderIndex == ArmorHelper.getArmorRenderIndex("mage")){
			if (event.slot == 2){
				event.texture = "arsmagica2:textures/models/mage_2.png";
			}else{
				event.texture = "arsmagica2:textures/models/mage_1.png";
			}
		}else if (event.renderIndex == ArmorHelper.getArmorRenderIndex("battlemage")){
			if (event.slot == 2){
				event.texture = "arsmagica2:textures/models/battlemage_2.png";
			}else{
				event.texture = "arsmagica2:textures/models/battlemage_1.png";
			}
		}else if (event.renderIndex == ArmorHelper.getArmorRenderIndex("archmage")){
			if (event.slot == 2){
				event.texture = "arsmagica2:textures/models/archmage_2.png";
			}else{
				event.texture = "arsmagica2:textures/models/archmage_1.png";
			}
		}else if (event.renderIndex == ArmorHelper.getArmorRenderIndex("bound")){
			if (event.slot == 2){
				event.texture = "arsmagica2:textures/models/bound_2.png";
			}else{
				event.texture = "arsmagica2:textures/models/bound_1.png";
			}
		}else if (event.renderIndex == ArmorHelper.getArmorRenderIndex("ender")){
			event.texture = "arsmagica2:textures/models/ender_1.png";
		}else if (event.renderIndex == ArmorHelper.getArmorRenderIndex("magitech")){
			event.texture = "arsmagica2:textures/models/magitech_1.png";
		}
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onPlayerRender(RenderPlayerEvent.Post event) {
		if (event.entityPlayer.getCurrentArmor(3) != null) {
			if (event.entityPlayer.getCurrentArmor(3).getItem() == ItemsCommonProxy.archmageHood) {
				EntityPlayer p_76986_1_ = event.entityPlayer;
				int i1 = Minecraft.getMinecraft().gameSettings.limitFramerate;
				float p_76986_9_ = System.nanoTime() + (long)(1000000000 / i1);
				float f5 = 0.0625F;
				float f2 = 0; // interpolateRotation(p_76986_1_.prevRenderYawOffset, p_76986_1_.renderYawOffset, p_76986_9_);
				float f3 = -p_76986_1_.prevRotationYawHead; // , p_76986_1_.rotationYawHead, p_76986_9_);
				float f4 = 0;
				float f13 = 63;
				float f6 = p_76986_1_.prevLimbSwingAmount + (p_76986_1_.limbSwingAmount - p_76986_1_.prevLimbSwingAmount) * p_76986_9_;
				float f7 = p_76986_1_.limbSwing - p_76986_1_.limbSwingAmount * (1.0F - p_76986_9_);
				if (f6 > 1.0F)
				{
					f6 = 1.0F;
				}
				GL11.glPushMatrix();
				GL11.glTranslated(0, 0.4, 0);
				Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("arsmagica2:" + ResourceManager.getMobTexturePath("bosses/fire_guardian.png")));
				ModelLibrary.instance.fireEars.saveValues = true;
				ModelLibrary.instance.fireEars.render(event.entityPlayer, f7, f6, f4, f3, -f13, f5);
				GL11.glPopMatrix();
			}
		}
		if (event.entityPlayer.getCurrentArmor(2) != null) {
			if (event.entityPlayer.getCurrentArmor(2).getItem() == ItemsCommonProxy.archmageArmor) {
				EntityPlayer p_76986_1_ = event.entityPlayer;
				int i1 = Minecraft.getMinecraft().gameSettings.limitFramerate;
				float p_76986_9_ = System.nanoTime() + (long)(1000000000 / i1);
				float f5 = 0.0625F;
				float f2 = 0; // interpolateRotation(p_76986_1_.prevRenderYawOffset, p_76986_1_.renderYawOffset, p_76986_9_);
				float f3 = -p_76986_1_.rotationYawHead; // , p_76986_1_.rotationYawHead, p_76986_9_);
				float f4 = 0;
				float f13 = 0;
				float f6 = 0; // p_76986_1_.prevLimbSwingAmount + (p_76986_1_.limbSwingAmount - p_76986_1_.prevLimbSwingAmount) * p_76986_9_;
				float f7 = p_76986_1_.limbSwing - p_76986_1_.limbSwingAmount * (1.0F - p_76986_9_) + 50;
				if (f6 > 1.0F)
				{
					f6 = 1.0F;
				}
				GL11.glPushMatrix();
				GL11.glTranslated(0, -0.75, 0);
				GL11.glScaled(0.6, 0.6, 0.6);
				setRotation(rotation+1);
				GL11.glRotated(rotation, 0, 1, 0);
				Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("arsmagica2:" + ResourceManager.getMobTexturePath("bosses/earth_guardian.png")));
				ModelLibrary.instance.earthArmor.saveValues = true;
				ModelLibrary.instance.earthArmor.renderArms(event.entityPlayer, f7, f6, f4, f3, f13, f5);
				GL11.glPopMatrix();
			}
		}
		if (event.entityPlayer.getCurrentArmor(1) != null) {
			if (event.entityPlayer.getCurrentArmor(1).getItem() == ItemsCommonProxy.archmageLeggings) {
				EntityPlayer p_76986_1_ = event.entityPlayer;
				int i1 = Minecraft.getMinecraft().gameSettings.limitFramerate;
				float p_76986_9_ = System.nanoTime() + (long)(1000000000 / i1);
				float f5 = 0.0625F;
				float f2 = 0; // interpolateRotation(p_76986_1_.prevRenderYawOffset, p_76986_1_.renderYawOffset, p_76986_9_);
				float f3 = -p_76986_1_.rotationYawHead; // , p_76986_1_.rotationYawHead, p_76986_9_);
				float f4 = 0;
				float f13 = 0;
				float f6 = 0; // p_76986_1_.prevLimbSwingAmount + (p_76986_1_.limbSwingAmount - p_76986_1_.prevLimbSwingAmount) * p_76986_9_;
				float f7 = p_76986_1_.limbSwing - p_76986_1_.limbSwingAmount * (1.0F - p_76986_9_) + 50;
				if (f6 > 1.0F)
				{
					f6 = 1.0F;
				}
				GL11.glPushMatrix();
				GL11.glTranslated(0, -1.3, 0);
				GL11.glScaled(1.05, 1.05, 1.05);
				setRotation2(rotation2+0.4f);
				GL11.glRotatef(rotation2, 0, -1, 0);
				Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("arsmagica2:" + ResourceManager.getMobTexturePath("bosses/water_guardian.png")));
				ModelLibrary.instance.earthArmor.saveValues = true;
				ModelLibrary.instance.waterOrbs.render(event.entityPlayer, f7, f6, f4, f3, f13, f5);
				GL11.glPopMatrix();
			}
		}
	}

	private int rotation = 0;
	public void setRotation(int newRotation) {
		rotation = newRotation;
		if (rotation == 360) {
			rotation = 0;
		}
	}

	private float rotation2 = 0;
	public void setRotation2(float newRotation) {
		rotation2 = newRotation;
		if (rotation >= 360) {
			rotation2 = 0;
		}
	}

	private float interpolateRotation(float p_77034_1_, float p_77034_2_, float p_77034_3_)
	{
		float f3;

		for (f3 = p_77034_2_ - p_77034_1_; f3 < -180.0F; f3 += 360.0F)
		{
			;
		}

		while (f3 >= 180.0F)
		{
			f3 -= 360.0F;
		}

		return p_77034_1_ + p_77034_3_ * f3;
	}
}
