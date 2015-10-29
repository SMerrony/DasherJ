/**
 * LogWriter - copy bytes from logQ and write to log file
 */
package components;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * @author Stephen Merrony
 *
 */
public class LogWriter implements Runnable {
	
	BufferedWriter logWriter;
	BlockingQueue<Byte> logQ;
	
	public LogWriter( BufferedWriter logBW, BlockingQueue<Byte> lQ ) {
		this.logWriter = logBW;
		this.logQ = lQ;
	}

	@Override
	public void run() {
		
		Byte ch = 0;
		
		while(true) {
			try {
				ch = logQ.take();
			} catch (InterruptedException e) {
				try {
					logWriter.flush();
					logWriter.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return;
			}
			
			try {
				logWriter.append( (char) (ch & 0xff) );
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

}
