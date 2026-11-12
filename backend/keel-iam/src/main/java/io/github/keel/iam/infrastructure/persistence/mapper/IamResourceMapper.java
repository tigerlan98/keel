package io.github.keel.iam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamResourcePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * IAM 资源 Mapper。
 */
@Mapper
public interface IamResourceMapper extends BaseMapper<IamResourcePO> {

    @Select("""
            SELECT DISTINCT res.code
            FROM iam_user_role ur
            JOIN iam_role r ON r.id = ur.role_id AND r.status = 'ENABLED'
            JOIN iam_role_resource rr ON rr.role_id = r.id
            JOIN iam_resource res ON res.id = rr.resource_id AND res.status = 'ENABLED'
            WHERE ur.user_id = #{userId}
              AND res.type IN ('MENU', 'BUTTON')
            """)
    List<String> selectPermissionCodes(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT rr.resource_id
            FROM iam_user_role ur
            JOIN iam_role r ON r.id = ur.role_id AND r.status = 'ENABLED'
            JOIN iam_role_resource rr ON rr.role_id = r.id
            JOIN iam_resource res ON res.id = rr.resource_id AND res.status = 'ENABLED'
            WHERE ur.user_id = #{userId}
            """)
    List<Long> selectGrantedResourceIds(@Param("userId") Long userId);
}
