/* The status bar updates itself independently based on the state of the Status object,
 * there is no need to update it explicitly from anywhere else.
 * 
 * v.0.8 - Add lines/cols to emulation status
 * v.0.5 - Add logging status, add "Serial/Telnet" to connection indicator
 */

package components;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

public class DasherStatusBar extends JPanel {
	
	private final JLabel onlineStatus = new JLabel();
	private final JLabel loggingStatus = new JLabel();
	private final JLabel connection = new JLabel();
	private final JLabel emulation = new JLabel();
	
	private static final int STATUS_REFRESH_MS = 500;
	
	private Status status;
	
	public DasherStatusBar( Status pStatus ) {
		super();
		
		status = pStatus;
		
		//Container c = super.getContentPane();
		setLayout( new GridLayout( 1, 4 ) );
		
		onlineStatus.setBorder( new EtchedBorder() );
		loggingStatus.setBorder( new EtchedBorder() );
		connection.setBorder( new EtchedBorder() );
		emulation.setBorder( new EtchedBorder() );
		add( onlineStatus );
		add( loggingStatus );
		add( connection );
		add( emulation );
		
		// Update independently
		Timer updateStatusBarTimer = new Timer( STATUS_REFRESH_MS, new ActionListener() {
			public void actionPerformed( ActionEvent ae ) {
				updateStatus();
			}
		});
		updateStatusBarTimer.start();
		
	}
	
	public void updateStatus() {
		
		switch (status.connection) {
		case DISCONNECTED:
			onlineStatus.setText( "Offline" );
			connection.setText( "" );
			break;
		case SERIAL_CONNECTED:
			onlineStatus.setText( "Online (Serial)" );
			connection.setText( status.serialPort );
			break;
		case TELNET_CONNECTED:
			onlineStatus.setText( "Online (Telnet)" );
			connection.setText( status.remoteHost + ":" + status.remotePort );
			break;
		}
		
		if (status.holding){
			onlineStatus.setText( onlineStatus.getText() + " (Hold)" );
		}
		
		if (status.logging){
			loggingStatus.setText( "Logging" );
		} else {
			loggingStatus.setText( "" );
		}
		
		emulation.setText( status.emulation.toString() + " (" + status.visLines + "x" + status.visCols +")" );

	}
	
}
