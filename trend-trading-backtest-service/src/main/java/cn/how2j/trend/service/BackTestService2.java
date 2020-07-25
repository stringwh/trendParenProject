package cn.how2j.trend.service;

import java.math.BigDecimal;
import java.util.*;

import cn.how2j.trend.client.IndexDataClient;
import cn.how2j.trend.pojo.AnnualProfit;
import cn.how2j.trend.pojo.IndexData;
import cn.how2j.trend.pojo.Profit;
import cn.how2j.trend.pojo.Trade;
import cn.how2j.trend.util.MathCaclateUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BackTestService2 {
    @Autowired
    IndexDataClient indexDataClient;
    public Map<String, Object> MACD(double serviceCharge,double[] closePrice, int fast, int slow, int signal, double[] macd, double[] dea, double[] diff,List<IndexData>allIndexDatas) {
        List<Profit> profits = new ArrayList<>();//利润集合
        List<Trade> trades = new ArrayList<>();//交易信息
        double initCash = 1000;//初始化金钱
        double cash = initCash;//本金
        double share = 0;//份额
        double value = 0;//最后计算的价值
        int winCount = 0;//赢的次数
        float totalWinRate = 0;//总共赢的比率
        float avgWinRate = 0;//平均赢得比率 （totalWinRate/winCount）
        float totalLossRate = 0;
        int lossCount = 0;//输得次数
        float avgLossRate = 0;//平均输得比率

        double preEma_12 = 0;
        double preEma_26 = 0;
        double preDEA = 0;

        double ema_12 = 0;
        double ema_26 = 0;

        double fastPeriod = Double.valueOf(new Integer(fast));// 日快线移动平均，标准为12，按照标准即可
        double slowPeriod = Double.valueOf(new Integer(slow));// 日慢线移动平均，标准为26，可理解为天数
        double signalPeriod = Double.valueOf(new Integer(signal));// 日移动平均，标准为9，按照标准即可

        double DEA = 0;
        double DIFF = 0;
        double MACD = 0;

        double init = 0;
        if (closePrice!=null)//初始值调整曲线最开始初始值相同
            init = closePrice[0];

        for (int i = 0; i < closePrice.length; i++) {
            ema_12 = i == 0 ? closePrice[i]
                    : MathCaclateUtil.add(
                    MathCaclateUtil.divide(
                            MathCaclateUtil.multiply(preEma_12, fastPeriod - 1, BigDecimal.ROUND_HALF_UP),
                            fastPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(
                            MathCaclateUtil.multiply(closePrice[i], 2D, BigDecimal.ROUND_HALF_UP),
                            fastPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// ema_12=preEma_12*(fastPeriod-1)/(fastPeriod+1)+closePrice*2/(fastPeriod+1)

            ema_26 = i == 0 ? closePrice[i]
                    : MathCaclateUtil.add(
                    MathCaclateUtil.divide(
                            MathCaclateUtil.multiply(preEma_26, slowPeriod - 1, BigDecimal.ROUND_HALF_UP),
                            slowPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(
                            MathCaclateUtil.multiply(closePrice[i], 2D, BigDecimal.ROUND_HALF_UP),
                            slowPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// ema_26=preEma_26*(slowPeriod-1)/(slowPeriod+1)+closePrice*2/(slowPeriod+1)

            DIFF = i == 0 ? 0 : MathCaclateUtil.subtract(ema_12, ema_26, BigDecimal.ROUND_HALF_UP);// Diff=ema_12-ema_26

            DEA = i == 0 ? 0
                    : MathCaclateUtil.add(
                    MathCaclateUtil.divide(
                            MathCaclateUtil.multiply(preDEA, signalPeriod - 1, BigDecimal.ROUND_HALF_UP),
                            signalPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    MathCaclateUtil.divide(MathCaclateUtil.multiply(DIFF, 2D, BigDecimal.ROUND_HALF_UP),
                            signalPeriod + 1, BigDecimal.ROUND_UNNECESSARY),
                    BigDecimal.ROUND_HALF_UP);// DEA=preDEA*(signalPeriod-1)/(signalPeriod+1)+Diff*2/(signalPeriod+1)

            MACD = i == 0 ? 0
                    : MathCaclateUtil.multiply(2D, MathCaclateUtil.subtract(DIFF, DEA, BigDecimal.ROUND_HALF_UP),
                    BigDecimal.ROUND_HALF_UP);// MACD=2×(Diff－DEA)

            preEma_12 = ema_12;
            preEma_26 = ema_26;
            preDEA = DEA;

            macd[i] = MACD;
            dea[i] = DEA;
            diff[i] = DIFF;
        }
        for(Double d: macd){
            System.out.println("macd:"+d+" ");
        }
        System.out.println();
        for(Double d: dea){
            System.out.println("DEA:"+d+" ");
        }
        System.out.println();
        for(Double d: diff){
            System.out.println("DIFF:"+d+" ");
        }
        System.out.println();
        for( int i =0 ;i<macd.length-1;i++){
            if(macd[i]<0&&macd[i+1]>0){
                if(0==share){
                    share=cash/closePrice[i+1];
                    cash = 0;

                    Trade trade = new Trade();
                    trade.setBuyDate(allIndexDatas.get(i+1).getDate());
                    trade.setBuyClosePoint((float) closePrice[i+1]);
                    trade.setSellDate("n/a");
                    trade.setSellClosePoint(0);
                    trades.add(trade);

                }
            }
            else if(macd[i]>0&&macd[i+1]<0){
                if(0!=share){
                    cash=closePrice[i+1]*share*(1-serviceCharge);
                    share=0;
                    Trade trade =trades.get(trades.size()-1);
                    trade.setSellDate(allIndexDatas.get(i+1).getDate());
                    trade.setSellClosePoint((float) closePrice[i+1]);

                    double rate = cash / initCash;
                    trade.setRate((float) rate);
                    if(trade.getSellClosePoint()-trade.getBuyClosePoint()>0) {
                        totalWinRate +=(trade.getSellClosePoint()-trade.getBuyClosePoint())/trade.getBuyClosePoint();
                        winCount++;
                    }

                    else {
                        totalLossRate +=(trade.getSellClosePoint()-trade.getBuyClosePoint())/trade.getBuyClosePoint();
                        lossCount ++;
                    }
                }
            }else{

            }
            if(share!=0) {
                value = closePrice[i+1] * share;
            }
            else {
                value = cash;
            }
            double rate = value/initCash;
            Profit profit = new Profit();
            profit.setDate(allIndexDatas.get(i+1).getDate());
            profit.setValue((float) (rate*init));

          //  System.out.println("profit.value:" + profit.getValue());
           // System.out.println("profit.getDate:"+profit.getDate());
            profits.add(profit);

        }
        avgWinRate = totalWinRate / winCount;
        avgLossRate = totalLossRate / lossCount;
        List<AnnualProfit> annualProfits = caculateAnnualProfits(allIndexDatas, profits);

        Map<String,Object> map = new HashMap<>();
        map.put("profits", profits);
        map.put("trades", trades);

        map.put("winCount", winCount);
        map.put("lossCount", lossCount);
        map.put("avgWinRate", avgWinRate);
        map.put("avgLossRate", avgLossRate);

        map.put("annualProfits", annualProfits);
        return map;
    }
    public List<IndexData> listIndexData(String code) {
        List<IndexData> result = indexDataClient.getIndexData(code);
        Collections.reverse(result);
//      for (IndexData indexData : result) {
//          System.out.println(indexData.getDate());
//      }
        return result;
    }
    public float getYear(List<IndexData> allIndexDatas) {
        float years;
        String sDateStart = allIndexDatas.get(0).getDate();
        String sDateEnd = allIndexDatas.get(allIndexDatas.size()-1).getDate();

        Date dateStart = DateUtil.parse(sDateStart);
        Date dateEnd = DateUtil.parse(sDateEnd);

        long days = DateUtil.between(dateStart, dateEnd, DateUnit.DAY);
        years = days/365f;
        return years;
    }
    private int getYear(String date) {
        String strYear= StrUtil.subBefore(date, "-", false);
        return Convert.toInt(strYear);
    }
    private float getIndexIncome(int year, List<IndexData> indexDatas) {
        IndexData first=null;
        IndexData last=null;
        for (IndexData indexData : indexDatas) {
            String strDate = indexData.getDate();
//			Date date = DateUtil.parse(strDate);
            int currentYear = getYear(strDate);
            if(currentYear == year) {
                if(null==first)
                    first = indexData;
                last = indexData;
            }
        }
        return (last.getClosePoint() - first.getClosePoint()) / first.getClosePoint();
    }
    private float getTrendIncome(int year, List<Profit> profits) {
        Profit first=null;
        Profit last=null;
        for (Profit profit : profits) {
            String strDate = profit.getDate();
            int currentYear = getYear(strDate);
            if(currentYear == year) {
                if(null==first)
                    first = profit;
                last = profit;
            }
            if(currentYear > year)
                break;
        }
        return (last.getValue() - first.getValue()) / first.getValue();
    }
    private List<AnnualProfit> caculateAnnualProfits(List<IndexData> indexDatas, List<Profit> profits) {
        List<AnnualProfit> result = new ArrayList<>();
        String strStartDate = indexDatas.get(0).getDate();
        String strEndDate = indexDatas.get(indexDatas.size()-1).getDate();
        Date startDate = DateUtil.parse(strStartDate);
        Date endDate = DateUtil.parse(strEndDate);
        int startYear = DateUtil.year(startDate);
        int endYear = DateUtil.year(endDate);
        for (int year =startYear; year <= endYear; year++) {
            AnnualProfit annualProfit = new AnnualProfit();
            annualProfit.setYear(year);
            float indexIncome = getIndexIncome(year,indexDatas);
            float trendIncome = getTrendIncome(year,profits);
            annualProfit.setIndexIncome(indexIncome);
            annualProfit.setTrendIncome(trendIncome);
            result.add(annualProfit);
        }
        return result;
    }
}
