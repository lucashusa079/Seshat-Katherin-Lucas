package com.lucas.sashat.ui.home;

public class ImgurResponse {

    private ImgurData data;
    private boolean success;
    private int status;

    public ImgurData getData() {
        return data;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public static class ImgurData {
        private String link;

        public String getLink() {
            return link;
        }
    }
}
