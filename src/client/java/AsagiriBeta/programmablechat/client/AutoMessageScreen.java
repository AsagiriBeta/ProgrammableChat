package AsagiriBeta.programmablechat.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.client.MinecraftClient;

public class AutoMessageScreen extends Screen {

    protected AutoMessageScreen() {
        super(Text.of("Auto Message Settings"));
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int startX = this.width / 2 - buttonWidth - 10; // 第一列起始X坐标
        int startY = this.height / 2 - 50; // 第一行起始Y坐标
        int gap = 20; // 按钮之间的间距

        // 添加间隔发送设置按钮
        ButtonWidget intervalSettingsButton = ButtonWidget.builder(Text.of("间隔发送设置"), button -> {
            if (this.client != null) {
                this.client.setScreen(new IntervalSettingsScreen());
            }
        }).dimensions(startX, startY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(intervalSettingsButton);

        // 添加自动建造设置按钮
        ButtonWidget autoBuildSettingsButton = ButtonWidget.builder(Text.of("自动建造设置"), button -> {
            if (this.client != null) {
                this.client.setScreen(new AutoBuildSettingsScreen());
            }
        }).dimensions(startX + buttonWidth + gap, startY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(autoBuildSettingsButton);

        // 添加自动进食按钮
        ButtonWidget autoEatButton = ButtonWidget.builder(Text.of(AutoMessageMod.isAutoEating() ? "自动进食已打开" : "打开自动进食"), button -> {
            if (AutoMessageMod.isAutoEating()) {
                AutoMessageMod.setAutoEating(false);
                button.setMessage(Text.of("打开自动进食"));
            } else {
                AutoMessageMod.setAutoEating(true);
                button.setMessage(Text.of("自动进食已打开"));
            }
        }).dimensions(startX, startY + buttonHeight + gap, buttonWidth, buttonHeight).build();
        this.addDrawableChild(autoEatButton);

        // 添加传送点大全按钮
        ButtonWidget teleportPointsButton = ButtonWidget.builder(Text.of("传送点大全"), button -> {
            if (this.client != null) {
                this.client.setScreen(new TeleportPointsScreen());
            }
        }).dimensions(startX + buttonWidth + gap, startY + buttonHeight + gap, buttonWidth, buttonHeight).build();
        this.addDrawableChild(teleportPointsButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        super.close(); // 只保留父类默认行为
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }

    private static class IntervalSettingsScreen extends Screen {
        private TextFieldWidget messageField;
        private TextFieldWidget intervalField;

        protected IntervalSettingsScreen() {
            super(Text.of("间隔发送设置"));
        }

        @Override
        protected void init() {
            // 修改返回按钮逻辑
            ButtonWidget backButton = ButtonWidget.builder(Text.of("← 返回"), button -> {
                saveSettings(); // 保存设置
                if (this.client != null) {
                    this.client.setScreen(new AutoMessageScreen()); // 返回主菜单
                }
            }).dimensions(10, 10, 60, 20).build();
            this.addDrawableChild(backButton);

            // 初始化消息输入框
            this.messageField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 50, 200, 20, Text.of("消息/指令："));
            this.messageField.setMaxLength(256);
            this.messageField.setText(AutoMessageMod.getMessage()); // 实时获取最新消息
            this.messageField.setChangedListener(text -> {
                AutoMessageMod.setMessage(text);
                AutoMessageMod.saveConfig(); // 实时保存
            });

            // 初始化间隔输入框
            this.intervalField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Text.of("间隔时间（秒）："));
            this.intervalField.setMaxLength(10);
            this.intervalField.setText(String.valueOf(AutoMessageMod.getInterval())); // 实时获取最新间隔
            this.intervalField.setTextPredicate(text -> {
                if (text.matches("\\d*")) { // 只允许数字输入
                    if (!text.isEmpty()) {
                        AutoMessageMod.setInterval(Integer.parseInt(text));
                        AutoMessageMod.saveConfig(); // 实时保存
                    }
                    return true;
                }
                return false;
            });

            // 添加输入框到界面
            this.addSelectableChild(this.messageField);
            this.addSelectableChild(this.intervalField);

            // 修改Start/Stop按钮的保存逻辑
            ButtonWidget startStopButton = ButtonWidget.builder(Text.of(AutoMessageMod.isSending() ? "间隔发送已打开" : "打开间隔发送"), button -> {
                saveSettings(); // 新增：按下按钮时保存设置
                if (AutoMessageMod.isSending()) {
                    AutoMessageMod.stopSending();
                    button.setMessage(Text.of("打开间隔发送"));
                } else {
                    AutoMessageMod.startSending();
                    button.setMessage(Text.of("间隔发送已打开"));
                }
            }).dimensions(this.width / 2 - 100, this.height / 2 + 10, 200, 20).build();
            this.addDrawableChild(startStopButton);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);

            // 绘制文本框的提示文本
            context.drawTextWithShadow(this.textRenderer, Text.of("消息/指令："), this.width / 2 - 100, this.height / 2 - 60, 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, Text.of("间隔时间（秒）："), this.width / 2 - 100, this.height / 2 - 30, 0xFFFFFF);

            // 渲染输入框
            this.messageField.render(context, mouseX, mouseY, delta);
            this.intervalField.render(context, mouseX, mouseY, delta);
        }

        @Override
        public void close() {
            this.messageField.setFocused(false);
            this.intervalField.setFocused(false);
            saveSettings();
            AutoMessageMod.saveConfig(); // 异步保存配置
            super.close();
        }

        // 新增：保存设置的方法
        private void saveSettings() {
            String message = this.messageField.getText();
            int interval = 0;
            try {
                interval = Integer.parseInt(this.intervalField.getText());
            } catch (NumberFormatException e) {
                System.out.println("间隔时间格式错误: " + e.getMessage());
            }
            AutoMessageMod.setMessage(message);
            AutoMessageMod.setInterval(interval);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                saveSettings(); // 新增：按下 ESC 键时保存设置
                this.close();
                return true;
            }
            return this.messageField.keyPressed(keyCode, scanCode, modifiers) || 
                   this.intervalField.keyPressed(keyCode, scanCode, modifiers) || 
                   super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return this.messageField.charTyped(chr, modifiers) || 
                   this.intervalField.charTyped(chr, modifiers) || 
                   super.charTyped(chr, modifiers);
        }
    }

    private static class AutoBuildSettingsScreen extends Screen {
        private TextFieldWidget coordinateField;

        protected AutoBuildSettingsScreen() {
            super(Text.of("自动建造设置"));
        }

        @Override
        protected void init() {
            // 修改返回按钮逻辑
            ButtonWidget backButton = ButtonWidget.builder(Text.of("← 返回"), button -> {
                saveCoordinate(); // 保存坐标
                if (this.client != null) {
                    this.client.setScreen(new AutoMessageScreen()); // 返回主菜单
                }
            }).dimensions(10, 10, 60, 20).build();
            this.addDrawableChild(backButton);

            // 初始化坐标输入框
            this.coordinateField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 50, 200, 20, Text.of("补货坐标（x y z）："));
            this.coordinateField.setMaxLength(50);
            this.coordinateField.setText(AutoMessageMod.getCoordinate()); // 实时获取最新坐标
            this.coordinateField.setChangedListener(text -> {
                String coord = text.trim().replaceAll("\\s+", " "); // 标准化空格
                AutoMessageMod.setCoordinate(coord);
                AutoMessageMod.saveConfig(); // 实时保存
            });

            // 添加输入框到界面
            this.addSelectableChild(this.coordinateField);

            // 修改自动建造按钮的保存逻辑
            ButtonWidget autoBuildButton = ButtonWidget.builder(Text.of(AutoMessageMod.isAutoBuilding() ? "自动建造已打开" : "打开自动建造"), button -> {
                saveCoordinate(); // 新增：按下按钮时保存坐标
                if (AutoMessageMod.isAutoBuilding()) {
                    AutoMessageMod.setAutoBuilding(false);
                    button.setMessage(Text.of("打开自动建造"));
                } else {
                    AutoMessageMod.setAutoBuilding(true);
                    button.setMessage(Text.of("自动建造已打开"));
                }
            }).dimensions(this.width / 2 - 100, this.height / 2 - 20, 200, 20).build();
            this.addDrawableChild(autoBuildButton);
        }

        // 新增：保存坐标的方法
        private void saveCoordinate() {
            String coord = this.coordinateField.getText().trim().replaceAll("\\s+", " ");
            AutoMessageMod.setCoordinate(coord);
            AutoMessageMod.saveConfig();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);

            // 绘制文本框的提示文本
            context.drawTextWithShadow(this.textRenderer, Text.of("补货坐标（x y z）："), this.width / 2 - 100, this.height / 2 - 60, 0xFFFFFF);

            // 渲染输入框
            this.coordinateField.render(context, mouseX, mouseY, delta);
        }

        @Override
        public void close() {
            this.coordinateField.setFocused(false);
            saveCoordinate();
            AutoMessageMod.saveConfig(); // 异步保存配置
            super.close();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                saveCoordinate(); // 新增：按下 ESC 键时保存坐标
                this.close();
                return true;
            }
            return this.coordinateField.keyPressed(keyCode, scanCode, modifiers) || 
                   super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return this.coordinateField.charTyped(chr, modifiers) || 
                   super.charTyped(chr, modifiers);
        }
    }

    private class TeleportPointsScreen extends Screen {
        private List<TeleportPoint> teleportPoints = new ArrayList<>();
        private final List<TeleportPoint> filteredTeleportPoints = new ArrayList<>();
        private int scrollOffset = 0;
        private static final int ITEMS_PER_PAGE = 5;
        private TextFieldWidget searchField;

        protected TeleportPointsScreen() {
            super(Text.of("传送点大全"));
            loadTeleportPoints();
            filteredTeleportPoints.addAll(teleportPoints);
        }

        @Override
        protected void init() {
            // 添加返回按钮
            ButtonWidget backButton = ButtonWidget.builder(Text.of("← 返回"), button -> {
                this.close();
                if (this.client != null) { // 增加null检查
                    this.client.setScreen(AutoMessageScreen.this); // 返回上一级菜单
                }
            }).dimensions(10, 10, 60, 20).build();
            this.addDrawableChild(backButton);

            // 添加搜索框
            this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 10, 200, 20, Text.of("搜索备注："));
            this.searchField.setMaxLength(256);
            this.searchField.setText(AutoMessageMod.getSearchText()); // 新增：加载搜索框的临时字符信息
            this.searchField.setChangedListener(text -> {
                AutoMessageMod.setSearchText(text); // 新增：保存搜索框的临时字符信息
                filterTeleportPoints(text);
                this.clearAndInit();
            });
            this.addSelectableChild(this.searchField);

            // 设置搜索框自动聚焦
            this.setInitialFocus(this.searchField);

            // 添加上一页按钮
            ButtonWidget prevPageButton = ButtonWidget.builder(Text.of("上一页"), button -> {
                if (scrollOffset > 0) {
                    scrollOffset -= ITEMS_PER_PAGE;
                    if (scrollOffset < 0) {
                        scrollOffset = 0;
                    }
                    this.clearAndInit();
                }
            }).dimensions(this.width / 2 - 110, this.height - 30, 100, 20).build();
            this.addDrawableChild(prevPageButton);

            // 添加下一页按钮
            ButtonWidget nextPageButton = ButtonWidget.builder(Text.of("下一页"), button -> {
                if (scrollOffset + ITEMS_PER_PAGE < filteredTeleportPoints.size()) {
                    scrollOffset += ITEMS_PER_PAGE;
                    this.clearAndInit();
                }
            }).dimensions(this.width / 2 + 10, this.height - 30, 100, 20).build();
            this.addDrawableChild(nextPageButton);

            // 添加传送点按钮
            int yOffset = 40;
            for (int i = scrollOffset; i < Math.min(scrollOffset + ITEMS_PER_PAGE, filteredTeleportPoints.size()); i++) {
                TeleportPoint point = filteredTeleportPoints.get(i);
                ButtonWidget teleportButton = ButtonWidget.builder(Text.of("传送"), button -> {
                    var player = MinecraftClient.getInstance().player;
                    if (player != null) {
                        player.networkHandler.sendChatCommand("res tp " + point.getName());
                    }
                }).dimensions(this.width - 160, yOffset, 50, 20).build();
                this.addDrawableChild(teleportButton);

                ButtonWidget editButton = ButtonWidget.builder(Text.of("编辑"), button -> {
                    if (this.client != null) {
                        this.client.setScreen(new EditTeleportPointScreen(point));
                    }
                }).dimensions(this.width - 100, yOffset, 50, 20).build();
                this.addDrawableChild(editButton);

                ButtonWidget deleteButton = ButtonWidget.builder(Text.of("删除"), button -> {
                    teleportPoints.remove(point);
                    filteredTeleportPoints.remove(point);
                    saveTeleportPoints();
                    if (this.client != null) {
                        this.client.setScreen(new TeleportPointsScreen());
                    }
                }).dimensions(this.width - 50, yOffset, 50, 20).build();
                this.addDrawableChild(deleteButton);

                yOffset += 30;
            }

            // 添加添加按钮
            ButtonWidget addButton = ButtonWidget.builder(Text.of("添加"), button -> {
                if (this.client != null) {
                    this.client.setScreen(new EditTeleportPointScreen(null));
                }
            }).dimensions(this.width - 60, 10, 50, 20).build();
            this.addDrawableChild(addButton);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);

            int currentPage = (scrollOffset / ITEMS_PER_PAGE) + 1;
            int totalPages = (int) Math.ceil((double) filteredTeleportPoints.size() / ITEMS_PER_PAGE);
            Text pageText = Text.of(currentPage + "/" + totalPages);
            context.drawTextWithShadow(this.textRenderer, pageText, this.width / 2 - 10, this.height - 25, 0xFFFFFF);

            int yOffset = 45; // 调整文本的垂直位置，使其与按钮对齐
            for (int i = scrollOffset; i < Math.min(scrollOffset + ITEMS_PER_PAGE, filteredTeleportPoints.size()); i++) {
                TeleportPoint point = filteredTeleportPoints.get(i);
                context.drawTextWithShadow(this.textRenderer, Text.of(point.getName()), 20, yOffset, 0xFFFFFF);
                context.drawTextWithShadow(this.textRenderer, Text.of(point.getNote()), 200, yOffset, 0xFFFFFF);
                yOffset += 30;
            }

            // 渲染搜索框
            this.searchField.render(context, mouseX, mouseY, delta);
        }

        private void filterTeleportPoints(String searchText) {
            filteredTeleportPoints.clear();
            if (searchText.isEmpty()) {
                filteredTeleportPoints.addAll(teleportPoints);
            } else {
                for (TeleportPoint point : teleportPoints) {
                    if (point.getNote().toLowerCase().contains(searchText.toLowerCase())) {
                        filteredTeleportPoints.add(point);
                    }
                }
            }
            scrollOffset = 0; // 重置滚动偏移量
        }

        private void loadTeleportPoints() {
            Path teleportPointsPath = Paths.get("restp", "teleport_points.json");
            if (Files.exists(teleportPointsPath)) {
                try (FileReader reader = new FileReader(teleportPointsPath.toFile())) {
                    teleportPoints = new Gson().fromJson(reader, new TypeToken<List<TeleportPoint>>() {}.getType());
                } catch (IOException e) {
                    System.out.println("加载传送点失败: " + e.getMessage());
                }
            }
        }

        private void saveTeleportPoints() {
            Path teleportPointsPath = Paths.get("restp", "teleport_points.json");
            try (FileWriter writer = new FileWriter(teleportPointsPath.toFile())) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(teleportPoints);
                // 格式化 JSON 字符串，确保每个传送点之间有换行
                json = json.replace("},{", "},\n{");
                writer.write(json);
            } catch (IOException e) {
                System.out.println("保存传送点失败: " + e.getMessage());
            }
        }

        @Override
        public void close() {
            AutoMessageMod.saveConfig(); // 异步保存配置
            super.close();
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            // 确保搜索框能够处理中文字符
            if (this.searchField.isFocused()) {
                this.searchField.charTyped(chr, modifiers);
                return true;
            }
            return super.charTyped(chr, modifiers);
        }

        private class EditTeleportPointScreen extends Screen {
            private TextFieldWidget nameField;
            private TextFieldWidget noteField;
            private final TeleportPoint point;

            protected EditTeleportPointScreen(TeleportPoint point) {
                super(Text.of("编辑传送点"));
                this.point = point != null ? point : new TeleportPoint("", "");
            }

            @Override
            protected void init() {
                // 添加返回按钮
                ButtonWidget backButton = ButtonWidget.builder(Text.of("← 返回"), button -> {
                    if (this.client != null) {
                        this.client.setScreen(TeleportPointsScreen.this);
                    }
                }).dimensions(10, 10, 60, 20).build();
                this.addDrawableChild(backButton);

                // 初始化名称输入框
                this.nameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 50, 200, 20, Text.of("传送点名称："));
                this.nameField.setMaxLength(256);
                this.nameField.setText(point.getName());

                // 初始化备注输入框
                this.noteField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Text.of("备注："));
                this.noteField.setMaxLength(256);
                this.noteField.setText(point.getNote());

                // 添加输入框到界面
                this.addSelectableChild(this.nameField);
                this.addSelectableChild(this.noteField);

                // 添加保存按钮
                ButtonWidget saveButton = ButtonWidget.builder(Text.of("保存"), button -> {
                    point.setName(this.nameField.getText());
                    point.setNote(this.noteField.getText());
                    if (!teleportPoints.contains(point)) {
                        teleportPoints.add(point);
                    }
                    saveTeleportPoints();
                    if (this.client != null) {
                        this.client.setScreen(TeleportPointsScreen.this);
                    }
                }).dimensions(this.width / 2 - 100, this.height / 2 + 10, 200, 20).build();
                this.addDrawableChild(saveButton);
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                this.renderBackground(context, mouseX, mouseY, delta);
                super.render(context, mouseX, mouseY, delta);

                // 绘制文本框的提示文本
                context.drawTextWithShadow(this.textRenderer, Text.of("传送点名称："), this.width / 2 - 100, this.height / 2 - 60, 0xFFFFFF);
                context.drawTextWithShadow(this.textRenderer, Text.of("备注："), this.width / 2 - 100, this.height / 2 - 30, 0xFFFFFF);

                // 渲染输入框
                this.nameField.render(context, mouseX, mouseY, delta);
                this.noteField.render(context, mouseX, mouseY, delta);
            }
        }
    }

    private static class TeleportPoint {
        private String name;
        private String note;

        public TeleportPoint(String name, String note) {
            this.name = name;
            this.note = note;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}