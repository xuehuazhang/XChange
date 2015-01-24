package com.xeiam.xchange.hitbtc.service.polling;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import si.mazi.rescu.SynchronizedValueFactory;

import com.xeiam.xchange.ExchangeException;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.NotAvailableFromExchangeException;
import com.xeiam.xchange.NotYetImplementedForExchangeException;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.dto.trade.MarketOrder;
import com.xeiam.xchange.hitbtc.HitbtcAdapters;
import com.xeiam.xchange.hitbtc.HitbtcAuthenticated;
import com.xeiam.xchange.hitbtc.dto.HitbtcException;
import com.xeiam.xchange.hitbtc.dto.marketdata.HitbtcTradeMetaData;
import com.xeiam.xchange.hitbtc.dto.trade.HitbtcExecutionReport;
import com.xeiam.xchange.hitbtc.dto.trade.HitbtcExecutionReportResponse;
import com.xeiam.xchange.hitbtc.dto.trade.HitbtcOrder;
import com.xeiam.xchange.hitbtc.dto.trade.HitbtcOrdersResponse;
import com.xeiam.xchange.hitbtc.dto.trade.HitbtcOwnTrade;
import com.xeiam.xchange.hitbtc.dto.trade.HitbtcTradeResponse;

public class HitbtcTradeServiceRaw extends HitbtcBasePollingService<HitbtcAuthenticated> {

  protected Map<CurrencyPair, HitbtcTradeMetaData> metadata;

  public HitbtcTradeServiceRaw(ExchangeSpecification exchangeSpecification, SynchronizedValueFactory<Long> nonceFactory) {

    super(HitbtcAuthenticated.class, exchangeSpecification, nonceFactory);
  }

