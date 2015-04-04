/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.tools;

import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.ImageDisplay;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * A brush tool that draws directly into the image of
 * the current image layer
 */
public class DirectBrushTool extends AbstractBrushTool {
    private BufferedImage copyBeforeStart;

    public DirectBrushTool(char activationKeyChar, String name, String iconFileName, String toolMessage) {
        super(activationKeyChar, name, iconFileName, toolMessage);
    }

    @Override
    public void toolMouseReleased(MouseEvent e, ImageDisplay ic) {
        super.toolMouseReleased(e, ic);
        copyBeforeStart.flush();
        copyBeforeStart = null;
    }

    @Override
    void createGraphics(Composition comp, ImageLayer layer) {
        // uses the graphics of the buffered image contained in the layer
        BufferedImage drawImage = layer.getCompositionSizedSubImage();
        graphics = drawImage.createGraphics();
        if (respectSelection) {
            comp.setSelectionClipping(graphics, null);
        }
        brush.setTarget(comp, graphics);

        BufferedImage image = ImageComponents.getActiveImageLayer().get().getImage();

        assert Utils.checkRasterMinimum(image);

        copyBeforeStart = ImageUtils.copyImage(image);
    }

    @Override
    BufferedImage getOriginalImage(Composition comp) {
        if (copyBeforeStart == null) {
            throw new IllegalStateException("EraseTool: copyBeforeStart == null");
        }

        return copyBeforeStart;
    }

    @Override
    void mergeTmpLayer(Composition comp) {
        // do nothing - this tool draws directly into the image
        // TODO should not have to implement this
    }
}