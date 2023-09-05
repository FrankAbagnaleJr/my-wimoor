package com.wimoor.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.common.pojo.entity.QuartzTask;

import java.util.Map;

@Mapper
public interface QuartzTaskMapper extends BaseMapper<QuartzTask> {
}
