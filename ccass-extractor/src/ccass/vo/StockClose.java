package ccass.vo;

import java.util.Date;

import ccass.Consts;

public class StockClose {
	public double getLow52() {
		return low52;
	}

	public void setLow52(double low52) {
		this.low52 = low52;
	}

	public double getHigh52() {
		return high52;
	}

	public void setHigh52(double high52) {
		this.high52 = high52;
	}

	public String getStockCode() {
		return stockCode;
	}

	public void setStockCode(String stockCode) {
		this.stockCode = stockCode.replaceAll("^0+", "");
	}

	public String getStockName() {
		return stockName;
	}

	public void setStockName(String stockName) {
		this.stockName = stockName;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public long getVol() {
		return vol;
	}

	public void setVol(long vol) {
		this.vol = vol;
	}

	public double getHigh() {
		return high;
	}

	public void setHigh(double high) {
		this.high = high;
	}

	public double getLow() {
		return low;
	}

	public void setLow(double low) {
		this.low = low;
	}

	public double getClose() {
		return close;
	}

	public void setClose(double close) {
		this.close = close;
	}

	public long getTurnover() {
		return (long) (vol * close);
	}

	public boolean inTargetPriceRange() {
		boolean inRange = false;
		if (low52 == 0 || high52 == 0)
			return true;

		double waterMark = (high52 - low52) * Consts.PERCENT_PRICE_52W + low52;
		if (close < waterMark) {
			inRange = true;
		}
		
		return inRange;
	}

	String stockCode;
	String stockName;
	Date date;
	long vol;
	double high;
	double low;
	double close;
	double low52;
	double high52;
}
