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

package pixelitor.tools.brushes;

import pixelitor.tools.AbstractBrushTool;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * A "uniform dabs" brush that is based on images.
 */
public class UniformImageBrush extends DabsBrush {
    private static Map<ImageBrushType, BufferedImage> templateImages = new EnumMap<>(ImageBrushType.class);
    private BufferedImage templateImage;
    private BufferedImage coloredBrushImage;
    private BufferedImage finalScaledImage;
    private Color lastColor;

    public UniformImageBrush(ImageBrushType imageBrushType, double spacingRatio, boolean angleAware) {
        super(spacingRatio, angleAware);

        // for each brush type multiple brush instances are created because of the symmetry
        // however the template image can be shared between them
        templateImage = templateImages.get(imageBrushType);
        if(templateImage == null) {
            templateImage = imageBrushType.createTemplateBrush(AbstractBrushTool.MAX_BRUSH_RADIUS);
            templateImages.put(imageBrushType, templateImage);
        }
    }

    @Override
    void setupBrushStamp() {
        Color c = g.getColor();

        if (!c.equals(lastColor)) {
            colorizeBrushImage(c);
            lastColor = c;
            resizeBrushImage(diameter, true);
        } else {
            resizeBrushImage(diameter, false);
        }

    }

    /**
     * This method assumes that the color of coloredBrushImage is OK
     */
    private void resizeBrushImage(float newSize, boolean force) {
        if (!force) {
            if (finalScaledImage != null && finalScaledImage.getWidth() == newSize) {
                return;
            }
        }

        if (finalScaledImage != null) {
            finalScaledImage.flush();
        }

        int newSizeInt = (int) newSize;
        finalScaledImage = new BufferedImage(newSizeInt, newSizeInt, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = finalScaledImage.createGraphics();
        g.drawImage(coloredBrushImage, 0, 0, newSizeInt, newSizeInt, null);
        g.dispose();
    }

    /**
     * Creates a colorized brush image from the template image according to the foreground color
     *
     * @param color
     */
    private void colorizeBrushImage(Color color) {
        coloredBrushImage = new BufferedImage(templateImage.getWidth(), templateImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] srcPixels = ImageUtils.getPixelsAsArray(templateImage);
        int[] destPixels = ImageUtils.getPixelsAsArray(coloredBrushImage);

        int destRed = color.getRed();
        int destGreen = color.getGreen();
        int destBlue = color.getBlue();
        for (int i = 0; i < destPixels.length; i++) {
            int srcRGB = srcPixels[i];

            //int a = (srcRGB >>> 24) & 0xFF;
            int srcRed = (srcRGB >>> 16) & 0xFF;
            int srcGreen = (srcRGB >>> 8) & 0xFF;
            int srcBlue = (srcRGB) & 0xFF;
            int srcAverage = (srcRed + srcGreen + srcBlue) / 3;

            destPixels[i] = (0xFF - srcAverage) << 24 | destRed << 16 | destGreen << 8 | destBlue;
        }
    }

    @Override
    public void putDab(double x, double y, double theta) {
        if(!angleAware || theta == 0) {
            setupBrushStamp();
            g.drawImage(finalScaledImage, (int) x - radius, (int) y - radius, null);
        } else {
            AffineTransform oldTransform = g.getTransform();
            g.rotate(theta, x, y);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(finalScaledImage, (int) x - radius, (int) y - radius, null);
            g.setTransform(oldTransform);
        }
        updateComp((int) x, (int) y);
    }
}
