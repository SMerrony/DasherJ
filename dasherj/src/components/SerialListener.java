package components;

/**
 * *
 * v.0.5 - Move to jssc serial library
 */
import java.util.concurrent.BlockingQueue;

import jssc.SerialPort;
import jssc.SerialPortException;

public final class SerialListener implements Runnable {

    private final SerialPort in;
    private final BlockingQueue<Byte> fromHostQ;

    public SerialListener(SerialPort in, BlockingQueue<Byte> fromHostQ) {

        this.in = in;
        this.fromHostQ = fromHostQ;
    }

    @Override
    public void run() {

        byte[] buffer = new byte[1024];

        try {
            while (true) {
                buffer = in.readBytes();
                if (buffer == null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        //e.printStackTrace();
                        System.out.println("Serial Listener stopping");
                        return;
                    }
                } else {
                    for (int c = 0; c < buffer.length; c++) {
                        fromHostQ.offer(buffer[c]);
                    }
                }
                // System.out.printf( "SerialListener got: %s\n", buffer.toString() );
            }
        } catch (SerialPortException ioe) {
            //ioe.printStackTrace();
            System.out.println("Serial Listener stopping");
        }
    }

}
