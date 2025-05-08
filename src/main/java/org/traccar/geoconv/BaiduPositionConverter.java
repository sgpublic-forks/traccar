package org.traccar.geoconv;

import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.Singleton;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.SignedRequestProvider;
import org.traccar.model.ConvertedPosition;
import org.traccar.model.Position;
import org.traccar.storage.Storage;

import java.util.List;
import java.util.Map;

/**
 * Coordinate Converter of Baidu Maps Open Platform
 * @see <a href="https://lbsyun.baidu.com/faq/api?title=webapi/guide/changeposition-base">Coordinate Conversion | Baidu Maps API SDK</a>
 * @see <a href="https://lbsyun.baidu.com/faq/api?title=webapi/appendix#sn%E8%AE%A1%E7%AE%97%E7%AE%97%E6%B3%95e">How to caculate sign | Baidu Maps API SDK</a>
 */
@Singleton
public class BaiduPositionConverter extends PositionConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaiduPositionConverter.class);

    private static final String API_URL = "https://api.map.baidu.com/geoconv/v2/";

    private static final int QPS = 3;
    private static final int MAX_POSITION_COUNT = 30;
    private static final int DAY_LIMIT = 5000;

    @Inject
    public BaiduPositionConverter(Storage storage, Config config, Client client) {
        super(storage, ConvertedPosition.PLATFORM_BAIDU, ConvertedPosition.CRS_BD_09, client,
                config.getString(Keys.API_BAIDU_KEY), config.getString(Keys.API_BAIDU_SECRET));
    }

    @Override
    protected void setRequestParams(List<Position> positions, Map<String, Object> params) {
        params.put("coords", positionsToQueryParam(positions));
        params.put("ak", super.apiKey);
        params.put("model", 2);
        params.put("output", "json");
    }

    @Override
    protected Invocation.Builder createRequest(SignedRequestProvider request) {
        return request.request("sn");
    }

    @Override
    protected RateLimiter createRateLimiter() {
        return RateLimiter.create(QPS);
    }

    @Override
    protected SignedRequestProvider createSignedRequestProvider(String secretKey, Client client) {
        return new SignedRequestProvider(secretKey, client, API_URL);
    }

    @Override
    public int getMaxPositionPerRequest() {
        return MAX_POSITION_COUNT;
    }
}