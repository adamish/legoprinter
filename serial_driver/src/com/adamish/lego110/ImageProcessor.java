/*
 * This file is part of lego110.
 *
 *  Lego110 is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Lego110 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with lego110.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.adamish.lego110;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;

/**
 * Process a BufferedImage into a matrix
 * ready for printing including re-scaling and
 * reduction to black and white.
 */
public class ImageProcessor {

  private int targetWidth = 30;
  private double targetRatio = 1;

  public BufferedImage process(BufferedImage image) {

    int width = this.targetWidth;
    double scaling = (double)image.getWidth() / (double)this.targetWidth;
    int height = (int)(image.getHeight() / scaling * this.targetRatio);

    BufferedImage returnValue = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    int [] pixels = new int[width * height];
    PixelGrabber grabber = new PixelGrabber(scaled, 0, 0, width, height, pixels, 0, width);
    try {
      grabber.grabPixels();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    ColorModel colorModel = grabber.getColorModel();

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int pixel = pixels[y * width + x];
        int r = colorModel.getRed(pixel);
        int g = colorModel.getGreen(pixel);
        int b = colorModel.getBlue(pixel);
        int a = colorModel.getAlpha(pixel);

        int rgb = (r | g << 8 | b << 16) & 0x00ffffff;

        int intensity = (0x00ffffff - rgb) >>> 20;

        int [] output;
        if (intensity > 0 && a != 0) {
          output = new int[]{0, 0, 0};
        } else {
          output = new int[]{0xff, 0xff, 0xff};
        }
        returnValue.getRaster().setPixel(x, y, output);
      }
    }
    return returnValue;

  }

  /**
   * Width of output.
   * @return
   */
  public int getTargetWidth() {
    return this.targetWidth;
  }

  public void setTargetWidth(int targetWidth) {
    this.targetWidth = targetWidth;
  }

  /**
   * Ratio of height to width. Typically 1:1,
   * but can be used to calibrate vertical resolution.
   * 0.5 = half as many vertical pixels as horizontal
   * @return
   */
  public double getTargetRatio() {
    return this.targetRatio;
  }

  public void setTargetRatio(double targetRatio) {
    this.targetRatio = targetRatio;
  }

}
