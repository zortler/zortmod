package com.zortmod;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.zortmod.config.ZortModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;


@Environment(EnvType.CLIENT)
public class ZortModClient implements ClientModInitializer {

	public String MOD_ID = "zortmod";
	public String NAME = "ZortMod", VERSION = "1.0.0", AUTHOR = "Zorty", NAMEVER = NAME + " " + VERSION;

	public static final Logger LOGGER = LoggerFactory.getLogger("zortmod");
	public static ZortModConfig CONFIG;
	public static int GLOBAL = 0;
	public static int TEMP = 0;
	public static int TIMER = 0;
	public static boolean RUN = false;
	public static boolean RESET_TEMP = false;
	public static MinecraftClient MC;
	public static ClientPlayerEntity PLAYER;
	public static ServerPlayerEntity SERVER;
	public Vec3d LAST_POS = new Vec3d(0.0D, 0.0D, 0.0D);
	public boolean LAST_ON_GROUND = true;
	public static int PB = 0;
	public int FINISH_TIME = 0;
	public static int SPLIT_TIMER = 0;

	public static int SPLIT_COUNT = 0;
	ArrayList<Vec3d> SPLITS = new ArrayList<Vec3d>();
	ArrayList<Double> SPLIT_DX = new ArrayList<Double>();
	ArrayList<Double> SPLIT_DZ = new ArrayList<Double>();
	static ArrayList<Boolean> SPLIT_PASSED = new ArrayList<Boolean>();
	static ArrayList<Integer> TEMP_SPLITS = new ArrayList<Integer>();
	static ArrayList<Integer> PB_SPLITS = new ArrayList<Integer>();
	static ArrayList<Integer> BEST_SPLITS = new ArrayList<Integer>();

	public static int SPLIT_X;
	public static int SPLIT_Y;
	public static int SPLIT_DISPLAY;

	public Vec3d START_POS = null;
	public Vec3d END_POS = null;
	public double START_DX = 0.1;
	public double START_DZ = 0.1;
	public double END_DX = 0.1;
	public double END_DZ = 0.1;


