package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextInput;
import com.watabou.utils.Reflection;

public class WndDevConsole extends Window {

    private static final int WIDTH_P = 130;
    private static final int WIDTH_L = 180;

    private static final int INPUT_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 18;
    private static final int GAP = 4;

    private TextInput input;

    public WndDevConsole() {

        super();

        int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

        float y = GAP;

        RenderedTextBlock title =
                PixelScene.renderTextBlock("Developer Console", 9);

        title.hardlight(TITLE_COLOR);
        title.maxWidth(width);
        title.setPos((width - title.width()) / 2f, y);
        add(title);

        y = title.bottom() + GAP * 2;

        RenderedTextBlock help =
                PixelScene.renderTextBlock(
                        "/give <ItemClass> [amount] [level]", 6);

        help.maxWidth(width - GAP * 2);
        help.setPos(GAP, y);
        add(help);

        y = help.bottom() + GAP;

        input = new TextInput(
                Chrome.get(Chrome.Type.GREY_BUTTON),
                false,
                8
        ) {
            @Override
            public void enterPressed() {
                executeCommand();
            }
        };

        input.setMaxLength(120);
        input.setText("/give ");

        /*
         * 중요:
         * 먼저 Window에 붙여서 parent/camera를 확보한 다음
         * setRect()를 호출해야 TextInput 내부의 libGDX TextField가
         * 올바른 화면 좌표를 계산합니다.
         */
        add(input);

        input.setRect(
                GAP,
                y,
                width - GAP * 2,
                INPUT_HEIGHT
        );

        y = input.bottom() + GAP;

        RedButton execute =
                new RedButton("EXECUTE", 8) {
                    @Override
                    protected void onClick() {
                        super.onClick();
                        executeCommand();
                    }
                };

        execute.setRect(
                GAP,
                y,
                width - GAP * 2,
                BUTTON_HEIGHT
        );

        add(execute);

        y = execute.bottom() + GAP;

        resize(width, (int) y);

        /*
         * resize 이후 한 번 더 layout을 실행합니다.
         * Window가 최종 크기를 얻은 뒤 TextInput의 위치를
         * 다시 계산하도록 하기 위함입니다.
         */
        input.setRect(
                GAP,
                help.bottom() + GAP,
                width - GAP * 2,
                INPUT_HEIGHT
        );
    }

    private void executeCommand() {

        String command = input.getText();

        if (command == null) {
            return;
        }

        command = command.trim();

        if (command.isEmpty()) {
            return;
        }

        String[] args = command.split("\\s+");

        if (!args[0].equalsIgnoreCase("/give")) {
            GLog.w("Unknown command: " + args[0]);
            GLog.i("Usage: /give <ItemClass> [amount] [level]");
            return;
        }

        if (args.length < 2) {
            GLog.w("Usage: /give <ItemClass> [amount] [level]");
            return;
        }

        String itemName = args[1];

        int amount = 1;

        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                GLog.w("Invalid amount: " + args[2]);
                return;
            }
        }

        int level = 0;

        if (args.length >= 4) {
            try {
                level = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                GLog.w("Invalid level: " + args[3]);
                return;
            }
        }

        if (amount < 1) {
            GLog.w("Amount must be at least 1.");
            return;
        }

        if (amount > 999) {
            GLog.w("Maximum amount is 999.");
            return;
        }

        if (level < -100 || level > 1000) {
            GLog.w("Level must be between -100 and 1000.");
            return;
        }

        Class<? extends Item> itemClass = findItemClass(itemName);

        if (itemClass == null) {
            GLog.w("Item not found: " + itemName);
            return;
        }

        int given = giveItem(itemClass, amount, level);

        if (given > 0) {

            String result =
                    "Given: "
                            + itemClass.getSimpleName()
                            + " x"
                            + given;

            if (level > 0) {
                result += " (+" + level + ")";
            } else if (level < 0) {
                result += " (" + level + ")";
            }

            GLog.p(result);
        }

        hide();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Item> findItemClass(String name) {

        for (Generator.Category category : Generator.Category.values()) {

            if (category.classes == null) {
                continue;
            }

            for (Class<?> cls : category.classes) {

                if (cls == null) {
                    continue;
                }

                if (cls.getSimpleName().equalsIgnoreCase(name)
                        && Item.class.isAssignableFrom(cls)) {

                    return (Class<? extends Item>) cls;
                }
            }
        }

        try {

            Class<?> cls = Class.forName(name);

            if (Item.class.isAssignableFrom(cls)) {
                return (Class<? extends Item>) cls;
            }

        } catch (ClassNotFoundException ignored) {
        }

        try {

            Class<?> cls = Class.forName(
                    "com.shatteredpixel.shatteredpixeldungeon.items."
                            + name
            );

            if (Item.class.isAssignableFrom(cls)) {
                return (Class<? extends Item>) cls;
            }

        } catch (ClassNotFoundException ignored) {
        }

        return null;
    }

    private int giveItem(
            Class<? extends Item> itemClass,
            int amount,
            int level) {

        Item first = Reflection.newInstance(itemClass);

        if (first == null) {
            GLog.w("Cannot instantiate: " + itemClass.getSimpleName());
            return 0;
        }

        first.level(level);
        first.identify();

        if (first.stackable) {

            first.quantity(amount);

            if (!first.collect()) {
                Dungeon.level
                        .drop(first, Dungeon.hero.pos)
                        .sprite.drop();
            }

            return amount;
        }

        int count = 0;

        for (int i = 0; i < amount; i++) {

            Item item;

            if (i == 0) {
                item = first;
            } else {
                item = Reflection.newInstance(itemClass);
            }

            if (item == null) {
                continue;
            }

            if (i != 0) {
                item.level(level);
                item.identify();
            }

            if (!item.collect()) {
                Dungeon.level
                        .drop(item, Dungeon.hero.pos)
                        .sprite.drop();
            }

            count++;
        }

        return count;
    }
}
