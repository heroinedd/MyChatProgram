package p2pserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;

public class Server {
 
	public static HashMap<String, String> user = new HashMap<String, String>();
	public static HashMap<String, InetAddress> userToIp = new HashMap<String, InetAddress>();
	public static HashMap<String, Integer> userToPort = new HashMap<String, Integer>();
	public static HashSet<String> user_online = new HashSet<String>();
	private DatagramSocket udp_socket;
	private int PORT = 8800;
	private static final long HEARTBEAT_INTERVAL = 30000;
	
	public Server() {
		user.put("001", "123456");
		user.put("002", "123456");
		user.put("003", "123456");
		user.put("004", "123456"); //测试用例
		try {
			udp_socket = new DatagramSocket(PORT);
			System.out.println("server up");
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		while(true) {
			
	        byte[] data = new byte[1024];// 创建字节数组，指定接收的数据包的大小
	        DatagramPacket pkt = recvPacket(data, data.length);
	        
	        if(pkt == null) continue;
	        
	        String info = new String(data, 0, pkt.getLength());
	        System.out.println(info);
	        String command = info.split(":")[0];
	        InetAddress ip = pkt.getAddress();
	        int port = pkt.getPort();
	        System.out.println("ip: " + ip.getHostAddress() + "port: " + port);
	        
	        if(command.equals("heartbeat")) continue;
	        else if(command.equals("login")) {
	        	String username = info.split(":")[1];
	        	String password = info.split(":")[2];
	        	byte[] loginResp;
	        	if(password.equals(user.get(username))) {
	        		loginResp = new String("success").getBytes();
	        		System.out.println("user " + username + " login");
	        		user_online.add(username);
	        		userToIp.put(username, ip);
	        		userToPort.put(username, port);
	        		
	        		HeartBeatThread HBThread = new HeartBeatThread(ip, port);
	        		HBThread.start();
	        		
	        	}
	        	else loginResp = new String("fail").getBytes();
	        	sendPacket(loginResp, loginResp.length, ip, port);
	        }
	        else if(command.equals("exit")) {
	        	String username = info.split(":")[1];
	        	user_online.remove(username);
	        	userToIp.remove(username);
	        	userToPort.remove(username);
	        }
	        else if(command.equals("text") || command.equals("audio")) {
	        	String receiver = info.split(":")[1];
	        	InetAddress recv_ip = userToIp.get(receiver);
	        	int recv_port =  userToPort.get(receiver);
	        	//给连接发起方回复
	        	String resp1 = "ip:" + recv_ip.getHostAddress() + ":port:" + recv_port;
	        	byte[] bresp1 = resp1.getBytes();
	        	sendPacket(bresp1, bresp1.length, ip, port);
	        	//给被连接方发消息
	        	String resp2 = "ip:" + ip.getHostAddress() + ":port:" + port;
	        	byte[] bresp2 = resp2.getBytes();
	        	sendPacket(bresp2, bresp2.length, recv_ip, recv_port);
	        }

		}
	}
 
	public static void main(String[] args) {
		new Server().start();
        
	}
	
	public void sendPacket(byte[] msg, int len, InetAddress addr, int port) {
		DatagramPacket pkt = new DatagramPacket(msg, len, addr, port);
		try {
			udp_socket.send(pkt);
		} catch (IOException e) {
			System.out.println("fail to send packet");
			e.printStackTrace();
		}
	}
	
	public DatagramPacket recvPacket(byte[] data, int len) {
		DatagramPacket pkt = new DatagramPacket(data, len);
		try {
			udp_socket.receive(pkt);
			return pkt;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	class HeartBeatThread extends Thread {
		InetAddress addr;
		int port;
		
		public HeartBeatThread(InetAddress addr, int port) {
			this.addr = addr;
			this.port = port;
		}
		
		@Override
		public void run() {
			byte[] heartBeatMsg = new String("heartbeat").getBytes();
			while(true) {
				if(this.isInterrupted()) break;
				sendPacket(heartBeatMsg, heartBeatMsg.length, addr, port);
				try {
					Thread.sleep(HEARTBEAT_INTERVAL);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
