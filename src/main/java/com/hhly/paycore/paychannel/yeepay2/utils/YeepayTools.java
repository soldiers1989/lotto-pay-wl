package com.hhly.paycore.paychannel.yeepay2.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hhly.paycore.paychannel.yeepay2.config.Yeepay2Config;
import com.hhly.paycore.paychannel.yeepay2.config.YopClient3;
import com.hhly.paycore.paychannel.yeepay2.config.YopRequest;
import com.yeepay.g3.sdk.yop.client.YopResponse;
import com.yeepay.g3.sdk.yop.encrypt.CertTypeEnum;
import com.yeepay.g3.sdk.yop.encrypt.DigestAlgEnum;
import com.yeepay.g3.sdk.yop.encrypt.DigitalEnvelopeDTO;
import com.yeepay.g3.sdk.yop.encrypt.DigitalSignatureDTO;
import com.yeepay.g3.sdk.yop.utils.DigitalEnvelopeUtils;
import com.yeepay.g3.sdk.yop.utils.InternalConfig;
import com.yeepay.g3.yop.sdk.api.StdApi;

public class YeepayTools {

	// 接口参数
	public static final String[] TRADEORDER = { "parentMerchantNo", "merchantNo", "orderId", "orderAmount", "timeoutExpress", "requestDate", "redirectUrl", "notifyUrl", "goodsParamExt", "paymentParamExt", "industryParamExt", "memo", "riskParamExt",
			"csUrl", "fundProcessType", "divideDetail", "divideNotifyUrl" };
	public static final String[] ORDERQUERY = { "parentMerchantNo", "merchantNo", "orderId", "uniqueOrderNo" };
	public static final String[] REFUND = { "parentMerchantNo", "merchantNo", "orderId", "uniqueOrderNo", "refundRequestId", "refundAmount", "description", "memo", "notifyUrl", "accountDivided" };
	public static final String[] REFUNDQUERY = { "parentMerchantNo", "merchantNo", "refundRequestId", "orderId", "uniqueRefundNo" };
	public static final String[] MULTIORDERQUERY = { "status", "parentMerchantNo", "merchantNo", "requestDateBegin", "requestDateEnd", "pageNo", "pageSize" };
	public static final String[] ORDERCLOSE = { "orderId", "parentMerchantNo", "merchantNo", "uniqueOrderNo", "description" };
	public static final String[] DIVIDEORDER = { "parentMerchantNo", "merchantNo", "divideRequestId", "orderId", "uniqueOrderNo", "divideDetail", "infoParamExt", "contractNo" };
	public static final String[] DIVIDEORDERQUERY = { "parentMerchantNo", "merchantNo", "divideRequestId", "orderId", "uniqueOrderNo" };
	public static final String[] FULLSETTLE = { "parentMerchantNo", "merchantNo", "orderId", "uniqueOrderNo" };
	public static final String[] CERTORDER = { "merchantNo", "requestNo", "bankCardNo", "idCardNo", "userName", "authType", "requestTime", "remark" };
	public static final String[] CERTORDERORDER = { "merchantNo", "requestNo", "ybOrderId" };

	// 支付方式
	public static final String[] CASHIER = { "merchantNo", "token", "timestamp", "directPayType", "cardType", "userNo", "userType", "ext" };
	public static final String[] APICASHIER = { "token", "payTool", "payType", "userNo", "userType", "appId", "openId", "payEmpowerNo", "merchantTerminalId", "merchantStoreNo", "userIp", "version" };

	// 获取对账类型
	public static final String TRADEDAYDOWNLOAD = "tradedaydownload";
	public static final String TRADEMONTHDOWNLOAD = "trademonthdownload";
	public static final String REMITDAYDOWNLOAD = "remitdaydownload";

	// 标准收银台——拼接支付链接
	public static String getUrl(Map<String, String> paramValues) throws UnsupportedEncodingException {
		StringBuffer url = new StringBuffer();
		url.append(Yeepay2Config.NEW_YEEPAY_CASHIER);
		StringBuilder stringBuilder = new StringBuilder();
		System.out.println(paramValues);
		for (int i = 0; i < CASHIER.length; i++) {
			String name = CASHIER[i];
			String value = paramValues.get(name);
			if (i != 0) {
				stringBuilder.append("&");
			}
			stringBuilder.append(name + "=").append(value);
		}
		System.out.println("stringbuilder:" + stringBuilder);
		String sign = getSign(stringBuilder.toString());
		url.append("?sign=" + sign + "&" + stringBuilder);
		return url.toString();
	}

