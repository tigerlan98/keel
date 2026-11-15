package io.github.keel.iam.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import io.github.keel.iam.infrastructure.persistence.po.IamRolePO;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** IAM 角色 Mapper。 */
@Mapper
public interface IamRoleMapper extends BaseMapper<IamRolePO> {
    @Select(
            """
<script>
SELECT ur.user_id, r.id, r.code, r.name, r.status
FROM iam_user_role ur JOIN iam_role r ON r.id = ur.role_id
WHERE ur.user_id IN
<foreach collection="userIds" item="userId" open="(" separator="," close=")">#{userId}</foreach>
ORDER BY ur.user_id, r.sort, r.id
</script>
""")
    List<UserRoleProjection> rolesByUsers(@Param("userIds") List<Long> userIds);
}
