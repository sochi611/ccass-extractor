package ccass;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ccass.vo.StockClose;
import ccass.vo.StockPI;
import ccass.vo.TopStockPI;

public class Extractor {
	static String trStarter = "Participant ID";
	static String trStopper = "Remarks:";
	static Logger log = Logger.getLogger(Extractor.class.getName());

	public static void main(String[] args) throws IOException,
			InterruptedException {

		Validate.isTrue(true, Consts.URL);
		Calendar todayCal = null;
		Calendar shareCalD1 = null;

		// STOCK LIST
		ArrayList<String> stockCodeLst = new ArrayList<String>();
		Document doc = Jsoup.connect(Consts.URL_STOCK_LIST).get();
		Elements table = doc.select("table[class=table_grey_border]");
		Elements trs = table.select("tr[class=tr_normal]");
		int cnt = 0;
		for (Element tr : trs) {
			cnt++;
			Element tds = tr.select("td[class=verd_black12]").first();
			if (false) {
			//if (cnt > 50) {
				log.debug("stockCode [" + cnt + "][SKIPPED]" + tds.text());
				break;
			}
			stockCodeLst.add(tds.text().replaceAll("^0+", ""));
		}
		log.debug("Step 1 - Stock Code List prepared ->" + stockCodeLst.size());
		// STOCK LIST

		// STOCK CLOSE LIST

		TreeMap<String, StockClose> stockCloses = getStockCloses(stockCodeLst);

		log.debug("Step 2 - Stock Closes filled with 52W ->"
				+ stockCloses.size());
		// STOCK CLOSE LIST

		log.debug("Step 3 - Starts CCASS Extraction");
		ArrayList<TopStockPI> sLst = new ArrayList<TopStockPI>();
		ArrayList<TopStockPI> bLst = new ArrayList<TopStockPI>();
		for (String stockCode : stockCodeLst) {
			todayCal = Calendar.getInstance();
			shareCalD1 = Calendar.getInstance();

			TopStockPI tPID1 = null;
			TopStockPI tPID2 = null;

			// turnover percentage check
			if (stockCloses.get(stockCode) != null
					&& stockCloses.get(stockCode).getTurnover() < Consts.STOCKQUOTE_TURNOVER)
				continue;

			// 33% 52Week price check
			if (stockCloses.get(stockCode) != null
					&& !stockCloses.get(stockCode).inTargetPriceRange()) {
				/*
				log.debug(stockCode + " [SKIPPED per 52Week] "
						+ stockCloses.get(stockCode).getClose() + " "
						+ stockCloses.get(stockCode).getHigh52() + " "
						+ stockCloses.get(stockCode).getLow52());
						*/
				continue;
			}

			// Holiday-Sun-Sat consideration
			while (null == tPID1) {
				prevWorkingDay(shareCalD1);
				tPID1 = getStockCCASS(stockCode, todayCal, shareCalD1, false);
			}
			// Holiday-Sun-Sat consideration
			while (null == tPID2) {
				prevWorkingDay(shareCalD1);
				tPID2 = getStockCCASS(stockCode, todayCal, shareCalD1, true);
			}
			if (tPID1 != null && tPID2 != null
					&& tPID1.qouteDone == tPID2.qouteDone == true) {
				switch (tPID1.compareTo(tPID2)) {
				case 1:
					bLst.add(tPID1);
					log.debug("BUY " + tPID1.getStockCode());
					break;

				case 2:
					sLst.add(tPID1);
					log.debug("SELL " + tPID1.getStockCode());
					break;
				}
			}
		}

		// Report
		log.debug("BUY ******************************************");
		for (TopStockPI tpSPI : bLst) {
			log.debug(tpSPI.getStockCode());
		}
		log.debug("SELL ******************************************");
		for (TopStockPI tpSPI : sLst) {
			log.debug(tpSPI.getStockCode());
		}
	}

