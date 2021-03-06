package com.hhly.paycore.paychannel.smartcloud.web;

import java.util.Map;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.smartcloud.config.CloudConfig;
import com.hhly.paycore.paychannel.smartcloud.util.CloudUtil;
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
 * @desc 智能云支付
 * @author xiongJinGang
 * @date 2018年8月3日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class CloudWapService extends PayAbstract {
	private static Logger logger = Logger.getLogger(CloudWapService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		logger.info("智能云wap支付开始，请求参数：" + paymentInfo.toString());
		try {
			// 构建扫码请求参数
			Map<String, String> map = CloudUtil.buildWapMapParam(paymentInfo);
			logger.info("智能云支付请求参数：" + map.toString());

			String json = HttpUtil.doPost(CloudConfig.CLOUD_SAVE_URL + "action", map);
			// {"status":0,"errmsg":"","url":"https://nv.hongzzpay.cn:4713/d.jsp?npH%2Bmvc%2FKL9nSI%2BsqEhCBLrIW%2BS7F6tvYBd%2FeOgTgHX3D77bbse4Dg%3D%3D","order":"GD061315345249007000920088385616","sign":"04e232b964c5c04f3d9ff00c2bc9d46e"}
			logger.info("智能云wap支付请求返回：" + json);
			if (ObjectUtil.isBlank(json)) {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
			JSONObject jsonObject = JSON.parseObject(json);// 为 200 表示没有错误
			if (jsonObject.getString("code").equals("200")) {
				JSONObject dataJson = JSONObject.parseObject(jsonObject.getString("data"));
				PayReqResultVO payReqResult = new PayReqResultVO(dataJson.getString("qrcode"));
				payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
				payReqResult.setTradeChannel(PayConstants.PayChannelEnum.CLOUD_RECHARGE.getKey());
				return ResultBO.ok(payReqResult);
			} else {
				return ResultBO.err(MessageCodeConstants.THIRD_API_READ_TIME_OUT);
			}
		} catch (Exception e) {
			logger.error("智能云支付请求异常：" + e);
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return null;
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return CloudUtil.queryResult(payQueryParamVO);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		// return HaoDianAUtil.refundQuery(payQueryParamVO, PayAbstract.PLATFORM_WEB);
		return null;
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return CloudUtil.payNotify(map);
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
