package org.traccar.schedule;

import com.google.inject.Injector;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.LifecycleObject;
import org.traccar.geoconv.PositionConverter;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TaskPositionConvertService implements LifecycleObject {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskPositionConvertService.class);

    private final Injector injector;

    private ScheduledExecutorService executor;

    @Inject
    public TaskPositionConvertService(Injector inject) {
        this.injector = inject;
    }

    @Override
    public void start() throws Exception {
        List<Class<? extends PositionConverter>> converters = PositionConverter.all().toList();
        executor = Executors.newScheduledThreadPool(converters.size(), r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);  // 守护线程
            return t;
        });

        for (Class<? extends PositionConverter> converterClass : converters) {
            PositionConverter converter = injector.getInstance(converterClass);
            if (converter.enable()) {
                converter.schedule(executor);
            }
        }
    }

    @Override
    public void stop() throws Exception {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
}
