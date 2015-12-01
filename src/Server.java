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
	private JButton startBtn; // ������ �����Ų ��ư
	private JTextArea textArea; // Ŭ���̾�Ʈ �� ���� �޽��� ���
	private int playerNum=0;
	private ServerSocket socket; //��������
	private Socket soc; // ������� 

	private Vector vc = new Vector(); // ����� ����ڸ� ������ ����

	public static void main(String[] args) {
		Server frame = new Server();
		frame.setVisible(true);	
	}

	public Server() {
		init();
	}

	private void init() { // GUI�� �����ϴ� �޼ҵ�
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

		startBtn = new JButton("���� ����");
		Myaction action = new Myaction();
		startBtn.addActionListener(action); // ����Ŭ������ �׼� �����ʸ� ��ӹ��� Ŭ������
		startBtn.setBounds(0, 325, 264, 37);
		contentPane.add(startBtn);
		textArea.setEditable(false); // textArea�� ����ڰ� ���� ���ϰԲ� ���´�.	
	}
	
	class Myaction implements ActionListener { // ����Ŭ������ �׼� �̺�Ʈ ó�� Ŭ����
		@Override
		public void actionPerformed(ActionEvent e) {
			// �׼� �̺�Ʈ�� sendBtn�϶� �Ǵ� textField ���� Enter key ġ��
			if (e.getSource() == startBtn)
				server_start();	
		}

	}
	private void server_start() {
		try {
			socket = new ServerSocket(PORT); // ������ ��Ʈ ���ºκ�
			startBtn.setText("����������");
			startBtn.setEnabled(false); // ������ ���̻� �����Ű�� �� �ϰ� ���´�
			
			if(socket!=null) { // socket �� ���������� ��������
				Connection();
			}
		} catch (IOException e) {
			textArea.append("������ �̹� ������Դϴ�...\n");

		}

	}

	private void Connection() {
		Thread th = new Thread(new Runnable() { // ����� ������ ���� ������
			@Override
			public void run() {
				while (true) { // ����� ������ ����ؼ� �ޱ� ���� while��
					try {
						textArea.append("����� ���� �����...\n");
						soc = socket.accept(); // accept�� �Ͼ�� �������� ���� �����
						playerNum++;
						textArea.append("����� ����!!\n");
						UserInfo user = new UserInfo(soc, vc, playerNum); // ����� ���� ������ �ݹ� ������Ƿ�, user Ŭ���� ���·� ��ü ����
	                                // �Ű������� ���� ����� ���ϰ�, ���͸� ��Ƶд�
						vc.add(user); // �ش� ���Ϳ� ����� ��ü�� �߰�
						
						user.start(); // ���� ��ü�� ������ ����
					} catch (IOException e) {
						textArea.append("!!!! accept ���� �߻�... !!!!\n");
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

		public UserInfo(Socket soc, Vector vc, int playerNo) { // �����ڸ޼ҵ�
			// �Ű������� �Ѿ�� �ڷ� ����
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

				//Nickname = dis.readUTF(); // ������� �г��� �޴ºκ�
				//byte[] b=new byte[128];
				//String Nickname = new String(b);
				//Nickname = Nickname.trim();
				dos.writeInt(playerNo);
				textArea.append("Player NO. " + playerNo + " ����\n");
				textArea.setCaretPosition(textArea.getText().length());	
			} catch (Exception e) {
				textArea.append("��Ʈ�� ���� ����\n");
				textArea.setCaretPosition(textArea.getText().length());
			}
		}

		public void InMessage(String str) {
			textArea.append("����ڷκ��� ���� �޼��� : " + str+"\n");
			textArea.append(str + "\n");
			textArea.setCaretPosition(textArea.getText().length());
			// ����� �޼��� ó��
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
				textArea.append("�޽��� �۽� ���� �߻�\n");	
				textArea.setCaretPosition(textArea.getText().length());
			}
		}

		public void run() { // ������ ����
			// ���� ��
			while (true) {
				try {
					// ����ڿ��� �޴� �޼���
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
						textArea.append("���� ����!\n");
						break;
					}
				}
				catch (IOException e) { 
					try {
						dos.close();
						dis.close();
						user_socket.close();
						vc.removeElement( this ); // �������� ���� ��ü�� ���Ϳ��� �����
						textArea.append("���� ���Ϳ� ����� ����� �� : " + vc.size() + "\n");
						textArea.append("����� ���� ������ �ڿ� �ݳ�\n");
						textArea.setCaretPosition(textArea.getText().length());
						playerNum--;
						return;
					} catch (Exception ee) {
					
					}// catch�� ��
				}// �ٱ� catch����
			}
			
			// ���� ��
			while (true) {
				try {
					// ����ڿ��� �޴� �޼���
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
						vc.removeElement(this); // �������� ���� ��ü�� ���Ϳ��� �����
						textArea.append("���� ���Ϳ� ����� ����� �� : " + vc.size() + "\n");
						textArea.append("����� ���� ������ �ڿ� �ݳ�\n");
						textArea.setCaretPosition(textArea.getText().length());
						playerNum--;
						return;
					} catch (Exception ee) {
					
					}// catch�� ��
				}// �ٱ� catch����
			}			
		}// run�޼ҵ� ��
	} // ���� userinfoŬ������
}
