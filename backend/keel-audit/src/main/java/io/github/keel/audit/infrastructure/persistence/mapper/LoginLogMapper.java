package io.github.keel.audit.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.keel.audit.infrastructure.persistence.po.LoginLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper。
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLogPO> {
}
