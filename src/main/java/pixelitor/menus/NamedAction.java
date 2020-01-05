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

package pixelitor.menus;

import pixelitor.utils.VisibleForTesting;

import javax.swing.*;

/**
 * An action that can be simply renamed.
 * Used for menus that need to change their name according
 * to some internal state.
 */
public abstract class NamedAction extends AbstractAction {
    protected NamedAction() {
    }

    protected NamedAction(String name) {
        super(name);
    }

    protected NamedAction(String name, Icon icon) {
        super(name, icon);
    }

    public void setName(String newName) {
        putValue(Action.NAME, newName);
    }

    @VisibleForTesting
    public String getName() {
        return (String) getValue(Action.NAME);
    }
}
