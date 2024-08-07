package com.example.myapplication;

public class UserData {
    public static class SignupRequest {
        private String username;
        private String password;
        private String email;

        private String name;

        public SignupRequest(String username, String password, String email) {
            this.name= username;
            this.password=password;
            this.email=email;
        }

        // Constructors, getters, and setters
    }

    public static class LoginRequest {
        private String username;
        private String password;
        private String email;

        public LoginRequest(String username, String password) {
            this.email=username;
            this.password=password;
        }

        // Constructors, getters, and setters
    }

    public class ApiResponse {
        private boolean success;
        private String message;

        public String getMessage() {
            return message;
        }

        // Constructors, getters, and setters
    }

    public static class UserDataPhone {
        private String name;
        private String email;
        private String password;
        private String phone_number;
        private String time_period;
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
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