  public HitbtcOrdersResponse getOpenOrdersRawBaseResponse() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    try {
      HitbtcOrdersResponse hitbtcActiveOrders = hitbtc.getHitbtcActiveOrders(signatureCreator, valueFactory, apiKey);
      return hitbtcActiveOrders;
    } catch (HitbtcException e) {
      throw handleException(e);
    }
  }

  public HitbtcOrder[] getOpenOrdersRaw() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    HitbtcOrdersResponse hitbtcActiveOrders = getOpenOrdersRawBaseResponse();
    return hitbtcActiveOrders.getOrders();
  }

  public HitbtcOrdersResponse getRecentOrdersRawBaseResponse(int max_results) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    try {
      HitbtcOrdersResponse hitbtcActiveOrders = hitbtc.getHitbtcRecentOrders(signatureCreator, valueFactory, apiKey, max_results);
      return hitbtcActiveOrders;
    } catch (HitbtcException e) {
      throw handleException(e);
    }
  }

  public HitbtcOrder[] getRecentOrdersRaw(int max_results) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    HitbtcOrdersResponse hitbtcActiveOrders = getRecentOrdersRawBaseResponse(max_results);
    return hitbtcActiveOrders.getOrders();
  }

  public HitbtcExecutionReportResponse placeMarketOrderRawBaseResponse(MarketOrder marketOrder) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
  IOException {

    String symbol = marketOrder.getCurrencyPair().baseSymbol + marketOrder.getCurrencyPair().counterSymbol;

    long nonce = valueFactory.createValue();
    String side = HitbtcAdapters.getSide(marketOrder.getType());
    String orderId = HitbtcAdapters.createOrderId(marketOrder, nonce);

    try {
      HitbtcExecutionReportResponse response = hitbtc.postHitbtcNewOrder(signatureCreator, valueFactory, apiKey, orderId, symbol, side, null, getLots(marketOrder), "market", "IOC");

      return response;
    } catch (HitbtcException e) {
      throw handleException(e);
    }
  }

  public HitbtcExecutionReport placeMarketOrderRaw(MarketOrder marketOrder) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    HitbtcExecutionReportResponse response = placeMarketOrderRawBaseResponse(marketOrder);
    return response.getExecutionReport();
  }

  public HitbtcExecutionReport placeLimitOrderRaw(LimitOrder limitOrder) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    try {
      HitbtcExecutionReportResponse postHitbtcNewOrder = fillHitbtcExecutionReportResponse(limitOrder);
      return postHitbtcNewOrder.getExecutionReport();
    } catch (HitbtcException e) {
      throw handleException(e);
    }
  }

  public HitbtcExecutionReportResponse placeLimitOrderRawReturningHitbtcExecutionReportResponse(LimitOrder limitOrder) throws ExchangeException, NotAvailableFromExchangeException,
  NotYetImplementedForExchangeException, IOException {

    HitbtcExecutionReportResponse postHitbtcNewOrder = fillHitbtcExecutionReportResponse(limitOrder);
    return postHitbtcNewOrder;
  }

  private HitbtcExecutionReportResponse fillHitbtcExecutionReportResponse(LimitOrder limitOrder) throws IOException {

    String symbol = HitbtcAdapters.adaptCurrencyPair(limitOrder.getCurrencyPair());
    long nonce = valueFactory.createValue();
    String side = HitbtcAdapters.getSide(limitOrder.getType());
    String orderId = HitbtcAdapters.createOrderId(limitOrder, nonce);

    try {
      return hitbtc.postHitbtcNewOrder(signatureCreator, valueFactory, apiKey, orderId, symbol, side, limitOrder.getLimitPrice(), getLots(limitOrder), "limit", "GTC");
    } catch (HitbtcException e) {
      throw handleException(e);
    }
  }

  public HitbtcExecutionReportResponse cancelOrderRaw(String orderId) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    // extract symbol and side from original order id: buy/sell
    String originalSide = HitbtcAdapters.getSide(HitbtcAdapters.readOrderType(orderId));
    String symbol = HitbtcAdapters.readSymbol(orderId);

    try {
      return hitbtc.postHitbtcCancelOrder(signatureCreator, valueFactory, apiKey, orderId, orderId, symbol, originalSide);
    } catch (HitbtcException e) {
      throw handleException(e);
    }
  }

  public HitbtcExecutionReportResponse cancelOrderRaw(String clientOrderId, String cancelRequestClientOrderId, String symbol, String side) throws ExchangeException, NotAvailableFromExchangeException,
  NotYetImplementedForExchangeException, IOException {

    try {
      return hitbtc.postHitbtcCancelOrder(signatureCreator, valueFactory, apiKey, clientOrderId, cancelRequestClientOrderId, symbol, side);
    } catch (HitbtcException e) {
      throw handleException(e);
    }
  }

  public HitbtcTradeResponse getTradeHistoryRawBaseResponse(int startIndex, int maxResults, String symbols) throws ExchangeException, NotAvailableFromExchangeException,
  NotYetImplementedForExchangeException, IOException {

    try {
      HitbtcTradeResponse hitbtcTrades = hitbtc.getHitbtcTrades(signatureCreator, valueFactory, apiKey, "ts", startIndex, maxResults, symbols, "desc", null, null);
      return hitbtcTrades;
    } catch (HitbtcException e) {
      throw handleException(e);
    }
  }

  public HitbtcOwnTrade[] getTradeHistoryRaw(int startIndex, int maxResults, String symbols) throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
  IOException {

    HitbtcTradeResponse hitbtcTrades = getTradeHistoryRawBaseResponse(startIndex, maxResults, symbols);
    return hitbtcTrades.getTrades();
  }

  /**
   * Represent tradableAmount in lots
   *
   * @throws java.lang.IllegalArgumentException if the result were to be less
   *           than lot size for given currency pair
   */
  protected BigInteger getLots(Order order) {

    CurrencyPair pair = order.getCurrencyPair();
    BigDecimal lotDivisor = metadata.get(pair).getAmountMinimum();

    BigDecimal lots = order.getTradableAmount().divide(lotDivisor, BigDecimal.ROUND_UNNECESSARY);
    if (lots.compareTo(BigDecimal.ONE) < 0) {
      throw new IllegalArgumentException("Tradable amount too low");
    }
    return lots.toBigIntegerExact();
  }

  /**
   * Fetch the {@link com.com.xeiam.xchange.service.polling.trade.TradeMetaData}
   * from the exchange.
   *
   * @return Map of currency pairs to their corresponding metadata.
   * @see com.com.xeiam.xchange.service.polling.trade.TradeMetaData
   */
  public Map<CurrencyPair, HitbtcTradeMetaData> getTradeMetaDataMap() throws IOException {
    metadata = HitbtcAdapters.adaptSymbolsToTradeMetadataMap(hitbtc.getSymbols());
    return metadata;
  }

}
