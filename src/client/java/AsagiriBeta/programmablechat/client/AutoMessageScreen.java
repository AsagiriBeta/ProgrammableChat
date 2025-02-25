package AsagiriBeta.programmablechat.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class AutoMessageScreen extends Screen {
    private TextFieldWidget messageField;
    private TextFieldWidget intervalField;
    private ButtonWidget startStopButton;
    private String savedMessage = "";
    private String savedInterval = "";

    protected AutoMessageScreen() {
        super(Text.of("Auto Message Settings"));
    }

    @Override
    protected void init() {
        // 初始化消息输入框
        this.messageField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 50, 200, 20, Text.of("Message"));
        this.messageField.setMaxLength(256); // 设置最大输入长度
        this.messageField.setFocused(true); // 默认聚焦在消息输入框
        this.messageField.setText(savedMessage); // 恢复之前保存的消息

        // 初始化间隔输入框
        this.intervalField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Text.of("Interval (seconds)"));
        this.intervalField.setMaxLength(10); // 设置最大输入长度
        this.intervalField.setText(savedInterval); // 恢复之前保存的间隔

        // 添加输入框到界面
        this.addSelectableChild(this.messageField);
        this.addSelectableChild(this.intervalField);

        // 添加保存按钮
        this.addDrawableChild(ButtonWidget.builder(Text.of("Save"), button -> {
            String intervalText = this.intervalField.getText();
            int interval = intervalText.isEmpty() ? 0 : Integer.parseInt(intervalText);
            if (interval <= 0) {
                // 可以在这里添加提示用户输入有效间隔的逻辑
                interval = 10; // 默认值
            }
            AutoMessageMod.setMessage(this.messageField.getText());
            AutoMessageMod.setInterval(interval);
            savedMessage = this.messageField.getText(); // 保存当前消息
            savedInterval = this.intervalField.getText(); // 保存当前间隔
            this.close();
        }).dimensions(this.width / 2 - 100, this.height / 2 + 10, 200, 20).build());

        // 添加Start/Stop按钮
        this.startStopButton = ButtonWidget.builder(Text.of(AutoMessageMod.isSending() ? "Stop" : "Start"), button -> {
            if (AutoMessageMod.isSending()) {
                AutoMessageMod.stopSending();
                button.setMessage(Text.of("Start"));
            } else {
                AutoMessageMod.startSending();
                button.setMessage(Text.of("Stop"));
            }
        }).dimensions(this.width / 2 - 100, this.height / 2 + 40, 200, 20).build();
        this.addDrawableChild(this.startStopButton);

        // 添加鼠标点击事件处理，确保点击文本框时能够正确切换焦点
        this.setInitialFocus(this.messageField);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.messageField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.messageField);
            return true;
        }
        if (this.intervalField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.intervalField);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // 绘制文本框的提示文本
        context.drawTextWithShadow(this.textRenderer, Text.of("Message:"), this.width / 2 - 100, this.height / 2 - 60, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.of("Interval (seconds):"), this.width / 2 - 100, this.height / 2 - 30, 0xFFFFFF);

        // 渲染输入框
        this.messageField.render(context, mouseX, mouseY, delta);
        this.intervalField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 处理键盘事件，确保文本框能够接收输入
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return this.messageField.keyPressed(keyCode, scanCode, modifiers) || 
               this.intervalField.keyPressed(keyCode, scanCode, modifiers) || 
               super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // 处理字符输入事件，确保文本框能够显示输入的字符
        return this.messageField.charTyped(chr, modifiers) || 
               this.intervalField.charTyped(chr, modifiers) || 
               super.charTyped(chr, modifiers);
    }
}