	private static TopStockPI getStockCCASS(String stockCode,
			Calendar todayCal, Calendar shareCal, boolean allPI)
			throws IOException, InterruptedException {
		Format dFormatter = new SimpleDateFormat("dd");
		Format mFormatter = new SimpleDateFormat("MM");
		Format yFormatter = new SimpleDateFormat("yyyy");
		Format formatter = new SimpleDateFormat("dd/MM/yyyy");

		String todayD = dFormatter.format(todayCal.getTime());
		String todayM = mFormatter.format(todayCal.getTime());
		String todayY = yFormatter.format(todayCal.getTime());

		String shareD = dFormatter.format(shareCal.getTime());
		String shareM = mFormatter.format(shareCal.getTime());
		String shareY = yFormatter.format(shareCal.getTime());

		todayCal = shareCal;
		Document doc = null;
		boolean doQuote = true;
		int sleepMulti = 1;
		while (doQuote) {
			try {
				doc = Jsoup
						.connect(Consts.URL)
						.data("txt_today_d", todayD, "txt_today_m", todayM,
								"txt_today_y", todayY, "current_page", "1",
								"stock_market", "HKEX", "IsExist_Slt_Stock_Id",
								"False", "IsExist_Slt_Part_Id", "False",
								"rdo_SelectSortBy", "Shareholding",
								"sel_ShareholdingDate_d", shareD,
								"sel_ShareholdingDate_m", shareM,
								"sel_ShareholdingDate_y", shareY,
								"txt_stock_code", stockCode, "txt_stock_name",
								"", "txt_ParticipantID", "",
								"txt_Participant_name", "").post();
				doQuote = false;
			} catch (SocketTimeoutException stex) {
				Thread.sleep(Consts.CCASS_QOUTE_SLEEP_MILLISECOND * sleepMulti);
				log.debug("sleep for " + Consts.CCASS_QOUTE_SLEEP_MILLISECOND
						+ "milli seconds");
				sleepMulti++;
			}
		}
		if (doc == null || doc.text().length() < 5000)
			return new TopStockPI(false);
		TopStockPI tPI = new TopStockPI(stockCode, shareCal.getTime());
		Elements table = doc.select("table[id=tbl_Result_inner]");
		Elements trs = table.select("tr");
		Element trShareDate = trs.select(":contains(Shareholding Date:)")
				.first();
		Element tdShareDate = trShareDate.select("td[class=arial12black]").get(
				1);
		String shareDate = tdShareDate.text();

		// To detect if share holding date = request date (holiday
		// consideration)
		if (!shareDate.equals(formatter.format(shareCal.getTime())))
			return null;

		int cnt = 0;
		boolean inLoop = false;

		for (Element tr : trs) {
			if (cnt > Consts.TOP_PI_SIZE && allPI == false)
				break;
			Elements tds = tr.select("td[class=arial12black]");
			if (tds.size() == 5) {
				if (trStarter.equals(tds.get(0).text())) {
					inLoop = true;
					continue;
				}
				if (!inLoop)
					continue;

				if (trStopper.equals(tds.get(0).text()))
					break;
				/*
				 * log.debug("td [" + cnt + "] 0 " + tds.get(0).text());
				 * log.debug("td [" + cnt + "] 1 " + tds.get(1).text());
				 * log.debug("td [" + cnt + "] 2 " + tds.get(2).text());
				 * log.debug("td [" + cnt + "] 3 " + tds.get(3).text());
				 * log.debug("td [" + cnt + "] 4 " + tds.get(4).text());
				 */
				cnt++;
				StockPI sPI = new StockPI();
				sPI.setPi(tds.get(0).text());
				sPI.setStockName(tds.get(1).text());
				sPI.setStockCode(stockCode);
				sPI.setDate(shareCal.getTime());
				sPI.setShare(new Long(tds.get(3).text().replaceAll(",", ""))
						.intValue());
				sPI.setPercentage(new Double(tds.get(4).text()
						.replaceAll("%", "")).doubleValue());
				tPI.PIMap.put(sPI.getPi(), sPI);
			}
		}
		return tPI;
	}

