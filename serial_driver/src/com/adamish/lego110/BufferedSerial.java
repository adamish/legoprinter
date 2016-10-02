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

import java.util.List;

import com.adamish.lego110.SerialConnection.LineListener;

/**
 * Wait for an acknowledge before sending
 * next line to prevent overflow.
 */
public class BufferedSerial {
  private final SerialConnection serial;
  private final Object blocker = new Object();
  private static final String RX_ACK = "OK";

  public BufferedSerial(SerialConnection serial) {
    this.serial = serial;
  }

  public void sendLines(List<String> lines) {
    LineListener listener = new InnerLineListener();
    this.serial.addLineListener(listener);
    for (String line : lines) {
      this.serial.write(line);
      System.out.println(line);
      this.waitForAck();
    }
    this.serial.removeLineListener(listener);
  }

  private void waitForAck() {
    try {
      synchronized (this.blocker) {
        this.blocker.wait();
      }
    } catch (InterruptedException e) {
      // Restore the interrupted status
      Thread.currentThread().interrupt();
    }
  }

  private class InnerLineListener implements LineListener {
    @Override
    public void notifyLine(String line) {
      if (RX_ACK.equals(line)) {
        synchronized (BufferedSerial.this.blocker) {
          BufferedSerial.this.blocker.notifyAll();
        }
      }
    }
  }
}
