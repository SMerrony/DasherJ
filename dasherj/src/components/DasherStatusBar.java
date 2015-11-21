/* The status bar updates itself independently based on the state of the Status object,
 * there is no need to update it explicitly from anywhere else.
 * 
 * v.0.9 - Add baud rate to connection indicator
 * v.0.8 - Add lines/cols to emulation status
 * v.0.5 - Add logging status, add "Serial/Telnet" to connection indicator
 */

package components;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class DasherStatusBar extends HBox {
	
	private final Label onlineStatus = new Label();
	private final Label loggingStatus = new Label();
	private final Label connection = new Label();
	private final Label emulation = new Label();
	
	private final String etchedStyle = "-fx-border-insets: 0; " +
									   "-fx-border-width: 2px; " +
									   "-fx-border-color: black lightgray lightgray black;";
	
	public static final int STATUS_REFRESH_MS = 500;
	
	private Status status;
	
	public DasherStatusBar( Status pStatus ) {
		
		status = pStatus;
		
		//Container c = super.getContentPane();
		// setLayout( new GridLayout( 1, 4 ) );

		onlineStatus.setStyle( etchedStyle );
		loggingStatus.setStyle( etchedStyle );
		loggingStatus.setMinWidth( 80.0 );
		connection.setStyle( etchedStyle );
		connection.setMaxWidth( Double.MAX_VALUE );
		HBox.setHgrow( connection, Priority.ALWAYS );
		emulation.setStyle( etchedStyle );
		getChildren().addAll( onlineStatus, loggingStatus, connection, emulation );
		
	}
	
	public void updateStatus() {
		
		switch (status.connection) {
		case DISCONNECTED:
			onlineStatus.setText( "Offline" );
			connection.setText( " " );
			break;
		case SERIAL_CONNECTED:
			onlineStatus.setText( "Online (Serial)" );
			connection.setText( status.serialPort + " @ " + status.baudRate + "baud" );
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
			loggingStatus.setText( " " );
		}
		
		emulation.setText( status.emulation.toString() + " (" + status.visLines + "x" + status.visCols +")" );

	}
	
}
