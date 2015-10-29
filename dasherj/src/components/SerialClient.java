package components;

/***
 * v. 0.5 - Move to JSSC serial library
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;


public class SerialClient {
	
	public boolean connected;
	public int baudRate = 9600;
	public static int serialPortCount = 0;
	public static String[] serialPortNames;
	
	// the shared queues
	private BlockingQueue<Byte> fromHostQ, fromKeybdQ;

	private Thread serialListenerThread, serialWriterThread;
	
	// private CommPort commPort;
	SerialPort serialPort;
	
	InputStream in;
	// BufferedInputStream bIn;
	OutputStream out;
	
	public SerialClient( BlockingQueue<Byte> fromHostQ, BlockingQueue<Byte> fromKeybdQ ) {
		this.fromHostQ = fromHostQ;
		this.fromKeybdQ = fromKeybdQ;
		serialPortNames = new String[16];
		getComPorts();
	}
	
	public boolean open( String portName ) {

		serialPort = new SerialPort( portName );
		try {
			serialPort.openPort();
			serialPort.setParams( baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE );
			serialPort.setFlowControlMode( SerialPort.FLOWCONTROL_NONE );
			// serialPort.setFlowControlMode( SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT );
		} catch (Exception e) {
			e.printStackTrace();
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

		try {
			serialPort.closePort();
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
		// serialPort.removeEventListener();
		// commPort.close(); // THIS HANGS ON WINDOWS 7 64-bit
		connected = false;
		System.exit(0);
	}
	
	 public static void getComPorts(){
		 
//	        String     port_type;
//	        Enumeration<?>  enu_ports  = CommPortIdentifier.getPortIdentifiers();
//	 
//	        while (enu_ports.hasMoreElements()) {
//	            CommPortIdentifier port_identifier = (CommPortIdentifier) enu_ports.nextElement();
//	 
//	            switch(port_identifier.getPortType()){
//	                case CommPortIdentifier.PORT_SERIAL:
//	                    port_type   =   "Serial";
//	                    serialPortNames[serialPortCount] = port_identifier.getName();
//	                    serialPortCount++;
//	                    break;
//	                case CommPortIdentifier.PORT_PARALLEL:
//	                    port_type   =   "Parallel";
//	                    break;
//	                 default:
//	                    port_type   =   "Unknown";
//	                    break;
//	            }
//	            // System.out.println("Port : "+port_identifier.getName() +" Port type : "+port_type);
//	        }

		 String[] serialPortNames = SerialPortList.getPortNames(); 
		 for (int p = 0; p < serialPortNames.length; p++) {
			 System.out.println( "Port : " + serialPortNames[p] );
		 }
		 
	    }
}
