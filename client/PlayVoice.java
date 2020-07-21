package p2pclient;

import java.net.DatagramPacket;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class PlayVoice extends Thread{
	private Client client;
	
	public PlayVoice(Client client) {
		this.client = client;
	}

	@Override
	public void run() {
		// TODO 接收udp数据包并播放音频
		try {
			SourceDataLine sdl = AudioUtils.getSourceDataLine();
			while(true) {
				if(this.isInterrupted()) break;
				
				byte[] data = new byte[1024];
				
				DatagramPacket pkt = client.recvPacket(data, data.length);
				String s = new String(data, 0, pkt.getLength());
				if(s.equals("heartbeat") || s.equals("p2p_connection")) continue;
				int len = (pkt.getLength() / 2) * 2;
				if(len != 0) {
					sdl.write(data, 0, len);
				}
			}
		} catch(LineUnavailableException e) {
			e.printStackTrace();
		}
	}
}
