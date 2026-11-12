package io.github.keel.iam.application.authentication;

/**
 * 登录设备类型，同时决定登录日志与 token 归属的 loginType。
 */
public enum Device {

    WEB("ADMIN"),
    APP("APP"),
    MINI("MINI");

    private final String loginType;

    Device(String loginType) {
        this.loginType = loginType;
    }

    public String loginType() {
        return loginType;
    }
}
