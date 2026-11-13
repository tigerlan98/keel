package io.github.keel.system.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.keel.common.persistence.entity.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统参数持久化对象。
 */
@Getter
@Setter
@TableName("sys_config")
public class SysConfigPO extends BasePO {

    private String configKey;

    private String configValue;

    private String configGroup;

    private Boolean publicFlag;

    private String description;

    @Version
    private Integer version;
}
