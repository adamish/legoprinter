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

import com.adamish.lego110.SerialConnection.LineListener;




/**
 * Send test commands.
 */
public class CommandTest {
  public CommandTest() {
  }

  public static void main(String [] args) {

    SerialConnection serial = new SerialConnection();
    serial.init();
    serial.addLineListener(new LineListener() {
      @Override
      public void notifyLine(String line) {
        System.out.println("RX:" + line);
      }
    });
     
    serial.write(new CommandBuilder().getResetCmd());
    serial.write(new CommandBuilder().getDebugCmd());
    
    serial.write(new CommandBuilder().getPosCmd(400));
    serial.write(new CommandBuilder().getDebugCmd());
    
    serial.write(new CommandBuilder().getPosCmd(200));
    serial.write(new CommandBuilder().getDebugCmd());
    System.exit(0);
  }


}
