package com.zortmod.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.zortmod.config.*;
import com.zortmod.ZortModClient;

import java.awt.*;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Inject(at = @At("TAIL"), method = "render")
	public void render(DrawContext context, float tickDelta, CallbackInfo ci) {

		ZortModConfig config = ZortModClient.CONFIG;
		MinecraftClient mc = ZortModClient.MC;

		if ((config.global_enabled || config.temp_enabled) && config.opacity > 0) {

			TextRenderer renderer = mc.textRenderer;
			int textColor = config.color | ((config.opacity & 0xFF) << 24);
			float scale = config.scale;
			float split_scale = config.split_scale;
			boolean shadowed = config.shadow;

			float global_x = config.global_x_pos;
			float global_y = config.global_y_pos;
			float temp_x = config.temp_x_pos;
			float temp_y = config.temp_y_pos;
			float split_x = ZortModClient.SPLIT_X;
			float split_y = ZortModClient.SPLIT_Y;


			int global = ZortModClient.GLOBAL;
			int temp = ZortModClient.TEMP;

			if (global > 0 && config.global_enabled) {
				this.renderText(context, renderer, String.valueOf(global), global_x, global_y, textColor, scale, shadowed, false);
			}
			if (ZortModClient.PLAYER != null && config.temp_enabled && temp > 0 && !ZortModClient.PLAYER.isOnGround()) {
				this.renderText(context, renderer, String.valueOf(temp), temp_x, temp_y, textColor, scale, shadowed, ZortModClient.CONFIG.temp_centered);
			}
			if(ZortModClient.PLAYER != null && ZortModClient.SPLIT_TIMER > 0) {
				if (ZortModClient.SPLIT_COLOR == Color.RED) {
					this.renderText(context, renderer, "+" + String.valueOf(ZortModClient.SPLIT_DISPLAY), split_x, split_y, Color.RED.getRGB(), split_scale, true, true);
				} else if (ZortModClient.SPLIT_COLOR == Color.GREEN) {
					this.renderText(context, renderer,  String.valueOf(ZortModClient.SPLIT_DISPLAY), split_x, split_y, Color.GREEN.getRGB(), split_scale, true, true);
				} else {
					this.renderText(context, renderer, String.valueOf(ZortModClient.SPLIT_DISPLAY), split_x, split_y, Color.WHITE.getRGB(), split_scale, true, true);
				}
			}
		}
	}


	@Unique
	private void renderText(DrawContext context, TextRenderer textRenderer, String text, float x, float y, int color, float scale, boolean shadowed, boolean centralised) {
			MatrixStack matrixStack = context.getMatrices();
			matrixStack.push();
			matrixStack.translate(x, y, 0);
			if (centralised) {
				matrixStack.translate(-3*scale*text.length(), 0, 0);
			}
			matrixStack.scale(scale, scale, scale);
			context.drawText(textRenderer, text, 0, 0, color, shadowed);
			matrixStack.pop();
	}
}