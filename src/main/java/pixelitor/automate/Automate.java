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

package pixelitor.automate;

import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.compactions.CompAction;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.io.Dirs;
import pixelitor.io.FileUtils;
import pixelitor.io.OpenSave;
import pixelitor.io.OutputFormat;
import pixelitor.io.SaveSettings;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.EventQueue;
import java.io.File;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static javax.swing.JOptionPane.WARNING_MESSAGE;

/**
 * Utility class with static methods for batch processing
 */
public class Automate {
    private static final String OVERWRITE_YES = "Yes";
    private static final String OVERWRITE_YES_ALL = "Yes, All";
    private static final String OVERWRITE_NO = "No (Skip)";
    private static final String OVERWRITE_CANCEL = "Cancel Processing";

    private static volatile boolean overwriteAll = false;
    private static volatile boolean stopProcessing = false;

    private Automate() {
    }

    /**
     * Processes each file in the input directory
     * with the given {@link CompAction}
     */
    public static void processEachFile(CompAction action,
                                       String dialogTitle) {
        File openDir = Dirs.getLastOpen();
        File saveDir = Dirs.getLastSave();

        File[] inputFiles = FileUtils.listSupportedInputFilesIn(openDir);
        if (inputFiles.length == 0) {
            Messages.showInfo("No files", "There are no supported files in " + openDir.getAbsolutePath());
            return;
        }

        var progressMonitor = GUIUtils.createPercentageProgressMonitor(
                dialogTitle);
        var worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                overwriteAll = false;

                for (int i = 0, nrOfFiles = inputFiles.length; i < nrOfFiles; i++) {
                    if (progressMonitor.isCanceled()) {
                        break;
                    }

                    File file = inputFiles[i];
                    progressMonitor.setProgress((int) ((float) i * 100 / nrOfFiles));

                    String msg = "Processing " + (i + 1) + " of " + nrOfFiles;
                    progressMonitor.setNote(msg);
                    System.out.println(msg);

                    processFile(file, action, saveDir);

                    if (stopProcessing) {
                        break;
                    }

                } // end of for loop
                progressMonitor.close();
                return null;
            } // end of doInBackground
        };
        worker.execute();
    }

    private static void processFile(File file, CompAction action, File saveDir) {
        OpenSave.openFileAsync(file)
                .thenComposeAsync(
                        comp -> process(comp, action),
                        EventQueue::invokeLater)
                .thenComposeAsync(
                        comp -> saveAndClose(comp, saveDir),
                        EventQueue::invokeLater)
                .exceptionally(Messages::showExceptionOnEDT)
                .join();
    }

    private static CompletableFuture<Composition> process(Composition comp,
                                                          CompAction action) {
        assert EventQueue.isDispatchThread() : "not EDT thread";
        assert comp.getView() != null : "no view for " + comp.getName();

        return action.process(comp);
    }

    private static CompletableFuture<Void> saveAndClose(Composition comp, File lastSaveDir) {
        View view = comp.getView();
        assert view != null : "no view for " + comp.getName();

        var outputFormat = OutputFormat.getLastUsed();
        File outputFile = calcOutputFile(comp, lastSaveDir, outputFormat);
        CompletableFuture<Void> retVal = null;

        // so that it doesn't ask to save again after we just saved it
        comp.setDirty(false);

        var saveSettings = new SaveSettings(outputFormat, outputFile);
        if (outputFile.exists() && !overwriteAll) {
            String answer = showOverwriteWarningDialog(outputFile);

            switch (answer) {
                case OVERWRITE_YES:
                    retVal = comp.saveAsync(saveSettings, false);
                    break;
                case OVERWRITE_YES_ALL:
                    retVal = comp.saveAsync(saveSettings, false);
                    overwriteAll = true;
                    break;
                case OVERWRITE_NO:
                    // do nothing
                    break;
                case OVERWRITE_CANCEL:
                    OpenImages.warnAndClose(view);
                    stopProcessing = true;
                    return CompletableFuture.completedFuture(null);
            }
        } else { // the file does not exist or overwrite all was pressed previously
            view.paintImmediately();
            retVal = comp.saveAsync(saveSettings, false);
        }
        OpenImages.warnAndClose(view);
        stopProcessing = false;
        if (retVal != null) {
            return retVal;
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static File calcOutputFile(Composition comp, File lastSaveDir, OutputFormat outputFormat) {
        String inFileName = comp.getFile().getName();
        String outFileName = FileUtils.replaceExt(inFileName, outputFormat.toString());
        return new File(lastSaveDir, outFileName);
    }

    private static String showOverwriteWarningDialog(File outputFile) {
        var optionPane = new JOptionPane(
                format("File %s already exists. Overwrite?", outputFile),
                WARNING_MESSAGE);

        optionPane.setOptions(new String[]{
                OVERWRITE_YES, OVERWRITE_YES_ALL, OVERWRITE_NO, OVERWRITE_CANCEL});
        optionPane.setInitialValue(OVERWRITE_NO);

        JDialog dialog = optionPane.createDialog(
                PixelitorWindow.getInstance(), "Warning");
        dialog.setVisible(true);
        String value = (String) optionPane.getValue();
        String answer;

        if (value == null) { // cancelled
            answer = OVERWRITE_CANCEL;
        } else {
            answer = value;
        }
        return answer;
    }
}