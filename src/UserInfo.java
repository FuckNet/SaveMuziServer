import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;

class UserInfo extends Thread {
	private static enum NETSTATE {
		Lobby, Room, Game
	};
	private Server server;
	private InputStream is;
	private OutputStream os;
	private DataInputStream dis;
	private DataOutputStream dos;
	private Socket user_socket;
	private Vector<UserInfo> users;
	private int playerNo;

	private NETSTATE netState = NETSTATE.Lobby;

	public void setNetState(NETSTATE netState) {
		this.netState = netState;
	}

	NETSTATE getNetState() {
		return this.netState;
	}

	void changeUsers(Vector<UserInfo> users) {
		this.users = users;
	}
	
	UserInfo(Socket soc, Vector<UserInfo> vc, int playerNo, Server server) { // 생성자메소드
		// 매개변수로 넘어온 자료 저장
		this.server = server;
		this.user_socket = soc;
		this.users = vc;
		this.playerNo = playerNo;

		User_network();
	}

	void User_network() {
		try {
			is = user_socket.getInputStream();
			dis = new DataInputStream(is);
			os = user_socket.getOutputStream();
			dos = new DataOutputStream(os);

			//dos.writeInt(playerNo);
			server.textArea.append("Player NO. " + playerNo + " 접속\n");
			server.textArea.setCaretPosition(server.textArea.getText().length());
		} catch (Exception e) {
			server.textArea.append("스트림 셋팅 에러\n");
			server.textArea.setCaretPosition(server.textArea.getText().length());
		}
	}

	void InMessage(String str) {
		server.textArea.append("사용자로부터 들어온 메세지 : " + str + "\n");
		server.textArea.append(str + "\n");
		server.textArea.setCaretPosition(server.textArea.getText().length());
		// 사용자 메세지 처리
		broad_cast(str);
	}

	void broad_cast(String str) {
		for (int i = 0; i < users.size(); i++) {
			UserInfo imsi = users.elementAt(i);
			imsi.send_Message(str);
			server.textArea.append("/CREATEROOM 보냄 to " + i + "\n");
		}
	}

	void send_Message(String str) {
		try {
			// dos.writeUTF(str);
			byte[] bb;
			bb = str.getBytes();
			dos.write(bb); // .writeUTF(str);
		} catch (IOException e) {
			server.textArea.append("메시지 송신 에러 발생\n");
			server.textArea.setCaretPosition(server.textArea.getText().length());
		}
	}

	public void run() { // 메세지 받아서 처리하는 쓰레드
		while (true) {
			byte[] buffer = new byte[128];
			String msg;
			String splitMsg[];
			try {
			dis.read(buffer);
			} catch (IOException e) {
				try {
					dos.close();
					dis.close();
					user_socket.close();
					users.removeElement(this); // 에러가난 현재 객체를 벡터에서 지운다
					server.textArea.append("현재 벡터에 담겨진 사용자 수 : " + users.size() + "\n");
					server.textArea.append("사용자 접속 끊어짐 자원 반납\n");
					server.textArea.setCaretPosition(server.textArea.getText().length());
					server.playerCnt--;
					return;
				} catch (Exception ee) {

				} // catch문 끝
			} // 바깥 catch문끝'
			msg = new String(buffer);
			msg = msg.trim();
			splitMsg = msg.split(" ");
			if (splitMsg.length < 1) return;
			switch (netState) {
			case Lobby:
				if (splitMsg[0].equals("/CREATEROOM")) {
					broad_cast("/CREATEROOM");
					// 자기 자신을 서버에서 제거하고 룸을 생성.
					Room room = new Room();
					users.remove(this);
					room.addUser(this);
					server.addRoom(room);
					netState = NETSTATE.Room;					
				}
				else if(splitMsg[0].equals("/ENTERROOM")) {
					// 자기 자신을 서버에서 제거하고 룸으로 이동.
					users.remove(this);
					server.addUserToRoom(this, Integer.parseInt(splitMsg[1])-1);
					netState = NETSTATE.Room;
				}
				break;
			case Room:
				if (splitMsg[0].equals("/START")) {
					for (int i = 0; i < users.size(); i++) {
						UserInfo imsi = users.elementAt(i);
						imsi.setNetState(NETSTATE.Game);
						msg = "/START" + " " + (i+1) + " " + users.size() + " "+ splitMsg[1];
						imsi.send_Message(msg);
					}
					server.textArea.append("게임 시작!\n");
				}
				break;
			case Game:
				// try {
				// // 사용자에게 받는 메세지
				// byte[] b = new byte[128];
				// dis.read(b);
				// String msg = new String(b);
				// msg = msg.trim();
				// if(msg.equals("/PING")) {
				// System.out.println(playerNo + " " +
				// System.currentTimeMillis());
				// continue;
				// }
				// InMessage(msg);
				// } catch (IOException e) {
				// try {
				// dos.close();
				// dis.close();
				// user_socket.close();
				// vc.removeElement(this); // 에러가난 현재 객체를 벡터에서 지운다
				// textArea.append("현재 벡터에 담겨진 사용자 수 : " + vc.size() +
				// "\n");
				// textArea.append("사용자 접속 끊어짐 자원 반납\n");
				// textArea.setCaretPosition(textArea.getText().length());
				// playerNum--;
				// return;
				// } catch (Exception ee) {
				//
				// } // catch문 끝
				// } // 바깥 catch문끝

				break;
			}
		} // run메소드 끝
	} // 내부 userinfo클래스끝
}