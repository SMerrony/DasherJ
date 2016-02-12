package components;

/***
 * v. 0.9 - Add DEFAULT_BAUD
 *          Catch exception on connection
 *          Remove System exit on close
 *          Add changeBaudRate method
 * v. 0.5 - Move to JSSC serial library
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;


public class SerialClient {
	
	public static final int DEFAULT_BAUD = 9600;

	public boolean connected;
	public String[] serialPortNames;
	
	// the shared queues
	private BlockingQueue<Byte> fromHostQ, fromKeybdQ;

	private Thread serialListenerThread, serialWriterThread;
	
	// private CommPort commPort;
	SerialPort serialPort;
	
	public SerialClient( BlockingQueue<Byte> fromHostQ, BlockingQueue<Byte> fromKeybdQ ) {
		this.fromHostQ = fromHostQ;
		this.fromKeybdQ = fromKeybdQ;
		serialPortNames = new String[16];
		getComPorts();
	}
	
	public boolean open( String portName, int baudRate ) {

		serialPort = new SerialPort( portName );
		try {
			serialPort.openPort();
			serialPort.setParams( baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE );
			serialPort.setFlowControlMode( SerialPort.FLOWCONTROL_NONE );
			// serialPort.setFlowControlMode( SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT );
		} catch (SerialPortException spe) {
			spe.printStackTrace();
			return false;
		}

		connected = true;
		(serialListenerThread = new Thread(new SerialListener( serialPort, fromHostQ ))).start();
		serialListenerThread.setName( "SerialListenerThread" );
		(serialWriterThread   = new Thread(new SerialWriter( serialPort, fromKeybdQ ))).start();
		serialWriterThread.setName( "SerialWriterThread" );

		return true;
	}
	
	public void close() {
		serialListenerThread.interrupt();
		serialWriterThread.interrupt();
		try {
			serialPort.closePort();
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
		connected = false;
	}
	
	 public static void getComPorts(){
            String[] serialPortNames = SerialPortList.getPortNames(); 
            for (String serialPortName : serialPortNames) {
                System.out.println("Port : " + serialPortName);
            }
	  }

	public void changeBaudRate( int i ) {
		try {
			serialPort.setParams( i, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE );
		} catch (SerialPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
