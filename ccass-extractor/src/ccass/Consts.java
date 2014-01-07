package ccass;

import java.io.FileInputStream;
import java.util.Properties;

public final class Consts {
	public static int TOP_PI_SIZE;
	public static int CCASS_QOUTE_SLEEP_MILLISECOND;
	public static double PERCENT_CHANGE_1D;
	public static double PERCENT_CHANGE_30D;
	public static double PERCENT_PRICE_52W;
	public static boolean PROXY_BLOCK;
	public static long STOCKQUOTE_TURNOVER;
	public static String URL = "http://www.hkexnews.hk/sdw/search/search_sdw.asp";
	public static String URL_STOCK_LIST = "http://www.hkex.com.hk/eng/market/sec_tradinfo/stockcode/eisdeqty.htm";
	public static String[] URLS_QUOTE = new String[] {
			"http://eoddata.com/stocklist/HKEX/0.htm",
			"http://eoddata.com/stocklist/HKEX/1.htm",
			"http://eoddata.com/stocklist/HKEX/2.htm",
			"http://eoddata.com/stocklist/HKEX/3.htm",
			"http://eoddata.com/stocklist/HKEX/6.htm",
			"http://eoddata.com/stocklist/HKEX/8.htm" };
	public static String URLS_52W = "http://www.aastocks.com/en/ltp/RTQuoteContent.aspx?symbol=%stockCode%&process=y";
	static {
		try {
			Properties props = new Properties();
			FileInputStream fis = new FileInputStream("ccass.properties");
			props.load(fis);

			TOP_PI_SIZE = new Integer(
					props.getProperty("ccass.stock.toppisize"));
			CCASS_QOUTE_SLEEP_MILLISECOND = new Integer(
					props.getProperty("ccass.stock.sleepmillisecond"));
			PROXY_BLOCK = props.getProperty("ccass.setting.proxyblock").equals(
					"true");
			PERCENT_CHANGE_1D = Double.valueOf(props
					.getProperty("ccass.stock.pisharechange_1d"));
			PERCENT_CHANGE_30D = Double.valueOf(props
					.getProperty("ccass.stock.pisharechange_30d"));
			STOCKQUOTE_TURNOVER = Long.valueOf(props
					.getProperty("ccass.stockQuote.turnover"));
			PERCENT_PRICE_52W = Double.valueOf(props
					.getProperty("ccass.stock.price52w"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
