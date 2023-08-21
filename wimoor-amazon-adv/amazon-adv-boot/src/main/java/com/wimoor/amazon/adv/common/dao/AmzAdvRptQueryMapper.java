package com.wimoor.amazon.adv.common.dao;

import org.apache.ibatis.annotations.Param;

import com.wimoor.amazon.adv.common.pojo.AmzAdvRptQuery;
import com.wimoor.amazon.base.BaseMapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmzAdvRptQueryMapper extends BaseMapper<AmzAdvRptQuery>{

	AmzAdvRptQuery selectByName(@Param("query")String query);
}