package p2pclient;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class CaptureVoice extends Thread {
	private Client client;
	
	public CaptureVoice(Client client) {
		this.client = client;
	}

	@Override
	public void run() {
		// TODO 捕获音频并发送udp数据包
		try {
			TargetDataLine tdl = AudioUtils.getTargetDataLine();
			while(true) {
				if(this.isInterrupted()) break;
				
				byte[] data = new byte[1024];
				int numBytesRead = 0;

				numBytesRead = tdl.read(data, 0, 128);
				client.sendPacket(data, numBytesRead, client.peer_addr, client.peer_port);
			}
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
