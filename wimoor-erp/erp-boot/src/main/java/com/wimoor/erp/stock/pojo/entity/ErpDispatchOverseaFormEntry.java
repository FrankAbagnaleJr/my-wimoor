package com.wimoor.erp.stock.pojo.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wimoor.erp.common.pojo.entity.BaseEntity;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author wimoor team
 * @since 2023-02-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_erp_dispatch_oversea_form_entry")
@ApiModel(value="ErpDispatchOverseaFormEntry对象", description="")
public class ErpDispatchOverseaFormEntry  extends BaseEntity{

    private static final long serialVersionUID=1L;

    private BigInteger formid;

    private BigInteger materialid;

    private String sellersku;

    private String fnsku;

    private Integer amount;


}
