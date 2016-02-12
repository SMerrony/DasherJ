package components;

import java.util.concurrent.BlockingQueue;

import jssc.SerialPort;
import jssc.SerialPortException;

public class SerialWriter implements Runnable {

    SerialPort serialPort;
    BlockingQueue<Byte> fromKeybdQ;

    public SerialWriter(SerialPort serialPort2, BlockingQueue<Byte> fromKeybdQ) {
        this.serialPort = serialPort2;
        this.fromKeybdQ = fromKeybdQ;
    }

    @Override
    public void run() {

        Byte b;
        boolean cont = true;

        while (cont) {
            try {
                b = fromKeybdQ.take();
            } catch (InterruptedException e) {
                return;
            }

            if (b == 2) {
                try {
                    serialPort.sendBreak(110);
                } catch (SerialPortException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                try {
                    serialPort.writeByte(b);
                    // System.out.printf( "SerialWriter - Wrote to serial port: %s\n", b);
                } catch (SerialPortException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

}
