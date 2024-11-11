package am2.worldgen;

import am2.AMCore;
import am2.blocks.BlocksCommonProxy;
import am2.configuration.AMConfig;
import am2.entities.SpawnBlacklists;
import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.ArrayList;
import java.util.Random;

import javax.security.auth.callback.ConfirmationCallback;

import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.LAKE;

public class AM2WorldDecorator implements IWorldGenerator{

	//ores
	private final WorldGenMinable vinteum;
	private final WorldGenMinable blueTopaz;
	private final WorldGenMinable chimerite;
	private final WorldGenMinable sunstone;

	//flowers
	private final AM2FlowerGen blueOrchid;
	private final AM2FlowerGen desertNova;
	private final AM2FlowerGen wakebloom;
	private final AM2FlowerGen aum;
	private final AM2FlowerGen tarmaRoot;

	private ArrayList<Integer> dimensionBlacklist = new ArrayList<Integer>();


	//pools
	private final AM2PoolGen pools;
	private final WorldGenEssenceLakes lakes;
	
	//config
	private int witchChance = AMCore.config.getWitchwoodFrequency();
	private int poolChance = AMCore.config.getPoolFrequency();
	private int wakeChance = AMCore.config.getWakebloomFrequency();
	
	private int vinteumMin = AMCore.config.getVinteumMinHeight();
	private int vinteumMax = AMCore.config.getVinteumMaxHeight();
	private int vinteumVein = AMCore.config.getVinteumVeinSize();
	private int vinteumFrequency = AMCore.config.getVinteumFrequency();
	
	private int chimeriteMin = AMCore.config.getChimeriteMinHeight();
	private int chimeriteMax = AMCore.config.getChimeriteMaxHeight();
	private int chimeriteVein = AMCore.config.getChimeriteVeinSize();
	private int chimeriteFrequency = AMCore.config.getChimeriteFrequency();
	
	private int topazMin = AMCore.config.getTopazMinHeight();
	private int topazMax = AMCore.config.getTopazMaxHeight();
	private int topazVein = AMCore.config.getTopazVeinSize();
	private int topazFrequency = AMCore.config.getTopazFrequency();	
	
	private int sunstoneMin = AMCore.config.getSunstoneMinHeight();
	private int sunstoneMax = AMCore.config.getSunstoneMaxHeight();
	private int sunstoneVein = AMCore.config.getSunstoneVeinSize();
	private int sunstoneFrequency = AMCore.config.getSunstoneFrequency();

