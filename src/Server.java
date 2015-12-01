// Java Chatting Server

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class Server extends JFrame {
	private static final int PORT = 30000;
	private JPanel contentPane;
	private JButton startBtn; // 서버를 실행시킨 버튼
	private JTextArea textArea; // 클라이언트 및 서버 메시지 출력
	private int playerNum=0;
	private ServerSocket socket; //서버소켓
	private Socket soc; // 연결소켓 

	private Vector vc = new Vector(); // 연결된 사용자를 저장할 벡터

	public static void main(String[] args) {
		Server frame = new Server();
		frame.setVisible(true);	
	}

	public Server() {
		init();
	}

	private void init() { // GUI를 구성하는 메소드
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 280, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JScrollPane js = new JScrollPane();
				
		textArea = new JTextArea();
		textArea.setColumns(20);
		textArea.setRows(5);
		js.setBounds(0, 0, 264, 320);
		contentPane.add(js);
		js.setViewportView(textArea);

		startBtn = new JButton("서버 실행");
		Myaction action = new Myaction();
		startBtn.addActionListener(action); // 내부클래스로 액션 리스너를 상속받은 클래스로
		startBtn.setBounds(0, 325, 264, 37);
		contentPane.add(startBtn);
		textArea.setEditable(false); // textArea를 사용자가 수정 못하게끔 막는다.	
	}
	
	class Myaction implements ActionListener { // 내부클래스로 액션 이벤트 처리 클래스
		@Override
		public void actionPerformed(ActionEvent e) {
			// 액션 이벤트가 sendBtn일때 또는 textField 에세 Enter key 치면
			if (e.getSource() == startBtn)
				server_start();	
		}

	}
	private void server_start() {
		try {
			socket = new ServerSocket(PORT); // 서버가 포트 여는부분
			startBtn.setText("서버실행중");
			startBtn.setEnabled(false); // 서버를 더이상 실행시키지 못 하게 막는다
			
			if(socket!=null) { // socket 이 정상적으로 열렸을때
				Connection();
			}
		} catch (IOException e) {
			textArea.append("소켓이 이미 사용중입니다...\n");

		}

	}

	private void Connection() {
		Thread th = new Thread(new Runnable() { // 사용자 접속을 받을 스레드
			@Override
			public void run() {
				while (true) { // 사용자 접속을 계속해서 받기 위해 while문
					try {
						textArea.append("사용자 접속 대기중...\n");
						soc = socket.accept(); // accept가 일어나기 전까지는 무한 대기중
						playerNum++;
						textArea.append("사용자 접속!!\n");
						UserInfo user = new UserInfo(soc, vc, playerNum); // 연결된 소켓 정보는 금방 사라지므로, user 클래스 형태로 객체 생성
	                                // 매개변수로 현재 연결된 소켓과, 벡터를 담아둔다
						vc.add(user); // 해당 벡터에 사용자 객체를 추가
						
						user.start(); // 만든 객체의 스레드 실행
					} catch (IOException e) {
						textArea.append("!!!! accept 에러 발생... !!!!\n");
					} 
				}
			}
		});
		th.start();
	}

	class UserInfo extends Thread {
		private InputStream is;
		private OutputStream os;
		private DataInputStream dis;
		private DataOutputStream dos;
		private Socket user_socket;
		private Vector user_vc;
		private String Nickname = "";
		private int playerNo;

		public UserInfo(Socket soc, Vector vc, int playerNo) { // 생성자메소드
			// 매개변수로 넘어온 자료 저장
			this.user_socket = soc;
			this.user_vc = vc;
			this.playerNo = playerNo;

			User_network();
		}

		public void User_network() {
			try {
				is = user_socket.getInputStream();
				dis = new DataInputStream(is);
				os = user_socket.getOutputStream();
				dos = new DataOutputStream(os);

				//Nickname = dis.readUTF(); // 사용자의 닉네임 받는부분
				//byte[] b=new byte[128];
				//String Nickname = new String(b);
				//Nickname = Nickname.trim();
				dos.writeInt(playerNo);
				textArea.append("Player NO. " + playerNo + " 접속\n");
				textArea.setCaretPosition(textArea.getText().length());	
			} catch (Exception e) {
				textArea.append("스트림 셋팅 에러\n");
				textArea.setCaretPosition(textArea.getText().length());
			}
		}

		public void InMessage(String str) {
			textArea.append("사용자로부터 들어온 메세지 : " + str+"\n");
			textArea.append(str + "\n");
			textArea.setCaretPosition(textArea.getText().length());
			// 사용자 메세지 처리
			broad_cast(str);
		}

		public void broad_cast(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserInfo imsi = (UserInfo) user_vc.elementAt(i);
				imsi.send_Message(str);
			}
		}

		public void send_Message(String str) {
			try {
				//dos.writeUTF(str);
				byte[] bb;		
				bb = str.getBytes();
				dos.write(bb); //.writeUTF(str);
			} 
			catch (IOException e) {
				textArea.append("메시지 송신 에러 발생\n");	
				textArea.setCaretPosition(textArea.getText().length());
			}
		}

		public void run() { // 스레드 정의
			// 게임 전
			while (true) {
				try {
					// 사용자에게 받는 메세지
					byte[] b = new byte[128];
					dis.read(b);
					String msg = new String(b);
					msg = msg.trim();
					String splitMsg[];
					splitMsg = msg.split(" ");
					if(splitMsg.length < 1)
						return;
					if(splitMsg[0].equals("/START")) {
						msg = "/START "+playerNum;
						InMessage(msg);
						textArea.append("게임 시작!\n");
						break;
					}
				}
				catch (IOException e) { 
					try {
						dos.close();
						dis.close();
						user_socket.close();
						vc.removeElement( this ); // 에러가난 현재 객체를 벡터에서 지운다
						textArea.append("현재 벡터에 담겨진 사용자 수 : " + vc.size() + "\n");
						textArea.append("사용자 접속 끊어짐 자원 반납\n");
						textArea.setCaretPosition(textArea.getText().length());
						playerNum--;
						return;
					} catch (Exception ee) {
					
					}// catch문 끝
				}// 바깥 catch문끝
			}
			
			// 게임 중
			while (true) {
				try {
					// 사용자에게 받는 메세지
					byte[] b = new byte[128];
					dis.read(b);
					String msg = new String(b);
					msg = msg.trim();
					InMessage(msg);
				} 
				catch (IOException e) { 
					try {
						dos.close();
						dis.close();
						user_socket.close();
						vc.removeElement(this); // 에러가난 현재 객체를 벡터에서 지운다
						textArea.append("현재 벡터에 담겨진 사용자 수 : " + vc.size() + "\n");
						textArea.append("사용자 접속 끊어짐 자원 반납\n");
						textArea.setCaretPosition(textArea.getText().length());
						playerNum--;
						return;
					} catch (Exception ee) {
					
					}// catch문 끝
				}// 바깥 catch문끝
			}			
		}// run메소드 끝
	} // 내부 userinfo클래스끝
}
