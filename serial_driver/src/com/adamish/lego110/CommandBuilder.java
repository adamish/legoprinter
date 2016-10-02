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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.LinkedList;
import java.util.List;

/**
 * Prepares commands needed for printing.
 */
public class CommandBuilder {

  private static final String CMD_RESET = "X";
  private static final String CMD_DEBUG = "D";
  private static final String CMD_GOTO_POS= "G";
  private static final String CMD_FORM_FEED = "F";
  private static final String CMD_PRINT_LINE = "P";
  private static final String CMD_END = "\n";
  
  /** move paper after each line. */
  private static final int lineFeed = 18;
  /** full page line feed. */
  private static final int lineFeedPage = 999;

  public CommandBuilder() {
  }

  /**
   * Commands to print a whole page.
   * @param lines
   */
  public List<String> getPageCmds(BufferedImage image) {
    List<String> returnVal = new LinkedList<String>();
    returnVal.add(getDebugCmd());
    returnVal.add(getResetCmd());

    for (int y = 0; y < image.getHeight(); y++) {

      int [] pixels = this.getLine(image, y);
      if (! this.isEmptyLine(pixels)) { // skip empty lines
        returnVal.add(this.getLineCmd(pixels));
      }
      returnVal.add(this.getFormFeedCmd(lineFeed));
    }

    returnVal.add(this.getFormFeedCmd(lineFeedPage));
    return returnVal;
  }

  /**
   * Command to print a line.
   * @param line
   * @return
   */
  public String getLineCmd(int [] pixels) {
    StringBuilder cmd = new StringBuilder(CMD_PRINT_LINE);
    for (int pixel : pixels) {
      cmd.append(pixel);
    }
    cmd.append(CMD_END);
    return cmd.toString();
  }

  /**
   * Determine if line has any non-white pixels.
   * @param pixels
   * @return True if empty.
   */
  private boolean isEmptyLine(int [] pixels) {
    boolean empty = true;
    for (int pixel : pixels) {
      if (pixel != 0) {
        empty = false;
        break;
      }
    }
    return empty;
  }

  /**
   * Reduce line down to array of pixels for printer.
   * @param image
   * @param y 0 <= y < image.getHeight()
   * @return
   */
  private int [] getLine(BufferedImage image, int y) {
    Raster line = image.getData(new Rectangle(0, y, image.getWidth(), 1));
    int [] returnValue = new int[image.getWidth()];
    for (int x = 0; x < line.getWidth(); x++) {
      int [] pixel = line.getPixel(x, y, (int [])null);
      int bit;
      if (pixel[0] < 0xff) {
        bit = 1;
      } else {
        bit = 0;
      }
      returnValue[x] = bit;
    }
    return returnValue;
  }

  /**
   * Command to move the form feed on.
   * @param amount
   * @return
   */
  public String getFormFeedCmd(int amount) {
    return CMD_FORM_FEED + String.format("%03d", amount) + CMD_END;
  }

  /**
   * Goto absolute position
   * @param pos
   * @return
   */
  public String getPosCmd(int pos) {
    return CMD_GOTO_POS + String.format("%03d", pos) + CMD_END;
  }

  /**
   * Report debug.
   * @return
   */
  public String getDebugCmd() {
    return CMD_DEBUG + CMD_END;
  }

  /**
   * Reset
   * @return
   */
  public String getResetCmd() {
    return CMD_RESET + CMD_END;
  }
}
