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
 * 操作日志持久化对象（只追加）。
 */
@Getter
@Setter
@TableName("aud_operation_log")
public class OperationLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;

    private Long userId;

    private String username;

    private String module;

    private String action;

    private String method;

    private String requestMethod;

    private String requestUri;

    private String requestParams;

    private Boolean success;

    private String errorMessage;

    private String ip;

    private String userAgent;

    private Integer durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
