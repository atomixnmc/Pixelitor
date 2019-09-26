/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.gui.utils;

import com.bric.util.JVM;
import pixelitor.Composition;
import pixelitor.filters.gui.FilterParam;
import pixelitor.gui.BlendingModePanel;
import pixelitor.gui.PixelitorWindow;
import pixelitor.io.OpenSave;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import static pixelitor.utils.Cursors.BUSY;
import static pixelitor.utils.Cursors.DEFAULT;
import static pixelitor.utils.Keys.ESC;

/**
 * Static GUI-related utility methods
 */
public final class GUIUtils {
    private static final int BUSY_CURSOR_DELAY = 300; // in milliseconds

    private GUIUtils() {
    }

    public static void centerOnScreen(Component component) {
        Dimension screen = getMaxWindowSize();
        Dimension window = component.getSize();

        if (window.height > screen.height) {
            window.height = screen.height;
        }

        if (window.width > screen.width) {
            window.width = screen.width;
        }

        // center it
        component.setLocation((screen.width - window.width) / 2,
                (screen.height - window.height) / 2);

        // if it was bigger than the screen, restrict it to screen size
        component.setSize(window);
    }

    public static Dimension getMaxWindowSize() {
        Rectangle bounds;
        try {
            // use this because it takes into account areas
            // like the taskbar, but it can throw
            // a "Window must not be zero" if there are 3 monitors
            // on Linux with some newer Java versions, see
            // https://github.com/lbalazscs/Pixelitor/issues/15
            bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getMaximumWindowBounds();
        } catch (Exception e) {
            return Toolkit.getDefaultToolkit().getScreenSize();
        }

        return new Dimension(bounds.width, bounds.height);
    }

    /**
     * @return true if any app window has focus
     */
    public static boolean appHasFocus() {
        return Utils.anyMatch(Window.getWindows(), Window::isActive);
    }

    public static void showCopyTextToClipboardDialog(JComponent content,
                                                     String text,
                                                     String title) {
        new DialogBuilder()
                .okText("Copy as Text to the Clipboard")
                .cancelText("Close")
                .validator(d -> {
                    Utils.copyStringToClipboard(text);
                    return false; // prevents the dialog from closing
                })
                .title(title)
                .content(content)
                .owner(PixelitorWindow.getInstance())
                .show();
    }

    public static JPanel arrangeParamsInVerticalGridBag(Iterable<FilterParam> params) {
        JPanel p = new JPanel();
        arrangeParamsInVerticalGridBag(p, params);
        return p;
    }

    public static void arrangeParamsInVerticalGridBag(JPanel p, Iterable<FilterParam> params) {
        p.setLayout(new GridBagLayout());

        int row = 0;

        GridBagHelper gbh = new GridBagHelper(p);
        for (FilterParam param : params) {
            JComponent control = param.createGUI();

            int numColumns = param.getNumGridBagCols();
            if (numColumns == 1) {
                gbh.addOnlyControlToRow(control, row);
            } else if (numColumns == 2) {
                gbh.addLabel(param.getName() + ':', 0, row);
                gbh.addLastControl(control);
            }

            row++;
        }
    }

    public static Container getTopContainer(Container c) {
        while (c.getParent() != null) {
            c = c.getParent();
            if (c instanceof Dialog) {
                // don't jump from dialogs to their parents
                return c;
            }
        }
        return c;
    }

    public static JDialog getDialogAncestor(Component c) {
        return (JDialog) SwingUtilities.getWindowAncestor(c);
    }

    public static void showDialog(JDialog d) {
        centerOnScreen(d);
        d.setVisible(true);
    }

    public static void closeDialog(JDialog d, boolean dispose) {
        if (d != null && d.isVisible()) {
            d.setVisible(false);
            if (dispose) {
                d.dispose();
            }
        }
    }

