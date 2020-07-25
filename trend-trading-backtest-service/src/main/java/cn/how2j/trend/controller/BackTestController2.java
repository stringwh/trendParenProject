package cn.how2j.trend.controller;

import cn.how2j.trend.pojo.AnnualProfit;
import cn.how2j.trend.pojo.IndexData;
import cn.how2j.trend.pojo.Profit;
import cn.how2j.trend.pojo.Trade;
import cn.how2j.trend.service.BackTestService2;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class BackTestController2 {
    @Autowired
    BackTestService2 backTestService2;

    @GetMapping("/simulate2/{code}/{startDate}/{endDate}/{serviceCharge}")
    @CrossOrigin
    public Map<String, Object> backTest(
            @PathVariable("code") String code
            , @PathVariable("startDate") String strStartDate
            , @PathVariable("endDate") String strEndDate
            ,@PathVariable("serviceCharge") float serviceCharge
    ) throws Exception {

        List<IndexData> allIndexDatas = backTestService2.listIndexData(code);
        String indexStartDate = allIndexDatas.get(0).getDate();
        System.out.println("indexStartDate"+indexStartDate);
        String indexEndDate = allIndexDatas.get(allIndexDatas.size() - 1).getDate();
        System.out.println("indexEndDate"+indexEndDate);
        allIndexDatas = filterByDateRange(allIndexDatas, strStartDate, strEndDate);
        double[] ClosePoint = new double[allIndexDatas.size()];
        double[] macd = new double[allIndexDatas.size()];
        ;
        double[] dea = new double[allIndexDatas.size()];
        ;
        double[] diff = new double[allIndexDatas.size()];
        ;
        for (int i = 0; i < allIndexDatas.size(); i++) {


            ClosePoint[i] = Double.valueOf(allIndexDatas.get(i).getClosePoint());
        }

        System.out.println();
        Map<String, ?> MACDResultj= backTestService2.MACD(serviceCharge,ClosePoint, 12, 26, 9, macd, dea, diff,allIndexDatas);
       // int ma = 20;
       // float sellRate = 0.95f;
       // float buyRate = 1.05f;
        // Map<String,?> simulateResult= backTestService2.simulate(ma,sellRate, buyRate,serviceCharge, allIndexDatas);
        //List<Profit> profits = (List<Profit>) simulateResult.get("profits");
        List<Profit> profits = (List<Profit>) MACDResultj.get("profits");
        List<Trade> trades = (List<Trade>) MACDResultj.get("trades");
        List<AnnualProfit> annualProfits = (List<AnnualProfit>) MACDResultj.get("annualProfits");
        float years = backTestService2.getYear(allIndexDatas);
        float indexIncomeTotal = (allIndexDatas.get(allIndexDatas.size()-1).getClosePoint() - allIndexDatas.get(0).getClosePoint()) / allIndexDatas.get(0).getClosePoint();
        float indexIncomeAnnual = (float) Math.pow(1+indexIncomeTotal, 1/years) - 1;
        float trendIncomeTotal = (profits.get(profits.size()-1).getValue() - profits.get(0).getValue()) / profits.get(0).getValue();
        float trendIncomeAnnual = (float) Math.pow(1+trendIncomeTotal, 1/years) - 1;



        int winCount = (Integer) MACDResultj.get("winCount");
        int lossCount = (Integer) MACDResultj.get("lossCount");
        float avgWinRate = (Float) MACDResultj.get("avgWinRate");
        float avgLossRate = (Float) MACDResultj.get("avgLossRate");

        for(int i=0;i<profits.size();i++){
            System.out.println("profit.value"+profits.get(i).getValue());
            System.out.println("profit.date"+profits.get(i).getDate());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("indexDatas", allIndexDatas);
        result.put("indexStartDate", indexStartDate);
         result.put("indexEndDate", indexEndDate);
        result.put("profits", profits);
        result.put("trades", trades);

        result.put("years", years);
        result.put("indexIncomeTotal", indexIncomeTotal);
        result.put("indexIncomeAnnual", indexIncomeAnnual);
        result.put("trendIncomeTotal", trendIncomeTotal);
        result.put("trendIncomeAnnual", trendIncomeAnnual);


        result.put("winCount", winCount);
        result.put("lossCount", lossCount);
        result.put("avgWinRate", avgWinRate);
        result.put("avgLossRate", avgLossRate);

        result.put("annualProfits", annualProfits);
        return result;
    }

    private List filterByDateRange(List<IndexData> allIndexDatas, String strStartDate, String strEndDate) {
        if (StrUtil.isBlankOrUndefined(strStartDate) || StrUtil.isBlankOrUndefined(strEndDate))
            return allIndexDatas;

        List result = new ArrayList<>();
        Date startDate = DateUtil.parse(strStartDate);
        Date endDate = DateUtil.parse(strEndDate);

        for (IndexData indexData : allIndexDatas) {
            Date date = DateUtil.parse(indexData.getDate());
            if (
                    date.getTime() >= startDate.getTime() &&
                            date.getTime() <= endDate.getTime()
            ) {
                result.add(indexData);
            }
        }
        return result;
    }
}