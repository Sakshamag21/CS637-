package com.example.myapplication;

public class UserData {

    // Signup request model
    public static class SignupRequest {
        private String username;
        private String password;
        private String email;
        private String name;

        public SignupRequest(String username, String password, String email) {
            this.username = username;
            this.password = password;
            this.email = email;
            this.name = username; // Name is initialized with the username
        }

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    // Login request model
    public static class LoginRequest {
        private String email;
        private String password;

        public LoginRequest(String username, String password) {

            this.email = username;
            this.password = password;
        }

        // Getters and Setters
        public String getUsername() {
            return email;
        }

        public void setUsername(String username) {
            this.email = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // API response model
    public class ApiResponse {
        private boolean success;
        private String message;
        private String access;  // Add this for access token
        private String refresh; // Add this for refresh token

        public String getMessage() {
            return message;
        }

        public String getAccess() {
            return access; // Getter for access token
        }

        public String getRefresh() {
            return refresh; // Getter for refresh token
        }

        @Override
        public String toString() {
            return "ApiResponse{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", access='" + access + '\'' +
                    ", refresh='" + refresh + '\'' +
                    '}';
        }
    }


    // User data with phone number model
    public static class UserDataPhone {
        private String name;
        private String email;
        private String password;
        private String phone_number;
        private String time_period;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPhone_number() {
            return phone_number;
        }

        public void setPhone_number(String phone_number) {
            this.phone_number = phone_number;
        }

        public String getTime_period() {
            return time_period;
        }

        public void setTime_period(String time_period) {
            this.time_period = time_period;
        }
    }
}
