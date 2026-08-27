package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Reflection;

public class WndDevConsole extends WndTextInput {

    public WndDevConsole() {
        super(
                "Developer Console",
                "/give <ItemClass> [amount] [level]",
                "/give ",
                120,
                false,
                "EXECUTE",
                "CANCEL"
        );
    }

    @Override
    public void onSelect(boolean positive, String text) {

        // CANCEL을 눌렀다면 아무것도 하지 않음
        if (!positive) {
            return;
        }

        executeCommand(text);
    }

    private void executeCommand(String command) {

        if (command == null) {
            return;
        }

        command = command.trim();

        if (command.isEmpty()) {
            return;
        }

        String[] args = command.split("\\s+");

        // 현재는 /give만 지원
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

        // 기본 수량
        int amount = 1;

        if (args.length >= 3) {

            try {

                amount = Integer.parseInt(args[2]);

            } catch (NumberFormatException e) {

                GLog.w("Invalid amount: " + args[2]);

                return;
            }
        }

        // 기본 강화수치
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

        Class<? extends Item> itemClass =
                findItemClass(itemName);

        if (itemClass == null) {

            GLog.w("Item not found: " + itemName);

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

                result += " (+" + level + ")";

            } else if (level < 0) {

                result += " (" + level + ")";
            }

            GLog.p(result);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Item> findItemClass(String name) {

        /*
         * Re-ARranged의 Generator에 등록된 아이템 검색.
         *
         * 무기, 총기, 방어구, 반지, 스크롤,
         * 포션 등 대부분의 일반 아이템은 여기서 검색됩니다.
         */
        for (Generator.Category category
                : Generator.Category.values()) {

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

        /*
         * 완전한 클래스 경로를 직접 입력한 경우.
         *
         * 예:
         * /give com.shatteredpixel.shatteredpixeldungeon.items.Ankh
         */
        try {

            Class<?> cls = Class.forName(name);

            if (Item.class.isAssignableFrom(cls)) {
                return (Class<? extends Item>) cls;
            }

        } catch (ClassNotFoundException ignored) {
        }

        /*
         * items 루트에 직접 위치하는 아이템.
         *
         * 예:
         * /give Ankh
         * /give Waterskin
         */
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

        Item first =
                Reflection.newInstance(itemClass);

        if (first == null) {

            GLog.w(
                    "Cannot instantiate: "
                            + itemClass.getSimpleName()
            );

            return 0;
        }

        // 강화수치 지정
        first.level(level);

        // 즉시 식별
        first.identify();

        /*
         * 포션, 스크롤, 탄약 등 스택 가능한 아이템
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
         * 무기, 방어구, 반지 등 비스택 아이템
         */
        int count = 0;

        for (int i = 0; i < amount; i++) {

            Item item;

            if (i == 0) {

                item = first;

            } else {

                item =
                        Reflection.newInstance(itemClass);
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
