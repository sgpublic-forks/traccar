package org.traccar.geoconv;

import com.google.inject.Singleton;
import jakarta.inject.Inject;
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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Coordinate Converter of Amap Open Platform
 * @see <a href="https://lbs.amap.com/api/webservice/guide/api/convert#t4">Coordinate Conversion - WebService API | Amap Open Platform</a>
 * @see <a href="https://lbs.amap.com/faq/quota-key/key/41181">How to caculate sign | Amap Open Platform</a>
 */
@Singleton
public class AutoNaviPositionConverter extends PositionConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoNaviPositionConverter.class);

    private static final String API_URL = "https://restapi.amap.com/v3/assistant/coordinate/convert";

    private static final int QPS = 3;
    private static final int MAX_POSITION_COUNT = 40;
    private static final int DAY_LIMIT = 5000;

    @Inject
    public AutoNaviPositionConverter(Storage storage, Config config, Client client) {
        super(storage, ConvertedPosition.PLATFORM_AUTONAVI, ConvertedPosition.CRS_GCJ_02, client,
                config.getString(Keys.API_AUTONAVI_KEY), config.getString(Keys.API_AUTONAVI_SECRET));
    }

    @Override
    protected String positionsToQueryParam(List<Position> positions) {
        return positionsToQueryParam(positions, "|");
    }

    @Override
    protected void setRequestParams(List<Position> positions, Map<String, Object> params) {
        params.put("key", apiKey);
        params.put("locations", positionsToQueryParam(positions));
        params.put("coordsys", "gps");
        params.put("output", "json");
    }

    @Override
    protected Invocation.Builder createRequest(SignedRequestProvider request) {
        return request.request("sig", false);
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
    public int getMaxRequestPerDay() {
        return DAY_LIMIT;
    }

    @Override
    public int getMaxRequestPerSec() {
        return QPS;
    }

    @Override
    protected List<ConvertedPosition> parseConvertedPosition(JsonObject response) throws GeoConvException {
        if (response.getInt("status", -1) != 0) {
            throw new GeoConvException(response.getString("info", "Unknown error."));
        }
        try {
            LinkedList<ConvertedPosition> convertedPositions = new LinkedList<>();
            for (String location : response.getString("locations").split(";")) {
                ConvertedPosition position = new ConvertedPosition(platform, crs);
                String[] latLng = location.split(",");
                position.setLatitude(Double.parseDouble(latLng[0]));
                position.setLongitude(Double.parseDouble(latLng[1]));
                convertedPositions.add(position);
            }
            return convertedPositions;
        } catch (Exception error) {
            throw new GeoConvException("Failed to parse converted position result.", error);
        }
    }
}