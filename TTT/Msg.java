package com.example.tictactoe;

public class Msg {

	public final String from;
	public final String to;
	public final String body;
	
	public Msg(String from, String to, String body) {
		this.from = from;
		this.to = to;
		this.body = body;
	}

	
	/**
	 * Create a Msg object from a line starting with +MSG, received from the server.
	 * 
	 * @param msgLineFromServer the line received from the server, e.g. "+MSG,Alice,@group2,Hello World!"
	 * @return the Msg object
	 */
	public static Msg createMsg(String msgLineFromServer) {
		// +MSG,from,to,body
		String[] tokens = msgLineFromServer.split(",");
		if(!"+MSG".equalsIgnoreCase(tokens[0]))
			return null;
		
		String from = tokens[1]; // this is the sender
		String to = tokens[2]; // this is the destination address
		StringBuilder sb = new StringBuilder();
		for(int i=3; i<tokens.length; i++) {
			sb.append(tokens[i]);
			sb.append(',');
		}
		if(sb.length() > 0)
			sb.deleteCharAt(sb.length() - 1);
		
		return new Msg(from, to, sb.toString());
	}
	
}