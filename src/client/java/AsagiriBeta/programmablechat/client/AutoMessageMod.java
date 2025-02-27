package AsagiriBeta.programmablechat.client;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class AutoMessageMod implements ModInitializer {
    private static String message = "";
    private static int interval = 0;
    private static long lastSentTime = 0;
    private static boolean isSending = false;
    private static boolean isAutoEating = false;
    private static Vec3d lastPlayerPos = Vec3d.ZERO;
    private static boolean hasUpdatedPosition = false;
    private static long eatStartTime = 0;

    @Override
    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            AutoMessageCommand.register(dispatcher);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isSending && interval > 0 && System.currentTimeMillis() - lastSentTime >= interval * 1000L) {
                if (!message.isEmpty() && MinecraftClient.getInstance().player != null) { // 添加空值检查
                    // 检测消息是否以/开头，如果是则视为指令，否则视为聊天信息
                    if (message.startsWith("/")) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand(message.substring(1));
                    } else {
                        MinecraftClient.getInstance().player.networkHandler.sendChatMessage(message);
                    }
                    lastSentTime = System.currentTimeMillis();
                }
            }

            // 自动进食逻辑
            if (isAutoEating && MinecraftClient.getInstance().player != null) { // 添加空值检查
                // 检查当前是否在设置界面，如果是，则不执行自动进食逻辑
                if (MinecraftClient.getInstance().currentScreen != null) {
                    return;
                }

                Vec3d currentPos = MinecraftClient.getInstance().player.getPos();
                if (!hasUpdatedPosition) {
                    lastPlayerPos = currentPos;
                    hasUpdatedPosition = true;
                }
                if (!currentPos.equals(lastPlayerPos)) {
                    int foodLevel = MinecraftClient.getInstance().player.getHungerManager().getFoodLevel();
                    if (foodLevel <= 10) {
                        if (eatStartTime == 0) { // 如果还未开始进食
                            for (int i = 0; i < 9; i++) {
                                var stack = MinecraftClient.getInstance().player.getInventory().getStack(i);
                                if (stack.getItem() == Items.COOKED_PORKCHOP) {
                                    MinecraftClient.getInstance().player.getInventory().selectedSlot = i;
                                    MinecraftClient.getInstance().options.useKey.setPressed(true);
                                    eatStartTime = System.currentTimeMillis(); // 记录开始进食的时间
                                    break;
                                }
                            }
                        } else if (System.currentTimeMillis() - eatStartTime >= 6000) { // 如果已经进食6秒
                            MinecraftClient.getInstance().options.useKey.setPressed(false);
                            eatStartTime = 0; // 重置进食时间
                        }
                    } else if (foodLevel >= 20) {
                        MinecraftClient.getInstance().options.useKey.setPressed(false);
                        eatStartTime = 0; // 重置进食时间
                        return;
                    }
                }
            }
        });

        // 修改UI初始化逻辑，将按钮添加到选项界面
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof net.minecraft.client.gui.screen.option.OptionsScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.of("ProgrammableChat"), button -> {
                    MinecraftClient.getInstance().setScreen(new AutoMessageScreen());
                }).dimensions(10, scaledHeight - 30, 150, 20).build());
            }
        });
    }

    public static void setMessage(String msg) {
        message = msg;
    }

    public static void setInterval(int intervalSeconds) {
        interval = intervalSeconds;
    }

    public static void startSending() {
        isSending = true;
    }

    public static void stopSending() {
        isSending = false;
    }

    public static boolean isSending() {
        return isSending;
    }

    public static void setAutoEating(boolean autoEating) {
        isAutoEating = autoEating;
        hasUpdatedPosition = false; // 重置位置更新标记
    }

    public static boolean isAutoEating() {
        return isAutoEating;
    }
}