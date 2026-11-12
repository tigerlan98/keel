package io.github.keel.iam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamResourcePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * IAM 资源 Mapper。
 */
@Mapper
public interface IamResourceMapper extends BaseMapper<IamResourcePO> {
}
