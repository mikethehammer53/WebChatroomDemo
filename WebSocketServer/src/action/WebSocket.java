package action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/chat/{user}")
public class WebSocket {
	private String currentUser;
	private String[] recvMsg;
	private static Map<String, String> map = new HashMap<>(); 
	//静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
	private static int clientCount = 0;

	//concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
	private static CopyOnWriteArraySet<WebSocket> webSocketSet = new CopyOnWriteArraySet<WebSocket>();

	//与某个客户端的连接会话，需要通过它来给客户端发送数据
	private Session session;



	//连接打开时执行
	@OnOpen
	public void onOpen(@PathParam("user") String user, Session session) {
		currentUser = user;	 
		this.session = session;
		webSocketSet.add(this);     //加入set中
		addOnlineCount();
		System.out.println("Session "+session.getId()+" connected as"+user+","+clientCount+" users online");
		add(session.getId(),user);
	}

	//收到消息时执行
	@OnMessage
	public void onMessage(String message, Session session) {
		recvMsg=message.split("\\|");
		String targetName="";
		String targetId="";
		System.out.println(currentUser + "：" + message);
		if(recvMsg.length==5){
			targetName=recvMsg[3];
			targetId=getIdByName(targetName);
			//System.out.println("target name:"+targetName);
			for(WebSocket client: webSocketSet){
				if(client.session.getId().equals(targetId)){
					//System.out.println("target id:"+targetId);
					try {
						client.sendMessage(message,targetId);
					} catch (IOException e) {
						e.printStackTrace();
						continue;
					}
				}
			}
		}
	}

	//连接关闭时执行
	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		System.out.println(String.format("Session %s closed because of %s", session.getId(), closeReason));
		webSocketSet.remove(this);  //从set中删除
		subOnlineCount();           //在线数减1    
		System.out.println("Client "+session.getId()+" disconnected,"+ getOnlineCount()+" users online");
		map.remove(session.getId());
	}

	//连接错误时执行
	@OnError
	public void onError(Throwable t) {
		t.printStackTrace();
	}

	public void sendMessage(String message,String clientId) throws IOException{
		this.session.getBasicRemote().sendText(message);
	}

	public String getIdByName(String name){
		String id="";
		Set<String>kset=map.keySet();
		//System.out.println("map size:"+map.size());
		//System.out.println("key set size:"+kset.size());
		//System.out.println("id    name");
		//for(String s:kset){
		//System.out.println(s+"-----"+map.get(s));
		//}

		for(String n:kset){

			if(name.equals(map.get(n))){
				id=n;
				//System.out.println("name:"+n+" id:"+id);
				break;
			}
		}
		return id;
	}

	public void add(String id,String name) {
		map.put(id, name);
	}

	public static synchronized int getOnlineCount() {
		return clientCount;
	}

	public static synchronized void addOnlineCount() {
		WebSocket.clientCount++;
	}

	public static synchronized void subOnlineCount() {
		WebSocket.clientCount--;
	}
}