	private static TreeMap<String, StockClose> getStockCloses(
			ArrayList<String> stockLst) throws IOException {
		Document doc = null;
		TreeMap<String, StockClose> stockCloses = new TreeMap<String, StockClose>();
		for (String stkCode : stockLst) {
			try {
				doc = Jsoup.connect(
						Consts.URLS_52W.replaceFirst("%stockCode%", stkCode))
						.get();
			} catch (SocketTimeoutException stex) {
				log.debug("sleep for " + Consts.CCASS_QOUTE_SLEEP_MILLISECOND
						+ "milli seconds -> " + stkCode);
				continue;
			}
			// parse 52 wk price range
			try {
				Element table = doc.select("table[class=tb-c]").first();

				String str52WK = table.select("td:contains(52 WK)")
						.select("div").get(2).text().replace(" ", "");
				String strVol = table.select("td:contains(Volume)")
						.select("div").get(2).text();
				String strCls = table.select("div[class=C font28 C bold]")
						.first().text();
				String strName = doc.select("div[class=floatL f15]").get(0)
						.text().replaceAll("&nbsp;.*$", "");

				String[] s52HL = str52WK.split("-");
				if (s52HL.length == 2) {
					StockClose stkC = new StockClose();
					stkC.setStockCode(stkCode);
					stkC.setStockName(strName);
					stkC.setClose(toDouble(strCls));
					stkC.setVol(toLong(strVol));
					stkC.setHigh52(Double.valueOf(toDouble(s52HL[0])));
					stkC.setLow52(Double.valueOf(toDouble(s52HL[1])));
					stockCloses.put(stkC.getStockCode(), stkC);

				}
			} catch (NumberFormatException nfex) {
				log.debug(stkCode + " " + doc + "52Week cannot be formatted",
						nfex);
			} catch (Exception ex) {
				log.debug(stkCode + " ", ex);
			}
		}
		return stockCloses;
	}

	private static TreeMap<String, StockClose> getStockCloses1()
			throws IOException {
		TreeMap<String, StockClose> stockCloses = new TreeMap<String, StockClose>();
		Document doc = null;
		Elements table = null;
		Elements trs = null;
		for (String url : Consts.URLS_QUOTE) {
			try {
				doc = Jsoup.connect(url).get();
			} catch (SocketTimeoutException stex) {
				log.debug("stockQuote exception:", stex);
				continue;
			}

			table = doc.select("div[id=ctl00_cph1_divSymbols]");
			trs = table.select("tr");
			for (Element tr : trs) {
				Elements tds = tr.select("td");
				if (tds.size() != 10)
					continue;
				StockClose stkClose = new StockClose();
				stkClose.setStockCode(tds.get(0).text());
				stkClose.setStockName(tds.get(1).text());
				stkClose.setClose(Double.valueOf(tds.get(4).text()
						.replace(",", "")));
				stkClose.setVol(Long.valueOf(tds.get(5).text()
						.replaceAll(",", "")));
				stockCloses.put(stkClose.getStockCode(), stkClose);
			}
		}

		return stockCloses;
	}

	private static void prevWorkingDay(Calendar cal) {
		cal.add(Calendar.DATE, -1);
		while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
				|| cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			cal.add(Calendar.DATE, -1);
		}
	}

	private static long toLong(String longStr) {
		double muli = 1;
		if (longStr.contains("K")) {
			muli = 1000;
		} else if (longStr.contains("M")) {
			muli = 1000000;
		} else if (longStr.contains("B")) {
			muli = 1000000000;
		}

		double volD = Double.valueOf(longStr.replaceAll("[ ,KMB]+", ""));
		return (long) (volD * muli);
	}

	private static double toDouble(String doubleStr) {
		return Double.valueOf(doubleStr.replace("[ ,KMB]+", ""));
	}
}
