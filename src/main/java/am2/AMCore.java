package am2;

import am2.api.ArsMagicaApi;
import am2.api.spell.enums.Affinity;
import am2.armor.infusions.ImbuementRegistry;
import am2.blocks.RecipesEssenceRefiner;
import am2.blocks.liquid.BlockLiquidEssence;
import am2.blocks.tileentities.flickers.*;
import am2.buffs.BuffList;
import am2.commands.*;
import am2.configuration.AMConfig;
import am2.configuration.SkillConfiguration;
import am2.customdata.CustomWorldData;
import am2.enchantments.AMEnchantmentHelper;
import am2.entities.EntityManager;
import am2.entities.SpawnBlacklists;
import am2.interop.TC4Interop;
import am2.items.ItemsCommonProxy;
import am2.network.AMNetHandler;
import am2.network.SeventhSanctum;
import am2.network.TickrateMessage;
import am2.network.TickrateMessageHandler;
import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.RiftStorage;
import am2.playerextensions.SkillData;
import am2.power.PowerNodeCache;
import am2.proxy.CommonProxy;
import am2.spell.SkillManager;
import am2.spell.SkillTreeManager;
import am2.spell.SpellUtils;
import am2.utility.KeystoneUtilities;
import am2.worldgen.BiomeWitchwoodForest;
import am2.worldgen.SCLWorldProvider;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.BiomeManager.BiomeEntry;
import net.minecraftforge.common.BiomeManager.BiomeType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidRegistry;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

//@Mod(modid = "arsmagica2", modLanguage = "java", name = "Ars Magica 2", version = "1.6.4", dependencies = "required-after:AnimationAPI")
@Mod(modid = "arsmagica2", modLanguage = "java", name = "Ars Magica 2", version = "1.6.4", dependencies = "required-after:AnimationAPI;required-after:CoFHCore")
public class AMCore{

	@Instance(value = "arsmagica2")
	public static AMCore instance;

	@SidedProxy(clientSide = "am2.proxy.ClientProxy", serverSide = "am2.proxy.CommonProxy")
	public static CommonProxy proxy;

	public static AMConfig config;
	public static SkillConfiguration skillConfig;
	public static final int ANY_META = 32767;
	public static SimpleNetworkWrapper NETWORK;

	private String compendiumBase;

	public AMCore(){
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event){

		String configBase = event.getSuggestedConfigurationFile().getAbsolutePath();
		configBase = popPathFolder(configBase);
		compendiumBase = popPathFolder(configBase);

		configBase += File.separatorChar + "AM2" + File.separatorChar;

		config = new AMConfig(new File(configBase + File.separatorChar + "AM2.cfg"));

		skillConfig = new SkillConfiguration(new File(configBase + "SkillConf.cfg"));

		NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("AM2TickrateChanger");
		NETWORK.registerMessage(TickrateMessageHandler.class, TickrateMessage.class, 0, Side.CLIENT);

		AMNetHandler.INSTANCE.init();

		proxy.InitializeAndRegisterHandlers();
		proxy.preinit();
	}

	private String popPathFolder(String path){
		int lastIndex = path.lastIndexOf(File.separatorChar);
		if (lastIndex == -1)
			lastIndex = path.length() - 1; //no path separator...strange, but ok.  Use full string.
		return path.substring(0, lastIndex);
	}

