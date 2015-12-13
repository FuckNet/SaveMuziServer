import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;

class UserInfo extends Thread {
	private static enum NETSTATE {
		Login, Lobby, Room, Game
	};

	private Server server;
	private InputStream is;
	private OutputStream os;
	private DataInputStream dis;
	private DataOutputStream dos;
	private Socket user_socket;
	private Vector<UserInfo> users;
	private int playerNo;
	private BufferedWriter out; // �������
	private NETSTATE netState = NETSTATE.Login;

	private String userID;

	public String getUserID() {
		return userID;
	}

	public void setNetState(NETSTATE netState) {
		this.netState = netState;
	}

	NETSTATE getNetState() {
		return this.netState;
	}

	void changeUsers(Vector<UserInfo> users) {
		this.users = users;
	}

	UserInfo(Socket soc, Vector<UserInfo> vc, int playerNo, Server server) { // �����ڸ޼ҵ�
		// �Ű������� �Ѿ�� �ڷ� ����
		this.server = server;
		this.user_socket = soc;
		this.users = vc;
		this.playerNo = playerNo;

		User_network();
	}

	public void addUserInfo(String id, String pw) {
		Server.logInfo.put(id, pw);
		try {
			out = new BufferedWriter(new FileWriter(Server.FILE_NAME, true));
			out.write(id);
			out.newLine();
			out.write(pw);
			out.newLine();
			out.close();
		} catch (Exception e) {
			System.err.println(e);
			System.exit(1);
		}
	}

	void User_network() {
		try {
			is = user_socket.getInputStream();
			dis = new DataInputStream(is);
			os = user_socket.getOutputStream();
			dos = new DataOutputStream(os);

			// dos.writeInt(playerNo);
			server.textArea.append("Player NO. " + playerNo + " ����\n");
			server.textArea.setCaretPosition(server.textArea.getText().length());
		} catch (Exception e) {
			server.textArea.append("��Ʈ�� ���� ����\n");
			server.textArea.setCaretPosition(server.textArea.getText().length());
		}
	}

	void InMessage(String str) {
		server.textArea.append("����ڷκ��� ���� �޼��� : " + str + "\n");
		server.textArea.append(str + "\n");
		server.textArea.setCaretPosition(server.textArea.getText().length());
		// ����� �޼��� ó��
		broad_cast(str);
	}

	void broad_cast(String str) {
		for (int i = 0; i < users.size(); i++) {
			UserInfo imsi = users.elementAt(i);
			imsi.send_Message(str);
		}
	}

	void send_Message(String str) {
		try {
			// dos.writeUTF(str);
			byte[] bb;
			bb = str.getBytes();
			dos.write(bb); // .writeUTF(str);
			server.textArea.append("���� �޼��� : " + str + "\n");
		} catch (IOException e) {
			server.textArea.append("�޽��� �۽� ���� �߻�\n");
			server.textArea.setCaretPosition(server.textArea.getText().length());
		}
	}

	public void run() { // �޼��� �޾Ƽ� ó���ϴ� ������
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
					users.removeElement(this); // �������� ���� ��ü�� ���Ϳ��� �����
					server.textArea.append("���� ���Ϳ� ����� ����� �� : " + users.size() + "\n");
					server.textArea.append("����� ���� ������ �ڿ� �ݳ�\n");
					server.textArea.setCaretPosition(server.textArea.getText().length());
					server.playerCnt--;
					return;
				} catch (Exception ee) {

				} // catch�� ��
			} // �ٱ� catch����'
			msg = new String(buffer);
			msg = msg.trim();
			splitMsg = msg.split(" ");
			if (splitMsg.length < 1)
				return;
			server.textArea.append("�޼��� ���� : " + msg + "\n");
			switch (netState) {
			case Login:
				if (splitMsg[0].equals("/LOGIN")) {
					if (!Server.logInfo.containsKey(splitMsg[1])) {
						send_Message("/NONEXTID");
					} else if (!Server.logInfo.get(splitMsg[1]).equals(splitMsg[2])) {
						send_Message("/WRONGPW");
					} else {
						send_Message("/SUCCESSLOGIN " + splitMsg[1] + " " + splitMsg[2] + " " + server.rooms.size());
						netState = NETSTATE.Lobby;
						userID = splitMsg[1];
						send_Message(splitMsg[1] + "�� ȯ���մϴ�."); // ����� ����ڿ���
					}
				} else if (splitMsg[0].equals("/SIGNUP")) {
					if (Server.logInfo.containsKey(splitMsg[1])) {
						send_Message("/EXTID");
					} else {
						send_Message("/SUCCESSSIGNUP");
						addUserInfo(splitMsg[1], splitMsg[2]);
					}
				}
				break;
			case Lobby:
				if (splitMsg[0].equals("/CREATEROOM")) {
					broad_cast("/CREATEROOM");
					// �ڱ� �ڽ��� �������� �����ϰ� ���� ����.
					Room room = new Room();
					users.remove(this);
					room.addUser(this);
					server.addRoom(room);
					netState = NETSTATE.Room;
				} else if (splitMsg[0].equals("/ENTERROOM")) {
					int roomNumber = Integer.parseInt(splitMsg[1]);
					Room room = server.rooms.get(roomNumber - 1);
					Vector<UserInfo> tempUsers = room.getUsers();
					String tempStr = "/ENTERROOM";
					// �ڱ� �ڽ��� �������� �����ϰ� ������ �̵�.
					for (int i = 0; i < tempUsers.size(); i++) {
						tempUsers.get(i).send_Message("/ENTERUSER " + splitMsg[2]);
						tempStr += " " + tempUsers.get(i).getUserID();
					}
					users.remove(this);
					server.addUserToRoom(this, roomNumber - 1);
					
					send_Message(tempStr);
					netState = NETSTATE.Room;
				} else {
					broad_cast(msg);
				}
				break;
			case Room:
				if (splitMsg[0].equals("/START")) {
					for (int i = 0; i < users.size(); i++) {
						UserInfo imsi = users.elementAt(i);
						imsi.setNetState(NETSTATE.Game);
						msg = "/START" + " " + (i + 1) + " " + users.size() + " " + splitMsg[1];
						imsi.send_Message(msg);
					}
					server.textArea.append("���� ����!\n");
				} else {
					broad_cast(msg);
				}
				break;
			case Game:
				// try {
				// // ����ڿ��� �޴� �޼���
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
				// vc.removeElement(this); // �������� ���� ��ü�� ���Ϳ��� �����
				// textArea.append("���� ���Ϳ� ����� ����� �� : " + vc.size() +
				// "\n");
				// textArea.append("����� ���� ������ �ڿ� �ݳ�\n");
				// textArea.setCaretPosition(textArea.getText().length());
				// playerNum--;
				// return;
				// } catch (Exception ee) {
				//
				// } // catch�� ��
				// } // �ٱ� catch����

				break;
			}
		} // run�޼ҵ� ��
	} // ���� userinfoŬ������
}