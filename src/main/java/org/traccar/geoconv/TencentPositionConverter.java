package org.traccar.geoconv;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Coordinate Converter of Tencent Location Service
 * @see <a href="https://lbs.qq.com/service/webService/webServiceGuide/webServiceTranslate">Tencent Location Service - WebService API: Coordinate Conversion</a>
 * @see <a href="https://lbs.qq.com/faq/serverFaq/webServiceKey">Tencent Location Service - How to caculate sign</a>
 */
public class TencentPositionConverter extends PositionConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TencentPositionConverter.class);

    private static final String API_URL = "https://apis.map.qq.com/ws/coord/v1/translate";

    private static final int QPS = 5;
    private static final int MAX_POSITION_COUNT = 100;
    private static final int DAY_LIMIT = 8000;

    @Inject
    public TencentPositionConverter(Storage storage, Config config, Client client) {
        super(storage, ConvertedPosition.PLATFORM_TENCENT, ConvertedPosition.CRS_GCJ_02, client,
                config.getString(Keys.API_TENCENT_KEY), config.getString(Keys.API_AUTONAVI_SECRET));
    }

    @Override
    protected String positionToString(String latitude, String longitude) {
        return latitude + "," + longitude;
    }

    @Override
    protected void setRequestParams(List<Position> positions, Map<String, Object> params) {
        params.put("key", apiKey);
        params.put("locations", positionsToQueryParam(positions));
        params.put("type", 1);
        params.put("output", "json");
    }

    @Override
    protected Invocation.Builder createRequest(SignedRequestProvider request) {
        return request.request("sig");
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

    @Override
    protected List<ConvertedPosition> parseConvertedPosition(JsonObject response) throws GeoConvException {
        if (response.getInt("status", -1) != 0) {
            throw new GeoConvException(response.getString("message", "Unknown error."));
        }
        try {
            LinkedList<ConvertedPosition> convertedPositions = new LinkedList<>();
            for (JsonObject location : response.getJsonArray("locations").getValuesAs(JsonObject.class)) {
                ConvertedPosition position = new ConvertedPosition(platform, crs);
                position.setLatitude(location.getJsonNumber("lat").doubleValue());
                position.setLongitude(location.getJsonNumber("lng").doubleValue());
                convertedPositions.add(position);
            }
            return convertedPositions;
        } catch (Exception error) {
            throw new GeoConvException("Failed to parse converted position result.", error);
        }
    }
}
