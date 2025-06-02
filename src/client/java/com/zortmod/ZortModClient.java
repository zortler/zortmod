package com.zortmod;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zortmod.config.ZortModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;

import com.google.gson.*;
import java.io.*;



@Environment(EnvType.CLIENT)
public class ZortModClient implements ClientModInitializer {

	public String MOD_ID = "zortmod";
	public String NAME = "ZortMod", VERSION = "1.2.1", AUTHOR = "Zorty", NAMEVER = NAME + " " + VERSION;

	public static final Logger LOGGER = LoggerFactory.getLogger("zortmod");
	public static ZortModConfig CONFIG;
	public static final File SAVE_DIR = new File(FabricLoader.getInstance().getGameDir().toFile(), "zmdata");
	public static int GLOBAL = 0;
	public static int TEMP = 0;
	public static int TIMER = 0;
	public static boolean RUN = false;
	public static boolean RESET_TIMER = false;
	public static MinecraftClient MC;
	public static ClientPlayerEntity PLAYER;
	public static ServerPlayerEntity SERVER;
	public Vec3d LAST_POS = new Vec3d(0.0D, 0.0D, 0.0D);
	public boolean LAST_ON_GROUND = true;
	public int FINISH_TIME = 0;
	public static boolean SPLIT_TIMER = false;

	public static int PB = 0;
	public Vec3d START_POS = null;
	public Vec3d END_POS = null;
	public double START_DX = 0.1;
	public double START_DZ = 0.1;
	public double END_DX = 0.1;
	public double END_DZ = 0.1;
	public static int SPLIT_COUNT = 0;
	ArrayList<Vec3d> SPLITS = new ArrayList<Vec3d>();
	ArrayList<Double> SPLIT_DX = new ArrayList<Double>();
	ArrayList<Double> SPLIT_DZ = new ArrayList<Double>();
	static ArrayList<Integer> PB_SPLITS = new ArrayList<Integer>();
	static ArrayList<Integer> BEST_SPLITS = new ArrayList<Integer>();
	static ArrayList<Integer> GOLD_SPLITS = new ArrayList<Integer>();
	static ArrayList<Integer> TEMP_SPLITS = new ArrayList<Integer>();

	static ArrayList<Boolean> SPLIT_PASSED = new ArrayList<Boolean>();

	public static int SPLIT_DISPLAY;
	public static Color SPLIT_COLOR = Color.WHITE;