	// 获取密钥P12
	public static PrivateKey getSecretKey() {
		PrivateKey isvPrivateKey = InternalConfig.getISVPrivateKey(CertTypeEnum.RSA2048);
		return isvPrivateKey;
	}

	// 获取公钥
	public static PublicKey getPublicKey() {
		PublicKey isvPublicKey = InternalConfig.getYopPublicKey(CertTypeEnum.RSA2048);
		return isvPublicKey;
	}

	// 获取sign
	public static String getSign(String stringBuilder) {
		String appKey = "OPR:" + Yeepay2Config.NEW_YEEPAY_MERCHANT_NO;

		PrivateKey isvPrivateKey = getSecretKey();

		DigitalSignatureDTO digitalSignatureDTO = new DigitalSignatureDTO();
		digitalSignatureDTO.setAppKey(appKey);
		digitalSignatureDTO.setCertType(CertTypeEnum.RSA2048);
		digitalSignatureDTO.setDigestAlg(DigestAlgEnum.SHA256);
		digitalSignatureDTO.setPlainText(stringBuilder.toString());
		String sign = DigitalEnvelopeUtils.sign0(digitalSignatureDTO, isvPrivateKey);
		return sign;
	}

	/**
	 * 请求YOP接口
	 * params 请求参数,parentMerchantNo除外
	 * uri 请求yop的应用URI地址
	 * paramSign 请求参数的验签顺序
	 * @throws IOException 
	 */
	public static Map<String, String> requestYop(Map<String, String> params, String uri, String[] paramSign, String path) throws IOException {
		Map<String, String> result = new HashMap<String, String>();
		params.put("parentMerchantNo", Yeepay2Config.NEW_YEEPAY_PARENT_MERCHANT_NO);
		params.put("merchantNo", Yeepay2Config.NEW_YEEPAY_MERCHANT_NO);

		// YopRequest request = new YopRequest("OPR:"+merchantNo,"");
		YopRequest request = new YopRequest("OPR:" + Yeepay2Config.NEW_YEEPAY_MERCHANT_NO, path + "/src/config/yop_sdk_config_default.json", Yeepay2Config.NEW_YEEPAY_URL);

		for (int i = 0; i < paramSign.length; i++) {
			String key = paramSign[i];
			request.addParam(key, params.get(key));
		}
		System.out.println(request.getParams());

		YopResponse response = YopClient3.postRsa(uri, request);

		System.out.println(response.toString());
		if ("FAILURE".equals(response.getState())) {
			if (response.getError() != null)
				result.put("code", response.getError().getCode());
			result.put("message", response.getError().getMessage());
			return result;
		}
		if (response.getStringResult() != null) {
			result = parseResponse(response.getStringResult());
		}

		return result;
	}

	// 将获取到的response解密完成的json转换成map
	public static Map<String, String> parseResponse(String response) {

		Map<String, String> jsonMap = new HashMap<>();
		jsonMap = JSON.parseObject(response, new TypeReference<TreeMap<String, String>>() {
		});

		return jsonMap;
	}