	@EventHandler
	public void init(FMLInitializationEvent event){

		FMLInterModComms.sendMessage("Waila", "register", "am2.interop.WailaSupport.callbackRegister");

		ForgeChunkManager.setForcedChunkLoadingCallback(this, AMChunkLoader.INSTANCE);
		proxy.init();

		initAPI();

		DimensionManager.registerProviderType(config.getMMFDimensionID(), SCLWorldProvider.class, false);
		DimensionManager.registerDimension(config.getMMFDimensionID(), config.getMMFDimensionID());

		if (AMCore.config.getEnableWitchwoodForest()){
			BiomeDictionary.registerBiomeType(BiomeWitchwoodForest.instance, Type.FOREST, Type.MAGICAL);
			BiomeManager.addBiome(BiomeType.WARM, new BiomeEntry(BiomeWitchwoodForest.instance, 6));
		}
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event){
		//Register Flicker Operators
		registerFlickerOperators();

		proxy.setCompendiumSaveBase(compendiumBase);
		proxy.postinit();

		if (config.retroactiveWorldgen()){
			LogHelper.info("Retroactive Worldgen is enabled");
		}

		FluidContainerRegistry.registerFluidContainer(
				new FluidContainerData(
						FluidRegistry.getFluidStack(BlockLiquidEssence.liquidEssenceFluid.getName(), FluidContainerRegistry.BUCKET_VOLUME),
						new ItemStack(ItemsCommonProxy.itemAMBucket),
						FluidContainerRegistry.EMPTY_BUCKET));

		SeventhSanctum.instance.init();
//		if (Loader.isModLoaded("BetterDungeons"))
//			BetterDungeons.init();
		if (Loader.isModLoaded("Thaumcraft"))
			TC4Interop.initialize();
//		if (Loader.isModLoaded("MineFactoryReloaded"))
//			MFRInterop.init();

		try {
			Class.forName("forestry.api.recipes.RecipeManagers", false, getClass().getClassLoader());
			Class.forName("magicbees.bees.BeeProductHelper", false, getClass().getClassLoader());
			Class.forName("magicbees.bees.BeeSpecies", false, getClass().getClassLoader());
			AMBeeCompat.init();
		} catch (ClassNotFoundException e) {
			LogHelper.info("A compatible MagicBees version was not found, compat not loading.");
		}

		// Reduce onEntityLiving() lag by skipping unnecessary tasks if disabled.
		AMEventHandler.enabled_accelerate = AMCore.skillConfig.isSkillEnabled("Accelerate");
		AMEventHandler.enabled_slow = AMCore.skillConfig.isSkillEnabled("Slow");
		AMEventHandler.enabled_timeFortified = AMCore.skillConfig.isSkillEnabled("FortifyTime");
		AMEventHandler.enabled_shield = AMCore.skillConfig.isSkillEnabled("Shield");
		AMEventHandler.enable_spatialVortex = AMCore.config.enableSpatialVortex();
	}

