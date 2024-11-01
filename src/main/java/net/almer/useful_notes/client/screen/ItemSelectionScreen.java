package net.almer.useful_notes.client.screen;

import net.almer.useful_notes.client.widget.DraggablePanel;
import net.almer.useful_notes.yaml.Note;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.almer.useful_notes.client.screen.NoteSettingScreen.NOTES_FILE_PATH;

public class ItemSelectionScreen extends Screen {
    private final List<Item> items;
    private final List<ItemWidget> itemWidgets = new ArrayList<>();
    private final int itemSize = 16;
    private final int padding = 5;
    private TextFieldWidget searchField;

    public ItemSelectionScreen() {
        super(Text.of("Select an Item"));
        this.items = new ArrayList<>();
        Registries.ITEM.forEach(items::add);
    }

    @Override
    protected void init() {
        super.init();
        searchField = new TextFieldWidget(this.textRenderer, 10, 10, this.width - 20, 20, Text.of("Search"));
        searchField.setChangedListener(this::updateItemWidgets);
        this.addDrawableChild(searchField);

        updateItemWidgets("");
    }

    private void updateItemWidgets(String search) {
        for(ItemWidget widget : itemWidgets){
            remove(widget);
        }
        itemWidgets.clear();

        List<Item> filteredItems = items.stream()
                .filter(item -> item.getName().getString().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());

        int x = 10;
        int y = 40;

        for (Item item : filteredItems) {
            ItemWidget itemWidget = new ItemWidget(item, x, y, itemSize, itemSize) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    Yaml yaml = new Yaml();

                    try (InputStream inputStream = new FileInputStream(NOTES_FILE_PATH)) {
                        List<Note> loadedNotes = yaml.loadAs(inputStream, List.class);
                        if (loadedNotes == null) {
                            return;
                        }

                        for (Note note : loadedNotes) {
                            if (note.isSelected()) {
                                note.setItem(Registries.ITEM.getId(item).getPath());
                                break;
                            }
                        }
                        try (PrintWriter writer = new PrintWriter(new FileWriter(NOTES_FILE_PATH))) {
                            yaml.dump(loadedNotes, writer);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    MinecraftClient.getInstance().setScreen(new NoteSettingScreen(Text.empty()));
                }
            };
            itemWidget.setTooltip(Tooltip.of(item.getName()));
            itemWidgets.add(itemWidget);
            this.addDrawableChild(itemWidget);

            x += itemSize + padding;
            if (x + itemSize > this.width - 10) {
                x = 10;
                y += itemSize + padding;
            }
        }
    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawText(this.textRenderer, "Select an Item", this.width / 2 - 50, 30, Colors.WHITE, true);
        searchField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (ItemWidget widget : itemWidgets) {
            int y = widget.getY();
            y -= ((int) verticalAmount * -5);
            widget.setY(y);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        searchField.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    static class ItemWidget extends ClickableWidget {
        private final Item item;

        public ItemWidget(Item item, int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty());
            this.item = item;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            context.drawItemWithoutEntity(new ItemStack(item), this.getX(), this.getY());
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {

        }
    }
}