	public AM2WorldDecorator(){

		if (AMCore.config.getWorldgenWhitelistEnabled()) {
			for (int i = -999; i <= 999; i++){
				dimensionBlacklist.add(i);
			}
			for (int k : AMCore.config.getWorldgenWhitelist()){
				dimensionBlacklist.remove((Integer)k);
			}
		} else{
			for (int i : AMCore.config.getWorldgenBlacklist()){
				if (i == -1) continue;
				dimensionBlacklist.add(i);
			}
		}

		vinteum = new WorldGenMinable(BlocksCommonProxy.AMOres, BlocksCommonProxy.AMOres.META_VINTEUM_ORE, vinteumVein, Blocks.stone);
		chimerite = new WorldGenMinable(BlocksCommonProxy.AMOres, BlocksCommonProxy.AMOres.META_CHIMERITE_ORE, chimeriteVein, Blocks.stone);
		blueTopaz = new WorldGenMinable(BlocksCommonProxy.AMOres, BlocksCommonProxy.AMOres.META_BLUE_TOPAZ_ORE, topazVein, Blocks.stone);
		sunstone = new WorldGenMinable(BlocksCommonProxy.AMOres, BlocksCommonProxy.AMOres.META_SUNSTONE_ORE, sunstoneVein, Blocks.lava);

		blueOrchid = new AM2FlowerGen(BlocksCommonProxy.cerublossom, 0);
		desertNova = new AM2FlowerGen(BlocksCommonProxy.desertNova, 0);
		wakebloom = new AM2FlowerGen(BlocksCommonProxy.wakebloom, 0);
		aum = new AM2FlowerGen(BlocksCommonProxy.aum, 0);
		tarmaRoot = new AM2FlowerGen(BlocksCommonProxy.tarmaRoot, 0);

		pools = new AM2PoolGen();

		lakes = new WorldGenEssenceLakes(BlocksCommonProxy.liquidEssence);
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider){

		if (!SpawnBlacklists.worldgenCanHappenInDimension(world.provider.dimensionId))
			return;

		if (world.provider.terrainType == WorldType.FLAT) return;
		switch (world.provider.dimensionId){
		case -1:
			generateNether(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
			break;
		case 1:
			break;
		default:
			generateOverworld(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
		}
	}

	public void generateNether(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider){
		boolean isDimensionBlacklisted = dimensionBlacklist.contains(world.provider.dimensionId);

		if ((AMCore.config.BlacklistAffectOres() && isDimensionBlacklisted) == false){
			generateOre(sunstone, 20, world, random, 5, 120, chunkX, chunkZ);
		}
	}

	public void generateOverworld(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider){
		boolean isDimensionBlacklisted = dimensionBlacklist.contains(world.provider.dimensionId);
		boolean typeValid = false;

		BiomeGenBase biome = world.getBiomeGenForCoords(chunkX << 4, chunkZ << 4);
		Type[] biomeTypes = BiomeDictionary.getTypesForBiome(biome);
		iteratorBiomeTypes:
		for (Type type : biomeTypes) {
			switch (type) {
			case BEACH:
			case SWAMP:
			case JUNGLE:
			case PLAINS:
			case WATER:
				typeValid = true;
				break;
			case SNOWY:
				typeValid = false;
				break iteratorBiomeTypes;
			}
		}

		if ((AMCore.config.BlacklistAffectOres() && isDimensionBlacklisted) == false){
			generateOre(vinteum, vinteumFrequency, world, random, vinteumMin, vinteumMax, chunkX, chunkZ);
			generateOre(chimerite, chimeriteFrequency, world, random, chimeriteMin, chimeriteMax, chunkX, chunkZ);
			generateOre(blueTopaz, topazFrequency, world, random, topazMin, topazMax, chunkX, chunkZ);
			generateOre(sunstone, sunstoneFrequency, world, random, sunstoneMin, sunstoneMax, chunkX, chunkZ);
		}
		if ((AMCore.config.BlacklistAffectTrees() && isDimensionBlacklisted) == false){
			if (random.nextInt(witchChance) == 0){
				generateTree(random.nextInt(AMCore.config.spawnHugeTrees() ? 6 : 1) == 0 ? new WitchwoodTreeHuge(true) : new WitchwoodTreeEvenMoreHuge(true), world, random, chunkX, chunkZ);
			}
		}
		if ((AMCore.config.BlacklistAffectFlora() && isDimensionBlacklisted) == false){
			generateFlowers(blueOrchid, world, random, chunkX, chunkZ);
			generateFlowers(desertNova, world, random, chunkX, chunkZ);
			generateFlowers(tarmaRoot, world, random, chunkX, chunkZ);

			if (biome != BiomeGenBase.ocean && typeValid && random.nextInt(wakeChance) < 7){
				generateFlowers(wakebloom, world, random, chunkX, chunkZ);
			}
		}
		if ((AMCore.config.BlacklistAffectPools() && isDimensionBlacklisted) == false){
			if (poolChance > 0){
				if (random.nextInt(poolChance) == 0){
					generatePools(world, random, chunkX, chunkZ);
				}
			}

			if ((BiomeDictionary.isBiomeOfType(biome, Type.MAGICAL) || BiomeDictionary.isBiomeOfType(biome, Type.FOREST)) && random.nextInt(4) == 0 && TerrainGen.populate(chunkProvider, world, random, chunkX, chunkZ, true, LAKE) && (AMCore.config.spawnEtherMode() == 0 || AMCore.config.spawnEtherMode() == 1)){
				int lakeGenX = (chunkX * 16) + random.nextInt(16) + 8;
				int lakeGenY = random.nextInt(128);
				int lakeGenZ = (chunkZ * 16) + random.nextInt(16) + 8;
				(new WorldGenEssenceLakes(BlocksCommonProxy.liquidEssence)).generate(world, random, lakeGenX, lakeGenY, lakeGenZ);
			}
		}
	}

	private void generateFlowers(AM2FlowerGen flowers, World world, Random random, int chunkX, int chunkZ){
		int x = (chunkX << 4) + random.nextInt(16) + 8;
		int y = random.nextInt(128);
		int z = (chunkZ << 4) + random.nextInt(16) + 8;

		flowers.generate(world, random, x, y, z);
	}

	private void generateOre(WorldGenMinable mineable, int amount, World world, Random random, int minY, int maxY, int chunkX, int chunkZ){
		for (int i = 0; i < amount; ++i){
			int x = (chunkX << 4) + random.nextInt(16);
			int y = random.nextInt(maxY - minY) + minY;
			int z = (chunkZ << 4) + random.nextInt(16);

			mineable.generate(world, random, x, y, z);
		}
	}

	private void generateTree(WorldGenerator trees, World world, Random random, int chunkX, int chunkZ){
		int x = (chunkX * 16) + random.nextInt(16);
		int z = (chunkZ * 16) + random.nextInt(16);
		int y = world.getHeightValue(x, z);

		if (trees.generate(world, random, x, y, z)){
			aum.generate(world, random, x, y, z);
		}
	}

	private void generatePools(World world, Random random, int chunkX, int chunkZ){
		int x = (chunkX * 16) + random.nextInt(16);
		int z = (chunkZ * 16) + random.nextInt(16);
		int y = world.getHeightValue(x, z);

		pools.generate(world, random, x, y, z);
	}
}
