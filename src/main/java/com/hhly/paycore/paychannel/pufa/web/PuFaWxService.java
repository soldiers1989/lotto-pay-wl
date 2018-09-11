package com.hhly.paycore.paychannel.pufa.web;

import java.net.URLEncoder;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.pufa.config.PuFaConfig;
import com.hhly.paycore.paychannel.pufa.util.PuFaUtil;
import com.hhly.paycore.sign.Base64;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 浦发微信公众号支付
 * @author xiongJinGang
 * @date 2018年1月3日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class PuFaWxService extends PayAbstract {
	private static Logger logger = Logger.getLogger(PuFaWxService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		logger.info("浦发微信公众号支付开始，请求参数：" + paymentInfo.toString());
		Map<String, String> map = PuFaUtil.buildWXParam(paymentInfo, PLATFORM_WAP);
		// 调用接口
		try {
			String result = HttpUtil.doPost(PuFaConfig.PF_PAY_URL, map);
			logger.info("浦发微信公众号支付请求返回：" + result);
			Map<String, String> resultMap = PuFaUtil.transStringToMap(result);

			if (!ObjectUtil.isBlank(resultMap)) {
				if (resultMap.containsKey("respCode")) {
					if (resultMap.get("respCode").equals(TRADE_SUCCESS_PUFA)) {
						if (PuFaUtil.verferSignData(resultMap)) {
							String payUrl = resultMap.get("payInfo");
							payUrl = new String(Base64.decode(payUrl));
							payUrl += "&redirect_url=" + URLEncoder.encode(paymentInfo.getUrlReturn(), "UTF-8");
							PayReqResultVO payReqResult = new PayReqResultVO(payUrl);
							payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
							payReqResult.setTradeChannel(PayConstants.PayChannelEnum.PUFA_RECHARGE.getKey());
							return ResultBO.ok(payReqResult);
						}
						return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
					} else {
						String errorMsg = "支付请求失败";
						if (resultMap.containsKey("respDesc")) {
							errorMsg = resultMap.get("respDesc");
						}
						return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMsg);
					}
				}
			}
		} catch (Exception e) {
			logger.error("请求浦发支付异常", e);
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return PuFaUtil.orderRefund(refundParam, PayAbstract.PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return PuFaUtil.queryResult(payQueryParamVO, PayAbstract.PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return PuFaUtil.refundQuery(payQueryParamVO, PayAbstract.PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return PuFaUtil.analyPayResult(map);
	}

	@Override
	public ResultBO<?> payReturn(Map<String, String> map) {
		return super.payReturn(map);
	}

	@Override
	public ResultBO<?> queryBill(Map<String, String> map) {
		return null;
	}

}
