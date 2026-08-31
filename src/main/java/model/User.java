// Decompiled with: VMN
package model;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import io.Session;
import server.Server;

public class User {

    private Session session;
    private int userID;
    private String username;
    private String password;
    private int serverID;
    private int clientID;
    private boolean admin;
    private boolean actived;
    private int goldBar;
    private long lastTimeLogin;
    private long lastTimeLogout;
    private String rewards;
    private int ruby;
    private int mocNap;
    private int server;
    private int isUseMaBaoVe;
    private int MaBaoVe;
    private int tongnap;
    private int vndBar;

    public User(String username, String password, int serverID, int clientID, Session session) {
        this.username = username;
        this.password = password;
        this.serverID = serverID;
        this.clientID = clientID;
        this.session = session;
    }

    public boolean login() {
        System.out.println("[LOGIN] username='" + this.username + "' password='" + this.password + "' serverID=" + this.serverID);
        try {
            MongoCollection<Document> collection = db.MongoDBConnection.getDatabase().getCollection("account");
            
            // Tìm theo username trước để xem account có tồn tại không
            Document byUsername = collection.find(Filters.eq("username", this.username)).first();
            if (byUsername == null) {
                System.out.println("[LOGIN] FAIL - Khong tim thay username: '" + this.username + "' trong database");
            } else {
                System.out.println("[LOGIN] Tim thay username: '" + this.username + "', password trong DB: '" + byUsername.get("password") + "', password nhap vao: '" + this.password + "'");
            }
            
            Document rs = collection.find(Filters.and(
                Filters.eq("username", this.username),
                Filters.or(
                    Filters.eq("password", this.password),
                    Filters.eq("password", this.password.matches("-?\\d+") ? Integer.parseInt(this.password) : this.password)
                )
            )).first();
            
            if (rs != null) {
                System.out.println("[LOGIN] Username+Password khop! Dang xu ly dang nhap...");
                int waitLogin;
                this.userID = rs.getInteger("id", 0);
                int serverLogin = rs.getInteger("server_login", 0);
                System.out.println("[LOGIN] userID=" + this.userID + " serverLogin=" + serverLogin + " requestedServer=" + this.serverID);
                if (serverLogin != this.serverID) {
                    System.out.println("[LOGIN] FAIL - Server khong khop: account thuoc SV" + serverLogin + " nhung request SV" + this.serverID);
                    this.session.getService().loginFailed(this.clientID, "Account nay thuoc may chu SV" + serverLogin);
                    boolean bl = false;
                    return bl;
                }
                User us = UserManager.getInstance().find(this.userID);
                if (us != null) {
                    System.out.println("[LOGIN] FAIL - User dang online, kick cu...");
                    us.disconnect();
                    this.session.getService().loginFailed(this.clientID, "Đăng nhập thất bại, vui lòng đăng nhập lại!");
                    boolean bl = false;
                    return bl;
                }
                // last_time_login/logout stored as String from MySQL migration, parse safely
                Object lastLoginObj = rs.get("last_time_login");
                this.lastTimeLogin = (lastLoginObj instanceof java.util.Date) ? ((java.util.Date) lastLoginObj).getTime() : 0L;
                Object lastLogoutObj = rs.get("last_time_logout");
                this.lastTimeLogout = (lastLogoutObj instanceof java.util.Date) ? ((java.util.Date) lastLogoutObj).getTime() : 0L;
                // is_admin stored as Integer 0/1 from MySQL migration
                Object isAdminObj = rs.get("is_admin");
                this.admin = isAdminObj != null && (isAdminObj.equals(1) || isAdminObj.equals(true));
                System.out.println("[LOGIN] is_admin=" + isAdminObj + " -> admin=" + this.admin);
                int secondsPass = (int) ((System.currentTimeMillis() - this.lastTimeLogout) / 1000L);
                if (secondsPass < (waitLogin = Server.getInstance().getConfig().getSecondWaitLogin())) {
                    System.out.println("[LOGIN] FAIL - Chua het thoi gian cho: " + secondsPass + "s / " + waitLogin + "s");
                    this.session.getService().loginFailed(this.clientID,
                            "Vui lòng chờ " + (waitLogin - secondsPass) + " giây để đăng nhập lại.");
                    boolean bl = false;
                    return bl;
                }
                // active stored as Integer 0/1 from MySQL migration
                Object activeObj = rs.get("active");
                this.actived = activeObj != null && (activeObj.equals(1) || activeObj.equals(true));
                this.goldBar = rs.getInteger("thoi_vang", 0);
                this.rewards = rs.getString("reward");
                // this.ruby = rs.getInteger("ruby", 0);
                // this.mocNap = rs.getInteger("count_card", 0);
                this.server = rs.getInteger("server_login", 0);
                // this.isUseMaBaoVe = rs.getInteger("isUseMaBaoVe", 0);
                // this.MaBaoVe = rs.getInteger("MaBaoVe", 0);
                this.tongnap = rs.getInteger("tongnap", 0);
                this.vndBar = rs.getInteger("vnd", 0);
                // ban stored as Integer 0/1 from MySQL migration
                Object banObj = rs.get("ban");
                boolean ban = banObj != null && (banObj.equals(1) || banObj.equals(true));
                System.out.println("[LOGIN] active=" + this.actived + " ban=" + ban + " testmode=" + Server.getInstance().getConfig().getTestmode());
                if (!this.admin && Server.getInstance().getConfig().getTestmode() == 1) {
                    System.out.println("[LOGIN] FAIL - Server dang o testmode, chi admin moi login duoc");
                    this.session.getService().loginFailed(this.clientID,
                            "Server đang được admin xử lý và kiểm tra lại,vui lòng quay lại sau");
                    boolean bl = false;
                    return bl;
                }
                if (ban) {
                    System.out.println("[LOGIN] FAIL - Tai khoan bi ban");
                    this.session.getService().loginFailed(this.clientID, "Tài khoản đã bị khóa do vi phạm điều khoản!");
                    boolean bl = false;
                    return bl;
                }
                System.out.println("[LOGIN] SUCCESS - Dang nhap thanh cong: username=" + this.username + " userID=" + this.userID);
                this.session.getService().loginSuccessful(this);
                boolean bl = true;
                return bl;
            }
            this.session.getService().loginFailed(this.clientID, "Thông tin tài khoản hoặc mật khẩu không chính xác");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        this.session.getService().disconnect(this.userID);
        UserManager.getInstance().remove(this);
    }

    public Session getSession() {
        return this.session;
    }

    public int getUserID() {
        return this.userID;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public int getServerID() {
        return this.serverID;
    }

    public int getClientID() {
        return this.clientID;
    }

    public boolean isAdmin() {
        return this.admin;
    }

    public boolean isActived() {
        return this.actived;
    }

    public int getisUseMaBaoVe() {
        return this.isUseMaBaoVe;
    }

    public int getMaBaoVe() {
        return this.MaBaoVe;
    }

    public int getGoldBar() {
        return this.goldBar;
    }

    public int getVndBar() {
        return this.vndBar;
    }

    public int getTongNap() {
        return this.tongnap;
    }

    public long getLastTimeLogin() {
        return this.lastTimeLogin;
    }

    public long getLastTimeLogout() {
        return this.lastTimeLogout;
    }

    public String getRewards() {
        return this.rewards;
    }

    public int getRuby() {
        return this.ruby;
    }

    public int getMocNap() {
        return this.mocNap;
    }

    public int getServer() {
        return this.server;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setServerID(int serverID) {
        this.serverID = serverID;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public void setActived(boolean actived) {
        this.actived = actived;
    }

    public void setGoldBar(int goldBar) {
        this.goldBar = goldBar;
    }

    public void setVndBar(int vndBar) {
        this.vndBar = vndBar;
    }

    public void setTongNap(int tongnap) {
        this.tongnap = tongnap;
    }

    public void setisUseMaBaoVe(int isUseMaBaoVe) {
        this.isUseMaBaoVe = isUseMaBaoVe;
    }

    public void setMaBaoVe(int MaBaoVe) {
        this.MaBaoVe = MaBaoVe;
    }

    public void setLastTimeLogin(long lastTimeLogin) {
        this.lastTimeLogin = lastTimeLogin;
    }

    public void setLastTimeLogout(long lastTimeLogout) {
        this.lastTimeLogout = lastTimeLogout;
    }

    public void setRewards(String rewards) {
        this.rewards = rewards;
    }

    public void setRuby(int ruby) {
        this.ruby = ruby;
    }

    public void setMocNap(int mocNap) {
        this.mocNap = mocNap;
    }

    public void setServer(int server) {
        this.server = server;
    }
}
