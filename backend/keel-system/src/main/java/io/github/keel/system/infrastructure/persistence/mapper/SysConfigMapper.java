package io.github.keel.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.keel.system.infrastructure.persistence.po.SysConfigPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统参数 Mapper。
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfigPO> {
}
