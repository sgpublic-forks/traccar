package org.traccar.geoconv;

import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.handler.BasePositionHandler;
import org.traccar.helper.CoordinateUtil;
import org.traccar.helper.SignedRequestProvider;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.ConvertedPosition;
import org.traccar.model.Position;
import org.traccar.schedule.ScheduleTask;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class PositionConverter extends BasePositionHandler implements ScheduleTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(PositionConverter.class);

    private final Storage storage;
    private final Client client;
    public final String platform;
    public final String crs;
    protected final String apiKey;
    private final String secretKey;

    private final BlockingBucket rateLimiter;

    protected PositionConverter(Storage storage, String platform, String crs, Client client, @Nullable String apiKey, @Nullable String secretKey) {
        this.storage = storage;
        this.platform = platform;
        this.crs = crs;
        this.client = client;
        this.apiKey = apiKey;
        this.secretKey = secretKey;

        this.rateLimiter = createRateLimiter();
    }

    protected String positionsToQueryParam(List<Position> positions) {
        return positionsToQueryParam(positions, ";");
    }

    protected String positionsToQueryParam(List<Position> positions, String delimiter) {
        StringJoiner joiner = new StringJoiner(delimiter);
        for (Position position : positions) {
            joiner.add(positionToString(
                    String.format("%.6f", position.getLatitude()),
                    String.format("%.6f", position.getLongitude())
            ));
        }
        return joiner.toString();
    }

    protected String positionToString(String latitude, String longitude) {
        return longitude + "," + latitude;
    }

    private BlockingBucket createRateLimiter() {
        return Bucket.builder()
                .addLimit(limit -> {
                    int qpd = getMaxRequestPerDay();
                    return limit.capacity(qpd).refillIntervally(qpd, Duration.ofDays(1));
                })
                .addLimit(limit -> {
                    int qps = getMaxRequestPerSec();
                    return limit.capacity(qps).refillGreedy(qps, Duration.ofSeconds(1));
                })
                .build()
                .asBlocking();
    }

    protected abstract SignedRequestProvider createSignedRequestProvider(String secretKey, Client client);

    protected abstract void setRequestParams(List<Position> positions, Map<String, Object> params);

    protected abstract Invocation.Builder createRequest(SignedRequestProvider request);

    public abstract int getMaxPositionPerRequest();

    public abstract int getMaxRequestPerSec();

    public abstract int getMaxRequestPerDay();

    private List<ConvertedPosition> getConvertedPosition(@Nonnull List<Position> positions) throws GeoConvException {
        SignedRequestProvider request = createSignedRequestProvider(secretKey, client);
        setRequestParams(positions, request);
        Response resp = createRequest(request).get();
        List<ConvertedPosition> result = parseConvertedPosition(resp.readEntity(JsonObject.class));
        for (int i = 0; i < positions.size(); i++) {
            result.get(i).setId(positions.get(i).getId());
        }
        return result;
    }

    protected abstract List<ConvertedPosition> parseConvertedPosition(JsonObject response) throws GeoConvException;

    @Override
    public void run() {
        while (true) {
            List<Position> requestPositions;
            try {
                rateLimiter.consume(1);
                synchronized (this) {
                    requestPositions = PositionUtil.getLatestUnconvertedPositions(storage, platform, getMaxPositionPerRequest());
                    if (requestPositions.isEmpty()) {
                        this.wait(30_000);
                        continue;
                    }
                }
            } catch (InterruptedException ignore) {
                break;
            } catch (Exception error) {
                LOGGER.debug("Failed to get non-converted position from database", error);
                continue;
            }
            List<ConvertedPosition> convertedPositions;
            try {
                convertedPositions = getConvertedPosition(requestPositions);
            } catch (Exception error) {
                LOGGER.warn("Failed to request converted position", error);
                continue;
            }
            for (ConvertedPosition position : convertedPositions) {
                save(position);
            }
        }
    }

    public boolean enable() {
        return apiKey != null;
    }

    @Override
    public boolean multipleInstances() {
        return false;
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.schedule(this, 5, TimeUnit.SECONDS);
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        if (!enable()) {
            callback.processed(true);
            return;
        }
        if (CoordinateUtil.outOfChina(position.getLatitude(), position.getLongitude())) {
            callback.processed(true);
            return;
        }
        this.notify();
        callback.processed(false);
    }

    private void save(ConvertedPosition convertedPosition) {
        try {
            storage.addObject(convertedPosition, new Request(new Columns.All()));
        } catch (Exception error) {
            LOGGER.warn("Failed to store converted position, id: {}", convertedPosition.getId(), error);
        }
    }

    public static Stream<Class<? extends PositionConverter>> all() {
        return Stream.of(
                AutoNaviPositionConverter.class,
                BaiduPositionConverter.class,
                TencentPositionConverter.class
        );
    }
}
