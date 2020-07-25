package cn.how2j.trend.service;

import cn.how2j.trend.client.IndexDataClient;
import cn.how2j.trend.pojo.AnnualProfit;
import cn.how2j.trend.pojo.IndexData;
import cn.how2j.trend.pojo.Profit;
import cn.how2j.trend.pojo.Trade;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BackTestService {
    @Autowired
    IndexDataClient indexDataClient;

    public List<IndexData> listIndexData(String code) {
        List<IndexData> result = indexDataClient.getIndexData(code);
        Collections.reverse(result);

//      for (IndexData indexData : result) {
//          System.out.println(indexData.getDate());
//      }

        return result;
    }

    public Map<String, Object> simulate(int ma, float sellRate, float buyRate, float serviceCharge, List<IndexData> indexDatas) {
//Ma 移动均线 sellRate出售阈值 buyRate购买阈值 serviceCharge交易费用
// List<IndexData> indexDatas传入的数据可以是所有数据也可以某一个数据

        List<Profit> profits = new ArrayList<>();//利润集合
        List<Trade> trades = new ArrayList<>();//交易信息
        float initCash = 1000;//初始化金钱
        float cash = initCash;//本金
        float share = 0;//份额
        float value = 0;//最后计算的价值

        int winCount = 0;//赢的次数
        float totalWinRate = 0;//总共赢的比率
        float avgWinRate = 0;//平均赢得比率 （totalWinRate/winCount）
        float totalLossRate = 0;
        int lossCount = 0;//输得次数
        float avgLossRate = 0;//平均输得比率

        float init = 0;
        if (!indexDatas.isEmpty())//初始值调整曲线最开始初始值相同
            init = indexDatas.get(0).getClosePoint();

        for (int i = 0; i < indexDatas.size(); i++) {//遍历指数收益
            IndexData indexData = indexDatas.get(i);//取出数据
            float closePoint = indexData.getClosePoint();//取出收盘点 （500）
            float avg = getMA(i, ma, indexDatas);//均线 （400）
            float max = getMax(i, ma, indexDatas);//往前涨的最大值 （450）

            float increase_rate = closePoint / avg;//增长率 500/400=1.2
            float decrease_rate = closePoint / max;//亏损率 500/450=1.1

            if (avg != 0) {
                //buy 超过了均线
                if (increase_rate > buyRate) {
                    //如果没买
                    if (0 == share) {
                        share = cash / closePoint;
                        cash = 0;
                        Trade trade = new Trade();
                        trade.setBuyDate(indexData.getDate());//购买日期
                        trade.setBuyClosePoint(indexData.getClosePoint());//购买时的收盘点
                        trade.setSellDate("n/a");
                        trade.setSellClosePoint(0);
                        trades.add(trade);
                    }
                }
                //sell 低于了卖点
                else if (decrease_rate < sellRate) {
                    //如果没卖
                    if (0 != share) {
                        cash = closePoint * share * (1 - serviceCharge);
                        share = 0;
                        Trade trade =trades.get(trades.size()-1);
                        trade.setSellDate(indexData.getDate());
                        trade.setSellClosePoint(indexData.getClosePoint());
                        float rate = cash / initCash;//收益比率 现金/最开始现金
                        trade.setRate(rate);
                        if(trade.getSellClosePoint()-trade.getBuyClosePoint()>0) {//出售收盘点>购买的 就是赚了 ==0是亏因为有费用
                            totalWinRate +=(trade.getSellClosePoint()-trade.getBuyClosePoint())/trade.getBuyClosePoint();
                            winCount++;
                        }

                        else {
                            totalLossRate +=(trade.getSellClosePoint()-trade.getBuyClosePoint())/trade.getBuyClosePoint();
                            lossCount ++;
                        }
                    }
                }
                //do nothing
                else {

                }
            }

            if (share != 0) {
                value = closePoint * share;
            } else {
                value = cash;
            }
            float rate = value / initCash;

            Profit profit = new Profit();
            profit.setDate(indexData.getDate());
            profit.setValue(rate * init);

            System.out.println("profit.value:" + profit.getValue());
            profits.add(profit);

        }
        avgWinRate = totalWinRate / winCount;
        avgLossRate = totalLossRate / lossCount;
        List<AnnualProfit> annualProfits = caculateAnnualProfits(indexDatas, profits);
        Map<String, Object> map = new HashMap<>();
        map.put("profits", profits);
        map.put("trades", trades);

        map.put("winCount", winCount);
        map.put("lossCount", lossCount);
        map.put("avgWinRate", avgWinRate);
        map.put("avgLossRate", avgLossRate);

        map.put("annualProfits", annualProfits);

        return map;
    }

    private static float getMax(int i, int day, List<IndexData> list) {
        int start = i - 1 - day;//往前推的天数
        if (start < 0)
            start = 0;
        int now = i - 1;

        if (start < 0)
            return 0;

        float max = 0;
        for (int j = start; j < now; j++) {
            IndexData bean = list.get(j);
            if (bean.getClosePoint() > max) {
                max = bean.getClosePoint();
            }
        }
        return max;
    }

    private static float getMA(int i, int ma, List<IndexData> list) {
        int start = i - 1 - ma;
        int now = i - 1;

        if (start < 0)
            return 0;

        float sum = 0;
        float avg = 0;
        for (int j = start; j < now; j++) {
            IndexData bean = list.get(j);
            sum += bean.getClosePoint();
        }
        avg = sum / (now - start);
        return avg;
    }
    public float getYear(List<IndexData> allIndexDatas) {
        float years;
        String sDateStart = allIndexDatas.get(0).getDate();
        String sDateEnd = allIndexDatas.get(allIndexDatas.size()-1).getDate();
        Date dateStart = DateUtil.parse(sDateStart);
        Date dateEnd = DateUtil.parse(sDateEnd);
        long days = DateUtil.between(dateStart, dateEnd, DateUnit.DAY);//间隔多少天
        years = days/365f;
        return years;
    }
    // 计算完整时间范围内，每一年的指数投资收益和趋势投资收益
    private List<AnnualProfit> caculateAnnualProfits(List<IndexData> indexDatas, List<Profit> profits) {
        List<AnnualProfit> result = new ArrayList<>();
        String strStartDate = indexDatas.get(0).getDate();
        String strEndDate = indexDatas.get(indexDatas.size()-1).getDate();
        Date startDate = DateUtil.parse(strStartDate);
        Date endDate = DateUtil.parse(strEndDate);
        //计算出开始日期和结束日期得年份
        int startYear = DateUtil.year(startDate);
        int endYear = DateUtil.year(endDate);
        //遍历每一年存入集合
        for (int year =startYear; year <= endYear; year++) {
            AnnualProfit annualProfit = new AnnualProfit();
            annualProfit.setYear(year);
            float indexIncome = getIndexIncome(year,indexDatas);//获取这一年指数收益
            float trendIncome = getTrendIncome(year,profits);//获取这一年趋势收益
            annualProfit.setIndexIncome(indexIncome);
            annualProfit.setTrendIncome(trendIncome);
            result.add(annualProfit);
        }
        return result;
    }
    //计算某一年指数收益
    private float getIndexIncome(int year, List<IndexData> indexDatas) {
        IndexData first=null;//指数收益 第一天
        IndexData last=null;// 最后一天

        //遍历出来时间  因为一年不一定是从1月1号开始得
        for (IndexData indexData : indexDatas) {
            String strDate = indexData.getDate();
//			Date date = DateUtil.parse(strDate);
            int currentYear = getYear(strDate);//把年份取出来
            if(currentYear == year) { //和对应得进行对比 如果遍历年还等于对应得 就进行开始和结束日期得赋值
                if(null==first)//第一个就是第一天的
                    first = indexData;
                last = indexData;//最后遍历结束得数据就是最后一天
            }
        }
        return (last.getClosePoint() - first.getClosePoint()) / first.getClosePoint();//收益增长率
    }
    //计算某一年得趋势收益
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
    //获取日期中得年份  比如 2019-10-10 取 “-”前得2019
    private int getYear(String date) {
        String strYear= StrUtil.subBefore(date, "-", false);
        return Convert.toInt(strYear);
    }
}