	@Override
	public void onInitializeClient() {

		LOGGER.info("Initializing " + NAMEVER + " by " + AUTHOR);
		AutoConfig.register(ZortModConfig.class, GsonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(ZortModConfig.class).getConfig();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			MC = MinecraftClient.getInstance();
			PLAYER = MC.player;
			setSplitPos(MC);
			if(CONFIG.temp_centered) {
				setTempPos(MC);
			}
			if(PLAYER != null) {
				Vec3d pos = PLAYER.getPos();

				if(LAST_ON_GROUND && (pos.getY() == LAST_POS.getY() || PLAYER.isOnGround())) {
					TEMP++;
					GLOBAL++;
				} else if(PLAYER.isOnGround()) {
					TEMP = 0;
				}
				if(START_POS != null) {
					if(Math.abs(START_POS.getX() - pos.getX()) <= START_DX/2 && Math.abs(START_POS.getZ() - pos.getZ()) <= START_DZ/2 && START_POS.getY() == pos.getY()) {
						GLOBAL = 0;
						TEMP = 0;
						TIMER = 0;
						for(int i = 0; i < SPLIT_COUNT; i++) {
							TEMP_SPLITS.set(i, 0);
							SPLIT_PASSED.set(i, false);
						}
						if(END_POS != null) RUN = true;
					}
				}
				if(SPLIT_COUNT > 0 && RUN) {
					for(int i = 0; i < SPLIT_COUNT; i++) {
						if(Math.abs(SPLITS.get(i).getX() - pos.getX()) <= SPLIT_DX.get(i)/2 && Math.abs(SPLITS.get(i).getZ() - pos.getZ()) <= SPLIT_DZ.get(i)/2 && SPLITS.get(i).getY() == pos.getY()) {
							if (!SPLIT_PASSED.get(i)) {

								TEMP_SPLITS.set(i, TIMER);
								SPLIT_TIMER = CONFIG.split_duration;

								if (CONFIG.sob_over_pb) {
									if (BEST_SPLITS.get(i) == 0) {
										SPLIT_DISPLAY = TIMER;
									} else {
										SPLIT_DISPLAY = TIMER - BEST_SPLITS.get(i);
									}
								} else {
									if (PB_SPLITS.get(i) == 0) {
										SPLIT_DISPLAY = TIMER;
									} else {
										SPLIT_DISPLAY = TIMER - PB_SPLITS.get(i);
									}
								}
								if (BEST_SPLITS.get(i) == 0 || TIMER < BEST_SPLITS.get(i)) {
									BEST_SPLITS.set(i, TIMER);
								}
								SPLIT_PASSED.set(i, true);
							}
						}

					}
				}
				if(END_POS != null && RUN) {
					if(Math.abs(END_POS.getX() - pos.getX()) <= END_DX/2 && Math.abs(END_POS.getZ() - pos.getZ()) <= END_DZ/2 && END_POS.getY() == pos.getY()) {
						GLOBAL = 0;
						TEMP = 0;
						FINISH_TIME = TIMER;
						SPLIT_TIMER = CONFIG.split_duration;
						SPLIT_DISPLAY = FINISH_TIME;

						if (PB == 0 || FINISH_TIME < PB) {
							PB = FINISH_TIME;
							for(int i = 0; i < SPLIT_COUNT; i++) {
								PB_SPLITS.set(i, TEMP_SPLITS.get(i));
							}
						}

						TIMER = 0;
						for(int i = 0; i < SPLIT_COUNT; i++) {
							SPLIT_PASSED.set(i, false);
							TEMP_SPLITS.set(i, 0);
						}
						RUN = false;
					}
				}

				LAST_POS = pos;
				LAST_ON_GROUND = PLAYER.isOnGround();
				TIMER++;
				if(SPLIT_TIMER > 0) SPLIT_TIMER--;
			}
		});


		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("zm")
				.then(ClientCommandManager.literal("setstart")
                        .executes(context -> {
									START_POS = PLAYER.getPos();
									START_DX = 0.1D;
									START_DZ = 0.1D;
									RUN = false;
									TIMER = 0;
									PB = 0;
									FINISH_TIME = 0;
									resetSplits();
									PLAYER.sendMessage(Text.literal("Starting Point set!"));
                                    return 0;
                                })
								.then(ClientCommandManager.argument("x-length", DoubleArgumentType.doubleArg())
									.then(ClientCommandManager.argument("z-length", DoubleArgumentType.doubleArg())
											.executes(context -> {

												START_POS = PLAYER.getPos();
												RUN = false;
												TIMER = 0;
												PB = 0;
												FINISH_TIME = 0;
												resetSplits();
												START_DX = DoubleArgumentType.getDouble(context, "x-length");
												START_DZ = DoubleArgumentType.getDouble(context, "z-length");
												PLAYER.sendMessage(Text.literal("Starting Box set!"));
                                                return 0;
                                            })
									)
								)
				)
				.then(ClientCommandManager.literal("setend")
						.then(ClientCommandManager.argument("x-length", DoubleArgumentType.doubleArg())
								.then(ClientCommandManager.argument("z-length", DoubleArgumentType.doubleArg())
										.executes(context -> {

											END_POS = PLAYER.getPos();
											END_DX = DoubleArgumentType.getDouble(context, "x-length");
											END_DZ = DoubleArgumentType.getDouble(context, "z-length");
											RUN = false;
											TIMER = 0;
											PB = 0;
											FINISH_TIME = 0;
											resetSplits();
											PLAYER.sendMessage(Text.literal("Finish Box set!"));
											return 0;
										})
								)
						)
				)
				.then(ClientCommandManager.literal("setsplit")
						.then(ClientCommandManager.argument("x-length", DoubleArgumentType.doubleArg())
								.then(ClientCommandManager.argument("z-length", DoubleArgumentType.doubleArg())
										.executes(context -> {

											SPLITS.add(PLAYER.getPos());
											SPLIT_DX.add(DoubleArgumentType.getDouble(context, "x-length"));
											SPLIT_DZ.add(DoubleArgumentType.getDouble(context, "z-length"));
											SPLIT_PASSED.add(SPLIT_COUNT, false);
											PB_SPLITS.add(0);
											BEST_SPLITS.add(0);
											TEMP_SPLITS.add(0);
											SPLIT_COUNT++;
											RUN = false;
											TIMER = 0;
											resetSplits();
											PLAYER.sendMessage(Text.literal("Split Box set!"));
											return 0;
										})
								)
						)
				)
				.then(ClientCommandManager.literal("removesplit")
						.executes(context -> {

							SPLITS.remove(SPLIT_COUNT - 1);
							SPLIT_DX.remove(SPLIT_COUNT - 1);
							SPLIT_DZ.remove(SPLIT_COUNT - 1);
							SPLIT_PASSED.remove(SPLIT_COUNT - 1);
							PB_SPLITS.remove(SPLIT_COUNT - 1);
							BEST_SPLITS.remove(SPLIT_COUNT - 1);
							TEMP_SPLITS.remove(SPLIT_COUNT - 1);
							SPLIT_COUNT--;
							RUN = false;
							TIMER = 0;
							resetSplits();
							PLAYER.sendMessage(Text.literal("Last Split Box removed!"));
							return 0;
						})
				)
				.then(ClientCommandManager.literal("reset")
						.executes(context -> {
							START_POS = null;
							TIMER = 0;
							RUN = false;
							PB = 0;
							FINISH_TIME = 0;
							SPLIT_COUNT = 0;
							SPLITS = new ArrayList<Vec3d>();
							SPLIT_DX = new ArrayList<Double>();
							SPLIT_DZ = new ArrayList<Double>();
							SPLIT_PASSED = new ArrayList<Boolean>();
							TEMP_SPLITS = new ArrayList<Integer>();
							PB_SPLITS = new ArrayList<Integer>();
							BEST_SPLITS = new ArrayList<Integer>();
							START_DX = 0.1;
							START_DZ = 0.1;
							END_DX = 0.1;
							END_DZ = 0.1;
							return 0;
                        }))

		));



	}
	public static void setTempPos(MinecraftClient client) {
		Window window = client.getWindow();
		int width = window.getWidth();
		int height = window.getHeight();
		int guiscale = client.options.getGuiScale().getValue();
		if(guiscale == 0) {
			guiscale = (int)Math.min(Math.floor((double) width/320), Math.floor((double) height/240));
		}
		int x = (int) Math.round((double)width/(2*guiscale));
		int y = (int) Math.round((double)height/(2*guiscale));
		CONFIG.temp_x_pos = x - (int)Math.round(CONFIG.scale*3);
		CONFIG.temp_y_pos = y + 5;
	}
	public static void setSplitPos(MinecraftClient client) {
		Window window = client.getWindow();
		int width = window.getWidth();
		int height = window.getHeight();
		int guiscale = client.options.getGuiScale().getValue();
		if(guiscale == 0) {
			guiscale = (int)Math.min(Math.floor((double) width/320), Math.floor((double) height/240));
		}
		int x = (int) Math.round((double)width/(2*guiscale));
		int y = (int) Math.round((double)height/(guiscale));
		SPLIT_X = x - (int)Math.round(CONFIG.split_scale*5.5);
		SPLIT_Y = y - (int)(40 + CONFIG.split_scale*10);
	}
	public static void resetSplits() {
		for(int i = 0; i < SPLIT_COUNT; i++) {
			SPLIT_PASSED.set(i, false);
			TEMP_SPLITS.set(i, 0);
			PB_SPLITS.set(i, 0);
			BEST_SPLITS.set(i, 0);
		}

	}
}