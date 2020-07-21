package p2pclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
	
	private static String SERVER_IP = "47.96.142.235";
//	private static String SERVER_IP = "127.0.0.1";
	private static InetAddress SERVER_ADDR;
	private static int SERVER_PORT = 8800;
	protected InetAddress peer_addr;
	protected int peer_port;
	private static final long HEARTBEAT_INTERVAL = 30000;
	private static final int RECV_TIMEOUT = 3000000;
	
	//输入scanner
	private Scanner scanner = new Scanner(System.in);
	private DatagramSocket udp_socket;

	private CaptureVoice cv = null;
	private PlayVoice pv = null;
	private sendTextThread stt = null;
	private receiveTextThread rtt = null;
	
	private HeartBeatThread sHBThread = null, pHBThread = null;
	
	public Client() {
		try {
			SERVER_ADDR = InetAddress.getByName(SERVER_IP);
			udp_socket = new DatagramSocket();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new Client().start();
	}
	
	public void start() {
		try {
	        String in = null;
	        
	        String resp = "fail";
	        while(resp.equals("fail")) {
	        	System.out.println("enter login:username:password to login");
		        in = scanner.next();
		        
		        byte[] loginMsg = in.getBytes();
		        sendPacket(loginMsg, loginMsg.length, SERVER_ADDR, SERVER_PORT);
		        
		        byte[] loginResp = new byte[1024];
		        DatagramPacket pkt = recvPacket(loginResp, loginResp.length);
		        if(pkt == null) continue;
		        resp = new String(loginResp, 0, pkt.getLength());
	        }
	        System.out.println("login success, welcome");
	        sHBThread = new HeartBeatThread(SERVER_ADDR, SERVER_PORT);
	        sHBThread.start();

        	System.out.println("*********************************************");
        	System.out.println("enter \"text:username\" to begin text chat");
        	System.out.println("enter \"audio:username\" to begin audio chat");
        	System.out.println("enter \"wait:text\" to wait for text connection");
        	System.out.println("enter \"wait:audio\" to wait for audio connection");
        	System.out.println("enter \"exit:username\" to logout");
        	System.out.println("*********************************************");
	        in = scanner.next();
			String command = in.split(":")[0];
			if(command.equals("text")) {
				byte[] connectMsg = in.getBytes();
				sendPacket(connectMsg, connectMsg.length, SERVER_ADDR, SERVER_PORT);
				
				p2pConnect(1);
			}
			else if(command.equals("audio")) {
				byte[] connectMsg = in.getBytes();
				sendPacket(connectMsg, connectMsg.length, SERVER_ADDR, SERVER_PORT);
				
				p2pConnect(2);
			}
			else if(command.equals("wait")) {
				String mod = in.split(":")[1];
				if(mod.equals("text")) p2pConnect(1);
				if(mod.equals("audio")) p2pConnect(2);
			}
			else if(command.equals("exit")) {
				logout(in);
			}		
		} catch(Exception e) {
			e.printStackTrace();
		}
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
			udp_socket.setSoTimeout(RECV_TIMEOUT);
			udp_socket.receive(pkt);
			return pkt;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			System.out.println("receive time out!!!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void digHole() {
		
		int cnt = 10;
		System.out.println("*******************dig hole*******************");
		try {
			while(cnt > 5) {
				Thread.sleep(10000);
				System.out.print(".");
				byte[] p2pMsg = new String("p2p_connection").getBytes();
				sendPacket(p2pMsg, p2pMsg.length, peer_addr, peer_port);
				cnt--;
			}
			InetAddress addr = SERVER_ADDR;
			while(addr.equals(SERVER_ADDR)) {
				byte[] peerMsg = new byte[1024];
				DatagramPacket pkt = recvPacket(peerMsg, peerMsg.length);
				addr = pkt.getAddress();
				int port = pkt.getPort();
				if(!addr.equals(peer_addr) || port != peer_port) {
					peer_addr = addr;
					peer_port = port;
				}
				System.out.print("ip: " + addr.getHostAddress() + " port: " + port);
			}
			while(cnt > 0) {
				Thread.sleep(10000);
				System.out.print(".");
				byte[] p2pMsg = new String("p2p_connection").getBytes();
				sendPacket(p2pMsg, p2pMsg.length, peer_addr, peer_port);
				cnt--;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\n********************finish********************");
	}
	
	public void p2pConnect(int mod) {
		try {
			String resp;
			while(true) {
				byte[] connectReq = new byte[1024];
				DatagramPacket pkt = recvPacket(connectReq, connectReq.length);
				if(pkt == null) continue;
				resp = new String(connectReq, 0, pkt.getLength());
//				System.out.println("resp: " + resp);
				if(pkt.getAddress().equals(SERVER_ADDR)) {
					if(resp.split(":")[0].equals("ip")) break;
				}
				
			}
			peer_addr = InetAddress.getByName(resp.split(":")[1]);
			peer_port = Integer.valueOf(resp.split(":")[3]);
			System.out.println("peer ip: " + peer_addr.getHostAddress() + " port: " + peer_port);
			
			digHole();
			
			pHBThread = new HeartBeatThread(peer_addr, peer_port);
			pHBThread.start();
			
			if(mod == 1) textChat(peer_addr, peer_port);
			else if(mod == 2) audioChat(peer_addr, peer_port);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void logout(String in) {
		byte[] logoutMsg = in.getBytes();
        sendPacket(logoutMsg, logoutMsg.length, SERVER_ADDR, SERVER_PORT);
        AudioUtils.close();
        if(cv != null) {
        	cv.interrupt();
        	cv = null;
        }
        if(pv != null) {
        	pv.interrupt();
        	pv = null;
        }
        if(stt != null) {
        	stt.interrupt();
        	stt = null;
        }
        if(rtt != null) {
        	rtt.interrupt();
        	rtt = null;
        }
        if(sHBThread != null) {
        	sHBThread.interrupt();
        	sHBThread = null;
        }
        if(pHBThread != null) {
        	pHBThread.interrupt();
        	pHBThread = null;
        }
	}

	private void audioChat(InetAddress peer_addr, int peer_port) {
		//建立p2p连接之后就可以互相发送语音了
		cv = new CaptureVoice(this);
		cv.start();
		pv = new PlayVoice(this);
		pv.start();
		String msg = scanner.next();
		String command = msg.split(":")[0];
		while(!command.equals("exit")) {
			msg = scanner.next();
			command = msg.split(":")[0];
		}
		logout(msg);
	}

	private void textChat(InetAddress peer_addr, int peer_port) {
		stt = new sendTextThread(peer_addr, peer_port);
		stt.start();
		rtt = new receiveTextThread();
		rtt.start();
	}
	
	class sendTextThread extends Thread {
		InetAddress addr;
		int port;
		
		public sendTextThread(InetAddress addr, int port) {
			this.addr = addr;
			this.port = port;
		}
		
		public void setAddrPort(InetAddress addr, int port) {
			this.addr = addr;
			this.port = port;
		}
		
		@Override
		public void run() {
			String msg = scanner.next();
			String command = msg.split(":")[0];
			while(!command.equals("exit")) {
				if(this.isInterrupted()) break;
				byte[] textMsg = msg.getBytes();
				sendPacket(textMsg, textMsg.length, addr, port);
				msg = scanner.next();
				command = msg.split(":")[0];
			}
			logout(msg);
		}
	}
	
	class receiveTextThread extends Thread {
		
		public receiveTextThread() {
			
		}
		
		@Override
		public void run() {
			while(true) {
				if(this.isInterrupted()) break;
				
				byte[] data = new byte[1024];
				
				DatagramPacket pkt = recvPacket(data, data.length);
				
				String s = new String(data, 0, pkt.getLength());
//				InetAddress addr = pkt.getAddress();
//				int port = pkt.getPort();
//				if(addr.equals(SERVER_ADDR) && port == SERVER_PORT) continue;
//				else {
//					if(!addr.equals(stt.addr) || port != stt.port) {
//						stt.setAddrPort(addr, port);
//						digHole(addr, port);
//					}
					if(s.equals("heartbeat") || s.equals("p2p_connection")) continue;
					System.out.println("对方说：" + s);
//				}
			}
		}
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