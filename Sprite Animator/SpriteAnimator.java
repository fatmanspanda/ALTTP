import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SpriteAnimator {

	public SpriteAnimator() {}
	static final SpriteAnimator controller = new SpriteAnimator();

	static final int SPRITESIZE = 896 * 32; // invariable lengths
	static final int PALETTESIZE = 0x78; // not simplified to understand the numbers
	static final int RASTERSIZE = 128 * 448 * 4;
	static final String HEX = "0123456789ABCDEF"; // HEX values

	// format of snes 4bpp {row (r), bit plane (b)}
	// bit plane 0 indexed such that 1011 corresponds to 0123
	static final int BPPI[][] = {
			{0,0},{0,1},{1,0},{1,1},{2,0},{2,1},{3,0},{3,1},
			{4,0},{4,1},{5,0},{5,1},{6,0},{6,1},{7,0},{7,1},
			{0,2},{0,3},{1,2},{1,3},{2,2},{2,3},{3,2},{3,3},
			{4,2},{4,3},{5,2},{5,3},{6,2},{6,3},{7,2},{7,3}
	};
	/* taken and modified from
	 * http://alttp.mymm1.com/sprites/includes/animations.txt
	 * credit: mike trethewey
	 */
	static final String[] ANIMNAMES = {
		"sta*nd", "standUp", "standDown", "walk", "walkUp", "walkDown", "bonk", "bonkUp", "bonkDown",
		"swim", "swimUp", "swimDown", "swimFlap", "treadingWater", "treadingWaterUp", "treadingWaterDown",
		"attack", "attackUp", "attackDown", "dashRelease", "dashReleaseUp", "dashReleaseDown",
		"spinAttack", "spinAttackLeft", "spinAttackUp", "spinAttackDown",
		"dashSpinup", "dashSpinupUp", "dashSpinupDown", "salute", "itemGet", "triforceGet",
		"readBook", "fall", "grab", "grabUp", "grabDown", "lift", "liftUp", "liftDown",
		"carry", "carryUp", "carryDown", "treePull", "throw", "throwUp", "throwDown",
		"push", "pushUp", "pushDown", "shovel", "boomerang", "boomerangUp", "boomerangDown",
		"rod", "rodUp", "rodDown", "powder", "powderUp", "powderDown", "cane", "caneUp", "caneDown",
		"bow", "bowUp", "bowDown", "bombos", "ether", "quake", "hookshot", "hookshotUp", "hookshotDown",
		"zap", "bunnyStand", "bunnyStandUp", "bunnyStandDown", "bunnyWalk", "bunnyWalkUp", "bunnyWalkDown",
		"walkDownstairs2F", "walkDownstairs1F", "walkUpstairs1F", "walkUpstairs2F",
		"deathSpin", "deathSplat", "poke", "pokeUp", "pokeDown", "tallGrass", "tallGrassUp", "tallGrassDown",
		"mapDungeon", "mapWorld", "sleep", "awake"
		};

	/*
	 * { ... } contains each animation
	 * { { ... } } contains each frame
	 * { { { ... } } contains each sprite
	 * for each sprite:
	 * { TopLeftX, TopLeftY, Width, Height, DrawX, DrawY, Flag }
	 * Flags:
	 * 0 - normal
	 * 1 - Mirror over Y axis
	 * 2 - Mirror over X axis
	 * 3 - Mirror over X and Y
	 * 
	 * TODO : fix -B, -R, -T (commented after numbers)
	 */
	static final int[][][][] FRAMES = {
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 16, 16, 16, 0, 0, 0 } }, { { 160, 48, 16, 16, 0, 0, 0 }, { 16, 32, 16, 16, 0, 0, 0 } }, { { 160, 64, 16, 16, 0, 0, 0 }, { 256, 112, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 288, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 272, 96, 16, 16, 0, 0, 0 } }, { { 160, 48, 16, 16, 0, 0, 0 }, { 272, 112, 16, 16, 0, 0, 0 } }, { { 160, 64, 16, 16, 0, 0, 0 }, { 288, 48, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 16, 96, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 0, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 288, 112, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 48, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 112, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 64, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 80, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 96, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 64, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 80, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 80, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 80, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 80, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 96, 16, 16, 0, 0, 1 } } },
			{ { { 80, 48, 16, 16, 0, 0, 0 } } },
			{ { { 80, 64, 16, 16, 0, 0, 0 } } },
			{ { { 80, 32, 16, 16, 0, 0, 0 } } },
			{ { { 112, 80, 16, 16, 0, 0, 0 }, { 128, 112, 16, 16, 0, 0, 0 } }, { { 112, 96, 16, 16, 0, 0, 0 }, { 144, 0, 16, 16, 0, 0, 0 } }, { { 112, 80, 16, 16, 0, 0, 0 }, { 128, 112, 16, 16, 0, 0, 0 } }, { { 112, 112, 16, 16, 0, 0, 0 }, { 144, 16, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 128, 80, 16, 16, 0, 0, 0 } }, { { 64, 64, 16, 16, 0, 0, 0 }, { 128, 96, 16, 16, 0, 0, 0 } } },
			{ { { 128, 48, 16, 16, 0, 0, 0 }, { 144, 48, 16, 16, 0, 0, 0 } }, { { 128, 64, 16, 16, 0, 0, 0 }, { 144, 64, 16, 16, 0, 0, 0 } } },
			{ { { 240, 0, 16, 16, 0, 0, 0 } }, { { 144, 80, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 176, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 176, 0, 16, 16, 0, 0, 1 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 144, 32, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 144, 32, 16, 16, 0, 0, 1 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 144, 32, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 144, 32, 16, 16, 0, 0, 1 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 416, 112, 16, 16, 0, 0, 0 } }, { { 400, 96, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 80, 16, 16, 0, 0, 0 } } },
			{ { { 80, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 32, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 432, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 32, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 176, 64, 16, 16, 0, 0, 0 } } },
			{ { { 80, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 32, 96, 16, 16, 0, 0, 0 } }, { { 0, 64, 16, 16, 0, 0, 0 }, { 48, 0, 16, 16, 0, 0, 0 } }, { { 0, 64, 16, 16, 0, 0, 0 }, { 432, 0, 16, 16, 0, 0, 0 } }, { { 0, 64, 16, 16, 0, 0, 0 }, { 48, 0, 16, 16, 0, 0, 0 } }, { { 0, 48, 16, 16, 0, 0, 0 }, { 176, 48, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 192, 96, 16, 16, 0, 0, 0 } }, { { 160, 48, 16, 16, 0, 0, 0 }, { 336, 16, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 192, 96, 16, 16, 0, 0, 0 } }, { { 160, 64, 16, 16, 0, 0, 0 }, { 192, 112, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 64, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 80, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 64, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 80, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 1 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 16, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 32, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 432, 32, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 432, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 0, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 128, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 240, 16, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 128, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 240, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 128, 0, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 1 }, { 128, 16, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 240, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 128, 16, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 128, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 128, 16, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 16, 16, 16, 0, 0, 1 } }, { { 80, 16, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 16, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 240, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 16, 16, 16, 0, 0, 1 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 32, 96, 16, 16, 0, 0, 0 } }, { { 80, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 32, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 240, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 32, 96, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 16, 16, 16, 0, 0, 0 } }, { { 160, 48, 16, 16, 0, 0, 0 }, { 16, 32, 16, 16, 0, 0, 0 } }, { { 160, 64, 16, 16, 0, 0, 0 }, { 256, 112, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 288, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 272, 96, 16, 16, 0, 0, 0 } }, { { 160, 48, 16, 16, 0, 0, 0 }, { 272, 112, 16, 16, 0, 0, 0 } }, { { 160, 64, 16, 16, 0, 0, 0 }, { 288, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 16, 16, 16, 0, 0, 0 } }, { { 160, 48, 16, 16, 0, 0, 0 }, { 16, 32, 16, 16, 0, 0, 0 } }, { { 160, 64, 16, 16, 0, 0, 0 }, { 256, 112, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 288, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 272, 96, 16, 16, 0, 0, 0 } }, { { 160, 48, 16, 16, 0, 0, 0 }, { 272, 112, 16, 16, 0, 0, 0 } }, { { 160, 64, 16, 16, 0, 0, 0 }, { 288, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 16, 16, 16, 0, 0, 0 } }, { { 160, 48, 16, 16, 0, 0, 0 }, { 16, 32, 16, 16, 0, 0, 0 } }, { { 160, 64, 16, 16, 0, 0, 0 }, { 256, 112, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 288, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 272, 96, 16, 16, 0, 0, 0 } }, { { 160, 48, 16, 16, 0, 0, 0 }, { 272, 112, 16, 16, 0, 0, 0 } }, { { 160, 64, 16, 16, 0, 0, 0 }, { 288, 48, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 16, 96, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 0, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 288, 112, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 48, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 112, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 64, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 80, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 96, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 16, 96, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 0, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 288, 112, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 48, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 112, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 64, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 80, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 96, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 16, 96, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 0, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 288, 112, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 48, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 112, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 64, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 304, 80, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 80, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 80, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 80, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 80, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 96, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 80, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 80, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 80, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 80, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 96, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 80, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 80, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 80, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 80, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 96, 16, 16, 0, 0, 1 } } },
			{ { { 0, 48, 16, 16, 0, 0, 0 }, { 240, 64, 16, 16, 0, 0, 1 } } },
			{ { { 176, 16, 16, 16, 0, 0, 0 }, { 176, 32, 16, 16, 0, 0, 0 } } },
			{ { { 400, 32, 16, 16, 0, 0, 0 }, { 432, 64, 16, 16, 0, 0, 0 } } },
			{ { { 160, 80, 16, 16, 0, 0, 0 }, { 160, 96, 16, 16, 0, 0, 0 } }, { { 272, 16, 16, 16, 0, 0, 0 } }, { { 0, 80, 16, 16, 0, 0, 0 }, { 256, 16, 16, 16, 0, 0, 0 } }, { { 0, 80, 16, 16, 0, 0, 0 }, { 256, 0, 16, 16, 0, 0, 0 } }, { { 288, 16, 16, 16, 0, 0, 0 } } },
			{ { { 96, 0, 16, 16, 0, 0, 0 } }, { { 64, 80, 16, 16, 0, 0, 0 } }, { { 64, 96, 16, 16, 0, 0, 0 } }, { { 112, 4 /* t */, 0, 16, 16, 0, 0 } }, { { 112, 4 /* b */, 0, 16, 16, 0, 0 } }, { { 96, 4 /* b */, 0, 16, 16, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 368, 32, 16, 16, 0, 0, 0 } }, { { 400, 48, 16, 16, 0, 0, 0 }, { 400, 64, 16, 16, 0, 0, 0 } } },
			{ { { 400, 0, 16, 16, 0, 0, 0 }, { 336, 80, 16, 16, 0, 0, 0 } }, { { 384, 96, 16, 16, 0, 0, 0 } } },
			{ { { 64, 48, 16, 16, 0, 0, 0 }, { 368, 80, 16, 16, 0, 0, 0 } }, { { 320, 0, 16, 16, 0, 0, 0 }, { 240, 112, 16, 16, 0, 0, 0 } } },
			{ { { 64, 32, 16, 16, 0, 0, 0 }, { 320, 80, 16, 16, 0, 0, 0 } }, { { 320, 16, 16, 16, 0, 0, 0 }, { 320, 96, 16, 16, 0, 0, 0 } }, { { 176, 96, 16, 16, 0, 0, 0 }, { 224, 32, 16, 16, 0, 0, 0 } } },
			{ { { 320, 32, 16, 16, 0, 0, 0 }, { 320, 112, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 336, 0, 16, 16, 0, 0, 0 } }, { { 176, 112, 16, 16, 0, 0, 0 }, { 224, 80, 16, 16, 0, 0, 0 } } },
			{ { { 320, 0, 16, 16, 0, 0, 0 }, { 320, 64, 16, 16, 0, 0, 0 } }, { { 320, 0, 16, 16, 0, 0, 0 }, { 320, 48, 16, 16, 0, 0, 0 } }, { { 176, 80, 16, 16, 0, 0, 0 }, { 208, 112, 16, 16, 0, 0, 0 } } },
			{ { { 176, 96, 16, 16, 0, 0, 0 }, { 224, 32, 16, 16, 0, 0, 0 } }, { { 176, 96, 16, 16, 0, 0, 0 }, { 224, 48, 16, 16, 0, 0, 0 } }, { { 176, 96, 16, 16, 0, 0, 0 }, { 224, 64, 16, 16, 0, 0, 0 } } },
			{ { { 176, 112, 16, 16, 0, 0, 0 }, { 224, 80, 16, 16, 0, 0, 0 } }, { { 176, 112, 16, 16, 0, 0, 0 }, { 224, 96, 16, 16, 0, 0, 0 } }, { { 176, 112, 16, 16, 0, 0, 0 }, { 224, 112, 16, 16, 0, 0, 0 } } },
			{ { { 176, 80, 16, 16, 0, 0, 0 }, { 208, 112, 16, 16, 0, 0, 0 } }, { { 176, 80, 16, 16, 0, 0, 0 }, { 224, 0, 16, 16, 0, 0, 0 } }, { { 176, 80, 16, 16, 0, 0, 0 }, { 224, 16, 16, 16, 0, 0, 0 } } },
			{ { { 240, 112, 16, 16, 0, 0, 3 }, { 64, 112, 16, 16, 0, 0, 0 } }, { { 208, 0, 16, 16, 0, 0, 0 }, { 0, 16, 16, 16, 0, 0, 3 } }, { { 160, 32, 16, 16, 0, 0, 0 }, { 160, 0, 16, 16, 0, 0, 0 } }, { { 160, 16, 16, 16, 0, 0, 0 } }, { { 80, 64, 16, 16, 0, 0, 0 } }, { { 208, 0, 16, 16, 0, 0, 0 }, { 0, 16, 16, 16, 0, 0, 3 } }, { { 160, 32, 16, 16, 0, 0, 0 }, { 160, 0, 16, 16, 0, 0, 0 } }, { { 160, 16, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 192, 96, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } } },
			{ { { 320, 16, 16, 16, 0, 0, 0 }, { 368, 32, 16, 16, 0, 0, 0 } }, { { 320, 16, 16, 16, 0, 0, 0 }, { 368, 48, 16, 16, 0, 0, 0 } }, { { 320, 16, 16, 16, 0, 0, 0 }, { 368, 64, 16, 16, 0, 0, 0 } }, { { 320, 16, 16, 16, 0, 0, 0 }, { 368, 32, 16, 16, 0, 0, 0 } }, { { 320, 16, 16, 16, 0, 0, 0 }, { 368, 48, 16, 16, 0, 0, 0 } }, { { 320, 16, 16, 16, 0, 0, 0 }, { 368, 32, 16, 16, 0, 0, 0 } }, { { 320, 16, 16, 16, 0, 0, 0 }, { 368, 48, 16, 16, 0, 0, 0 } }, { { 320, 16, 16, 16, 0, 0, 0 }, { 368, 64, 16, 16, 0, 0, 0 } } },
			{ { { 320, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 320, 32, 16, 16, 0, 0, 0 }, { 192, 64, 16, 16, 0, 0, 0 } }, { { 320, 32, 16, 16, 0, 0, 0 }, { 192, 80, 16, 16, 0, 0, 0 } }, { { 320, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 320, 32, 16, 16, 0, 0, 0 }, { 192, 64, 16, 16, 0, 0, 1 } }, { { 320, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 320, 32, 16, 16, 0, 0, 0 }, { 192, 64, 16, 16, 0, 0, 0 } }, { { 320, 32, 16, 16, 0, 0, 0 }, { 192, 80, 16, 16, 0, 0, 0 } } },
			{ { { 320, 0, 16, 16, 0, 0, 0 }, { 368, 80, 16, 16, 0, 0, 0 } }, { { 320, 0, 16, 16, 0, 0, 0 }, { 368, 96, 16, 16, 0, 0, 0 } }, { { 320, 0, 16, 16, 0, 0, 0 }, { 368, 112, 16, 16, 0, 0, 0 } }, { { 320, 0, 16, 16, 0, 0, 0 }, { 368, 80, 16, 16, 0, 0, 0 } }, { { 320, 0, 16, 16, 0, 0, 0 }, { 368, 96, 16, 16, 0, 0, 1 } }, { { 320, 0, 16, 16, 0, 0, 0 }, { 368, 80, 16, 16, 0, 0, 0 } }, { { 320, 0, 16, 16, 0, 0, 0 }, { 368, 96, 16, 16, 0, 0, 0 } }, { { 320, 0, 16, 16, 0, 0, 0 }, { 368, 112, 16, 16, 0, 0, 0 } } },
			{ { { 16, 112, 16, 16, 0, 0, 0 }, { 48, 112, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 80, 80, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 112, 16, 16, 0, 0, 0 } } },
			{ { { 288, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } } },
			{ { { 272, 32, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 256, 96, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 256, 80, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 48, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } } },
			{ { { 96, 2 /* r */, 0, 16, 16, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 208, 64, 16, 16, 0, 0, 0 } } },
			{ { { 96, 3 /* r */, 0, 16, 16, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 32, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 208, 80, 16, 16, 0, 0, 0 } } },
			{ { { 96, 1 /* r */, 0, 16, 16, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 208, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 48, 0, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 80, 16, 16, 0, 0, 0 } } },
			{ { { 80, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 32, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 176, 64, 16, 16, 0, 0, 0 } } },
			{ { { 80, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 32, 96, 16, 16, 0, 0, 0 } }, { { 0, 48, 16, 16, 0, 0, 0 }, { 48, 0, 16, 16, 0, 0, 0 } }, { { 0, 48, 16, 16, 0, 0, 0 }, { 176, 48, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 128, 32, 16, 16, 0, 0, 1 } }, { { 176, 16, 16, 16, 0, 0, 0 }, { 224, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 64, 16, 16, 0, 0, 0 } } },
			{ { { 80, 16, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 240, 80, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 32, 16, 16, 0, 0, 0 } } },
			{ { { 80, 0, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 48, 0, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 192, 96, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 240, 96, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 64, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 240, 80, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 64, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 64, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 192, 96, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 192, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 192, 96, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 192, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 176, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 48, 0, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 192, 96, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 192, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 192, 96, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 192, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 48, 16, 16, 0, 0, 0 } }, { { 0, 112, 16, 16, 0, 0, 0 }, { 240, 64, 16, 16, 0, 0, 1 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 192, 96, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 192, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 192, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 192, 96, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 192, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 240, 64, 16, 16, 0, 0, 1 } }, { { 176, 80, 16, 16, 0, 0, 0 }, { 208, 112, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 256, 16, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 32, 64, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 32, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 48, 0, 16, 16, 0, 0, 0 } } },
			{ { { 272, 0, 16, 16, 0, 0, 0 } }, { { 288, 0, 16, 16, 0, 0, 0 } } },
			{ { { 416, 64, 16, 16, 0, 0, 0 }, { 416, 80, 16, 16, 0, 0, 0 } } },
			{ { { 416, 16, 16, 16, 0, 0, 0 }, { 416, 32, 16, 16, 0, 0, 0 } } },
			{ { { 400, 80, 16, 16, 0, 0, 0 }, { 416, 0, 16, 16, 0, 0, 0 } } },
			{ { { 416, 64, 16, 16, 0, 0, 0 }, { 416, 80, 16, 16, 0, 0, 0 } }, { { 416, 64, 16, 16, 0, 0, 0 }, { 416, 96, 16, 16, 0, 0, 0 } } },
			{ { { 416, 16, 16, 16, 0, 0, 0 }, { 416, 32, 16, 16, 0, 0, 0 } }, { { 416, 16, 16, 16, 0, 0, 0 }, { 416, 48, 16, 16, 0, 0, 0 } } },
			{ { { 400, 80, 16, 16, 0, 0, 0 }, { 416, 0, 16, 16, 0, 0, 0 } }, { { 400, 80, 16, 16, 0, 0, 0 }, { 400, 112, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 336, 80, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 336, 96, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 64, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 80, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 336, 80, 16, 16, 0, 0, 0 } }, { { 368, 16, 16, 16, 0, 0, 1 }, { 384, 64, 16, 16, 0, 0, 1 } }, { { 368, 16, 16, 16, 0, 0, 1 }, { 384, 80, 16, 16, 0, 0, 1 } }, { { 368, 16, 16, 16, 0, 0, 1 }, { 384, 48, 16, 16, 0, 0, 1 } }, { { 368, 16, 16, 16, 0, 0, 1 }, { 384, 64, 16, 16, 0, 0, 1 } }, { { 368, 16, 16, 16, 0, 0, 1 }, { 384, 80, 16, 16, 0, 0, 1 } }, { { 368, 16, 16, 16, 0, 0, 1 }, { 384, 48, 16, 16, 0, 0, 1 } }, { { 368, 16, 16, 16, 0, 0, 1 }, { 384, 64, 16, 16, 0, 0, 1 } }, { { 368, 16, 16, 16, 0, 0, 1 }, { 384, 80, 16, 16, 0, 0, 1 } }, { { 368, 16, 16, 16, 0, 0, 1 }, { 384, 48, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 336, 16, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 336, 32, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 336, 16, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 336, 32, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 336, 16, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 336, 32, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 336, 16, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 336, 32, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } } },
			{ { { 0, 0, 16, 16, 0, 0, 1 }, { 336, 32, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 336, 16, 16, 16, 0, 0, 1 } }, { { 16, 112, 16, 16, 0, 0, 1 }, { 384, 16, 16, 16, 0, 0, 1 } }, { { 16, 112, 16, 16, 0, 0, 1 }, { 384, 32, 16, 16, 0, 0, 1 } }, { { 16, 112, 16, 16, 0, 0, 1 }, { 384, 0, 16, 16, 0, 0, 1 } }, { { 16, 112, 16, 16, 0, 0, 1 }, { 384, 16, 16, 16, 0, 0, 1 } }, { { 16, 112, 16, 16, 0, 0, 1 }, { 384, 32, 16, 16, 0, 0, 1 } }, { { 16, 112, 16, 16, 0, 0, 1 }, { 384, 0, 16, 16, 0, 0, 1 } }, { { 16, 112, 16, 16, 0, 0, 1 }, { 384, 16, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 64, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 48, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 64, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 48, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 288, 96, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 336, 80, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 336, 96, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } }, { { 368, 16, 16, 16, 0, 0, 0 }, { 384, 48, 16, 16, 0, 0, 0 } }, { { 368, 16, 16, 16, 0, 0, 0 }, { 384, 64, 16, 16, 0, 0, 0 } }, { { 368, 16, 16, 16, 0, 0, 0 }, { 384, 80, 16, 16, 0, 0, 0 } }, { { 368, 16, 16, 16, 0, 0, 0 }, { 384, 48, 16, 16, 0, 0, 0 } }, { { 368, 16, 16, 16, 0, 0, 0 }, { 384, 64, 16, 16, 0, 0, 0 } }, { { 368, 16, 16, 16, 0, 0, 0 }, { 384, 80, 16, 16, 0, 0, 0 } }, { { 368, 16, 16, 16, 0, 0, 0 }, { 384, 48, 16, 16, 0, 0, 0 } }, { { 368, 16, 16, 16, 0, 0, 0 }, { 384, 64, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 336, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 336, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 336, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 336, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 336, 16, 16, 16, 0, 0, 0 } }, { { 16, 112, 16, 16, 0, 0, 0 }, { 384, 16, 16, 16, 0, 0, 0 } }, { { 16, 112, 16, 16, 0, 0, 0 }, { 384, 32, 16, 16, 0, 0, 0 } }, { { 16, 112, 16, 16, 0, 0, 0 }, { 384, 0, 16, 16, 0, 0, 0 } }, { { 16, 112, 16, 16, 0, 0, 0 }, { 384, 16, 16, 16, 0, 0, 0 } }, { { 16, 112, 16, 16, 0, 0, 0 }, { 384, 32, 16, 16, 0, 0, 0 } }, { { 16, 112, 16, 16, 0, 0, 0 }, { 384, 0, 16, 16, 0, 0, 0 } }, { { 16, 112, 16, 16, 0, 0, 0 }, { 384, 16, 16, 16, 0, 0, 0 } }, { { 16, 112, 16, 16, 0, 0, 0 }, { 384, 32, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 48, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 64, 16, 16, 0, 0, 1 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 64, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 1 }, { 16, 0, 16, 16, 0, 0, 1 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 64, 32, 16, 16, 0, 0, 0 }, { 144, 96, 16, 16, 0, 0, 0 } }, { { 144, 112, 16, 16, 0, 0, 0 } } },
			{ { { 64, 32, 16, 16, 0, 0, 0 }, { 144, 96, 16, 16, 0, 0, 0 } }, { { 144, 112, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 208, 48, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 208, 32, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 80, 96, 16, 16, 0, 0, 0 } } },
			{ { { 64, 64, 16, 16, 0, 0, 0 }, { 48, 32, 16, 16, 0, 0, 0 } }, { { 64, 64, 16, 16, 0, 0, 0 }, { 80, 112, 16, 16, 0, 0, 0 } }, { { 64, 64, 16, 16, 0, 0, 0 }, { 176, 64, 16, 16, 0, 0, 0 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 208, 16, 16, 16, 0, 0, 0 } }, { { 64, 48, 16, 16, 0, 0, 0 }, { 96, 112, 16, 16, 0, 0, 0 } } },
			{ { { 0, 0, 16, 16, 0, 0, 0 }, { 16, 0, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 336, 16, 16, 16, 0, 0, 0 } }, { { 0, 0, 16, 16, 0, 0, 0 }, { 336, 32, 16, 16, 0, 0, 0 } } },
			{ { { 0, 32, 16, 16, 0, 0, 0 }, { 336, 80, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 336, 96, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 32, 16, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 48, 64, 16, 16, 0, 0, 0 } }, { { 0, 32, 16, 16, 0, 0, 0 }, { 192, 80, 16, 16, 0, 0, 1 } } },
			{ { { 0, 16, 16, 16, 0, 0, 0 }, { 16, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 48, 16, 16, 0, 0, 0 } }, { { 0, 16, 16, 16, 0, 0, 0 }, { 336, 64, 16, 16, 0, 0, 0 } } },
			{ { { 160, 112, 16, 16, 0, 0, 0 } } },
			{ { { 384, 112, 16, 16, 0, 0, 0 } } },
			{ { { 0, 96, 16, 16, 0, 0, 0 }, { 48, 48, 16, 16, 0, 0, 0 } } },
			{ { { 64, 48, 16, 16, 0, 0, 0 }, { 48, 48, 16, 16, 0, 0, 0 } } }
	};
	public static void main(String[] args) {

	}
	/**
	 * Reads a sprite file
	 * @throws IOException
	 */
	public static byte[] readSprite(String path) throws IOException {
		File file = new File(path);
		byte[] ret = new byte[(int) file.length()];
		FileInputStream s;
		try {
			s = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw e;
		}
		try {
			s.read(ret);
			s.close();
		} catch (IOException e) {
			throw e;
		}

		return ret;
	}
	/**
	 * Takes a sprite and turns it into 896 blocks of 8x8 pixels
	 * @param sprite
	 */
	public static byte[][][] sprTo8x8(byte[] sprite) {
		byte[][][] ret = new byte[896][8][8];

		// current block we're working on, each sized 32
		// start at -1 since we're incrementing at 0mod32
		int b = -1;
		// locate where in interlacing map we're reading from
		int g;
		for (int i = 0; i < SPRITESIZE; i++) {
			// find interlacing index
			g = i%32;
			// increment at 0th index
			if (g == 0)
				b++;
			// row to look at
			int r = BPPI[g][0];
			// bit plane of byte
			int p = BPPI[g][1];

			// byte to unravel
			byte q = sprite[i];

			// run through the byte
			for (int c = 0; c < 8; c++) {
				// AND with 1 shifted to the correct plane
				boolean bitOn = (q & (1 << (7-c))) != 0;
				// if true, OR with that plane in index map
				if (bitOn)
					ret[b][r][c] |= (1 << (p));
			}
		}
		return ret;
	}
	
	/**
	 * Turn index map in 8x8 format into an array of ABGR values
	 */
	public static byte[] makeRaster(byte[][][] ebe, byte[][] palette) {
		byte[] ret = new byte[RASTERSIZE];
		int largeCol = 0;
		int intRow = 0;
		int intCol = 0;
		int index = 0;
		byte[] color;
		// read image
		for (int i = 0; i < RASTERSIZE; i++) {
			// get pixel color index
			byte coli = ebe[index][intRow][intCol];
			// get palette color
			color = palette[coli];
			// index 0 = trans
			if (coli == 0)
				ret[i*4] = 0;
			else
				ret[i*4] = (byte) 255;

			// BGR
			ret[i*4+1] = color[2];
			ret[i*4+2] = color[1];
			ret[i*4+3] = color[0];

			// count up square by square
			// at 8, reset the "Interior column" which we use to locate the pixel in 8x8
			// increments the "Large column", which is the index of the 8x8 sprite on the sheet
			// at 16, reset the index and move to the next row
			// (so we can wrap around back to our old 8x8)
			// after 8 rows, undo the index reset, and move on to the next super row
			intCol++;
			if (intCol == 8) {
				index++;
				largeCol++;
				intCol = 0;
				if (largeCol == 16) {
					index -= 16;
					largeCol = 0;
					intRow++;
					if (intRow == 8) {
						index += 16;
						intRow = 0;
					}
				}
			}
		}
		return ret;
	}
	/**
	 * Splits a palette into RGB arrays.
	 * Only uses the first 16 colors.
	 * Automatically makes first index black.
	 */
	public static byte[][] splitPal(int[] pal) {
		byte[][] ret = new byte[16][3];
		for (int i = 0; i < 16; i++) {
			int color = pal[i];
			byte r = (byte) (color / 1000000);
			byte g = (byte) ((color % 1000000) / 1000);
			byte b = (byte) (color % 1000);

			ret[i][0] = r;
			ret[i][1] = g;
			ret[i][2] = b;
		}

		// make black;
		// separate operation just in case I don't wanna change pal's values
		ret[0][0] = 0;
		ret[0][1] = 0;
		ret[0][2] = 0;

		return ret;
	}

	/*
	 * GUI related functions
	 */
	/**
	 * gives file extension name from a string
	 * @param s - test case
	 * @return extension type
	 */
	public static String getFileType(String s) {
		String ret = s.substring(s.lastIndexOf(".") + 1);
		return ret;
	}

	/**
	 * Test a file against multiple extensions.
	 * The way <b>getFileType</b> works should allow
	 * both full paths and lone file types to work.
	 * 
	 * @param s - file name or extension
	 * @param type - list of all extensions to test against
	 * @return <tt>true</tt> if any extension is matched
	 */
	public static boolean testFileType(String s, String[] type) {
		boolean ret = false;
		String filesType = getFileType(s);
		for (String t : type) {
			if (filesType.equalsIgnoreCase(t)) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	/**
	 * Test a file against a single extension.
	 * 
	 * @param s - file name or extension
	 * @param type - extension
	 * @return <tt>true</tt> if extension is matched
	 */
	public static boolean testFileType(String s, String type) {
		return testFileType(s, new String[] { type });
	}
}
