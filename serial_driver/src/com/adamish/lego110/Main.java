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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Print each image file passed as separate page.
 */
public class Main {
  public Main() {
  }

  public static void main(String [] args) {
    if (args.length != 1) {
      System.err.println("Usage Main <filename1> .. <filenameN>");
      System.exit(1);
    }

    SerialConnection serial = new SerialConnection();
    serial.init();

    BufferedSerial bufferedSerial = new BufferedSerial(serial);

    for (String filename : args) {

      // load
      BufferedImage img = null;
      try {
        img = ImageIO.read(new File(filename));
      } catch (IOException e) {
        System.err.println("Cannot read \"" + filename + "\":" + e.getMessage());
        System.exit(1);
      }

      // scale and reduce color.
      ImageProcessor imageProcessor = new ImageProcessor();
      imageProcessor.setTargetWidth(640); // approx. resolution of horizontal sensor across a page.
      imageProcessor.setTargetRatio(0.7); // don't bother with as much vertical resolution.
      BufferedImage outputImage = imageProcessor.process(img);

      // build commands
      CommandBuilder commandProcessor = new CommandBuilder();
      List<String> commands = commandProcessor.getPageCmds(outputImage);

      // send
      bufferedSerial.sendLines(commands);

      System.exit(0);
    }
  }


}
