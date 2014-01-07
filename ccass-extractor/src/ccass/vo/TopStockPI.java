package ccass.vo;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ccass.Consts;

public class TopStockPI implements Comparable {
	public LinkedHashMap<String, StockPI> PIMap = new LinkedHashMap<String, StockPI>();
	static Logger log = Logger.getLogger(TopStockPI.class.getName());
	String stockCode;
	Date date;
	TreeMap changePercentPIs = new TreeMap();
	public boolean qouteDone = true;
	
	public String getStockCode() {
		return stockCode;
	}

	public void setStockCode(String stockCode) {
		this.stockCode = stockCode.replaceAll("^0+","");
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public TopStockPI(String stkCde, Date dt) {
		this.setStockCode(stkCde);
		this.date = dt;
	}

	public TopStockPI(boolean qd) {
		qouteDone = qd;
	}
	
	@Override
	public int compareTo(Object o) {
		TopStockPI iTopPI = (TopStockPI) o;
		PIMap.entrySet().size();
		for (Map.Entry<String, StockPI> stockPIEntry : PIMap.entrySet()) {
			String key = stockPIEntry.getKey();
			StockPI PI = stockPIEntry.getValue();

			StockPI iPI = iTopPI.PIMap.get(key);
			// TODO: the comparable should be refined
			if (null == iPI) {
				return 1;
			} else if (PI.getPercentage() - iPI.getPercentage() > Consts.PERCENT_CHANGE_1D) {
				return 1;
			} else if (iPI.getPercentage() - PI.getPercentage() > Consts.PERCENT_CHANGE_1D) {
				return 2;
			}
		}
		return 0;
	}

}