	private void registerFlickerOperators(){
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorItemTransport(),
				Affinity.AIR
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorButchery(),
				Affinity.FIRE, Affinity.LIFE
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorContainment(),
				Affinity.AIR, Affinity.ENDER
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorFelledOak(),
				Affinity.NATURE, Affinity.LIGHTNING
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorFlatLands(),
				Affinity.EARTH, Affinity.ICE
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorGentleRains(),
				Affinity.WATER
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorInterdiction(),
				Affinity.AIR, Affinity.ARCANE
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorLight(),
				Affinity.FIRE, Affinity.LIGHTNING
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorMoonstoneAttractor(),
				Affinity.LIGHTNING, Affinity.ARCANE, Affinity.EARTH
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorNaturesBounty(),
				Affinity.NATURE, Affinity.WATER, Affinity.LIFE
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorPackedEarth(),
				Affinity.EARTH
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorProgeny(),
				Affinity.LIFE
		);
		FlickerOperatorRegistry.instance.registerFlickerOperator(
				new FlickerOperatorFishing(),
				Affinity.WATER, Affinity.NATURE
		);
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event){
		ICommandManager commandManager = event.getServer().getCommandManager();
		ServerCommandManager serverCommandManager = ((ServerCommandManager)commandManager);
		serverCommandManager.registerCommand(new SetMagicLevelCommand());
		serverCommandManager.registerCommand(new UnlockAugmentedCastingCommand());
		serverCommandManager.registerCommand(new SetAffinityCommand());
		serverCommandManager.registerCommand(new ShiftAffinityCommand());
		serverCommandManager.registerCommand(new RecoverKeystoneCommand());
		serverCommandManager.registerCommand(new RegisterTeamHostilityCommand());
		serverCommandManager.registerCommand(new FillManaBarCommand());
		serverCommandManager.registerCommand(new ReloadSkillTree());
		serverCommandManager.registerCommand(new GiveSkillPoints());
		serverCommandManager.registerCommand(new TakeSkillPoints());
		serverCommandManager.registerCommand(new ClearKnownSpellParts());
		serverCommandManager.registerCommand(new Explosions());
		serverCommandManager.registerCommand(new DumpNBT());
		serverCommandManager.registerCommand(new Respec());
		serverCommandManager.registerCommand(new UnlockCompendiumEntry());
	}

	@EventHandler
	public void serverStarted(FMLServerStartedEvent event){
		// custom data
		CustomWorldData.loadAllWorldData();
	}

	@EventHandler
	public void serverStopping(FMLServerStoppingEvent event){
		for (WorldServer ws : MinecraftServer.getServer().worldServers){
			PowerNodeCache.instance.saveWorldToFile(ws);
		}
		// custom data
		CustomWorldData.saveAllWorldData();
	}

	@EventHandler
	public void onIMCReceived(FMLInterModComms.IMCEvent event){
		for (IMCMessage msg : event.getMessages()){
			if (msg.key == "dsb"){
				LogHelper.info("Received dimension spawn blacklist IMC!  Processing.");
				String[] split = msg.getStringValue().split("|");
				if (split.length != 2){
					LogHelper.warn("Could not parse dsb IMC - malformed identifiers!  Syntax is 'ClassName|DimensionID', for example:  EntityDryad|22");
					continue;
				}
				try{
					SpawnBlacklists.addBlacklistedDimensionSpawn(split[0], Integer.parseInt(split[1]));
				}catch (NumberFormatException nex){
					LogHelper.warn("Could not parse dsb IMC - improper dimension ID (not a number)!  Syntax is 'ClassName|DimensionID', for example:  EntityDryad|22");
				}
			}else if (msg.key == "bsb"){
				LogHelper.info("Received biome spawn blacklist IMC!  Processing.");
				String[] split = msg.getStringValue().split("|");
				if (split.length != 2){
					LogHelper.warn("Could not parse bsb IMC - malformed identifiers!  Syntax is 'ClassName|BiomeID', for example:  EntityDryad|22");
					continue;
				}
				try{
					SpawnBlacklists.addBlacklistedBiomeSpawn(split[0], Integer.parseInt(split[1]));
				}catch (NumberFormatException nex){
					LogHelper.warn("Could not parse bsb IMC - improper biome ID (not a number)!  Syntax is 'ClassName|BiomeID', for example:  EntityDryad|22");
				}
			}else if (msg.key == "dwg"){
				LogHelper.info("Received dimension worldgen blacklist IMC!  Processing.");
				try{
					SpawnBlacklists.addBlacklistedDimensionForWorldgen(Integer.parseInt(msg.getStringValue()));
				}catch (NumberFormatException nex){
					LogHelper.warn("Could not parse dwg IMC - improper dimension ID (not a number)!  Syntax is 'dimensionID', for example:  2");
				}
			}else if (msg.key == "adb"){
				LogHelper.info("Received dispel blacklist IMC!  Processing.");
				try{
					BuffList.instance.addDispelExclusion(Integer.parseInt(msg.getStringValue()));
				}catch (NumberFormatException nex){
					LogHelper.warn("Could not parse adb IMC - improper potion ID (not a number)!  Syntax is 'potionID', for example:  10");
				}
			}
		}
	}

	public void initAPI(){
		LogHelper.info("Initializing API Hooks...");
		ArsMagicaApi.instance.setSpellPartManager(SkillManager.instance);
		ArsMagicaApi.instance.setEnchantmentHelper(new AMEnchantmentHelper());
		ArsMagicaApi.instance.setSkillTreeManager(SkillTreeManager.instance);
		ArsMagicaApi.instance.setKeystoneHelper(KeystoneUtilities.instance);
		ArsMagicaApi.instance.setEntityManager(EntityManager.instance);
		ArsMagicaApi.instance.setObeliskFuelHelper(ObeliskFuelHelper.instance);
		ArsMagicaApi.instance.setFlickerOperatorRegistry(FlickerOperatorRegistry.instance);
		ArsMagicaApi.instance.setInfusionRegistry(ImbuementRegistry.instance);
		ArsMagicaApi.instance.setEssenceRecipeHandler(RecipesEssenceRefiner.essenceRefinement());
		ArsMagicaApi.instance.setColourblindMode(config.colourblindMode());
		ArsMagicaApi.instance.setBuffHelper(BuffList.instance);
		ArsMagicaApi.instance.setSpellUtils(SpellUtils.instance);

		ArsMagicaApi.instance.setAffinityDataID(AffinityData.identifier);
		ArsMagicaApi.instance.setSkillDataID(SkillData.identifier);
		ArsMagicaApi.instance.setExtendedPropertiesID(ExtendedProperties.identifier);
		ArsMagicaApi.instance.setRiftStorageID(RiftStorage.identifier);
		LogHelper.info("Finished API Initialization");
	}

	public String getVersion(){
		Mod modclass = this.getClass().getAnnotation(Mod.class);
		return modclass.version();
	}
}
