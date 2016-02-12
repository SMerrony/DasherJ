package components;

import java.util.concurrent.BlockingQueue;
import javafx.event.EventHandler;
import javafx.print.PrinterJob;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * *
 * This class takes keyboard input from the user, does any special handling
 * required for DASHER compatibility, then passes the data on to the fromKbdQ
 * for transmission to the host by either TelnetWriter or SerialWriter
 *
 * @author steve
 *
 * v. 0.9 - Map PC Alt-Gr to DASHER CMD Refix sending of NewLines v. 0.7 -
 * Handle real function keys v. 0.6 - Fix sending of NewLines to be
 * DASHER-compliant (and not doubled-up!)
 *
 */
public class KeyboardHandler implements EventHandler<KeyEvent> {

    BlockingQueue<Byte> lFromKbdQ;
    Status status;
    private int modifier;

    public KeyboardHandler(BlockingQueue<Byte> fromKbdQ, Status pStatus) {
        lFromKbdQ = fromKbdQ;
        status = pStatus;
        modifier = 0;
    }

    @Override
    public void handle(KeyEvent kev) {

        // System.out.println( "Debug - KeyboardHandler triggered" );
        if (kev.getEventType() == KeyEvent.KEY_PRESSED) {
            keyPressed(kev);
        } else if (kev.getEventType() == KeyEvent.KEY_RELEASED) {
            keyReleased(kev);
        } else if (kev.getEventType() == KeyEvent.KEY_TYPED) {
            keyTyped(kev);
        }

        kev.consume();
        // return true;  // no further action required
    }

    private void keyPressed(KeyEvent kEv) {

        if (kEv.isControlDown()) {
            status.control_pressed = true;
        } else if (kEv.isShiftDown()) {
            status.shift_pressed = true;
        }

        modifier = 0;
        if (status.control_pressed && status.shift_pressed) {
            modifier = -80;
        }  // Ctrl-Shift
        if (status.control_pressed && !status.shift_pressed) {
            modifier = -64;
        } // Ctrl
        if (!status.control_pressed && status.shift_pressed) {
            modifier = -16;
        } // Shift
    }

    private void keyReleased(KeyEvent ke) {

        KeyCode kc = ke.getCode();
        switch (kc) {
            /*case ESCAPE:
			lFromKbdQ.offer( (byte) 30 );
			break;*/
            // cursor keys
            case DOWN:
                if (status.shift_pressed) {
                    lFromKbdQ.offer((byte) 30);
                }
                lFromKbdQ.offer((byte) 26);
                break;
            case END:
                lFromKbdQ.offer((byte) 30);	// Shift C3 on Dasher
                lFromKbdQ.offer((byte) 90);
                break;
            case HOME:
                lFromKbdQ.offer((byte) 30);	// Shift C1 on Dasher
                lFromKbdQ.offer((byte) 88);
                break;
            case LEFT:
                if (status.shift_pressed) {
                    lFromKbdQ.offer((byte) 30);
                }
                lFromKbdQ.offer((byte) 25);
                break;
            case PAGE_DOWN:
                lFromKbdQ.offer((byte) 30);	// Shift C4 on Dasher
                lFromKbdQ.offer((byte) 91);
                break;
            case PAGE_UP:
                lFromKbdQ.offer((byte) 30);	// Shift C2 on Dasher
                lFromKbdQ.offer((byte) 89);
                break;
            case RIGHT:
                if (status.shift_pressed) {
                    lFromKbdQ.offer((byte) 30);
                }
                lFromKbdQ.offer((byte) 24);
                break;
            case UP:
                if (status.shift_pressed) {
                    lFromKbdQ.offer((byte) 30);
                }
                lFromKbdQ.offer((byte) 23);
                break;
            // modifiers
            case CONTROL:
                status.control_pressed = false;
                break;
            case SHIFT:
                status.shift_pressed = false;
                break;
            // ENTER also results in a KeyTyped event, ignore it here
//		case ENTER:
//			lFromKbdQ.offer( (byte) 10 );
//			break;

            // Function and emulated keys...	
            case CLEAR:
                lFromKbdQ.offer((byte) 12);
                break;
            case PAUSE:  // Dasher: HOLD
                status.holding = !status.holding;
                break;
            case PRINTSCREEN:
                PrinterJob printJob = PrinterJob.createPrinterJob();
                if (printJob != null) {
                    //printJob.printPage( );
                }
                break;
            case ALT_GRAPH: // We are mapping Alt Gr to DASHER CMD
                lFromKbdQ.offer((byte) 30);
                break;
            case F16: // Dummy value for Break button
                lFromKbdQ.offer((byte) 2); // special CMD_BREAK indicator
                break;
            case F24: // Dummy value for CR button
                lFromKbdQ.offer((byte) 13);
                break;
            case F23: // Dummy value for Er EOL button
                lFromKbdQ.offer((byte) 11);
                break;
            case F1:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (113 + modifier));
                break;
            case F2:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (114 + modifier));
                break;
            case F3:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (115 + modifier));
                break;
            case F4:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (116 + modifier));
                break;
            case F5:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (117 + modifier));
                break;
            case F6:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (118 + modifier));
                break;
            case F7:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (119 + modifier));
                break;
            case F8:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (120 + modifier));
                break;
            case F9:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (121 + modifier));
                break;
            case F10:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (122 + modifier));
                break;
            case F11:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (123 + modifier));
                break;
            case F12:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (124 + modifier));
                break;
            case F13:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (125 + modifier));
                break;
            case F14:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (126 + modifier));
                break;
            case F15:
                lFromKbdQ.offer((byte) 30);
                lFromKbdQ.offer((byte) (112 + modifier));
                break;

            default:
                break;
        }

    }

    private void keyTyped(KeyEvent ke) {
        char c = ke.getCharacter().charAt(0);
        lFromKbdQ.offer((byte) c);
    }

}