	/**
	 * 支付成功，页面回调验签
	 * @param responseMap
	 * @return
	 */
	public static boolean verifyCallback(Map<String, String> responseMap) {
		boolean flag = false;
		String merchantNo = responseMap.get("merchantNo");
		String parentMerchantNo = responseMap.get("parentMerchantNo");
		String orderId = responseMap.get("orderId");
		String signResp = responseMap.get("sign");
		String s = "merchantNo=" + merchantNo + "&parentMerchantNo=" + parentMerchantNo + "&orderId=" + orderId;
		System.out.println("s====" + s);
		String appKey = "OPR:" + Yeepay2Config.NEW_YEEPAY_MERCHANT_NO;
		PublicKey isvPublicKey = getPublicKey();
		DigitalSignatureDTO digitalSignatureDTO = new DigitalSignatureDTO();
		digitalSignatureDTO.setAppKey(appKey);
		digitalSignatureDTO.setCertType(CertTypeEnum.RSA2048);
		digitalSignatureDTO.setDigestAlg(DigestAlgEnum.SHA256);
		digitalSignatureDTO.setPlainText(s.toString());
		digitalSignatureDTO.setSignature(signResp);
		try {
			DigitalEnvelopeUtils.verify0(digitalSignatureDTO, isvPublicKey);
			flag = true;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return flag;
	}

	/**
	 * 异步回调验签
	 * @param response
	 * @return
	 */
	public static Map<String, String> callback(String response) {
		DigitalEnvelopeDTO dto = new DigitalEnvelopeDTO();
		dto.setCipherText(response);
		Map<String, String> jsonMap = new HashMap<>();
		try {
			PrivateKey privateKey = InternalConfig.getISVPrivateKey(CertTypeEnum.RSA2048);
			PublicKey publicKey = InternalConfig.getYopPublicKey(CertTypeEnum.RSA2048);
			dto = DigitalEnvelopeUtils.decrypt(dto, privateKey, publicKey);
			System.out.println(dto.getPlainText());
			jsonMap = parseResponse(dto.getPlainText());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonMap;
	}

	/**
	 * 下载对账单
	 * @param method
	 * @param parameters
	 * @return
	 */
	public static String yosFile(Map<String, String> params, String path) {
		StdApi apidApi = new StdApi();
		InputStream inputStream = null;
		OutputStream outputStream = null;
		String merchantNo = Yeepay2Config.NEW_YEEPAY_MERCHANT_NO;
		String method = params.get("method");
		String dateday = params.get("dateday");
		String datemonth = params.get("datemonth");
		String dataType = params.get("dataType");

		String fileName = "";
		String filePath = "";
		try {
			if (method.equals(YeepayTools.TRADEDAYDOWNLOAD)) {
				System.out.println("1");
				inputStream = apidApi.tradeDayBillDownload(merchantNo, dateday);
				fileName = "tradeday-" + dateday + ".csv";

			} else if (method.equals(YeepayTools.TRADEMONTHDOWNLOAD)) {
				System.out.println("2");
				inputStream = apidApi.tradeMonthBillDownload(merchantNo, datemonth);
				fileName = "trademonth-" + datemonth + ".csv";

			} else if (method.equals(YeepayTools.REMITDAYDOWNLOAD)) {
				System.out.println("2");
				inputStream = apidApi.remitDayBillDownload(merchantNo, dateday, dataType);
				fileName = "remitday-" + dataType + "-" + dateday + ".csv";

			}
			filePath = path + File.separator + fileName;
			System.out.println("filePath=====" + filePath);
			outputStream = new FileOutputStream(new File(filePath));
			byte[] bs = new byte[1024];
			int readNum;
			while ((readNum = inputStream.read(bs)) != -1) {
				outputStream.write(bs, 0, readNum);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		} finally {
			try {
				outputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				inputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return filePath;
	}

	public static Map<String, String> requestYOP(Map<String, String> params, String uri, String[] paramSign) throws IOException {
		Map<String, String> result = new HashMap<String, String>();
		params.put("parentMerchantNo", Yeepay2Config.NEW_YEEPAY_PARENT_MERCHANT_NO);
		params.put("merchantNo", Yeepay2Config.NEW_YEEPAY_MERCHANT_NO);
		// 第三种方式，传appkey和privatekey
		// String privatekey=Configuration.getInstance().getValue("privatekey");
		// YopRequest request = new YopRequest("OPR:"+merchantNo,privatekey);

		// 第一种方式，不传参数
		// YopRequest request = new YopRequest();
		/**
		 * 第二种方式：只传appkey
		 */
		YopRequest request = new YopRequest("OPR:" + Yeepay2Config.NEW_YEEPAY_MERCHANT_NO);

		for (int i = 0; i < paramSign.length; i++) {
			String key = paramSign[i];
			request.addParam(key, params.get(key));
		}
		System.out.println(request.getParams());

		YopResponse response = YopClient3.postRsa(uri, request);

		System.out.println(response.toString());
		if ("FAILURE".equals(response.getState())) {
			if (response.getError() != null)
				result.put("code", response.getError().getCode());
			result.put("message", response.getError().getMessage());
			return result;
		}
		if (response.getStringResult() != null) {
			result = parseResponse(response.getStringResult());
		}

		return result;
	}

}