	@Override
	public void onInitializeClient() {

		LOGGER.info("Initializing " + NAMEVER + " by " + AUTHOR);
		AutoConfig.register(ZortModConfig.class, GsonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(ZortModConfig.class).getConfig();
		if (!SAVE_DIR.exists()) {
			SAVE_DIR.mkdirs();
		}

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if(!RUN) GLOBAL = 0;
			MC = MinecraftClient.getInstance();
			PLAYER = MC.player;
			if(CONFIG.temp_centered) {
				CONFIG.temp_x_pos = getCenterx(MC);
			}
			if(PLAYER != null) {
				if(RESET_TIMER) {
					TIMER = 0;
					RESET_TIMER = false;
				}
				Vec3d pos = PLAYER.getPos();

				if(LAST_ON_GROUND && (pos.getY() == LAST_POS.getY() || PLAYER.isOnGround())) {
					TEMP++;
					if(RUN) GLOBAL++;
				} else if(PLAYER.isOnGround()) {
					TEMP = 0;
				}
				if(START_POS != null) {
					if(Math.abs(START_POS.getX() - pos.getX()) <= START_DX/2 && Math.abs(START_POS.getZ() - pos.getZ()) <= START_DZ/2 && START_POS.getY() == pos.getY()) {
						GLOBAL = 0;
						TEMP = 0;
						TIMER = 0;
						for(int i = 0; i < SPLIT_COUNT; i++) {
							TEMP_SPLITS.set(i, Integer.valueOf(0));
							SPLIT_PASSED.set(i, Boolean.FALSE);
						}
						if(END_POS != null) RUN = true;
					}
				}
				if(SPLIT_COUNT > 0 && RUN) {
					for(int i = 0; i < SPLIT_COUNT; i++) {
						if(Math.abs(SPLITS.get(i).getX() - pos.getX()) <= SPLIT_DX.get(i)/2 && Math.abs(SPLITS.get(i).getZ() - pos.getZ()) <= SPLIT_DZ.get(i)/2 && SPLITS.get(i).getY() == pos.getY()) {
							if (!SPLIT_PASSED.get(i)) {

								TEMP_SPLITS.set(i, Integer.valueOf(TIMER));
								SPLIT_TIMER = true;
								SPLIT_COLOR = Color.WHITE;

								if (CONFIG.sob_over_pb) {
									if (BEST_SPLITS.get(i) == 0) {
										SPLIT_DISPLAY = TIMER;
									} else {
										SPLIT_DISPLAY = TIMER - BEST_SPLITS.get(i);
										if (SPLIT_DISPLAY > 0) SPLIT_COLOR = Color.RED;
										else if (SPLIT_DISPLAY < 0) SPLIT_COLOR = Color.GREEN;
									}
								} else {
									if (PB_SPLITS.get(i) == 0) {
										SPLIT_DISPLAY = TIMER;
									} else {
										SPLIT_DISPLAY = TIMER - PB_SPLITS.get(i);
										if (SPLIT_DISPLAY > 0) SPLIT_COLOR = Color.RED;
										else if (SPLIT_DISPLAY < 0) SPLIT_COLOR = Color.GREEN;
									}
								}
								if (BEST_SPLITS.get(i) == 0 || TIMER < BEST_SPLITS.get(i)) {
									BEST_SPLITS.set(i, Integer.valueOf(TIMER));
								}
								SPLIT_PASSED.set(i, Boolean.TRUE);
							}
						}

					}
				}
				if(END_POS != null && RUN) {
					if(Math.abs(END_POS.getX() - pos.getX()) <= END_DX/2 && Math.abs(END_POS.getZ() - pos.getZ()) <= END_DZ/2 && END_POS.getY() == pos.getY()) {
						GLOBAL = 0;
						TEMP = 0;
						FINISH_TIME = TIMER;
						SPLIT_TIMER = true;
						SPLIT_DISPLAY = FINISH_TIME - PB;
						SPLIT_COLOR = Color.WHITE;
						if (SPLIT_DISPLAY > 0 && PB > 0) SPLIT_COLOR = Color.RED;
						else if (SPLIT_DISPLAY < 0 && PB > 0) SPLIT_COLOR = Color.GREEN;


						if (PB == 0 || FINISH_TIME < PB) {
							PB = FINISH_TIME;
							for(int i = 0; i < SPLIT_COUNT; i++) {
								PB_SPLITS.set(i, TEMP_SPLITS.get(i));
							}
						}

						RESET_TIMER = true;
						for(int i = 0; i < SPLIT_COUNT; i++) {
							SPLIT_PASSED.set(i, Boolean.FALSE);
							TEMP_SPLITS.set(i, Integer.valueOf(0));
						}
						RUN = false;
					}
				}

				LAST_POS = pos;
				LAST_ON_GROUND = PLAYER.isOnGround();
				TIMER++;

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
									PLAYER.sendMessage(Text.literal("§7<§6zm§7> Starting Point set!"), false);
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
												PLAYER.sendMessage(Text.literal("§7<§6zm§7> Starting Box set!"), false);
                                                return 0;
                                            })
									)
								)
				)

				.then(ClientCommandManager.literal("setend")
						.executes(context -> {
							PLAYER.sendMessage(Text.literal("§7<§6zm§7> /zm setend <x-length> <y-length>"), false);
							return 0;
						})
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
											PLAYER.sendMessage(Text.literal("§7<§6zm§7> Finish Box set!"), false);
											return 0;
										})
								)
						)
				)
				.then(ClientCommandManager.literal("setsplit")
						.executes(context -> {
							PLAYER.sendMessage(Text.literal("§7<§6zm§7> /zm setsplit <x-length> <y-length>"), false);
							return 0;
						})
						.then(ClientCommandManager.argument("x-length", DoubleArgumentType.doubleArg())
								.then(ClientCommandManager.argument("z-length", DoubleArgumentType.doubleArg())
										.executes(context -> {

											SPLITS.add(PLAYER.getPos());
											SPLIT_DX.add(Double.valueOf(DoubleArgumentType.getDouble(context, "x-length")));
											SPLIT_DZ.add(Double.valueOf(DoubleArgumentType.getDouble(context, "z-length")));
											SPLIT_PASSED.add(SPLIT_COUNT, Boolean.FALSE);
											PB_SPLITS.add(Integer.valueOf(0));
											BEST_SPLITS.add(Integer.valueOf(0));
											TEMP_SPLITS.add(Integer.valueOf(0));
											SPLIT_COUNT++;
											RUN = false;
											TIMER = 0;
											resetSplits();
											PLAYER.sendMessage(Text.literal("§7<§6zm§7> Split Box set!"), false);
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
							PLAYER.sendMessage(Text.literal("§7<§6zm§7> Last Split Box removed!"), false);
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
                        })
				)

				.then(ClientCommandManager.literal("save")
						.then(ClientCommandManager.argument("name", StringArgumentType.string())
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									File file = new File(SAVE_DIR, name + ".json");
									if(!file.exists()) {
										try {
											save(name);
											PLAYER.sendMessage(Text.literal("§7<§6zm§7> Saved data as: " + name), false);
										} catch (IOException e) {
											ctx.getSource().sendError(Text.literal("§7<§6zm§7> Save failed: " + e.getMessage()));
										}
									} else {
										PLAYER.sendMessage(Text.literal("§7<§6zm§7> Save failed: " + name + " already exists"), false);
									}
									return 1;
								})
						)
				)
				.then(ClientCommandManager.literal("load")
						.then(ClientCommandManager.argument("name", StringArgumentType.string())
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									try {
										load(name);
										PLAYER.sendMessage(Text.literal("§7<§6zm§7> Loaded data from: " + name), false);
									} catch (IOException e) {
										ctx.getSource().sendError(Text.literal("§7<§6zm§7> Load failed: " + e.getMessage()));
									}
									RUN = false;
									return 1;
								})
						)
				)
				.then(ClientCommandManager.literal("list")
						.executes(ctx -> {
							File[] files = SAVE_DIR.listFiles((dir, name) -> name.endsWith(".json"));
							ArrayList<String> names = new ArrayList<String>();
							if (files != null) {
								for (File file : files) {
									String name = file.getName();
									names.add(name.substring(0, name.length() - 5)); // remove .json
								}
							}
							PLAYER.sendMessage(Text.literal("§7<§6zm§7> List of Saves: " + String.join(", ", names)), false);

							return 1;
						})
				)
				.then(ClientCommandManager.literal("delete")
						.then(ClientCommandManager.argument("name", StringArgumentType.string())
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									File file = new File(SAVE_DIR, name + ".json");
									if(file.exists()) {
										file.delete();
										PLAYER.sendMessage(Text.literal("§7<§6zm§7> Deleted data: " + name), false);
									} else {
										PLAYER.sendMessage(Text.literal("§7<§6zm§7> Data not found: " + name), false);
									}
									return 1;
								})
						)
				)
		));



	}

	public void save(String name) throws IOException {
		File file = new File(SAVE_DIR, name + ".json");
		JsonObject root = new JsonObject();

		if (START_POS != null) root.add("startPos", serializeVec3d(START_POS));
		if (END_POS != null) root.add("endPos", serializeVec3d(END_POS));

		root.addProperty("pb", PB);
		root.addProperty("startDx", START_DX);
		root.addProperty("startDz", START_DZ);
		root.addProperty("endDx", END_DX);
		root.addProperty("endDz", END_DZ);
		root.addProperty("splitCount", SPLIT_COUNT);

		JsonArray splitArray = new JsonArray();
		for (Vec3d split : SPLITS) splitArray.add(serializeVec3d(split));
		root.add("splits", splitArray);

		JsonArray splitDxArray = new JsonArray();
		for (double d : SPLIT_DX) splitDxArray.add(d);
		root.add("splitDx", splitDxArray);

		JsonArray splitDzArray = new JsonArray();
		for (double d : SPLIT_DZ) splitDzArray.add(d);
		root.add("splitDz", splitDzArray);

		JsonArray pbArray = new JsonArray();
		for (int i : PB_SPLITS) pbArray.add(i);
		root.add("pbSplits", pbArray);

		JsonArray bestArray = new JsonArray();
		for (int i : BEST_SPLITS) bestArray.add(i);
		root.add("bestSplits", bestArray);

		JsonArray goldArray = new JsonArray();
		for (int i : GOLD_SPLITS) goldArray.add(i);
		root.add("goldSplits", goldArray);

		try (FileWriter writer = new FileWriter(file)) {
			new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
		}
	}

	public void load(String name) throws IOException {
		File file = new File(SAVE_DIR, name + ".json");
		if (!file.exists()) throw new FileNotFoundException("File " + name + " not found!");

		try (FileReader reader = new FileReader(file)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			START_POS = root.has("startPos") ? deserializeVec3d(root.getAsJsonObject("startPos")) : null;
			END_POS = root.has("endPos") ? deserializeVec3d(root.getAsJsonObject("endPos")) : null;

			PB = root.get("pb").getAsInt();
			START_DX = root.get("startDx").getAsDouble();
			START_DZ = root.get("startDz").getAsDouble();
			END_DX = root.get("endDx").getAsDouble();
			END_DZ = root.get("endDz").getAsDouble();
			SPLIT_COUNT = root.get("splitCount").getAsInt();

			SPLITS.clear();
			for (JsonElement el : root.getAsJsonArray("splits")) {
				SPLITS.add(deserializeVec3d(el.getAsJsonObject()));
			}

			SPLIT_DX.clear();
			for (JsonElement el : root.getAsJsonArray("splitDx")) {
				SPLIT_DX.add(el.getAsDouble());
			}

			SPLIT_DZ.clear();
			for (JsonElement el : root.getAsJsonArray("splitDz")) {
				SPLIT_DZ.add(el.getAsDouble());
			}

			PB_SPLITS.clear();
			for (JsonElement el : root.getAsJsonArray("pbSplits")) {
				PB_SPLITS.add(el.getAsInt());
			}

			BEST_SPLITS.clear();
			for (JsonElement el : root.getAsJsonArray("bestSplits")) {
				BEST_SPLITS.add(el.getAsInt());
			}

			GOLD_SPLITS.clear();
			for (JsonElement el : root.getAsJsonArray("goldSplits")) {
				GOLD_SPLITS.add(el.getAsInt());
			}

			TEMP_SPLITS.clear();
			SPLIT_PASSED.clear();
			for(int i = 0; i < SPLIT_COUNT; i++) {
				TEMP_SPLITS.add(0);
				SPLIT_PASSED.add(false);
			}
		}
	}

	private static JsonObject serializeVec3d(Vec3d vec) {
		if (vec == null) return null;
		JsonObject obj = new JsonObject();
		obj.addProperty("x", vec.x);
		obj.addProperty("y", vec.y);
		obj.addProperty("z", vec.z);
		return obj;
	}

	private static Vec3d deserializeVec3d(JsonObject obj) {
		return new Vec3d(obj.get("x").getAsDouble(), obj.get("y").getAsDouble(), obj.get("z").getAsDouble());
	}

	public static float getCenterx(MinecraftClient client) {
		Window window = client.getWindow();
		int width = window.getWidth();
		int height = window.getHeight();
		int guiscale = client.options.getGuiScale().getValue();
		if(guiscale == 0) {
			guiscale = (int)Math.min(Math.floor((double) width/320), Math.floor((double) height/240));
		}
		float x = ((float) width)/(2*guiscale);
        return x;
    }

	public static void resetSplits() {
		for(int i = 0; i < SPLIT_COUNT; i++) {
			SPLIT_PASSED.set(i, Boolean.FALSE);
			TEMP_SPLITS.set(i, Integer.valueOf(0));
			PB_SPLITS.set(i, Integer.valueOf(0));
			BEST_SPLITS.set(i, Integer.valueOf(0));
		}

	}
}