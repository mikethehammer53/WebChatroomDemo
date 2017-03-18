package debug;

import action.WebSocket;

public class Debug {

	public static void main(String[] args) {
		WebSocket ws=new WebSocket();
		ws.add("ID1", "1");
		ws.add("ID2", "2");
		System.out.println(ws.getIdByName("2"));
		
		
	}

}
