package components;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class TelnetListener implements Runnable {

	Socket sock;
	InputStream in;
	BlockingQueue<Byte> fromHostQ;
	
	public TelnetListener( Socket pSock, BlockingQueue<Byte> plFromHostQ ) {
		sock = pSock;
		fromHostQ = plFromHostQ;
		
		try {
			//fromHostStream = new DataInputStream( sock.getInputStream() );
			in =  sock.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void run() {
		
		byte[] buffer = new byte[1024];
		int len = -1;

		try {
			while (true) {
				if((len = in.read( buffer )) > -1) {
					for (int c= 0; c < len; c++) {
						fromHostQ.offer( buffer[c] );
						//System.out.printf( "TelnetListener got %d\n", buffer[c] );
					}
					//System.out.printf( "TelnetListener got: %s\n", buffer );
				}
			}
		} catch (IOException ioe) {
			//ioe.printStackTrace();
		} 
		
		System.out.printf( "TelnetListener stopping\n" );
		
	}

}
