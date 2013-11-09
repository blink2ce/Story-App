package com.example.tictactoe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This code needs network access. Remember to add the following line to
 * Manifest.xml (before the line starting with <application ...
 * 
 * <uses-permission android:name="android.permission.INTERNET" />
 * 
 */
public class MainActivity extends Activity {

	// TAG for logging
	private static final String TAG = "TTTActivity";

	// server to connect to
	protected static final int GROUPCAST_PORT = 20002; // make sure you change
														// the port
	protected static final String GROUPCAST_SERVER = "ec2-23-20-136-30.compute-1.amazonaws.com";

	// networking
	Socket socket = null;
	BufferedReader in = null;
	PrintWriter out = null;
	boolean connected = false;

	// UI elements
	Button table[][] = new Button[3][3];
	Button bConnect = null;
	EditText etName = null;
	TextView player1 = null;

	// group name
	String groupName;
	
	//user is x or o
	String myLetter;
	String opponentLetter;
	String myName;
	String opponentName;

	/***** Activity Lifecycle **************************************************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	
		// start the AsynchTask that connects to the server
		// and listens for whatever the server is sending to us
		connect();
	
		// find UI elements defined in xml
		bConnect = (Button) this.findViewById(R.id.bConnect);
		etName = (EditText) this.findViewById(R.id.etName);
		table[0][0] = (Button) this.findViewById(R.id.b00);
		table[0][1] = (Button) this.findViewById(R.id.b01);
		table[0][2] = (Button) this.findViewById(R.id.b02);
		table[1][0] = (Button) this.findViewById(R.id.b10);
		table[1][1] = (Button) this.findViewById(R.id.b11);
		table[1][2] = (Button) this.findViewById(R.id.b12);
		table[2][0] = (Button) this.findViewById(R.id.b20);
		table[2][1] = (Button) this.findViewById(R.id.b21);
		table[2][2] = (Button) this.findViewById(R.id.b22);
		player1 = (TextView) this.findViewById(R.id.tvP1);
		
	
		// make the table non-clickable
		disableTableClick();
	
		// assign OnClickListener to connect button
		bConnect.setOnClickListener(buttonClickListener);
		
		// assign OnClickListeners to table buttons
		for (int x = 0; x < 3; x++)
			for (int y = 0; y < 3; y++)
				table[x][y].setOnClickListener(buttonClickListener);
	}

	
	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy called");
		disconnect();	
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// build menu
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle menu click events
		if (item.getItemId() == R.id.clear) { 
			for (int x = 0; x < 3; x++)
				for (int y = 0; y < 3; y++)
					table[x][y].setText(R.string.Blank);
			enableTableClick();
			send("MSG,"+groupName+",clear");
			return true;
		
		}
	
		if (item.getItemId() == R.id.exit) {
			send("MSG,"+groupName+",finish");
			send("BYE");
			return true;
		}
	
		return super.onOptionsItemSelected(item);
	
	}

	/***** Tictactoe table **************************************************/

	/**
	 * Make the buttons of the tictactoe table clickable if they are not marked yet
	 */
	void enableTableClick() {
		for (int x = 0; x < 3; x++)
			for (int y = 0; y < 3; y++)
				if ("".equals(table[x][y].getText().toString()))
					table[x][y].setEnabled(true);
	}

	/**
	 * Make the tictactoe table non-clickable
	 */
	void disableTableClick() {
		for (int x = 0; x < 3; x++)
			for (int y = 0; y < 3; y++)
				table[x][y].setEnabled(false);
	}

	
	/***** Handler for incoming messages ****************************************/
	
	/**
	 * We need a Handler to update GUI elements in response to an incoming
	 * message from the network.
	 */
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			String lineFromServer = (String) message.obj;

			// split it along commas and colons
			String[] tokens = lineFromServer.split(",|:");

			if ("+OK".equals(tokens[0])) {
				// it's an OK
				Log.i(TAG, "got +OK: " + lineFromServer);

				if ("NAME".equalsIgnoreCase(tokens[1])) {
					Log.i(TAG, "NAME set to " + tokens[2]);

					// We got a +OK reply to our NAME command from the server

					// TODO: so we do something...
					send("LIST,GROUPS");


					return;
				}

				if ("LIST".equalsIgnoreCase(tokens[1])
						&& "GROUPS".equalsIgnoreCase(tokens[2])) {

					// we got a +OK response to our LIST,GROUPS command from the
					// server
					// the response looks like
					// "+OK,LIST,GROUPS:@group1(1/2),@group2(2/2),@group3(4/0)"

					if (tokens.length > 2) { // check if there are any groups
						for (int i = 3; i < tokens.length; i++) { // for each
																	// token
																	// representing
																	// a group
							Group g = Group.createGroup(tokens[i]); // create a
																	// group
																	// object
							// figure out if the group has a member count cap of
							// 2, and see whether it's full
							if (g.groupMemberMax == 2 && g.groupMemberCnt == 1) {
								// it's not yet full
								// so we send request to join

								// TODO: send join request to server
								send("JOIN,"+g.groupName);
								// and quit searching for another group like
								// this
								return;
							}

						}
					}

					// we couldn't find a group that has a member cap 2 that we
					// could join

					// TODO: figure out a new group name (random?) and send join
					//       request to server
					Random rand = new Random();
					groupName="@new_group_"+rand.nextInt();
					send("JOIN,"+groupName+",2");
					
					return;
				}
				if ("LIST".equalsIgnoreCase(tokens[1])
						&& "USERS".equalsIgnoreCase(tokens[2])){
					if (etName.equals(tokens[3])){
						opponentName=tokens[4];
					}else{
						opponentName=tokens[3];
					}
					player1.setText("You are playing against "+opponentName+".");
				}
				
				if("BYE".equals(tokens[1])){
					 finish();
					 return;
				}

				if ("JOIN".equalsIgnoreCase(tokens[1])) {
					// got a +OK response to a join request
					// the server replied something like "+OK,JOIN,@group2(1/2)"
					// or "+OK,JOIN,@group2(2/2)"

					Group g = Group.createGroup(tokens[2]);
					MainActivity.this.groupName = g.groupName;
					Log.i(TAG, "JOINed group " + g.groupName + "("
							+ g.groupMemberCnt + "/" + g.groupMemberMax + ")");

					if (g.groupMemberCnt == 1) {
						myName=etName.getText().toString();
						myLetter="X";
						opponentLetter="O";
						disableTableClick();
						

					} else {
						// we're the second member of the group

						// enable board, wait for click from user
						myLetter="O";
						opponentLetter="X";
						myName=etName.getText().toString();
						send("MSG,"+groupName+","+myName);
						send("MSG,"+groupName+",?");
						//etName.setText("");
						enableTableClick();
						
					}

					return;
				}

				// TODO: handle other kinds of +OK responses

				Log.i(TAG, "+OK response not handled: " + lineFromServer);

			} else if ("+ERROR".equals(tokens[0])) {
				// it's an ERROR
				Log.i(TAG, "got +ERROR: " + lineFromServer);

				// TODO: do something if we get a +ERROR back from the server

				Log.i(TAG, "+OK response not handled: " + lineFromServer);

			} else if ("+MSG".equals(tokens[0])) {
				// it's a MSG
				Log.i(TAG, "got +MSG: " + lineFromServer);

				// turn the line we get from the server into a Msg object
				Msg msg = Msg.createMsg(lineFromServer);
				String message1=msg.body;
				if("clear".equals(message1)){	
					disableTableClick();
					for (int x = 0; x < 3; x++)
						for (int y = 0; y < 3; y++)
							table[x][y].setText(R.string.Blank);
					Toast.makeText(getApplicationContext(),
							opponentName+" cleared the board.", Toast.LENGTH_SHORT)
							.show();
					
				}else if("finish".equals(message1)){
					Toast.makeText(getApplicationContext(),
							opponentName+" exited the game.", Toast.LENGTH_SHORT)
							.show();
					player1.setText("");
					disableTableClick();
					for (int x = 0; x < 3; x++)
						for (int y = 0; y < 3; y++)
							table[x][y].setText("");
					
				}
				else if ('#'==message1.charAt(0)){
					int x=message1.charAt(1)-'0';
					int y=message1.charAt(2)-'0';
					table[x][y].setText(opponentLetter);
					enableTableClick();
					Toast.makeText(getApplicationContext(),
						"Your turn!", Toast.LENGTH_SHORT)
						.show();
				}else if("?".equals(message1)){
					send("MSG,"+groupName+","+myName);
				}
				else{
					opponentName=message1;
					player1.setText("You are playing against "+opponentName+".");
				}

				// TODO: figure out what field the other player clicked,
				// mark the field on our board, etc..

			}

		}
	};

	/***** Handle button clicks ****************************************/
	
	/**
	 * This is a common OnClickListener for all buttons on the UI.
	 */
	OnClickListener buttonClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int x, y;
			switch (v.getId()) {
			case R.id.bConnect:
				String name = etName.getText().toString();
				if (name == null || name.startsWith("@")) {
					Toast.makeText(getApplicationContext(), "Invalid name",
							Toast.LENGTH_SHORT).show();
				} else {
					// the name looks OK
					// TODO: so we do something
					send("NAME,"+name);
				}
				break;
			case R.id.b00:
				x = 0;
				y = 0;
				madeMove(x,y);

				// TODO: what do we do if the user clicked field (0,0)?
				break;
			case R.id.b01:
				x = 0;
				y = 1;
				madeMove(x,y);

				// TODO: what do we do if the user clicked field (0,1)?
				break;
			case R.id.b02:
				x = 0;
				y = 2;
				madeMove(x,y);
				break;
			case R.id.b10:
				x = 1;
				y = 0;
				madeMove(x,y);
				break;
			case R.id.b11:
				x = 1;
				y = 1;
				madeMove(x,y);
				break;
			case R.id.b12:
				x = 1;
				y = 2;
				madeMove(x,y);
				break;
			case R.id.b20:
				x = 2;
				y = 0;
				madeMove(x,y);
				break;
			case R.id.b21:
				x = 2;
				y = 1;
				madeMove(x,y);
				break;
			case R.id.b22:
				x = 2;
				y = 2;
				madeMove(x,y);
				break;

			// [ ... and so on for the other buttons ]				

			default:
				break;
			}
		}
	};
	
	void madeMove(int x, int y){
		table[x][y].setText(myLetter);
		send("MSG,"+groupName+","+"#"+x+y);
		disableTableClick();
	}

	
	/***** Networking *********************************************/

	/**
	 * Connect to the server and listen for incoming one-line messages over the
	 * TCP connection. Incoming messages are send to the handler. This method is
	 * safe to call from the UI thread.
	 */
	void connect() {

		new AsyncTask<Void, Void, String>() {

			String errorMsg = null;

			@Override
			protected String doInBackground(Void... args) {

				try {
					connected = false;
					socket = new Socket(GROUPCAST_SERVER, GROUPCAST_PORT);
					in = new BufferedReader(new InputStreamReader(
							socket.getInputStream()));
					out = new PrintWriter(socket.getOutputStream());

					connected = true;

					while (true) {

						String msg = in.readLine();

						if (msg == null) { // other side closed the
											// connection
							break;
						}
						handler.obtainMessage(0, msg).sendToTarget();

					}

				} catch (UnknownHostException e1) {
					errorMsg = e1.getMessage();
				} catch (IOException e1) {
					errorMsg = e1.getMessage();
				} finally {
					connected = false;
					try {
						if (in != null)
							in.close();
						if (socket != null)
							socket.close();
					} catch (IOException e) {
					}
				}

				return errorMsg;
			}

			@Override
			protected void onPostExecute(String errorMsg) {
				if (errorMsg == null) {
					Toast.makeText(getApplicationContext(),
							"Disconnected from server", Toast.LENGTH_SHORT)
							.show();
				} else {
					Toast.makeText(getApplicationContext(),
							"Error: " + errorMsg, Toast.LENGTH_SHORT).show();
				}
			}
		}.execute();
	}

	void disconnect() {

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... args) {

				try {
					if (connected)
						connected = false;
					if (in != null)
						in.close();
					if (socket != null)
						socket.close();
				} catch (IOException e) {
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void arg) {
				Toast.makeText(getApplicationContext(),
						"Disconnected from server", Toast.LENGTH_SHORT).show();
			}
		}.execute();
	}

	/**
	 * Send a one-line message to the server over the TCP connection. This
	 * method is safe to call from the UI thread.
	 * 
	 * @param msg
	 *            The message to be sent.
	 * @return true if sending was successful, false otherwise
	 */
	boolean send(String msg) {
		if (!connected)
			return false;

		new AsyncTask<String, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(String... msg) {
				Log.i(TAG, "sending: " + msg[0]);
				out.println(msg[0]);
				return out.checkError();
			}

			@Override
			protected void onPostExecute(Boolean error) {
				if (!error) {
					Toast.makeText(getApplicationContext(),
							"Message sent to server", Toast.LENGTH_SHORT)
							.show();
				} else {
					Toast.makeText(getApplicationContext(),
							"Error sending message to server",
							Toast.LENGTH_SHORT).show();
				}
			}
		}.execute(msg);

		return true;
	}

}