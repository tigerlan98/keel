package io.github.keel.audit.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.keel.audit.infrastructure.persistence.po.LoginLogPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 登录日志 Mapper。
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLogPO> {

    /**
     * 分批删除保留期之前的日志，避免长事务。
     */
    @Delete("DELETE FROM aud_login_log WHERE created_at < #{cutoff} LIMIT #{limit}")
    int deleteBefore(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
