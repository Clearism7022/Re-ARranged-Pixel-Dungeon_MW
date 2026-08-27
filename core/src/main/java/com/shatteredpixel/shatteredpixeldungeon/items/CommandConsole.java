package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndDevConsole;

import java.util.ArrayList;

public class CommandConsole extends Item {

    private static final String AC_OPEN = "OPEN";

    {
        // 임시 아이콘.
        // 나중에 별도 스프라이트를 추가하지 않는다면 그대로 사용해도 됩니다.
        image = 0;

        unique = true;
        stackable = false;
        defaultAction = AC_OPEN;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {

        ArrayList<String> actions = super.actions(hero);

        if (!actions.contains(AC_OPEN)) {
            actions.add(AC_OPEN);
        }

        return actions;
    }

    @Override
    public void execute(Hero hero, String action) {

        super.execute(hero, action);

        if (AC_OPEN.equals(action)) {
            GameScene.show(new WndDevConsole());
        }
    }

    @Override
    public String actionName(String action, Hero hero) {

        if (AC_OPEN.equals(action)) {
            return "OPEN";
        }

        return super.actionName(action, hero);
    }

    @Override
    public String name() {
        return "Deus Ex Machina";
    }

    @Override
    public String desc() {
        return "An omnipotent console. Use /give <ItemClass> [amount] [level] to create items.";
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public int value() {
        return 0;
    }
}
