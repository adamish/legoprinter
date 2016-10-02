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
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.TooManyListenersException;

public class SerialConnection {

	private InputStream inputStream;
	private OutputStream outputStream;
	private SerialPort serialPort;
	private byte [] readBuffer;
	private int readBufferCounter;
	private final List<LineListener> listeners = new LinkedList<LineListener>();
	private String defaultPort;
	private static final String SERIAL_ID = "Lego110Driver";


	/**
	 * Have a got a working out default port name for this OS.
	 */
	private String determineDefaultPort() {
		String returnVal = null;
		String osname = System.getProperty("os.name").toLowerCase();
		if ( osname.startsWith("windows") ) {
			returnVal = "COM1";
		} else if (osname.startsWith("linux")) {
			returnVal = "/dev/ttyS0";
		} else if ( osname.startsWith("mac") ) {
			returnVal = "/dev/tty.usbserial-0000103D";
		}
		return returnVal;
	}

	/**
	 * Search for a serial port with a given name.
	 * @param portName
	 * @return Port, or null if cannot be found.
	 */
	private CommPortIdentifier findPort(String portName) {
		Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
		CommPortIdentifier returnVal = null;

		while (portList.hasMoreElements()) {
			CommPortIdentifier eachPort = (CommPortIdentifier) portList.nextElement();
			if (eachPort.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portName.equals(eachPort.getName())) {
					returnVal = eachPort;
					break;
				}
			}
		}
		return returnVal;
	}

	public void init() {
		String portName = this.defaultPort;

		if (portName == null) {
			portName = this.determineDefaultPort();
			if (portName == null) {
				System.err.println("Could not determine default port");
			}
		}

		CommPortIdentifier port = this.findPort(portName);
		if (port == null) {
			System.err.println(portName + " not found.");
			System.exit(1);
		}

		try {
			this.serialPort = (SerialPort) port.open(SERIAL_ID, 2000);
		} catch (PortInUseException e) {
			System.err.println("PortInUseException " + portName);
			System.exit(1);
		}

		try {
			this.inputStream = this.serialPort.getInputStream();
		} catch (IOException e) {
			System.err.println("IOException whilst connecting." + portName);
			System.exit(1);
		}

		try {
			this.serialPort.addEventListener(new SerialEventListener());
		} catch (TooManyListenersException e) {
			System.err.println("TooManyListenersException " + portName);
			System.exit(-1);
		}
		// activate the DATA_AVAILABLE notifier
		this.serialPort.notifyOnDataAvailable(true);
		this.serialPort.notifyOnRingIndicator(true);
		this.serialPort.notifyOnOutputEmpty(true);

		try {
			this.serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		} catch (UnsupportedCommOperationException e) {
			System.err.println("UnsupportedCommOperationException." + portName);
			System.exit(1);
		}

		try {
			this.outputStream = this.serialPort.getOutputStream();
		} catch (IOException e) {
			System.err.println("IOException whilst connecting." + portName);
			System.exit(1);
		}
	}

	/**
	 * Write to port.
	 * @param message String to send
	 */
	public void write(String data) {
		try {
			this.outputStream.write(data.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read available data.
	 */
	private void read() {
		// read data
		try {
			while (this.inputStream.available() > 0) {
				if (this.readBuffer == null) {
					this.readBuffer = new byte[255];
				}
				byte nextByte = (byte)this.inputStream.read();
				if (nextByte == 10) {
					String line = new String(this.readBuffer).trim();
					for (LineListener listener : this.listeners) {
						listener.notifyLine(line);
					}
					this.readBuffer = null;
					this.readBufferCounter = 0;
				} else {
					this.readBuffer[this.readBufferCounter] = nextByte;
					this.readBufferCounter++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public interface LineListener {
		void notifyLine(String line);
	}

	public void addLineListener(LineListener lineListener) {
		this.listeners.add(lineListener);
	}

	public void removeLineListener(LineListener lineListener) {
		this.listeners.remove(lineListener);
	}

	private class SerialEventListener implements SerialPortEventListener {
		public void serialEvent(SerialPortEvent event) {
			switch (event.getEventType()) {
			case SerialPortEvent.BI:
			case SerialPortEvent.OE:
			case SerialPortEvent.FE:
			case SerialPortEvent.PE:
			case SerialPortEvent.CD:
			case SerialPortEvent.CTS:
			case SerialPortEvent.DSR:
			case SerialPortEvent.RI:
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
				break;
			case SerialPortEvent.DATA_AVAILABLE:
				break;
			default:
				break;
			}
			SerialConnection.this.read();
		}
	}
}
