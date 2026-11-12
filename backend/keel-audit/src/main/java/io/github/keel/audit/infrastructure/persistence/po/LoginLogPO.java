package io.github.keel.audit.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 登录日志持久化对象（只追加）。
 */
@Getter
@Setter
@TableName("aud_login_log")
public class LoginLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;

    private Long userId;

    private String username;

    private String action;

    private String loginType;

    private String device;

    private String ip;

    private String userAgent;

    private Boolean success;

    private String failureReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
