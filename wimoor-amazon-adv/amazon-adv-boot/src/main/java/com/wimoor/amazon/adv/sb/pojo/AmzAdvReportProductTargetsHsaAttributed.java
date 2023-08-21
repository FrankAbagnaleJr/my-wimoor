package com.wimoor.amazon.adv.sb.pojo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wimoor.amazon.adv.common.pojo.JsonBigIntergeSerializer;

@Entity
@Table(name="t_amz_adv_rpt2_hsa_product_targets_attributed")
public class AmzAdvReportProductTargetsHsaAttributed   {
	
	@Id
	@Column(name="bydate")
    private Date bydate;

	@Id
	@Column(name="targetId")
	@JsonSerialize(using = JsonBigIntergeSerializer.class)
    private BigInteger targetid;

	@Column(name="attributedConversions14d")
    private Integer attributedconversions14d;

	@Column(name="attributedConversions14dSameSKU")
    private Integer attributedconversions14dsamesku;

	@Column(name="attributedSales14d")
    private BigDecimal attributedsales14d;

	@Column(name="attributedSales14dSameSKU")
    private BigDecimal attributedsales14dsamesku;


    public Date getBydate() {
        return bydate;
    }

    public void setBydate(Date bydate) {
        this.bydate = bydate;
    }

	public BigInteger getTargetid() {
		return targetid;
	}

	public void setTargetid(BigInteger targetid) {
		this.targetid = targetid;
	}

    public Integer getAttributedconversions14d() {
        return attributedconversions14d;
    }

    public void setAttributedconversions14d(Integer attributedconversions14d) {
        this.attributedconversions14d = attributedconversions14d;
    }

    public Integer getAttributedconversions14dsamesku() {
        return attributedconversions14dsamesku;
    }

    public void setAttributedconversions14dsamesku(Integer attributedconversions14dsamesku) {
        this.attributedconversions14dsamesku = attributedconversions14dsamesku;
    }

    public BigDecimal getAttributedsales14d() {
        return attributedsales14d;
    }

    public void setAttributedsales14d(BigDecimal attributedsales14d) {
        this.attributedsales14d = attributedsales14d;
    }

    public BigDecimal getAttributedsales14dsamesku() {
        return attributedsales14dsamesku;
    }

    public void setAttributedsales14dsamesku(BigDecimal attributedsales14dsamesku) {
        this.attributedsales14dsamesku = attributedsales14dsamesku;
    }
    
    public boolean isZero() {
    	if(attributedsales14d!=null&&attributedsales14d.floatValue()>0.001)return false;
    	if(attributedsales14dsamesku!=null&&attributedsales14dsamesku.floatValue()>0.001)return false;
    	if(attributedconversions14d!=null&&attributedconversions14d!=0)return false;
    	if(attributedconversions14dsamesku!=null&&attributedconversions14dsamesku!=0)return false;
        return true;
    }
}