    public static void randomizeGUIWidgetsOn(JPanel panel) {
        int count = panel.getComponentCount();
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            Component child = panel.getComponent(i);
            //noinspection ChainOfInstanceofChecks
            if (child instanceof JComboBox) {
                @SuppressWarnings("rawtypes")
                JComboBox box = (JComboBox) child;

                int itemCount = box.getItemCount();
                box.setSelectedIndex(rand.nextInt(itemCount));
            } else if (child instanceof JCheckBox) {
                JCheckBox box = (JCheckBox) child;
                box.setSelected(rand.nextBoolean());
            } else if (child instanceof SliderSpinner) {
                SliderSpinner spinner = (SliderSpinner) child;
                spinner.getModel().randomize();
            } else if (child instanceof BlendingModePanel) {
                BlendingModePanel bmp = (BlendingModePanel) child;
                bmp.randomize();
            }
        }
    }

    public static void invokeAndWait(Runnable task) {
        assert !EventQueue.isDispatchThread() : "EDT thread";
        try {
            EventQueue.invokeAndWait(task);
        } catch (InterruptedException | InvocationTargetException e) {
            Messages.showExceptionOnEDT(e);
        }
    }

    public static ProgressMonitor createPercentageProgressMonitor(String msg) {
        return new ProgressMonitor(PixelitorWindow.getInstance(),
                msg, "", 0, 100);
    }

    public static ProgressMonitor createPercentageProgressMonitor(String msg,
                                                                  String cancelButtonText) {
        String oldText = UIManager.getString("OptionPane.cancelButtonText");
        UIManager.put("OptionPane.cancelButtonText", cancelButtonText);
        ProgressMonitor pm = new ProgressMonitor(PixelitorWindow.getInstance(),
                msg, "", 0, 100);
        UIManager.put("OptionPane.cancelButtonText", oldText);
        return pm;
    }

    public static void addOKCancelButtons(JPanel panel,
                                          JButton okButton, JButton cancelButton) {
        if (JVM.isMac) {
            panel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            panel.add(cancelButton);
            panel.add(okButton);
        } else {
            panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            panel.add(okButton);
            panel.add(cancelButton);
        }
    }

    public static void setupCancelWhenTheDialogIsClosed(JDialog d, Runnable cancelAction) {
        d.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // the user pressed the X button...
                cancelAction.run();
            }
        });
    }

    public static void setupCancelWhenEscIsPressed(JDialog d, Runnable cancelAction) {
        JComponent contentPane = (JComponent) d.getContentPane();
        contentPane.registerKeyboardAction(e -> cancelAction.run(),
                ESC, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public static void runWithBusyCursor(Runnable task) {
        runWithBusyCursor(PixelitorWindow.getInstance(), task);
    }

    public static void runWithBusyCursor(Component parent, Runnable task) {
        java.util.Timer timer = new Timer();
        TimerTask startBusyCursorTask = new TimerTask() {
            @Override
            public void run() {
                parent.setCursor(BUSY);
            }
        };

        try {
            // if after BUSY_CURSOR_DELAY the original task is still running,
            // set the cursor to the delay cursor
            timer.schedule(startBusyCursorTask, BUSY_CURSOR_DELAY);
            task.run(); // on the current thread!
        } finally {
            // when the original task has stopped running, the cursor is reset
            timer.cancel();
            parent.setCursor(DEFAULT);
        }
    }

    public static void setupSharedScrollModels(JScrollPane from, JScrollPane to) {
        to.getVerticalScrollBar().setModel(
                from.getVerticalScrollBar().getModel());
        to.getHorizontalScrollBar().setModel(
                from.getHorizontalScrollBar().getModel());
    }

    public static void addColorDialogListener(JComponent colorSwatch, Runnable showDialogAction) {
        colorSwatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                // on Linux/Mac the popup trigger check is not enough
                // probably because the popups are started by mousePressed
                boolean showDialog = !e.isPopupTrigger()
                        && SwingUtilities.isLeftMouseButton(e);
                if (showDialog) {
                    showDialogAction.run();
                }
            }
        });
    }

    public static Action createShowInFileManagerAction(File file) {
        String menuName;
        if (JVM.isWindows) {
            menuName = "Show in Explorer...";
        } else if (JVM.isMac) {
            menuName = "Show in Finder...";
        } else {
            menuName = "Show Folder...";
        }
        return new AbstractAction(menuName) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // TODO in Java 9 it can be done in a platform-independent way
                    // see https://stackoverflow.com/questions/7357969/how-to-use-java-code-to-open-windows-file-explorer-and-highlight-the-specified-f
                    if (JVM.isWindows) {
                        Runtime.getRuntime().exec("explorer.exe /select," + file.getCanonicalPath());
                    } else if (JVM.isMac) {
                        Runtime.getRuntime().exec("open -R " + file.getCanonicalPath());
                    } else {
                        // for now (java 8 compatibility) this only
                        // opens the folder, but without selecting the file
                        File dir = file.getParentFile();
                        Desktop.getDesktop().open(dir);
                    }
                } catch (IOException ex) {
                    Messages.showException(ex);
                }
            }
        };
    }

    public static AbstractAction createPrintFileAction(Composition comp, File file) {
        return new AbstractAction("Print...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (comp.isDirty()) {
                    String msg = "<html>The file <i>" + file.getName() +
                            "</i> contains unsaved changes.<br>" +
                            "Only the saved changes can be printed.<br>" +
                            "Do you want to save your changes now?";
                    boolean saveAndPrint = Dialogs.showOKCancelDialog(msg, "Unsaved Changes",
                            new String[]{"Save and Print", "Cancel"}, 0, JOptionPane.QUESTION_MESSAGE);
                    if (!saveAndPrint) {
                        return;
                    }
                    OpenSave.save(comp, false);
                }
                try {
                    Desktop.getDesktop().print(file);
                } catch (IOException ex) {
                    Messages.showException(ex);
                }
            }
        };
    }
}
