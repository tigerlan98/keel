package io.github.keel.iam.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.keel.common.persistence.entity.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * IAM 用户持久化对象。
 */
@Getter
@Setter
@TableName("iam_user")
public class IamUserPO extends BasePO {

    private String username;

    private String passwordHash;

    private String nickname;

    private String email;

    private String phone;

    private String status;

    private Boolean mustChangePassword;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    @Version
    private Integer version;
}
