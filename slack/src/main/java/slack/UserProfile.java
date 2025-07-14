package slack;

public class UserProfile {
    private String username;
    private String email;

    public UserProfile(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    // Setters (optional, but good for JavaBeans pattern)
    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
               "username='" + username + '\'' +
               ", email='" + email + '\'' +
               '}';
    }
}