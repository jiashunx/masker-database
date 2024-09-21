package io.github.jiashunx.masker.database.sample.mapper;

import io.github.jiashunx.masker.database.sample.entity.TbUser;

/**
 * @author jiashunx
 */
public interface TbUserMapper {

    default String getSqlStatement(String methodName) {
        return TbUserMapper.class.getName() + "." + methodName;
    }

    int deleteByPrimaryKey(String id);

    int insert(TbUser record);

    int insertSelective(TbUser record);

    TbUser selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(TbUser record);

    int updateByPrimaryKey(TbUser record);

    int countWithCondition(TbUser record);

}
