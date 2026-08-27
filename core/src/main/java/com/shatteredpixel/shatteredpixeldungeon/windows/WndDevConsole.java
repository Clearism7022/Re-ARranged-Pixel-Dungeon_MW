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
                PixelScene.renderTextBlock(
                        "Developer Console",
                        9
                );

        title.hardlight(TITLE_COLOR);
        title.maxWidth(width);
        title.setPos(
                (width - title.width()) / 2f,
                y
        );

        add(title);

        y = title.bottom() + GAP * 2;

        RenderedTextBlock help =
                PixelScene.renderTextBlock(
                        "/give <ItemClass> [amount] [level]",
                        6
                );

        help.maxWidth(width - GAP * 2);
        help.setPos(GAP, y);

        add(help);

        y = help.bottom() + GAP;

        /*
         * TextInput의 background는 null이면 안 됩니다.
         * 이전 크래시 원인이 바로 이 부분이었습니다.
         */
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

        input.setRect(
                GAP,
                y,
                width - GAP * 2,
                INPUT_HEIGHT
        );

        add(input);

        y = input.bottom() + GAP;

        RedButton execute =
                new RedButton(
                        "EXECUTE",
                        8
                ) {

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

        resize(
                width,
                (int) y
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

        /*
         * 현재 /give만 지원
         */
        if (!args[0].equalsIgnoreCase("/give")) {

            GLog.w(
                    "Unknown command: "
                            + args[0]
            );

            GLog.i(
                    "Usage: /give <ItemClass> [amount] [level]"
            );

            return;
        }

        if (args.length < 2) {

            GLog.w(
                    "Usage: /give <ItemClass> [amount] [level]"
            );

            return;
        }

        String itemName = args[1];

        /*
         * 기본 수량
         */
        int amount = 1;

        if (args.length >= 3) {

            try {

                amount =
                        Integer.parseInt(args[2]);

            } catch (NumberFormatException e) {

                GLog.w(
                        "Invalid amount: "
                                + args[2]
                );

                return;
            }
        }

        /*
         * 기본 강화수치
         */
        int level = 0;

        if (args.length >= 4) {

            try {

                level =
                        Integer.parseInt(args[3]);

            } catch (NumberFormatException e) {

                GLog.w(
                        "Invalid level: "
                                + args[3]
                );

                return;
            }
        }

        if (amount < 1) {

            GLog.w(
                    "Amount must be at least 1."
            );

            return;
        }

        if (amount > 999) {

            GLog.w(
                    "Maximum amount is 999."
            );

            return;
        }

        if (level < -100
                || level > 1000) {

            GLog.w(
                    "Level must be between -100 and 1000."
            );

            return;
        }

        Class<? extends Item> itemClass =
                findItemClass(itemName);

        if (itemClass == null) {

            GLog.w(
                    "Item not found: "
                            + itemName
            );

            return;
        }

        int given =
                giveItem(
                        itemClass,
                        amount,
                        level
                );

        if (given > 0) {

            String result =
                    "Given: "
                            + itemClass.getSimpleName()
                            + " x"
                            + given;

            if (level > 0) {

                result +=
                        " (+"
                                + level
                                + ")";

            } else if (level < 0) {

                result +=
                        " ("
                                + level
                                + ")";
            }

            GLog.p(result);
        }

        hide();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Item> findItemClass(
            String name) {

        /*
         * Generator에 등록되어 있는 아이템을 먼저 검색합니다.
         *
         * 무기, 총기, 방어구, 반지, 스크롤,
         * 포션 등 대부분의 일반 아이템이 여기에 들어갑니다.
         */
        for (Generator.Category category
                : Generator.Category.values()) {

            if (category.classes == null) {
                continue;
            }

            for (Class<?> cls
                    : category.classes) {

                if (cls == null) {
                    continue;
                }

                if (cls
                        .getSimpleName()
                        .equalsIgnoreCase(name)

                        && Item.class
                        .isAssignableFrom(cls)) {

                    return
                            (Class<? extends Item>) cls;
                }
            }
        }

        /*
         * 완전한 Java 클래스 경로를 입력했을 경우
         *
         * 예:
         *
         * /give
         * com.shatteredpixel.shatteredpixeldungeon.items.Ankh
         */
        try {

            Class<?> cls =
                    Class.forName(name);

            if (Item.class
                    .isAssignableFrom(cls)) {

                return
                        (Class<? extends Item>) cls;
            }

        } catch (ClassNotFoundException ignored) {
        }

        /*
         * items 루트 패키지 검색
         *
         * 예:
         *
         * /give Ankh
         * /give Waterskin
         */
        try {

            Class<?> cls =
                    Class.forName(
                            "com.shatteredpixel.shatteredpixeldungeon.items."
                                    + name
                    );

            if (Item.class
                    .isAssignableFrom(cls)) {

                return
                        (Class<? extends Item>) cls;
            }

        } catch (ClassNotFoundException ignored) {
        }

        return null;
    }

    private int giveItem(
            Class<? extends Item> itemClass,
            int amount,
            int level) {

        Item first =
                Reflection.newInstance(
                        itemClass
                );

        if (first == null) {

            GLog.w(
                    "Cannot instantiate: "
                            + itemClass.getSimpleName()
            );

            return 0;
        }

        /*
         * 강화수치 지정
         */
        first.level(level);

        /*
         * 바로 식별된 상태로 지급
         */
        first.identify();

        /*
         * 포션 / 스크롤 / 탄약 등
         * stackable 아이템이면 객체 하나에
         * quantity만 설정합니다.
         */
        if (first.stackable) {

            first.quantity(amount);

            if (!first.collect()) {

                Dungeon.level
                        .drop(
                                first,
                                Dungeon.hero.pos
                        )
                        .sprite
                        .drop();
            }

            return amount;
        }

        /*
         * 무기 / 방어구 / 반지 등
         * stack 불가능한 아이템은
         * 각각 별도의 객체로 생성합니다.
         */
        int count = 0;

        for (int i = 0;
             i < amount;
             i++) {

            Item item;

            if (i == 0) {

                item = first;

            } else {

                item =
                        Reflection.newInstance(
                                itemClass
                        );
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
                        .drop(
                                item,
                                Dungeon.hero.pos
                        )
                        .sprite
                        .drop();
            }

            count++;
        }

        return count;
    }
}
