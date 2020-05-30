/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.gui;

import pixelitor.OpenImages;
import pixelitor.utils.Keys;
import pixelitor.utils.Lazy;
import pixelitor.utils.test.RandomGUITest;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * A user interface ({@link ImageAreaUI} implementation)
 * where the edited images are in tabs
 */
public class TabsUI extends JTabbedPane implements ImageAreaUI {
    private final Lazy<JMenu> tabPlacementMenu = Lazy.of(this::createTabPlacementMenu);
    private boolean userInitiated = true;

    public TabsUI(int tabPlacement) {
        setTabPlacement(tabPlacement);
        addChangeListener(e -> tabsChanged());

        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(Keys.CTRL_TAB, "navigateNext");
        inputMap.put(Keys.CTRL_SHIFT_TAB, "navigatePrevious");
    }

    private void tabsChanged() {
        if (!userInitiated) {
            return;
        }
        int selectedIndex = getSelectedIndex();
        if (selectedIndex != -1) { // it is -1 if all tabs have been closed
            TabViewContainer tab = (TabViewContainer) getComponentAt(selectedIndex);
            tab.activated();
        }
    }

    @Override
    public void activateView(View view) {
        TabViewContainer tab = (TabViewContainer) view.getViewContainer();
        setSelectedIndex(indexOfComponent(tab));
    }

    @Override
    public void addNewView(View view) {
        TabViewContainer tab = new TabViewContainer(view, this);
        view.setViewContainer(tab);

        int myIndex = getTabCount();

        try {
            userInitiated = false;
            addTab(view.getName(), tab);
        } finally {
            userInitiated = true;
        }

        setTabComponentAt(myIndex, new TabTitleRenderer(view.getName(), tab));
        setSelectedIndex(myIndex);
        tab.activated();
    }

    public static void warnAndCloseTab(TabViewContainer tab) {
        if (!RandomGUITest.isRunning()) {
            // this will call closeTab
            OpenImages.warnAndClose(tab.getView());
        }
    }

    public void closeTab(TabViewContainer tab) {
        remove(indexOfComponent(tab));
        View view = tab.getView();
        OpenImages.imageClosed(view);
    }

    public void selectTab(TabViewContainer tab) {
        setSelectedIndex(indexOfComponent(tab));
    }

    private JMenu createTabPlacementMenu() {
        JMenu menu = new JMenu("Tab Placement");

        JRadioButtonMenuItem topMI = createTabPlacementMenuItem("Top", TOP);
        JRadioButtonMenuItem bottomMI = createTabPlacementMenuItem("Bottom", BOTTOM);
        JRadioButtonMenuItem leftMI = createTabPlacementMenuItem("Left", LEFT);
        JRadioButtonMenuItem rightMI = createTabPlacementMenuItem("Right", RIGHT);

        ButtonGroup group = new ButtonGroup();
        group.add(topMI);
        group.add(bottomMI);
        group.add(leftMI);
        group.add(rightMI);

        assert tabPlacement == ImageArea.getTabPlacement();
        if (tabPlacement == TOP) {
            topMI.setSelected(true);
        } else if (tabPlacement == BOTTOM) {
            bottomMI.setSelected(true);
        } else if (tabPlacement == LEFT) {
            leftMI.setSelected(true);
        } else if (tabPlacement == RIGHT) {
            rightMI.setSelected(true);
        }

        menu.add(topMI);
        menu.add(bottomMI);
        menu.add(leftMI);
        menu.add(rightMI);
        return menu;
    }

    private JRadioButtonMenuItem createTabPlacementMenuItem(String name, int pos) {
        return new JRadioButtonMenuItem(new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setTabPlacement(pos);
                ImageArea.setTabPlacement(pos);
            }
        });
    }

    public JMenu getTabPlacementMenu() {
        return tabPlacementMenu.get();
    }

}
