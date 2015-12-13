import java.util.Vector;

public class Room {
   private Vector<UserInfo> users;
   Room() {
      users = new Vector<UserInfo>();
   }
   void addUser(UserInfo user) {
      users.add(user);
      user.changeUsers(users);
   }
   public Vector<UserInfo> getUsers() {
      return users;
   }
}